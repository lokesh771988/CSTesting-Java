@echo off
REM Stop Java processes that may lock target\*.jar, then clean and build.
echo Stopping Java processes...
taskkill /F /IM java.exe 2>nul
timeout /t 2 /nobreak >nul

if exist target (
    echo Removing target folder...
    rmdir /s /q target 2>nul
    timeout /t 1 /nobreak >nul
)

echo Running: mvn clean package %*
call mvn clean package %*
exit /b %ERRORLEVEL%
