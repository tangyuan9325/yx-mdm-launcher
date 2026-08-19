# YX MDM Launcher - 管控桌面仿制应用

> 仿言心教育 MDM 管控启动器的 Android 应用，实现开机自启动、全屏管控、仅允许指定应用运行。

## 功能特性

- **开机自启动**：设备开机后自动启动管控界面
- **全屏锁定**：沉浸式全屏界面，禁止返回键退出
- **仅三应用**：只显示并允许启动三个指定应用
  - `com.edu.k12.hippo` - 学习中心
  - `com.xiaoluxue.ai.student` - 小鹿AI
  - `com.yx.appstore` - 应用商店
- **用户中心**：右上角按钮进入用户中心界面
- **隐藏退出**：仅通过特定手段可临时退出管控
  - 方式一：连续点击右上角 7 次 → 输入管理密码
  - 方式二：音量键组合（上上下下左右左右）→ 输入管理密码
- **前台保活服务**：每 3 秒检测并拉回主界面，服务被杀死后自动重启
- **自定义壁纸**：使用言心教育原版壁纸

## 管理密码

默认管理密码：`yxmdm2024`

> 可在 `App.java` 中修改 `EXIT_PASSWORD` 常量。

## 退出管控说明

1. **连续点击右上角区域 7 次**（2秒内完成），弹出密码输入框
2. 或按音量键组合：**上上下下左右左右**（Konami Code）
3. 输入正确密码后，临时退出管控 5 秒，可选择其他桌面
4. 5 秒后自动恢复管控模式

## 项目结构

```
yx-mdm-launcher/
├── app/src/main/
│   ├── AndroidManifest.xml          # 应用清单（Launcher + 开机广播）
│   ├── java/com/yx/mdmlauncher/
│   │   ├── App.java                 # Application 类（全局状态、密码）
│   │   ├── MainActivity.java        # 主界面（管控桌面核心）
│   │   ├── BootReceiver.java        # 开机自启动广播接收器
│   │   └── LockService.java         # 前台保活服务
│   └── res/
│       ├── drawable/                # 壁纸和按钮图片
│       │   ├── ic_main_bg.png       # 主界面壁纸
│       │   ├── ic_user.png          # 用户中心按钮
│       │   └── ic_user_center.png   # 用户中心界面图
│       ├── mipmap-*/                # 应用图标
│       └── values/                  # 字符串、颜色、样式
├── build.ps1                        # Windows 构建脚本
└── README.md
```

## 构建方法

### 环境要求

- JDK 17+
- Android SDK（Build Tools 36.0.0, Platform android-36）
- Windows PowerShell

### 构建步骤

```powershell
# 设置 Android SDK 路径（如不同）
# 编辑 build.ps1 中的 $SdkRoot 变量

# 运行构建
.\build.ps1
```

构建成功后，APK 输出在 `output/yx-mdm-launcher.apk`。

## 安装使用

1. 将 APK 安装到 Android 设备（Android 7.0+）
2. 首次启动时，按 Home 键，选择"言心管控"作为默认桌面
3. 重启设备验证开机自启动
4. 如需卸载：先在设置中清除默认桌面，再卸载

## 技术实现

- **纯 Java 实现**，无第三方依赖，无 AndroidX
- **Launcher 模式**：通过 `HOME` / `DEFAULT` category 注册为桌面
- **单任务模式**：`launchMode="singleTask"` + `excludeFromRecents`
- **音量键拦截**：`onKeyDown` 消费音量键事件，检测组合键
- **返回键拦截**：重写 `onBackPressed`，不调用 super
- **服务保活**：`START_STICKY` + `onTaskRemoved` 重启 + 3秒轮询拉回
- **d8 脱糖**：支持 Java 8 Lambda 表达式

## 免责声明

本项目仅用于学习和研究目的。请勿用于非法用途或侵犯他人权益。使用本应用所产生的一切后果由使用者自行承担。

## License

MIT License
