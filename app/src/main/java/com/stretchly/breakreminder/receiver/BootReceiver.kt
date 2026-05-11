package com.stretchly.breakreminder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.stretchly.breakreminder.util.KeepAliveManager
import com.stretchly.breakreminder.util.PreferencesManager

/**
 * 开机自启广播接收器
 *
 * 监听设备启动完成的广播，在设备重启后自动恢复休息提醒服务。
 *
 * 监听的广播：
 * - BOOT_COMPLETED：设备启动完成
 * - LOCKED_BOOT_COMPLETED：直接启动模式完成（Android 7.0+）
 * - QUICKBOOT_POWERON：HTC设备快速启动
 *
 * 注意：从Android 10开始，安装后首次启动需要用户手动打开应用一次，
 * 之后才能接收BOOT_COMPLETED广播。
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // 只处理开机相关的广播
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON" -> {
                onBootCompleted(context)
            }
        }
    }

    /**
     * 开机完成后的处理
     * 检查用户是否之前启用了服务，如果是则自动恢复
     */
    private fun onBootCompleted(context: Context) {
        val prefsManager = PreferencesManager(context)

        // 只有用户之前启用了服务，才自动恢复
        if (prefsManager.isServiceEnabled()) {
            // 启动所有保活机制
            KeepAliveManager.startAll(context)
        }
    }
}
