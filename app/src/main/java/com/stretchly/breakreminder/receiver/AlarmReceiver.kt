package com.stretchly.breakreminder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.stretchly.breakreminder.service.BreakReminderService
import com.stretchly.breakreminder.util.BreakScheduler
import com.stretchly.breakreminder.util.NotificationHelper
import com.stretchly.breakreminder.util.PreferencesManager

/**
 * 定时闹钟广播接收器
 *
 * 接收BreakScheduler设置的精确闹钟广播，
 * 根据不同的Action触发对应的操作：
 * - ACTION_MINI_BREAK：触发小休息
 * - ACTION_LONG_BREAK：触发长休息
 * - ACTION_PRE_BREAK：触发休息前预告通知
 *
 * 闹钟由BreakScheduler调度，此接收器负责执行
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            // 小休息闹钟触发
            BreakScheduler.ACTION_MINI_BREAK -> {
                triggerMiniBreak(context)
            }
            // 长休息闹钟触发
            BreakScheduler.ACTION_LONG_BREAK -> {
                triggerLongBreak(context)
            }
            // 预告通知闹钟触发
            BreakScheduler.ACTION_PRE_BREAK -> {
                showPreBreakNotification(context)
            }
        }
    }

    /**
     * 触发小休息
     * 向BreakReminderService发送触发小休息的Intent
     */
    private fun triggerMiniBreak(context: Context) {
        val serviceIntent = Intent(context, BreakReminderService::class.java).apply {
            action = BreakReminderService.ACTION_TRIGGER_MINI_BREAK
        }
        // Android 8.0+需要使用startForegroundService
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    /**
     * 触发长休息
     * 向BreakReminderService发送触发长休息的Intent
     */
    private fun triggerLongBreak(context: Context) {
        val serviceIntent = Intent(context, BreakReminderService::class.java).apply {
            action = BreakReminderService.ACTION_TRIGGER_LONG_BREAK
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    /**
     * 显示休息前预告通知
     * 提醒用户即将开始休息
     */
    private fun showPreBreakNotification(context: Context) {
        val prefsManager = PreferencesManager(context)
        val config = prefsManager.getConfig()
        val notificationHelper = NotificationHelper(context)

        // 计算距离休息还有多少秒
        val secondsLeft = (config.preBreakNotificationTime / 1000).toInt()
        notificationHelper.showPreBreakNotification(secondsLeft)
    }
}
