@echo off
setlocal enabledelayedexpansion

set JAVA_HOME=d:\jdk-17.0.19
set PATH=%JAVA_HOME%\bin;%PATH%

echo ========================================
echo [1/3] Build frontend (npm run build)
echo ========================================
cd frontend
call npm run build
if errorlevel 1 (
    echo Frontend build failed!
    exit /b 1
)
cd ..

echo ========================================
echo [2/3] Build backend (mvn clean package)
echo ========================================
call mvn clean package -DskipTests
if errorlevel 1 (
    echo Backend build failed!
    exit /b 1
)

echo ========================================
echo [3/3] Copy jar to current directory
echo ========================================
for %%f in (target\*.jar) do (
    echo %%~nxf | findstr /v /i "sources javadoc original" >nul
    if not errorlevel 1 (
        echo Copying %%f
        copy /Y "%%f" .
    )
)

echo ========================================
echo Build finished.
echo ========================================
