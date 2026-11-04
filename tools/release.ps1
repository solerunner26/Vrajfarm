# One-command release (owner request 2026-11-04: "version update incremental and automatic").
#
#   powershell -ExecutionPolicy Bypass -File tools\release.ps1 -Notes "નવું: ..."
#
# 1. versionCode = git commit count (build.gradle.kts), versionName = 0.2.<code> — no manual edits.
# 2. Builds the R8-shrunk, release-signed APK (key: %USERPROFILE%\.vrajkheti\, never in the repo).
# 3. Writes update.json {versionCode, versionName, apk, sha256, size, notes}.
# 4. Publishes GitHub release v<versionName> with both files and marks it "latest".
#    Phones read releases/latest/download/update.json on start and offer "હમણાં અપડેટ કરો".
param(
    [string]$Notes = "",
    [switch]$Draft
)
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

if (-not (Test-Path "$env:USERPROFILE\.vrajkheti\keystore.properties")) {
    throw "Release key missing: $env:USERPROFILE\.vrajkheti\keystore.properties (see HANDOVER.md 'Release signing')."
}
if (git status --porcelain) { throw "Commit or stash your changes first: the version number comes from the commit count." }

$code = [int](git rev-list --count HEAD)
$name = "0.2.$code"
$tag = "v$name"
Write-Host "Building $name (code $code)…"
& .\gradlew.bat assembleRelease --console=plain -q
if ($LASTEXITCODE -ne 0) { throw "assembleRelease failed" }

$apkSrc = Join-Path $env:LOCALAPPDATA "VrajKhetiBuild\app\outputs\apk\release\app-release.apk"
$out = Join-Path $env:LOCALAPPDATA "VrajKhetiBuild\release-out"
New-Item -ItemType Directory -Force $out | Out-Null
$apkName = "VrajKhetiHisab-$name.apk"
$apk = Join-Path $out $apkName
Copy-Item $apkSrc $apk -Force
$sha = (Get-FileHash $apk -Algorithm SHA256).Hash.ToLower()
$size = (Get-Item $apk).Length
$repo = "solerunner26/Vrajfarm"

$update = [ordered]@{
    versionCode = $code
    versionName = $name
    apk         = "https://github.com/$repo/releases/download/$tag/$apkName"
    sha256      = $sha
    size        = $size
    notes       = $Notes
}
$json = Join-Path $out "update.json"
# UTF-8 without BOM (Gujarati notes).
[System.IO.File]::WriteAllText($json, ($update | ConvertTo-Json), (New-Object System.Text.UTF8Encoding $false))

$body = "Vraj Kheti Hisab $name`n`n$Notes`n`nAndroid: download $apkName and install. Installed apps update themselves from Settings › એપ અપડેટ."
$args = @("release", "create", $tag, $apk, $json, "--repo", $repo, "--title", "Vraj Kheti Hisab $name", "--notes", $body, "--latest")
if ($Draft) { $args += "--draft" }
& gh @args
if ($LASTEXITCODE -ne 0) { throw "gh release create failed" }
Write-Host "Released $tag  ($([math]::Round($size / 1MB, 1)) MB, sha256 $sha)"
