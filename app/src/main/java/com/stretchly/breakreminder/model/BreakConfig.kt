package com.stretchly.breakreminder.model

/**
 * 休息配置数据类
 *
 * 参考Stretchly的默认行为设计：
 * - 小休息（Mini Break）：默认每20分钟提醒，持续20秒
 * - 长休息（Long Break）：默认每3次小休息后触发，持续5分钟
 *
 * 所有时间单位均为毫秒，方便直接用于Timer/Scheduler
 */
data class BreakConfig(

    // ==================== 小休息配置 ====================

    // 小休息间隔时间（毫秒），默认20分钟
    val miniBreakInterval: Long = 20 * 60 * 1000L,

    // 小休息持续时间（毫秒），默认20秒
    val miniBreakDuration: Long = 20 * 1000L,

    // ==================== 长休息配置 ====================

    // 长休息间隔时间（毫秒），默认30分钟
    // 当longBreakIntervalCount为0时，使用此值作为时间间隔
    val longBreakInterval: Long = 30 * 60 * 1000L,

    // 长休息持续时间（毫秒），默认5分钟
    val longBreakDuration: Long = 5 * 60 * 1000L,

    // 每多少次小休息后触发一次长休息，默认3次
    // 设为0则使用longBreakInterval作为时间间隔
    val longBreakIntervalCount: Int = 3,

    // ==================== 行为配置 ====================

    // 严格模式：开启后不可跳过休息
    val strictMode: Boolean = false,

    // 是否启用声音提醒
    val soundEnabled: Boolean = true,

    // 是否启用振动提醒
    val vibrationEnabled: Boolean = true,

    // 自然休息检测：检测屏幕关闭时暂停计时
    val naturalBreakEnabled: Boolean = true,

    // 休息前预告通知（毫秒），默认30秒前提醒
    val preBreakNotificationTime: Long = 30 * 1000L,

    // ==================== 界面配置 ====================

    // 是否显示休息建议文字
    val showBreakIdeas: Boolean = true,

    // 小休息背景颜色（十六进制颜色值）
    val miniBreakColor: String = "#4CAF50",

    // 长休息背景颜色（十六进制颜色值）
    val longBreakColor: String = "#2196F3"
)
