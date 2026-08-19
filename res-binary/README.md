# 资源文件说明

本目录存放二进制图片资源的 base64 编码版本。

由于 GitHub API 对二进制文件上传的限制，图片资源以 `.b64` 文本格式存储。

## 使用方法

构建前运行解码脚本，将 base64 文件还原为 PNG/JPG 图片：

```powershell
.\decode-resources.ps1
```

脚本会自动将所有 `.b64` 文件解码到对应的 `res/` 目录。

## 资源列表

| 文件名 | 目标路径 | 说明 |
|--------|---------|------|
| app_src_main_res_drawable_ic_main_bg.jpg.b64 | res/drawable/ic_main_bg.jpg | 主界面壁纸 |
| app_src_main_res_drawable_ic_user.png.b64 | res/drawable/ic_user.png | 用户中心按钮 |
| app_src_main_res_drawable_ic_user_center.png.b64 | res/drawable/ic_user_center.png | 用户中心界面图 |
| app_src_main_res_mipmap-mdpi_ic_launcher.png.b64 | res/mipmap-mdpi/ic_launcher.png | 应用图标 |
| app_src_main_res_mipmap-hdpi_ic_launcher.png.b64 | res/mipmap-hdpi/ic_launcher.png | 应用图标 |
| app_src_main_res_mipmap-xhdpi_ic_launcher.png.b64 | res/mipmap-xhdpi/ic_launcher.png | 应用图标 |
| app_src_main_res_mipmap-xxhdpi_ic_launcher.png.b64 | res/mipmap-xxhdpi/ic_launcher.png | 应用图标 |
| app_src_main_res_mipmap-xxxhdpi_ic_launcher.png.b64 | res/mipmap-xxxhdpi/ic_launcher.png | 应用图标 |

## 注意

- 预构建的 APK 已包含所有图片资源，无需手动解码
- 仅在从源码构建时需要运行解码脚本
