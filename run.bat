@echo off

if "%1"=="" (
    echo Please enter a peer id.
    echo Example: run.bat 1001
    goto end
)

echo Compiling Java files...
javac *.java

if errorlevel 1 (
    echo Compilation failed.
    goto end
)

echo Running peerProcess with peer id %1...
java peerProcess %1

:end
