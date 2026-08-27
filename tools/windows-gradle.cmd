@echo off
setlocal
set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "PATH=C:\Program Files\Android\Android Studio\jbr\bin;%PATH%"
cd /d C:\Users\matin\Desktop\HvA\SIT\PepperSITMascot
java -version
call gradlew.bat %*
exit /b %ERRORLEVEL%
