@echo off
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start.ps1" -Background
if errorlevel 1 pause
