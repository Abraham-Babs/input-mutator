@echo off
set "JAVA_BIN=C:\Program Files\Eclipse Adoptium\jdk-25.0.1.8-hotspot\bin\javaw.exe"
if exist "%JAVA_BIN%" (
    start "" "%JAVA_BIN%" -jar "%~dp0target\input-mutator-1.0.0-SNAPSHOT.jar"
) else (
    start "" javaw -jar "%~dp0target\input-mutator-1.0.0-SNAPSHOT.jar"
)
