@echo off
set %JAVA_HOME%="%~dp0Java"
set PATH=%JAVA_HOME%\bin;%PATH%

set NAV_DIR="%~dp0\SD-Karte\ego\navigation"

pushd ".\GraphHopper\"
echo Aktualisiere Navigationsdaten...
call ".\start-0.7.bat" %NAV_DIR%
popd

goto:eof