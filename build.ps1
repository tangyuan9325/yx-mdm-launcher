# YX MDM Launcher - APK Build Script
# 使用 Android SDK 命令行工具直接构建

$ErrorActionPreference = "Continue"

# === 路径配置 ===
$ProjectRoot = $PSScriptRoot
if (-not $ProjectRoot) { $ProjectRoot = Get-Location }

$SdkRoot = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } elseif ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } else { "C:\Users\27775\AppData\Local\Android\Sdk" }
$BuildTools = "$SdkRoot\build-tools\36.0.0"
$PlatformJar = "$SdkRoot\platforms\android-36\android.jar"
$JavaHome = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { "C:\Program Files\Java\jdk-17" }

$SrcDir = "$ProjectRoot\app\src\main"
$BuildDir = "$ProjectRoot\build"
$OutputDir = "$ProjectRoot\output"

# 工具路径
$Aapt2 = "$BuildTools\aapt2.exe"
$D8 = "$BuildTools\d8.bat"
$Zipalign = "$BuildTools\zipalign.exe"
$Apksigner = "$BuildTools\apksigner.bat"
$Javac = "$JavaHome\bin\javac.exe"
$Jar = "$JavaHome\bin\jar.exe"

# 签名配置
$Keystore = "$ProjectRoot\tools\yx-release.keystore"
$KeystorePass = "yx123456"
$KeyAlias = "yxlauncher"
$KeyPass = "yx123456"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  YX MDM Launcher - APK Build" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# === 1. 清理 ===
Write-Host "`n[1/7] Cleaning build directory..." -ForegroundColor Yellow
if (Test-Path $BuildDir) { Remove-Item $BuildDir -Recurse -Force }
if (Test-Path $OutputDir) { Remove-Item $OutputDir -Recurse -Force }
New-Item -ItemType Directory -Path $BuildDir -Force | Out-Null
New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
$CompiledResDir = "$BuildDir\compiled_res"
$ClassesDir = "$BuildDir\classes"
$DexDir = "$BuildDir\dex"
New-Item -ItemType Directory -Path $CompiledResDir -Force | Out-Null
New-Item -ItemType Directory -Path $ClassesDir -Force | Out-Null
New-Item -ItemType Directory -Path $DexDir -Force | Out-Null

# === 2. 编译资源 ===
Write-Host "`n[2/7] Compiling resources with aapt2..." -ForegroundColor Yellow
& $Aapt2 compile --dir "$SrcDir\res" -o $CompiledResDir 2>&1 | ForEach-Object { Write-Host "  $_" }
if ($LASTEXITCODE -ne 0) { throw "aapt2 compile failed" }
$flatFiles = Get-ChildItem $CompiledResDir -Filter "*.flat"
Write-Host "  Compiled $($flatFiles.Count) resource files"

# === 3. 链接资源 + 生成R.java ===
Write-Host "`n[3/7] Linking resources and generating R.java..." -ForegroundColor Yellow
$flatList = $flatFiles | ForEach-Object { $_.FullName }
$GenDir = "$BuildDir\gen"
New-Item -ItemType Directory -Path $GenDir -Force | Out-Null

& $Aapt2 link `
    --manifest "$SrcDir\AndroidManifest.xml" `
    -I $PlatformJar `
    --java $GenDir `
    --auto-add-overlay `
    -o "$BuildDir\base.apk" `
    @flatList 2>&1 | ForEach-Object { Write-Host "  $_" }

if ($LASTEXITCODE -ne 0) { throw "aapt2 link failed" }
Write-Host "  Resources linked, base.apk created"

# 查找R.java
$RJava = Get-ChildItem $GenDir -Recurse -Filter "R.java" | Select-Object -First 1
if ($RJava) { Write-Host "  R.java generated: $($RJava.FullName)" } else { throw "R.java not found" }

# === 4. 编译Java ===
Write-Host "`n[4/7] Compiling Java sources..." -ForegroundColor Yellow
$JavaFiles = @()
$JavaFiles += Get-ChildItem "$SrcDir\java" -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
$JavaFiles += $RJava.FullName
Write-Host "  Found $($JavaFiles.Count) Java files"

& $Javac `
    -source 1.8 -target 1.8 `
    -classpath $PlatformJar `
    -d $ClassesDir `
    -encoding UTF-8 `
    @JavaFiles 2>&1 | ForEach-Object { Write-Host "  $_" }

if ($LASTEXITCODE -ne 0) { throw "javac compilation failed" }
$classCount = (Get-ChildItem $ClassesDir -Recurse -Filter "*.class").Count
Write-Host "  Compiled $classCount class files"

# === 5. 转换为Dex ===
Write-Host "`n[5/7] Converting to dex with d8..." -ForegroundColor Yellow
$classFiles = Get-ChildItem $ClassesDir -Recurse -Filter "*.class" | ForEach-Object { $_.FullName }

& $D8 `
    --lib $PlatformJar `
    --output $DexDir `
    --min-api 24 `
    @classFiles 2>&1 | ForEach-Object { Write-Host "  $_" }

if ($LASTEXITCODE -ne 0) { throw "d8 failed" }
$dexFile = Get-ChildItem $DexDir -Filter "*.dex" | Select-Object -First 1
Write-Host "  Dex generated: $($dexFile.Name)"

# === 6. 打包APK ===
Write-Host "`n[6/7] Packaging APK..." -ForegroundColor Yellow
$UnsignedApk = "$BuildDir\app-unsigned.apk"
Copy-Item "$BuildDir\base.apk" $UnsignedApk

# 添加dex到APK
Push-Location $DexDir
& $Jar uf $UnsignedApk $dexFile.Name 2>&1 | ForEach-Object { Write-Host "  $_" }
Pop-Location
Write-Host "  Dex added to APK"

# === 7. 对齐 + 签名 ===
Write-Host "`n[7/7] Aligning and signing APK..." -ForegroundColor Yellow

# zipalign
$AlignedApk = "$BuildDir\app-aligned.apk"
& $Zipalign -f -p 4 $UnsignedApk $AlignedApk 2>&1 | ForEach-Object { Write-Host "  $_" }
if ($LASTEXITCODE -ne 0) { throw "zipalign failed" }
Write-Host "  APK aligned"

# 签名
$SignedApk = "$OutputDir\yx-mdm-launcher.apk"
& $Apksigner sign `
    --ks $Keystore `
    --ks-pass "pass:$KeystorePass" `
    --ks-key-alias $KeyAlias `
    --key-pass "pass:$KeyPass" `
    --out $SignedApk `
    $AlignedApk 2>&1 | ForEach-Object { Write-Host "  $_" }

if ($LASTEXITCODE -ne 0) { throw "apksigner failed" }

# 验证签名
Write-Host "`nVerifying signature..." -ForegroundColor Yellow
& $Apksigner verify --verbose $SignedApk 2>&1 | Select-Object -First 5 | ForEach-Object { Write-Host "  $_" }

$apkSize = [math]::Round((Get-Item $SignedApk).Length / 1MB, 2)
Write-Host "`n========================================" -ForegroundColor Green
Write-Host "  BUILD SUCCESS!" -ForegroundColor Green
Write-Host "  Output: $SignedApk" -ForegroundColor Green
Write-Host "  Size: $apkSize MB" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
