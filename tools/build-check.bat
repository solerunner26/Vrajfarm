@echo off
rem Builds the debug APK and runs unit tests; writes everything to tools\build-log.txt
cd /d "%~dp0.."
if exist "C:\Program Files\Android\Android Studio\jbr\bin\java.exe" set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
if exist "%LOCALAPPDATA%\Programs\Android Studio\jbr\bin\java.exe" set "JAVA_HOME=%LOCALAPPDATA%\Programs\Android Studio\jbr"
echo START %DATE% %TIME% > tools\build-log.txt
echo JAVA_HOME=%JAVA_HOME% >> tools\build-log.txt
call gradlew.bat assembleDebug testDebugUnitTest --console=plain --continue >> tools\build-log.txt 2>&1
echo EXIT %ERRORLEVEL% >> tools\build-log.txt
echo DONE %DATE% %TIME% >> tools\build-log.txt
