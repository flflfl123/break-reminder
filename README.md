# 休息提醒（BreakReminder）

一款 Android 平台的定时休息提醒应用，灵感来源于 [Stretchly](https://github.com/hovancik/stretchly) 桌面端休息提醒软件。

## 功能特性

### 核心功能
- **小休息（Mini Break）**：默认每 20 分钟提醒一次，持续 20 秒
- **长休息（Long Break）**：默认每 3 次小休息后触发，持续 5 分钟
- **全屏休息覆盖**：休息时全屏显示倒计时和放松建议，无法忽略
- **严格模式**：开启后不可跳过休息，强制执行
- **休息前预告**：休息开始前 30 秒发送预告通知
- **放松建议**：随机显示放松建议文字（可关闭）

### 保活机制
为确保应用在后台持续运行，实现了多层保活策略：

| 保活方式 | 说明 |
|----------|------|
| 前台服务 | 系统优先级高，不易被杀 |
| 双进程守护 | 独立进程监控主服务，互为守护 |
| WorkManager | 每 15 分钟检查服务状态，可靠执行 |
| AlarmManager | 精确定时触发休息提醒 |
| 开机自启 | 设备重启后自动恢复服务 |
| START_STICKY | 服务被杀后自动重启 |
| 厂商适配 | 引导用户开启自启动权限 |

### 厂商适配
- 小米：引导开启安全中心自启动管理
- 华为：引导设置电池启动管理
- OPPO/vivo：引导开启后台运行权限

## 项目结构

```
app/src/main/java/com/stretchly/breakreminder/
├── App.kt                          # Application入口，初始化通知渠道
├── MainActivity.kt                 # 主界面，服务启停控制
├── BreakOverlayActivity.kt         # 全屏休息覆盖界面
├── SettingsActivity.kt             # 设置界面
├── model/
│   └── BreakConfig.kt              # 休息配置数据类
├── util/
│   ├── PreferencesManager.kt       # 偏好设置管理
│   ├── NotificationHelper.kt       # 通知管理
│   ├── BreakScheduler.kt           # 休息调度器（AlarmManager）
│   └── KeepAliveManager.kt         # 保活管理器
├── service/
│   ├── BreakReminderService.kt     # 休息提醒前台服务
│   └── KeepAliveService.kt         # 保活守护服务（独立进程）
├── worker/
│   └── KeepAliveWorker.kt          # WorkManager保活工作器
└── receiver/
    ├── BootReceiver.kt             # 开机自启广播接收器
    └── AlarmReceiver.kt            # 定时闹钟广播接收器
```

## 构建说明

### 环境要求
- Android Studio Hedgehog 或更高版本
- JDK 8+
- Android SDK：compileSdk 34，minSdk 24（Android 7.0+）

### 构建步骤
1. 使用 Android Studio 打开项目
2. 等待 Gradle 同步完成
3. 连接 Android 设备或启动模拟器
4. 点击 Run 按钮或使用 `./gradlew installDebug`

### 命令行构建
```bash
# 调试版
./gradlew assembleDebug

# 发布版
./gradlew assembleRelease
```

## 权限说明

| 权限 | 用途 |
|------|------|
| RECEIVE_BOOT_COMPLETED | 设备重启后自动恢复服务 |
| FOREGROUND_SERVICE | 前台服务保活 |
| FOREGROUND_SERVICE_SPECIAL_USE | Android 14+ 前台服务类型 |
| WAKE_LOCK | 定时唤醒设备执行提醒 |
| VIBRATE | 休息提醒时振动 |
| POST_NOTIFICATIONS | Android 13+ 通知权限 |
| SCHEDULE_EXACT_ALARM | 精确闹钟定时 |
| USE_EXACT_ALARM | 使用精确闹钟 |

## 致谢

- [Stretchly](https://github.com/hovancik/stretchly) - 桌面端休息提醒软件，本项目的灵感来源
