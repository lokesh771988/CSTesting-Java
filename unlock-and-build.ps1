# Run this if "The process cannot access the file" blocks mvn package.
# Stops Java and Chrome processes that may lock target\*.jar, then removes target.

Get-Process -Name java -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Get-Process -Name chrome -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 2
Remove-Item -Path ".\target" -Recurse -Force -ErrorAction SilentlyContinue
if (Test-Path ".\target") {
    Write-Host "Some files in target are still locked. Close IDE or other apps using the project, then run this script again."
    exit 1
}
Write-Host "Target removed. Running: mvn package"
mvn package
