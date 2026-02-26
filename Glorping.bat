@echo off
title Glorping - Cave Explorer
cd /d "%~dp0"

echo Building Glorping...
javac -cp "lib\handout\bin" -d bin -sourcepath src src\*.java src\entities\*.java src\physics\*.java src\projectiles\*.java src\sound\*.java src\spells\*.java src\ui\*.java src\utils\*.java src\world\*.java 2>nul

if errorlevel 1 (
    echo Build failed! Make sure Java JDK is installed.
    pause
    exit /b 1
)

echo Launching Glorping...
cd lib\handout
java -cp "../../bin;bin" App
