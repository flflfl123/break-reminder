package com.stretchly.breakreminder

import android.app.Application
import com.stretchly.breakreminder.util.NotificationHelper

/**
 * 自定义Application类
 *
 * 在应用启动时执行全局初始化操作：
 * 1. 创建通知渠道（Android 8.0+必需）
 * 2. 其他全局组件初始化
 */
class App : Application() {

    /**
     * 应用创建时的回调
     * 在所有Activity/Service之前执行
     */
    override fun onCreate() {
        super.onCreate()

        // 创建通知渠道
        // 必须在使用通知之前创建，否则通知无法显示
        NotificationHelper(this).createChannels()
    }
}
