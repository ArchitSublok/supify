@echo off
cd /d "%~dp0"
chcp 65001 >nul
echo ==========================================================
echo   Compiling and Launching Supify Backend CLI System
echo ==========================================================
if not exist "bin" mkdir bin
javac -d bin -sourcepath src src\com\supify\app\Main.java src\com\supify\test\SupifyTestRunner.java src\com\supify\dao\JdbcSupplierDao.java
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed.
    pause
    exit /b %ERRORLEVEL%
)
java -cp "bin;lib\*" com.supify.app.Main
pause
