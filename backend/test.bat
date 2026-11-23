@echo off
cd /d "%~dp0"
chcp 65001 >nul
echo ==========================================================
echo   Running Supify Backend Automated Verification Tests
echo ==========================================================
if not exist "bin" mkdir bin
javac -d bin -sourcepath src src\com\supify\test\SupifyTestRunner.java
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed.
    pause
    exit /b %ERRORLEVEL%
)
java -cp bin com.supify.test.SupifyTestRunner
pause
