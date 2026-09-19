@echo off
setlocal

set "JAR_PATH=%~dp0target\input-mutator-1.0.0-SNAPSHOT.jar"

if not exist "%JAR_PATH%" (
    echo [Error] Target JAR not found at "%JAR_PATH%".
    echo Please run 'mvn clean package' first.
    pause
    exit /b 1
)

if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\javaw.exe" (
        start "" "%JAVA_HOME%\bin\javaw.exe" -jar "%JAR_PATH%"
        exit /b 0
    )
    if exist "%JAVA_HOME%\bin\java.exe" (
        start "" "%JAVA_HOME%\bin\java.exe" -jar "%JAR_PATH%"
        exit /b 0
    )
)

rem Check Adoptium and JDK installation directories
for /d %%D in ("%ProgramFiles%\Eclipse Adoptium\jdk-*" "%ProgramFiles%\Java\jdk-*") do (
    if exist "%%D\bin\javaw.exe" (
        start "" "%%D\bin\javaw.exe" -jar "%JAR_PATH%"
        exit /b 0
    )
    if exist "%%D\bin\java.exe" (
        start "" "%%D\bin\java.exe" -jar "%JAR_PATH%"
        exit /b 0
    )
)

where javaw >nul 2>&1
if %errorlevel% equ 0 (
    start "" javaw -jar "%JAR_PATH%"
    exit /b 0
)

where java >nul 2>&1
if %errorlevel% equ 0 (
    start "" java -jar "%JAR_PATH%"
    exit /b 0
)

echo [Error] Java 21+ runtime not found. Please install Java 21+ or set JAVA_HOME.
pause
exit /b 1
