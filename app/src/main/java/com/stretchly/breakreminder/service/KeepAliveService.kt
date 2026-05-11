package com.stretchly.breakreminder.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.stretchly.breakreminder.util.KeepAliveManager
import com.stretchly.breakreminder.util.NotificationHelper
import com.stretchly.breakreminder.util.PreferencesManager

/**
 * 保活守护服务
 *
 * 运行在独立进程(:keepalive)中，与主服务互为守护：
 * - 定期检查主服务是否存活
 * - 若主服务已停止，自动重启
 *
 * 双进程守护原理：
 * 两个服务运行在不同进程中，当一个进程被杀死时，
 * 另一个进程仍然存活，可以检测到并重启被杀的服务。
 *
 * 注意：此服务运行在:keepalive独立进程中，
 * 与主进程不共享内存，需要通过Intent通信。
 */
class KeepAliveService : Service() {

    companion object {
        const val ACTION_START = "action_keepalive_start"
        const val ACTION_STOP = "action_keepalive_stop"

        // 检查间隔（毫秒），每60秒检查一次主服务
        private const val CHECK_INTERVAL = 60 * 1000L
    }

    // 通知工具
    private lateinit var notificationHelper: NotificationHelper
    // 偏好设置管理器
    private lateinit var prefsManager: PreferencesManager

    // 检查线程是否运行
    private var isChecking = false

    // 检查线程
    private var checkThread: Thread? = null

    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelper(this)
        prefsManager = PreferencesManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_STICKY

        when (intent.action) {
            ACTION_START -> startKeepAlive()
            ACTION_STOP -> stopKeepAlive()
        }

        // 服务被杀后自动重启
        return START_STICKY
    }

    /**
     * 启动守护服务
     * 显示前台通知并开始定期检查
     */
    private fun startKeepAlive() {
        // 显示前台通知（独立进程也需要自己的前台通知）
        val notification = notificationHelper.buildForegroundNotification(
            "休息提醒守护服务运行中"
        )
        startForeground(NotificationHelper.FOREGROUND_NOTIFICATION_ID + 1, notification)

        // 启动检查线程
        if (!isChecking) {
            isChecking = true
            checkThread = Thread {
                while (isChecking) {
                    try {
                        // 检查主服务是否需要重启
                        checkAndRestartMainService()
                        // 等待下一次检查
                        Thread.sleep(CHECK_INTERVAL)
                    } catch (e: InterruptedException) {
                        // 线程被中断，退出循环
                        break
                    }
                }
            }.also { it.start() }
        }
    }

    /**
     * 停止守护服务
     */
    private fun stopKeepAlive() {
        isChecking = false
        checkThread?.interrupt()
        checkThread = null

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * 检查主服务状态并重启
     *
     * 判断逻辑：
     * 如果用户设置了服务启用（isServiceEnabled=true），
     * 但主服务未运行，则重启主服务
     */
    private fun checkAndRestartMainService() {
        val shouldRun = prefsManager.isServiceEnabled()
        if (shouldRun) {
            // 用户期望服务运行，检查是否真的在运行
            // 这里通过尝试启动来确保服务存活
            KeepAliveManager.startForegroundService(this)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isChecking = false
        checkThread?.interrupt()
    }
}
