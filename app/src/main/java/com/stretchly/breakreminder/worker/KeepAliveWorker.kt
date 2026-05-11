package com.stretchly.breakreminder.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.stretchly.breakreminder.util.KeepAliveManager
import com.stretchly.breakreminder.util.PreferencesManager

/**
 * WorkManager保活工作器
 *
 * WorkManager是Android推荐的可靠后台任务调度方案：
 * - 即使应用被杀死也能执行
 * - 支持Doze模式
 * - 系统保证执行（可能延迟但不会丢失）
 *
 * 此Worker定期检查休息提醒服务是否存活，
 * 若服务已停止但用户期望运行，则自动重启服务。
 *
 * 与KeepAliveService双进程守护互补：
 * - KeepAliveService：实时监控，但可能一起被杀
 * - WorkManager：延迟但可靠，即使应用完全被杀也能执行
 */
class KeepAliveWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    /**
     * 执行后台工作
     * WorkManager会在合适的时机调用此方法
     *
     * @return 工作结果
     */
    override fun doWork(): Result {
        val prefsManager = PreferencesManager(applicationContext)

        // 检查用户是否期望服务运行
        val shouldRun = prefsManager.isServiceEnabled()

        if (shouldRun) {
            // 用户期望服务运行，确保服务已启动
            // 即使服务已在运行，重复启动也不会有问题
            KeepAliveManager.startForegroundService(applicationContext)
        }

        // 返回成功，WorkManager会按照周期继续调度
        return Result.success()
    }
}
