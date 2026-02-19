@echo off
REM Build and deploy to Maven Central with GPG signing.
REM Set GPG passphrase first (so it is not stored in the script or command history):
REM   set GPG_PASSPHRASE=your_passphrase
REM Then run: deploy-signed.cmd

if "%GPG_PASSPHRASE%"=="" (
    echo ERROR: Set GPG_PASSPHRASE first. Example:
    echo   set GPG_PASSPHRASE=your_passphrase
    echo   deploy-signed.cmd
    exit /b 1
)

echo Stopping Java processes that may lock target...
taskkill /F /IM java.exe 2>nul
timeout /t 2 /nobreak >nul

if exist target (
    rmdir /s /q target 2>nul
    timeout /t 1 /nobreak >nul
)

echo Running: mvn clean deploy -DskipSigning=false -Dgpg.passphrase=***
call mvn clean deploy -DskipSigning=false -Dgpg.passphrase=%GPG_PASSPHRASE%
exit /b %ERRORLEVEL%
