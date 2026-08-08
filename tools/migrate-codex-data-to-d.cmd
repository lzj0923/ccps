@echo off
setlocal
title Move Codex data and developer caches to D drive
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0migrate-codex-data-to-d.ps1"
echo.
pause
endlocal
