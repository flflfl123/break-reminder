package com.stretchly.breakreminder.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.stretchly.breakreminder.service.BreakReminderService
import com.stretchly.breakreminder.service.KeepAliveService
import com.stretchly.breakreminder.worker.KeepAliveWorker
import java.util.concurrent.TimeUnit

/**
 * 保活管理器
 *
 * 综合管理所有保活机制，确保休息提醒服务不被系统杀死：
 *
 * 1. 前台服务（Foreground Service）：最核心的保活手段
 * 2. 双进程守护（KeepAliveService）：独立进程监控主服务
 * 3. WorkManager定时任务：周期性检查服务状态
 * 4. AlarmManager精确闹钟：定时唤醒检查
 * 5. 开机自启（BootReceiver）：设备重启后自动恢复
 *
 * 各厂商适配说明：
 * - 小米：需要引导用户开启自启动权限
 * - 华为：需要引导用户设置受保护应用
 * - OPPO/vivo：需要引导用户开启后台运行权限
 */
object KeepAliveManager {

    // WorkManager定时任务的名称
    private const val WORK_NAME = "keep_alive_work"

    // WorkManager检查间隔（分钟）
    private const val WORK_INTERVAL_MINUTES = 15L

    /**
     * 启动所有保活机制
     *
     * @param context 上下文
     */
    fun startAll(context: Context) {
        startForegroundService(context)
        startKeepAliveService(context)
        scheduleWorkManagerCheck(context)
    }

    /**
     * 停止所有保活机制
     *
     * @param context 上下文
     */
    fun stopAll(context: Context) {
        stopForegroundService(context)
        stopKeepAliveService(context)
        cancelWorkManagerCheck(context)
    }

    // ==================== 前台服务管理 ====================

    /**
     * 启动前台服务（主服务）
     * 前台服务拥有更高的系统优先级，不容易被杀死
     */
    fun startForegroundService(context: Context) {
        val intent = Intent(context, BreakReminderService::class.java).apply {
            action = BreakReminderService.ACTION_START
        }
        // Android 8.0+必须使用startForegroundService
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    /**
     * 停止前台服务
     */
    fun stopForegroundService(context: Context) {
        val intent = Intent(context, BreakReminderService::class.java).apply {
            action = BreakReminderService.ACTION_STOP
        }
        context.startService(intent)
    }

    // ==================== 双进程守护管理 ====================

    /**
     * 启动保活守护服务（独立进程）
     * 该服务运行在:keepalive进程中，与主服务互为守护
     */
    private fun startKeepAliveService(context: Context) {
        val intent = Intent(context, KeepAliveService::class.java).apply {
            action = KeepAliveService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    /**
     * 停止保活守护服务
     */
    private fun stopKeepAliveService(context: Context) {
        val intent = Intent(context, KeepAliveService::class.java).apply {
            action = KeepAliveService.ACTION_STOP
        }
        context.startService(intent)
    }

    // ==================== WorkManager管理 ====================

    /**
     * 调度WorkManager周期性检查任务
     * 每15分钟检查一次服务是否存活，若已停止则重启
     *
     * WorkManager的优势：
     * - 即使应用被杀死也能执行
     * - 支持Doze模式
     * - 系统保证执行
     */
    fun scheduleWorkManagerCheck(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<KeepAliveWorker>(
            WORK_INTERVAL_MINUTES, TimeUnit.MINUTES
        )
            // 设置退避策略，失败后重试
            .setBackoffCriteria(
                androidx.work.BackoffPolicy.LINEAR,
                1, TimeUnit.MINUTES
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    /**
     * 取消WorkManager定时任务
     */
    private fun cancelWorkManagerCheck(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    // ==================== 厂商适配工具方法 ====================

    /**
     * 检查是否需要引导用户开启自启动权限
     * 不同厂商有不同的权限管理界面
     *
     * @return 是否需要引导（即当前未开启自启动）
     */
    fun needAutoStartPermission(context: Context): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        // 小米、华为、OPPO、vivo等国产手机通常需要手动开启自启动
        return when {
            manufacturer.contains("xiaomi") -> true
            manufacturer.contains("huawei") -> true
            manufacturer.contains("oppo") -> true
            manufacturer.contains("vivo") -> true
            manufacturer.contains("meizu") -> true
            manufacturer.contains("samsung") -> true
            else -> false
        }
    }

    /**
     * 获取自启动设置界面的Intent
     * 引导用户前往系统设置开启自启动权限
     *
     * @return 设置界面的Intent，若无法确定则返回应用详情页
     */
    fun getAutoStartIntent(context: Context): Intent? {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return when {
            // 小米：安全中心-自启动管理
            manufacturer.contains("xiaomi") -> {
                Intent().apply {
                    component = ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"
                    )
                }
            }
            // 华为：设置-电池-启动管理
            manufacturer.contains("huawei") -> {
                Intent().apply {
                    component = ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.optimize.process.ProtectActivity"
                    )
                }
            }
            // OPPO：设置-电池-应用启动管理
            manufacturer.contains("oppo") -> {
                Intent().apply {
                    component = ComponentName(
                        "com.coloros.safecenter",
                        "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                    )
                }
            }
            // 其他厂商：打开应用详情页
            else -> {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                }
            }
        }
    }

    /**
     * 检查服务是否正在运行
     * 通过检查前台服务通知来判断
     */
    fun isServiceRunning(context: Context): Boolean {
        val prefs = PreferencesManager(context)
        return prefs.isServiceEnabled()
    }
}
