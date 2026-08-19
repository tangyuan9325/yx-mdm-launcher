# 解码 base64 资源文件到对应目录
$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$ResBinaryDir = Join-Path $ProjectRoot "res-binary"
$SrcDir = Join-Path $ProjectRoot "app\src\main"

Write-Host "Decoding binary resources..." -ForegroundColor Cyan

$b64Files = Get-ChildItem $ResBinaryDir -Filter "*.b64"
foreach ($file in $b64Files) {
    $relPath = $file.BaseName -replace "_", "\"
    $targetPath = Join-Path $SrcDir $relPath

    $targetDir = Split-Path -Parent $targetPath
    if (!(Test-Path $targetDir)) {
        New-Item -ItemType Directory -Path $targetDir -Force | Out-Null
    }

    $b64 = [System.IO.File]::ReadAllText($file.FullName)
    $bytes = [System.Convert]::FromBase64String($b64)
    [System.IO.File]::WriteAllBytes($targetPath, $bytes)

    Write-Host "  $($file.Name) -> $relPath ($($bytes.Length) bytes)" -ForegroundColor Green
}

Write-Host "`nAll resources decoded successfully!" -ForegroundColor Cyan
