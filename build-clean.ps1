# Stop any Java processes that might be locking target\*.jar, then clean and build.
# Run from project root: .\build-clean.ps1

Write-Host "Stopping Java processes that may lock target..." -ForegroundColor Yellow
Get-Process -Name java -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 2

if (Test-Path "target") {
    Write-Host "Removing target folder..." -ForegroundColor Yellow
    Remove-Item -Path "target" -Recurse -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 1
}

Write-Host "Running: mvn clean package" -ForegroundColor Green
& mvn clean package @args
exit $LASTEXITCODE
