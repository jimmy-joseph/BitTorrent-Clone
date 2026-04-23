@echo off
setlocal

if "%1"=="" (
    echo Usage: run.bat ^<peer_id^>
    echo Example: run.bat 1001
    exit /b 1
)

set SCRIPT_DIR=%~dp0
set REPO_ROOT=%SCRIPT_DIR%..
set BUILD_DIR=%REPO_ROOT%\build\classes

if not exist "%BUILD_DIR%" mkdir "%BUILD_DIR%"

echo Compiling Java sources...
pushd "%REPO_ROOT%"
javac -d build\classes src\*.java
if errorlevel 1 (
    popd
    echo Compilation failed.
    exit /b 1
)

echo Running peerProcess with peer id %1...
java -cp "%BUILD_DIR%" peerProcess %1
popd

endlocal
