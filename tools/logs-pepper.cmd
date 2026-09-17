@echo off
setlocal
if "%~1"=="" ( echo Gebruik: logs-pepper.cmd PEPPER_IP & exit /b 2 )
set "ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
"%ADB%" -s %~1:5555 logcat -d -s PepperQuest:V AndroidRuntime:E QiSDK:V
exit /b 0
