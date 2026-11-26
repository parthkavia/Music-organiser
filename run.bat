@echo off
rem Moodify launcher for Windows. Builds the app on first run, then starts it.
cd /d "%~dp0"

where java >nul 2>nul
if errorlevel 1 (
  echo Java 17 or newer is required. Install a JDK from https://adoptium.net and try again.
  pause
  exit /b 1
)

if not exist target\moodify.jar (
  where mvn >nul 2>nul
  if errorlevel 1 (
    echo Apache Maven is required to build Moodify. Install it from https://maven.apache.org and try again.
    pause
    exit /b 1
  )
  echo Building Moodify ^(first run only^)...
  call mvn -q -DskipTests package
  if errorlevel 1 (
    echo Build failed.
    pause
    exit /b 1
  )
)

start "" javaw -jar target\moodify.jar %*
