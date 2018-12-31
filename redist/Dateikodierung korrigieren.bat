@echo off
powershell -Command "Get-Content "%~1" | Set-Content -Encoding utf8 "%~1.new""
del /f "%~1"
move "%~1.new" "%~1"