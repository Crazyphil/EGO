@echo off
set OUT_DIR=%1
if "%OUT_DIR%" == "" set OUT_DIR="./graph-ch/"
del /f /s /q %OUT_DIR%
call download.bat
java -Xms256m -Xmx1024m -server -cp "jar/graphhopper-web-0.7.0-with-dep.jar;jar/graphhopper-tools-0.7.0.jar" com.graphhopper.tools.Import config=config.properties graph.location="%OUT_DIR%" osmreader.osm="austria-latest.osm.pbf"