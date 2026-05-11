# ProGuard混淆规则
# 保护代码安全，优化APK体积

# ==================== 通用规则 ====================
# 代码混淆优化级别
-optimizationpasses 5
# 混淆时不使用大小写混合类名
-dontusemixedcaseclassnames
# 不跳过非公共的库类
-dontskipnonpubliclibraryclasses
# 混淆时做预校验
-dontpreverify
# 混淆时保留行号，方便调试崩溃日志
-keepattributes SourceFile,LineNumberTable
# 保留注解
-keepattributes *Annotation*
# 保留泛型签名
-keepattributes Signature
# 保留异常声明
-keepattributes Exceptions

# ==================== 四大组件保留 ====================
# 保留所有Activity（系统通过反射调用）
-keep public class * extends android.app.Activity
# 保留所有Service（系统通过反射调用）
-keep public class * extends android.app.Service
# 保留所有BroadcastReceiver
-keep public class * extends android.content.BroadcastReceiver
# 保留所有Application子类
-keep public class * extends android.app.Application

# ==================== 数据类保留 ====================
# BreakConfig是数据类，序列化/反序列化需要保留字段名
-keep class com.stretchly.breakreminder.model.BreakConfig {
    *;
}

# ==================== WorkManager保留 ====================
# WorkManager的Worker类不能被混淆
-keep class * extends androidx.work.Worker {
    *;
}
-keep class * extends androidx.work.CoroutineWorker {
    *;
}

# ==================== SharedPreferences保留 ====================
# SharedPreferences的Key值不能被混淆
# 否则无法正确读取之前保存的配置
-keepclassmembers class com.stretchly.breakreminder.util.PreferencesManager {
    *;
}

# ==================== Kotlin保留 ====================
# Kotlin反射需要保留元数据
-keep class kotlin.Metadata { *; }
# Kotlin协程保留
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ==================== AndroidX保留 ====================
# AndroidX核心库保留
-keep class androidx.** { *; }
-keep interface androidx.** { *; }

# ==================== 警告抑制 ====================
# 抑制常见的第三方库警告
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.**
-dontwarn okhttp3.**
