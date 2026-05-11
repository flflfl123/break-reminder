package com.stretchly.breakreminder.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.stretchly.breakreminder.model.BreakConfig
import com.stretchly.breakreminder.receiver.AlarmReceiver

/**
 * 休息调度器
 *
 * 负责使用AlarmManager精确定时触发休息提醒。
 * 使用精确闹钟（Exact Alarm）确保在Doze模式下也能准时提醒。
 *
 * 调度逻辑：
 * 1. 每次小休息结束后，调度下一次小休息
 * 2. 每N次小休息后，调度一次长休息
 * 3. 支持预告通知（休息前30秒提醒）
 */
class BreakScheduler(private val context: Context) {

    // 闹钟请求码，用于区分不同类型的闹钟
    companion object {
        // 小休息闹钟请求码
        private const val REQUEST_CODE_MINI_BREAK = 2001
        // 长休息闹钟请求码
        private const val REQUEST_CODE_LONG_BREAK = 2002
        // 预告通知闹钟请求码
        private const val REQUEST_CODE_PRE_BREAK = 2003

        // Intent中的Action标识
        const val ACTION_MINI_BREAK = "com.stretchly.breakreminder.MINI_BREAK"
        const val ACTION_LONG_BREAK = "com.stretchly.breakreminder.LONG_BREAK"
        const val ACTION_PRE_BREAK = "com.stretchly.breakreminder.PRE_BREAK"

        // Intent中携带的休息类型Key
        const val EXTRA_BREAK_TYPE = "break_type"
        const val EXTRA_IS_LONG_BREAK = "is_long_break"
    }

    // 系统闹钟管理器
    private val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * 调度下一次休息提醒
     *
     * 根据当前已完成的小休息次数判断下一次是
     * 小休息还是长休息
     *
     * @param config 当前休息配置
     * @param miniBreakCount 已完成的小休息次数
     */
    fun scheduleNextBreak(config: BreakConfig, miniBreakCount: Int) {
        // 先取消所有已存在的闹钟
        cancelAll()

        // 判断是否应该触发长休息
        val shouldLongBreak = config.longBreakIntervalCount > 0 &&
            miniBreakCount > 0 &&
            miniBreakCount % config.longBreakIntervalCount == 0

        if (shouldLongBreak) {
            scheduleLongBreak(config)
        } else {
            scheduleMiniBreak(config)
        }
    }

    /**
     * 调度小休息闹钟
     * 同时调度预告通知闹钟
     */
    private fun scheduleMiniBreak(config: BreakConfig) {
        val triggerTime = System.currentTimeMillis() + config.miniBreakInterval

        // 如果配置了预告通知，先调度预告
        if (config.preBreakNotificationTime > 0 &&
            config.preBreakNotificationTime < config.miniBreakInterval) {
            val preTime = triggerTime - config.preBreakNotificationTime
            scheduleAlarm(
                REQUEST_CODE_PRE_BREAK,
                ACTION_PRE_BREAK,
                preTime,
                false
            )
        }

        // 调度小休息闹钟（精确闹钟）
        scheduleAlarm(
            REQUEST_CODE_MINI_BREAK,
            ACTION_MINI_BREAK,
            triggerTime,
            true
        )
    }

    /**
     * 调度长休息闹钟
     */
    private fun scheduleLongBreak(config: BreakConfig) {
        val triggerTime = System.currentTimeMillis() + config.longBreakInterval

        // 调度预告通知
        if (config.preBreakNotificationTime > 0 &&
            config.preBreakNotificationTime < config.longBreakInterval) {
            val preTime = triggerTime - config.preBreakNotificationTime
            scheduleAlarm(
                REQUEST_CODE_PRE_BREAK,
                ACTION_PRE_BREAK,
                preTime,
                false
            )
        }

        // 调度长休息闹钟
        scheduleAlarm(
            REQUEST_CODE_LONG_BREAK,
            ACTION_LONG_BREAK,
            triggerTime,
            true
        )
    }

    /**
     * 设置精确闹钟
     *
     * 使用AlarmManager的精确闹钟API，确保在Doze模式下也能唤醒
     *
     * @param requestCode 闹钟请求码
     * @param action 闹钟触发的Action
     * @param triggerTime 触发时间（毫秒时间戳）
     * @param isExact 是否使用精确闹钟
     */
    private fun scheduleAlarm(
        requestCode: Int,
        action: String,
        triggerTime: Long,
        isExact: Boolean
    ) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            this.action = action
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (isExact) {
            // Android 12+需要检查精确闹钟权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    // 使用精确闹钟，在Doze模式下也能唤醒
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    // 没有精确闹钟权限，使用非精确闹钟
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Android 6.0+，使用精确闹钟+Doze模式兼容
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                // 旧版本，直接使用精确闹钟
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } else {
            // 非精确闹钟（用于预告通知）
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    /** 取消所有已调度的闹钟 */
    fun cancelAll() {
        cancelAlarm(REQUEST_CODE_MINI_BREAK, ACTION_MINI_BREAK)
        cancelAlarm(REQUEST_CODE_LONG_BREAK, ACTION_LONG_BREAK)
        cancelAlarm(REQUEST_CODE_PRE_BREAK, ACTION_PRE_BREAK)
    }

    /**
     * 取消指定闹钟
     */
    private fun cancelAlarm(requestCode: Int, action: String) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            this.action = action
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }
}
