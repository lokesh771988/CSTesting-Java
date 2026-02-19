# Build and deploy to Maven Central with GPG signing.
# Set passphrase via env (so it is not in script or history):
#   $env:GPG_PASSPHRASE = "your_passphrase"
#   .\deploy-signed.ps1
# Or run once: .\deploy-signed.ps1 -Passphrase "your_passphrase"

param([string]$Passphrase = $env:GPG_PASSPHRASE)

if (-not $Passphrase) {
    Write-Error "Set GPG_PASSPHRASE or pass -Passphrase 'your_passphrase'"
    exit 1
}

Write-Host "Stopping Java processes..." -ForegroundColor Yellow
Get-Process -Name java -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 2
if (Test-Path target) {
    Remove-Item -Path target -Recurse -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 1
}

Write-Host "Running: mvn clean deploy -Prelease" -ForegroundColor Green
& mvn clean deploy -Prelease "-Dgpg.passphrase=$Passphrase"
exit $LASTEXITCODE
