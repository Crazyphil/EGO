@echo off
set %JAVA_HOME%="%~dp0Java"
set PATH=%JAVA_HOME%\bin;%PATH%

set WORKING_DIR=".\atlas"
set MAPS_DIR="%~dp0SD-Karte\ego\karten"

pushd ".\MOBAC\"
echo Aktualisiere Straßenkarte (1/4)...
call:createAtlas "Basemap RO-UU-L"
echo Aktualisiere hochauflösende Straßenkarte (2/4)...
call:createAtlas  "Basemap RO-UU-L HD"
echo Aktualisiere Satellitenbild (3/4)...
call:createAtlas  "Orthofoto RO-UU-L"
echo Aktualisiere hochauflösendes Satellitenbild (4/4)...
call:createAtlas  "Orthofoto RO-UU-L HD"
popd

goto:eof

:createAtlas
call ".\start.bat" create "%~1" %WORKING_DIR%
if ERRORLEVEL 0 call:moveAtlas
goto:eof

:moveAtlas
move "%WORKING_DIR%\*.sqlite" %MAPS_DIR%
rmdir %WORKING_DIR%
goto:eof