package com.stretchly.breakreminder.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.stretchly.breakreminder.BreakOverlayActivity
import com.stretchly.breakreminder.util.BreakScheduler
import com.stretchly.breakreminder.util.NotificationHelper
import com.stretchly.breakreminder.util.PreferencesManager

/**
 * 休息提醒前台服务
 *
 * 这是整个应用的核心服务，负责：
 * 1. 以前台服务形式常驻后台（保活核心）
 * 2. 通过BreakScheduler调度下一次休息
 * 3. 接收闹钟广播后启动休息覆盖界面
 * 4. 维护前台通知显示倒计时信息
 *
 * 前台服务优势：
 * - 系统优先级高，不容易被杀死
 * - 必须显示通知（Android系统要求）
 * - 在Doze模式下也能运行
 */
class BreakReminderService : Service() {

    companion object {
        // 服务启动Action
        const val ACTION_START = "action_start"
        // 服务停止Action
        const val ACTION_STOP = "action_stop"
        // 触发小休息Action
        const val ACTION_TRIGGER_MINI_BREAK = "action_trigger_mini_break"
        // 触发长休息Action
        const val ACTION_TRIGGER_LONG_BREAK = "action_trigger_long_break"
        // 跳过休息Action
        const val ACTION_SKIP_BREAK = "action_skip_break"
    }

    // 偏好设置管理器
    private lateinit var prefsManager: PreferencesManager
    // 通知工具
    private lateinit var notificationHelper: NotificationHelper
    // 休息调度器
    private lateinit var breakScheduler: BreakScheduler

    // 服务是否正在运行
    private var isRunning = false

    override fun onCreate() {
        super.onCreate()
        // 初始化各工具类
        prefsManager = PreferencesManager(this)
        notificationHelper = NotificationHelper(this)
        breakScheduler = BreakScheduler(this)
    }

    /**
     * 服务启动回调
     * 每次startService都会调用
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_STICKY

        when (intent.action) {
            // 启动服务
            ACTION_START -> {
                startBreakService()
            }
            // 停止服务
            ACTION_STOP -> {
                stopBreakService()
            }
            // 触发小休息
            ACTION_TRIGGER_MINI_BREAK -> {
                triggerBreak(isLongBreak = false)
            }
            // 触发长休息
            ACTION_TRIGGER_LONG_BREAK -> {
                triggerBreak(isLongBreak = true)
            }
            // 跳过休息
            ACTION_SKIP_BREAK -> {
                skipBreak()
            }
        }

        // START_STICKY：服务被杀死后自动重启
        // 这是保活的重要一环
        return START_STICKY
    }

    /**
     * 启动休息提醒服务
     * 设置前台通知并调度下一次休息
     */
    private fun startBreakService() {
        if (isRunning) return

        isRunning = true
        prefsManager.setServiceEnabled(true)

        // 显示前台通知（必须在前台服务启动后5秒内调用）
        val config = prefsManager.getConfig()
        val initialText = "休息提醒已启动"
        val notification = notificationHelper.buildForegroundNotification(initialText)
        startForeground(NotificationHelper.FOREGROUND_NOTIFICATION_ID, notification)

        // 调度下一次休息
        val miniBreakCount = prefsManager.getMiniBreakCount()
        breakScheduler.scheduleNextBreak(config, miniBreakCount)

        // 更新通知显示下次休息时间
        updateNotificationWithNextBreakTime(config, miniBreakCount)
    }

    /**
     * 停止休息提醒服务
     * 取消所有闹钟并停止前台服务
     */
    private fun stopBreakService() {
        isRunning = false
        prefsManager.setServiceEnabled(false)

        // 取消所有已调度的闹钟
        breakScheduler.cancelAll()

        // 取消休息提醒通知
        notificationHelper.cancelBreakNotification()

        // 停止前台服务
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * 触发休息提醒
     * 打开全屏休息覆盖界面
     *
     * @param isLongBreak 是否为长休息
     */
    private fun triggerBreak(isLongBreak: Boolean) {
        if (!isRunning) return

        // 记录休息时间
        prefsManager.setLastBreakTime(System.currentTimeMillis())

        // 如果是小休息，增加计数
        if (!isLongBreak) {
            val count = prefsManager.getMiniBreakCount() + 1
            prefsManager.setMiniBreakCount(count)
        }

        // 发送休息提醒通知
        notificationHelper.showBreakNotification(isLongBreak)

        // 打开全屏休息覆盖界面
        val overlayIntent = Intent(this, BreakOverlayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(BreakOverlayActivity.EXTRA_IS_LONG_BREAK, isLongBreak)
        }
        startActivity(overlayIntent)
    }

    /**
     * 跳过当前休息
     * 直接调度下一次休息
     */
    private fun skipBreak() {
        if (!isRunning) return

        val config = prefsManager.getConfig()
        val miniBreakCount = prefsManager.getMiniBreakCount()

        // 取消当前通知
        notificationHelper.cancelBreakNotification()

        // 调度下一次休息
        breakScheduler.scheduleNextBreak(config, miniBreakCount)
        updateNotificationWithNextBreakTime(config, miniBreakCount)
    }

    /**
     * 更新前台通知，显示下次休息的倒计时
     */
    private fun updateNotificationWithNextBreakTime(
        config: com.stretchly.breakreminder.model.BreakConfig,
        miniBreakCount: Int
    ) {
        val shouldLongBreak = config.longBreakIntervalCount > 0 &&
            miniBreakCount > 0 &&
            miniBreakCount % config.longBreakIntervalCount == 0

        val nextInterval = if (shouldLongBreak) {
            config.longBreakInterval
        } else {
            config.miniBreakInterval
        }

        val minutesLeft = nextInterval / (60 * 1000)
        val breakType = if (shouldLongBreak) "长休息" else "小休息"
        val text = "下次$breakType：${minutesLeft}分钟后"

        notificationHelper.updateForegroundNotification(text)
    }

    /**
     * 服务被系统杀死时的回调
     * 返回null，使用START_STICKY自动重启
     */
    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * 服务销毁回调
     * 如果不是用户主动停止，尝试重启服务
     */
    override fun onDestroy() {
        super.onDestroy()
        if (isRunning) {
            // 服务异常终止，尝试重启
            isRunning = false
            prefsManager.setServiceEnabled(false)
        }
    }
}
