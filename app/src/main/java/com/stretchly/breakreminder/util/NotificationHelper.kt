package com.stretchly.breakreminder.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.stretchly.breakreminder.MainActivity
import com.stretchly.breakreminder.R

/**
 * 通知工具类
 *
 * 负责创建和管理所有通知：
 * - 前台服务常驻通知（保活必需）
 * - 休息提醒通知
 * - 休息前预告通知
 *
 * Android 8.0+必须创建通知渠道才能显示通知
 */
class NotificationHelper(private val context: Context) {

    companion object {
        // 前台服务通知ID
        const val FOREGROUND_NOTIFICATION_ID = 1001
        // 休息提醒通知ID
        const val BREAK_NOTIFICATION_ID = 1002
        // 预告通知ID
        const val PRE_BREAK_NOTIFICATION_ID = 1003

        // 前台服务通知渠道ID
        const val CHANNEL_FOREGROUND = "channel_foreground"
        // 休息提醒通知渠道ID
        const val CHANNEL_BREAK = "channel_break"
    }

    // 系统通知管理器
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    /**
     * 创建所有通知渠道
     * Android 8.0+必须调用，否则通知不显示
     */
    fun createChannels() {
        // Android 8.0以下不需要创建渠道
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        // 前台服务通知渠道：低优先级，不发出声音
        val foregroundChannel = NotificationChannel(
            CHANNEL_FOREGROUND,
            context.getString(R.string.channel_foreground_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.channel_foreground_desc)
            // 禁用声音和振动，避免打扰用户
            setSound(null, null)
            enableVibration(false)
            // 不在锁屏显示
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        }

        // 休息提醒通知渠道：高优先级，需要引起注意
        val breakChannel = NotificationChannel(
            CHANNEL_BREAK,
            context.getString(R.string.channel_break_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_break_desc)
            enableVibration(true)
            // 在锁屏显示完整内容
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        // 注册通知渠道
        notificationManager.createNotificationChannel(foregroundChannel)
        notificationManager.createNotificationChannel(breakChannel)
    }

    /**
     * 创建前台服务常驻通知
     * 前台服务必须显示通知，这是Android系统的要求
     *
     * @param contentText 通知内容文本，如"下次休息：5分钟后"
     * @return 构建好的Notification对象
     */
    fun buildForegroundNotification(contentText: String): Notification {
        // 点击通知打开主界面
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_FOREGROUND)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            // 常驻通知不可滑动删除
            .setOngoing(true)
            // 低优先级，不发出声音
            .setPriority(NotificationCompat.PRIORITY_LOW)
            // 设置分类为服务状态
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    /**
     * 更新前台服务通知内容
     * 用于实时显示倒计时等信息
     *
     * @param contentText 新的通知内容文本
     */
    fun updateForegroundNotification(contentText: String) {
        val notification = buildForegroundNotification(contentText)
        notificationManager.notify(FOREGROUND_NOTIFICATION_ID, notification)
    }

    /**
     * 显示休息提醒通知
     * 高优先级通知，提醒用户该休息了
     *
     * @param isLongBreak 是否为长休息
     */
    fun showBreakNotification(isLongBreak: Boolean) {
        val title = if (isLongBreak) {
            context.getString(R.string.notification_long_break_title)
        } else {
            context.getString(R.string.notification_mini_break_title)
        }

        val text = if (isLongBreak) {
            context.getString(R.string.notification_long_break_text)
        } else {
            context.getString(R.string.notification_mini_break_text)
        }

        // 点击通知打开休息覆盖界面
        val breakIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            action = "ACTION_SHOW_BREAK"
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, breakIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_BREAK)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()

        notificationManager.notify(BREAK_NOTIFICATION_ID, notification)
    }

    /**
     * 显示休息前预告通知
     * 在休息开始前30秒提醒用户
     *
     * @param secondsLeft 距离休息还有多少秒
     */
    fun showPreBreakNotification(secondsLeft: Int) {
        val text = context.getString(R.string.notification_pre_break_text, secondsLeft)

        val notification = NotificationCompat.Builder(context, CHANNEL_BREAK)
            .setContentTitle(context.getString(R.string.notification_pre_break_title))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        notificationManager.notify(PRE_BREAK_NOTIFICATION_ID, notification)
    }

    /** 取消休息提醒通知 */
    fun cancelBreakNotification() {
        notificationManager.cancel(BREAK_NOTIFICATION_ID)
        notificationManager.cancel(PRE_BREAK_NOTIFICATION_ID)
    }
}
