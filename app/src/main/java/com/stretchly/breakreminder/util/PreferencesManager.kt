package com.stretchly.breakreminder.util

import android.content.Context
import android.content.SharedPreferences
import com.stretchly.breakreminder.model.BreakConfig

/**
 * 偏好设置管理器
 *
 * 负责将BreakConfig持久化存储到SharedPreferences中，
 * 提供读取和保存配置的方法，支持单独修改某一项配置。
 */
class PreferencesManager(context: Context) {

    // SharedPreferences文件名
    companion object {
        private const val PREFS_NAME = "break_reminder_prefs"

        // ==================== 小休息相关Key ====================
        private const val KEY_MINI_BREAK_INTERVAL = "mini_break_interval"
        private const val KEY_MINI_BREAK_DURATION = "mini_break_duration"

        // ==================== 长休息相关Key ====================
        private const val KEY_LONG_BREAK_INTERVAL = "long_break_interval"
        private const val KEY_LONG_BREAK_DURATION = "long_break_duration"
        private const val KEY_LONG_BREAK_INTERVAL_COUNT = "long_break_interval_count"

        // ==================== 行为配置相关Key ====================
        private const val KEY_STRICT_MODE = "strict_mode"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        private const val KEY_NATURAL_BREAK_ENABLED = "natural_break_enabled"
        private const val KEY_PRE_BREAK_NOTIFICATION_TIME = "pre_break_notification_time"

        // ==================== 界面配置相关Key ====================
        private const val KEY_SHOW_BREAK_IDEAS = "show_break_ideas"
        private const val KEY_MINI_BREAK_COLOR = "mini_break_color"
        private const val KEY_LONG_BREAK_COLOR = "long_break_color"

        // ==================== 服务状态相关Key ====================
        private const val KEY_SERVICE_ENABLED = "service_enabled"
        private const val KEY_MINI_BREAK_COUNT = "mini_break_count"
        private const val KEY_LAST_BREAK_TIME = "last_break_time"
    }

    // SharedPreferences实例，使用apply()异步提交
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * 读取完整的休息配置
     * @return 当前存储的BreakConfig，若无记录则返回默认值
     */
    fun getConfig(): BreakConfig {
        return BreakConfig(
            miniBreakInterval = prefs.getLong(KEY_MINI_BREAK_INTERVAL, 20 * 60 * 1000L),
            miniBreakDuration = prefs.getLong(KEY_MINI_BREAK_DURATION, 20 * 1000L),
            longBreakInterval = prefs.getLong(KEY_LONG_BREAK_INTERVAL, 30 * 60 * 1000L),
            longBreakDuration = prefs.getLong(KEY_LONG_BREAK_DURATION, 5 * 60 * 1000L),
            longBreakIntervalCount = prefs.getInt(KEY_LONG_BREAK_INTERVAL_COUNT, 3),
            strictMode = prefs.getBoolean(KEY_STRICT_MODE, false),
            soundEnabled = prefs.getBoolean(KEY_SOUND_ENABLED, true),
            vibrationEnabled = prefs.getBoolean(KEY_VIBRATION_ENABLED, true),
            naturalBreakEnabled = prefs.getBoolean(KEY_NATURAL_BREAK_ENABLED, true),
            preBreakNotificationTime = prefs.getLong(KEY_PRE_BREAK_NOTIFICATION_TIME, 30 * 1000L),
            showBreakIdeas = prefs.getBoolean(KEY_SHOW_BREAK_IDEAS, true),
            miniBreakColor = prefs.getString(KEY_MINI_BREAK_COLOR, "#4CAF50") ?: "#4CAF50",
            longBreakColor = prefs.getString(KEY_LONG_BREAK_COLOR, "#2196F3") ?: "#2196F3"
        )
    }

    /**
     * 保存完整的休息配置
     * @param config 要保存的BreakConfig
     */
    fun saveConfig(config: BreakConfig) {
        prefs.edit().apply {
            putLong(KEY_MINI_BREAK_INTERVAL, config.miniBreakInterval)
            putLong(KEY_MINI_BREAK_DURATION, config.miniBreakDuration)
            putLong(KEY_LONG_BREAK_INTERVAL, config.longBreakInterval)
            putLong(KEY_LONG_BREAK_DURATION, config.longBreakDuration)
            putInt(KEY_LONG_BREAK_INTERVAL_COUNT, config.longBreakIntervalCount)
            putBoolean(KEY_STRICT_MODE, config.strictMode)
            putBoolean(KEY_SOUND_ENABLED, config.soundEnabled)
            putBoolean(KEY_VIBRATION_ENABLED, config.vibrationEnabled)
            putBoolean(KEY_NATURAL_BREAK_ENABLED, config.naturalBreakEnabled)
            putLong(KEY_PRE_BREAK_NOTIFICATION_TIME, config.preBreakNotificationTime)
            putBoolean(KEY_SHOW_BREAK_IDEAS, config.showBreakIdeas)
            putString(KEY_MINI_BREAK_COLOR, config.miniBreakColor)
            putString(KEY_LONG_BREAK_COLOR, config.longBreakColor)
            apply()
        }
    }

    // ==================== 服务状态管理 ====================

    /** 获取服务是否已启用 */
    fun isServiceEnabled(): Boolean = prefs.getBoolean(KEY_SERVICE_ENABLED, false)

    /** 设置服务启用状态 */
    fun setServiceEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SERVICE_ENABLED, enabled).apply()
    }

    /** 获取已完成的小休息次数 */
    fun getMiniBreakCount(): Int = prefs.getInt(KEY_MINI_BREAK_COUNT, 0)

    /** 设置已完成的小休息次数 */
    fun setMiniBreakCount(count: Int) {
        prefs.edit().putInt(KEY_MINI_BREAK_COUNT, count).apply()
    }

    /** 获取上次休息的时间戳 */
    fun getLastBreakTime(): Long = prefs.getLong(KEY_LAST_BREAK_TIME, 0L)

    /** 设置上次休息的时间戳 */
    fun setLastBreakTime(time: Long) {
        prefs.edit().putLong(KEY_LAST_BREAK_TIME, time).apply()
    }

    /** 重置所有配置为默认值 */
    fun resetToDefaults() {
        prefs.edit().clear().apply()
    }
}
