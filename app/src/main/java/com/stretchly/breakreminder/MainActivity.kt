package com.stretchly.breakreminder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.stretchly.breakreminder.databinding.ActivityMainBinding
import com.stretchly.breakreminder.util.KeepAliveManager
import com.stretchly.breakreminder.util.PreferencesManager

/**
 * 主界面Activity
 *
 * 应用的入口界面，参考Stretchly的托盘图标功能设计：
 * - 显示当前服务状态（运行中/已停止）
 * - 显示下次休息倒计时
 * - 启动/停止休息提醒服务
 * - 快速跳转到设置页面
 * - 引导用户开启自启动权限（厂商适配）
 *
 * 界面布局：简约清新风格
 * - 顶部：应用Logo和名称
 * - 中部：状态显示区域
 * - 底部：操作按钮区域
 */
class MainActivity : AppCompatActivity() {

    // 通知权限请求码
    private companion object {
        const val REQUEST_NOTIFICATION_PERMISSION = 1001
    }

    // ViewBinding实例
    private lateinit var binding: ActivityMainBinding
    // 偏好设置管理器
    private lateinit var prefsManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化偏好设置管理器
        prefsManager = PreferencesManager(this)

        // 设置点击事件监听
        setupClickListeners()

        // 请求必要权限
        requestPermissions()
    }

    override fun onResume() {
        super.onResume()
        // 每次回到前台时更新界面状态
        updateUI()
    }

    /**
     * 设置所有按钮的点击事件监听
     */
    private fun setupClickListeners() {
        // 启动/停止服务按钮
        binding.btnToggleService.setOnClickListener {
            toggleService()
        }

        // 设置按钮
        binding.btnSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        // 自启动权限引导按钮
        binding.btnAutoStart.setOnClickListener {
            guideAutoStartPermission()
        }
    }

    /**
     * 切换服务启停状态
     */
    private fun toggleService() {
        val isRunning = prefsManager.isServiceEnabled()

        if (isRunning) {
            // 当前运行中，停止服务
            KeepAliveManager.stopAll(this)
            Toast.makeText(this, "休息提醒已停止", Toast.LENGTH_SHORT).show()
        } else {
            // 当前已停止，启动服务
            // 先检查通知权限
            if (!hasNotificationPermission()) {
                requestPermissions()
                Toast.makeText(this, "请先授予通知权限", Toast.LENGTH_SHORT).show()
                return
            }
            KeepAliveManager.startAll(this)
            Toast.makeText(this, "休息提醒已启动", Toast.LENGTH_SHORT).show()
        }

        // 更新界面
        updateUI()
    }

    /**
     * 更新界面显示
     * 根据服务运行状态更新按钮文字和状态信息
     */
    private fun updateUI() {
        val isRunning = prefsManager.isServiceEnabled()
        val config = prefsManager.getConfig()

        // 更新服务状态文字
        if (isRunning) {
            binding.tvStatus.text = "运行中"
            binding.tvStatus.setTextColor(
                ContextCompat.getColor(this, R.color.status_running)
            )
            binding.btnToggleService.text = "停止提醒"

            // 显示配置信息
            val miniMinutes = config.miniBreakInterval / (60 * 1000)
            val longMinutes = config.longBreakInterval / (60 * 1000)
            binding.tvNextBreakInfo.text =
                "小休息：每${miniMinutes}分钟\n长休息：每${longMinutes}分钟"
        } else {
            binding.tvStatus.text = "已停止"
            binding.tvStatus.setTextColor(
                ContextCompat.getColor(this, R.color.status_stopped)
            )
            binding.btnToggleService.text = "开始提醒"
            binding.tvNextBreakInfo.text = "点击\"开始提醒\"启动服务"
        }

        // 更新自启动引导按钮的可见性
        if (KeepAliveManager.needAutoStartPermission(this)) {
            binding.btnAutoStart.visibility = android.view.View.VISIBLE
        } else {
            binding.btnAutoStart.visibility = android.view.View.GONE
        }
    }

    /**
     * 引导用户前往系统设置开启自启动权限
     */
    private fun guideAutoStartPermission() {
        val intent = KeepAliveManager.getAutoStartIntent(this)
        try {
            startActivity(intent)
            Toast.makeText(
                this,
                "请开启本应用的自启动权限，确保休息提醒不被系统关闭",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            // 如果无法打开厂商设置页面，打开应用详情页
            val fallbackIntent = Intent(
                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            ).apply {
                data = android.net.Uri.parse("package:$packageName")
            }
            startActivity(fallbackIntent)
        }
    }

    // ==================== 权限处理 ====================

    /**
     * 请求必要权限
     * Android 13+需要动态申请通知权限
     */
    private fun requestPermissions() {
        // Android 13+需要通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission()) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION_PERMISSION
                )
            }
        }
    }

    /**
     * 检查是否有通知权限
     */
    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 13以下不需要动态申请
            true
        }
    }

    /**
     * 权限请求结果回调
     * 授权成功后自动启动服务，无需用户再次点击
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 授权成功，自动启动服务
                KeepAliveManager.startAll(this)
                updateUI()
                Toast.makeText(this, "通知权限已授予，休息提醒已启动", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    this,
                    "需要通知权限才能显示休息提醒",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
