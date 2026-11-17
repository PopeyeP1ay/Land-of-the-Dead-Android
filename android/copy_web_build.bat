@echo off
setlocal
set "SRC=E:\LOTDAndroid\lotd-webport\build_web\out"
set "DST=%~dp0app\src\main\assets\www"
if not exist "%SRC%\index.html" (
    echo ERROR: Cannot find %SRC%\index.html
    exit /b 1
)
for %%F in (index.html index.js index.wasm game.js game.data vnet.js) do (
    if not exist "%SRC%\%%F" (
        echo ERROR: Missing %%F in %SRC%
        exit /b 1
    )
)
if not exist "%DST%" mkdir "%DST%"
robocopy "%SRC%" "%DST%" index.html index.js index.wasm game.js game.data vnet.js /R:1 /W:1
if errorlevel 8 exit /b 1
echo.
echo Copied six files into the local Android APK assets.
echo The source build must be rebuilt BEFORE copying when the mobile controls change.
exit /b 0
