@echo off
rem Lists devices; if one is attached, installs the debug APK and launches the app. Log: tools\device-log.txt
cd /d "%~dp0.."
set "ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
echo START %DATE% %TIME% > tools\device-log.txt
"%ADB%" devices >> tools\device-log.txt 2>&1
"%ADB%" install -r "%LOCALAPPDATA%\VrajKhetiBuild\app\outputs\apk\debug\app-debug.apk" >> tools\device-log.txt 2>&1
"%ADB%" shell am start -n com.vraj.khetihisab/.MainActivity >> tools\device-log.txt 2>&1
echo DONE >> tools\device-log.txt
