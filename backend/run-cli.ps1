# PowerShell runner for Supify Java Backend CLI
Write-Host "Compiling Supify Java Backend..." -ForegroundColor Cyan
$javaFiles = (Get-ChildItem -Path "src/main/java" -Recurse -Filter "*.java").FullName
if (!(Test-Path "bin")) { New-Item -ItemType Directory -Path "bin" | Out-Null }
javac -d bin $javaFiles

if ($LASTEXITCODE -eq 0) {
    Write-Host "Compilation successful! Starting Supify CLI..." -ForegroundColor Green
    java -cp bin com.supify.SupifyApplication
} else {
    Write-Host "Compilation failed with exit code $LASTEXITCODE" -ForegroundColor Red
}
