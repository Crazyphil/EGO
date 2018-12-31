REM This file will start the Mobile Atlas Creator with custom memory settings for
REM the JVM. With the below settings the heap size (Available memory for the application)
REM will range from 64 megabyte up to 4096 megabyte.

java -Xms64m -Xmx4096M -jar %~dp0\Mobile_Atlas_Creator.jar %*