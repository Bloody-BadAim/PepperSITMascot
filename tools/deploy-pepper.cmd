@echo off
setlocal
if "%~1"=="" (
  echo Gebruik: deploy-pepper.cmd PEPPER_IP
  exit /b 2
)
set "PEPPER=%~1"
set "ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
set "APK=C:\Users\matin\Desktop\HvA\SIT\PepperSITMascot\dist\sit-quest-debug.apk"

echo === 1 ADB verbinden met %PEPPER% ===
"%ADB%" disconnect %PEPPER%:5555 >nul 2>&1
"%ADB%" connect %PEPPER%:5555
"%ADB%" devices

echo === 2 Installeren ===
"%ADB%" -s %PEPPER%:5555 install -r "%APK%"
if errorlevel 1 (
  echo install -r mislukt, probeer schone installatie
  "%ADB%" -s %PEPPER%:5555 uninstall nl.svsit.pepperquest
  "%ADB%" -s %PEPPER%:5555 install "%APK%"
)

echo === 3 Starten ===
"%ADB%" -s %PEPPER%:5555 shell am force-stop nl.svsit.pepperquest
"%ADB%" -s %PEPPER%:5555 shell am start -n nl.svsit.pepperquest/.MainActivity

echo === 4 Verificatie ===
"%ADB%" -s %PEPPER%:5555 shell pidof nl.svsit.pepperquest
"%ADB%" -s %PEPPER%:5555 shell dumpsys package nl.svsit.pepperquest ^| findstr /C:"versionName" /C:"firstInstallTime" /C:"lastUpdateTime"
exit /b 0
