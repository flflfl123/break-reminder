package com.stretchly.breakreminder

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.stretchly.breakreminder.service.BreakReminderService
import com.stretchly.breakreminder.util.NotificationHelper
import com.stretchly.breakreminder.util.PreferencesManager
import java.util.Locale

/**
 * 休息提醒全屏覆盖界面
 *
 * 参考Stretchly的全屏休息提醒设计：
 * - 全屏覆盖，无法忽略
 * - 显示休息类型（小休息/长休息）
 * - 倒计时显示剩余时间
 * - 显示放松建议文字
 * - 严格模式下不可跳过
 * - 非严格模式下可跳过
 *
 * 界面特点：
 * - 小休息：绿色背景，简短放松建议
 * - 长休息：蓝色背景，详细休息指导
 * - 全屏沉浸式体验
 */
class BreakOverlayActivity : AppCompatActivity() {

    companion object {
        // Intent中携带的休息类型Key
        const val EXTRA_IS_LONG_BREAK = "is_long_break"
    }

    // ==================== 放松建议文字资源 ====================

    // 小休息放松建议
    private val miniBreakIdeas = listOf(
        "站起来伸展一下身体",
        "转动你的脖子和肩膀",
        "闭上眼睛深呼吸3次",
        "看看窗外的远处",
        "活动一下手腕和手指",
        "喝一口水",
        "放松你的面部肌肉",
        "轻轻按摩太阳穴"
    )

    // 长休息放松建议
    private val longBreakIdeas = listOf(
        "离开屏幕，走动5分钟",
        "做一些简单的拉伸运动",
        "去倒杯水或泡杯茶",
        "和同事/家人聊聊天",
        "做几个深蹲或俯卧撑",
        "到窗边看看远处的风景",
        "闭眼冥想几分钟",
        "整理一下桌面"
    )

    // 偏好设置管理器
    private lateinit var prefsManager: PreferencesManager
    // 通知工具
    private lateinit var notificationHelper: NotificationHelper

    // 倒计时器
    private var countDownTimer: CountDownTimer? = null

    // 是否为长休息
    private var isLongBreak = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 设置全屏沉浸式显示
        setupFullScreen()

        setContentView(R.layout.activity_break_overlay)

        // 初始化工具类
        prefsManager = PreferencesManager(this)
        notificationHelper = NotificationHelper(this)

        // 获取休息类型
        isLongBreak = intent.getBooleanExtra(EXTRA_IS_LONG_BREAK, false)

        // 初始化界面
        setupUI()

        // 开始倒计时
        startCountDown()
    }

    /**
     * 设置全屏沉浸式显示
     * 隐藏状态栏和导航栏，覆盖整个屏幕
     */
    private fun setupFullScreen() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Android 11+使用新的沉浸模式API
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { controller ->
                controller.hide(
                    android.view.WindowInsets.Type.statusBars() or
                    android.view.WindowInsets.Type.navigationBars()
                )
                controller.systemBarsBehavior =
                    android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // 旧版本使用flag方式
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            window.decorView.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }

        // 在锁屏上也显示
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    /**
     * 初始化界面元素
     */
    private fun setupUI() {
        val config = prefsManager.getConfig()

        // 设置背景颜色
        val bgColor = if (isLongBreak) {
            android.graphics.Color.parseColor(config.longBreakColor)
        } else {
            android.graphics.Color.parseColor(config.miniBreakColor)
        }
        window.decorView.setBackgroundColor(bgColor)

        // 设置休息类型标题
        val tvTitle = findViewById<TextView>(R.id.tvBreakTitle)
        tvTitle.text = if (isLongBreak) "长休息时间" else "小休息时间"

        // 设置放松建议
        val tvIdea = findViewById<TextView>(R.id.tvBreakIdea)
        if (config.showBreakIdeas) {
            val ideas = if (isLongBreak) longBreakIdeas else miniBreakIdeas
            tvIdea.text = ideas.random()
            tvIdea.visibility = android.view.View.VISIBLE
        } else {
            tvIdea.visibility = android.view.View.GONE
        }

        // 设置跳过按钮
        val btnSkip = findViewById<android.widget.Button>(R.id.btnSkipBreak)
        if (config.strictMode) {
            // 严格模式下隐藏跳过按钮
            btnSkip.visibility = android.view.View.GONE
        } else {
            btnSkip.visibility = android.view.View.VISIBLE
            btnSkip.setOnClickListener {
                skipBreak()
            }
        }
    }

    /**
     * 开始倒计时
     * 根据休息类型显示不同的倒计时时长
     */
    private fun startCountDown() {
        val config = prefsManager.getConfig()
        val duration = if (isLongBreak) config.longBreakDuration else config.miniBreakDuration

        val tvCountdown = findViewById<TextView>(R.id.tvCountdown)

        countDownTimer = object : CountDownTimer(duration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // 格式化剩余时间
                val seconds = millisUntilFinished / 1000
                val minutes = seconds / 60
                val remainingSeconds = seconds % 60

                val timeText = if (minutes > 0) {
                    String.format(Locale.getDefault(), "%d:%02d", minutes, remainingSeconds)
                } else {
                    String.format(Locale.getDefault(), "%d秒", remainingSeconds)
                }
                tvCountdown.text = timeText
            }

            override fun onFinish() {
                // 倒计时结束，休息完成
                tvCountdown.text = "休息结束"
                finishBreak()
            }
        }.start()
    }

    /**
     * 跳过当前休息
     * 通知服务跳过并调度下一次休息
     */
    private fun skipBreak() {
        // 向服务发送跳过指令
        val intent = Intent(this, BreakReminderService::class.java).apply {
            action = BreakReminderService.ACTION_SKIP_BREAK
        }
        startService(intent)

        // 关闭覆盖界面
        finishBreak()
    }

    /**
     * 结束休息，关闭覆盖界面
     */
    private fun finishBreak() {
        countDownTimer?.cancel()
        notificationHelper.cancelBreakNotification()
        finish()
    }

    /**
     * 禁用返回键，防止用户通过返回键跳过休息
     * 严格模式下尤为重要
     */
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        val config = prefsManager.getConfig()
        if (config.strictMode) {
            // 严格模式下禁用返回键
            return
        }
        // 非严格模式允许返回键跳过
        super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}
