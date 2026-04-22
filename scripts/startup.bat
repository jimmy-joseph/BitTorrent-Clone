@echo off
set SCRIPT_DIR=%~dp0
set REPO_ROOT=%SCRIPT_DIR%..
set BUILD_DIR=%REPO_ROOT%\build\classes

if not exist "%BUILD_DIR%" mkdir "%BUILD_DIR%"
pushd "%REPO_ROOT%"
javac -d build\classes src\*.java
popd

start powershell.exe -NoExit -Command "Set-Location '%REPO_ROOT%'; java -cp '%BUILD_DIR%' peerProcess 1001"
start powershell.exe -NoExit -Command "Set-Location '%REPO_ROOT%'; java -cp '%BUILD_DIR%' peerProcess 1002"
start powershell.exe -NoExit -Command "Set-Location '%REPO_ROOT%'; java -cp '%BUILD_DIR%' peerProcess 1003"
start powershell.exe -NoExit -Command "Set-Location '%REPO_ROOT%'; java -cp '%BUILD_DIR%' peerProcess 1004"
start powershell.exe -NoExit -Command "Set-Location '%REPO_ROOT%'; java -cp '%BUILD_DIR%' peerProcess 1005"
start powershell.exe -NoExit -Command "Set-Location '%REPO_ROOT%'; java -cp '%BUILD_DIR%' peerProcess 1006"
