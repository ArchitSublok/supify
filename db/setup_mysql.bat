@echo off
cd /d "%~dp0"
echo ==========================================================
echo   Supify MySQL 8.0 Setup (5 Basic Tables + Seeds)
echo ==========================================================

set "MYSQL_EXE="
where mysql >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    for /f "delims=" %%I in ('where mysql') do (
        set "MYSQL_EXE=%%I"
        goto :found_mysql
    )
)
if exist "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" (
    set "MYSQL_EXE=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
)

:found_mysql
if "%MYSQL_EXE%"=="" (
    echo [ERROR] mysql.exe not found in PATH or C:\Program Files\MySQL\MySQL Server 8.0\bin\
    pause
    exit /b 1
)

set "DB_USER=root"
set /p DB_USER="Enter MySQL username [root]: "

echo.
echo Creating 5 tables and loading sample supplier data...
"%MYSQL_EXE%" -u "%DB_USER%" -p -e "SOURCE schema/01_supify_schema.sql; SOURCE seeds/01_canonical_suppliers.sql;"

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] MySQL setup failed.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo [OK] Supify MySQL database 'supify' is ready!
pause
