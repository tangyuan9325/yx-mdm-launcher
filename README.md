# YX MDM Launcher - 管控桌面仿制应用

> 仿言心教育 MDM 管控启动器的 Android 应用，实现开机自启动、横屏全屏锁定、仅允许指定应用运行。

## 功能特性

- **开机自启动**：设备开机后自动启动管控界面
- **横屏全屏锁定**：强制横屏显示，沉浸式全屏界面，禁止返回键退出
- **仅三应用**：只显示并允许启动三个指定应用
  - `com.edu.k12.hippo` - 学习中心（河马爱学）
  - `com.xiaoluxue.ai.student` - 小鹿AI（小鹿爱学）
  - `com.yx.appstore` - 应用商店（应用中心）
- **用户中心**：正中偏上方按钮进入用户中心界面（ic_user_center.png 全屏背景）
- **界面切换动画**：用户中心淡入淡出切换，流畅过渡
- **隐藏退出**：仅通过特定手段可永久退出管控
  - 方式一：进入用户中心 → 右上角透明区域连续点击7次（第5次起提示）→ 输入管理密码
  - 方式二：音量键组合（上上下下左右左右）→ 输入管理密码
- **永久退出**：验证密码后永久退出管控，重新启动应用才恢复锁定
- **前台保活服务**：每 3 秒检测并拉回主界面，服务被杀死后自动重启
- **自定义壁纸**：使用言心教育原版黑板壁纸

## 管理密码

默认管理密码：`yxmdm2024`

> 可在 `App.java` 中修改 `EXIT_PASSWORD` 常量。

## 退出管控说明

### 方式一：用户中心内连续点击（推荐）

1. 点击主界面正中偏上方的用户按钮，进入用户中心界面
2. 在用户中心界面**右上角透明区域连续点击7次**（2秒内完成）
   - 前4次点击无提示（静默计数）
   - 第5次起显示 Toast 提示剩余次数
3. 弹出密码输入框，输入正确密码后永久退出管控
4. 重新启动"言心管控"应用即可恢复锁定

### 方式二：音量键 Konami 码

1. 在管控主界面按音量键组合：**上上下下左右左右**
2. 弹出密码输入框，输入正确密码后永久退出管控

> 退出管控后，`onPause`/`onStop`/`LockService` 均不会再自动拉回前台。

## 界面布局

### 主界面

- **背景**：ic_main_bg.png（黑板壁纸，1920×1200）
- **三个应用图标**：水平排列，位于屏幕下方偏左（约58%高度，8%左边距）
- **用户中心按钮**：ic_user.png（72dp），位于屏幕正中偏上方（约22%高度，水平居中）

### 用户中心界面

- **背景**：ic_user_center.png（全屏，1920×1200）
- **右上角透明恢复按钮**：90dp×90dp 透明区域，连续点击7次触发退出管控
- **点击其他区域**：淡出动画关闭用户中心，返回主界面

## 项目结构

```
yx-mdm-launcher/
├── app/src/main/
│   ├── AndroidManifest.xml          # 应用清单（Launcher + 横屏 + 开机广播 + 前台服务）
│   ├── java/com/yx/mdmlauncher/
│   │   ├── App.java                 # Application 类（全局状态、密码、启动强制锁定）
│   │   ├── MainActivity.java        # 主界面（管控桌面核心、用户中心、退出逻辑）
│   │   ├── BootReceiver.java        # 开机自启动广播接收器
│   │   └── LockService.java         # 前台保活服务
│   └── res/
│       ├── drawable/                # 壁纸和按钮图片
│       │   ├── ic_main_bg.jpg       # 主界面壁纸（黑板）
│       │   ├── ic_user.png          # 用户中心按钮（96×96）
│       │   └── ic_user_center.png   # 用户中心全屏背景（1920×1200）
│       ├── mipmap-*/                # 应用图标（mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi）
│       └── values/                  # 字符串、颜色、样式
├── build.ps1                        # Windows 一键构建脚本
├── decode-resources.ps1             # base64 资源解码脚本（GitHub 克隆后使用）
├── res-binary/                      # base64 编码的二进制资源
└── README.md
```

## 构建方法

### 环境要求

- JDK 17+
- Android SDK（Build Tools 36.0.0, Platform android-36）
- Windows PowerShell

### 构建步骤

```powershell
# 1. 如果从 GitHub 克隆，先解码二进制资源
.\decode-resources.ps1

# 2. 运行构建
.\build.ps1
```

构建成功后，APK 输出在 `output/yx-mdm-launcher.apk`（已签名 v1+v2+v3）。

### 签名信息

- Keystore：`tools/yx-release.keystore`
- 别名：`yxlauncher`
- 密码：`yx123456`
- 算法：RSA 2048 / SHA256withRSA

> 如需使用自己的签名，修改 `build.ps1` 中的 `$KeystorePath`、`$KeyAlias`、`$StorePass` 变量。

## 安装使用

1. 将 APK 安装到 Android 设备（Android 7.0+，平板横屏设备最佳）
2. 首次启动时，按 Home 键，选择"言心管控"作为默认桌面
3. 重启设备验证开机自启动
4. 如需卸载：先退出管控（输入密码），在设置中清除默认桌面，再卸载

## 权限说明

| 权限 | 用途 |
|------|------|
| RECEIVE_BOOT_COMPLETED | 开机自启动 |
| FOREGROUND_SERVICE | 前台保活服务 |
| FOREGROUND_SERVICE_SPECIAL_USE | Android 14+ 前台服务类型 |
| WAKE_LOCK | 保持屏幕唤醒 |
| DISABLE_KEYGUARD | 禁用锁屏 |
| EXPAND_STATUS_BAR | 状态栏控制 |
| REORDER_TASKS | 任务栈管理 |
| KILL_BACKGROUND_PROCESSES | 保活机制 |
| REQUEST_IGNORE_BATTERY_OPTIMIZATIONS | 忽略电池优化 |

## 技术实现

- **纯 Java 实现**，无第三方依赖，无 AndroidX / Gradle
- **Launcher 模式**：通过 `HOME` / `DEFAULT` category 注册为桌面
- **横屏锁定**：`android:screenOrientation="landscape"`
- **单任务模式**：`launchMode="singleTask"` + `excludeFromRecents`
- **永久退出机制**：`App.isLocked()` 全局状态，退出后所有拉回逻辑失效；`App.onCreate()` 每次启动强制重置为锁定
- **音量键拦截**：`onKeyDown` 消费音量键事件，检测 Konami 组合键
- **返回键拦截**：重写 `onBackPressed`，用户中心打开时关闭用户中心，否则不响应
- **7次点击触发**：用户中心右上角透明区域，2秒超时重置，第5次起提示
- **淡入淡出动画**：`ViewPropertyAnimator` alpha 动画，250ms 过渡
- **服务保活**：`START_STICKY` + `onTaskRemoved` 重启 + 3秒轮询拉回
- **d8 脱糖**：支持 Java 8 Lambda 表达式

## 免责声明

本项目仅用于学习和研究目的。请勿用于非法用途或侵犯他人权益。使用本应用所产生的一切后果由使用者自行承担。

## License

MIT License
