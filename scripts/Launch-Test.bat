@echo off
setlocal
cd /d "%~dp0.."
set "STUDIO_JAVA=%LOCALAPPDATA%\RuneLite\jre\bin\java.exe"
if not exist "%STUDIO_JAVA%" set "STUDIO_JAVA=%JAVA_HOME%\bin\java.exe"
if not exist "%STUDIO_JAVA%" (
  echo Java not found. Install RuneLite or set JAVA_HOME to Java 11 or newer.
  pause
  exit /b 1
)
if not exist "build\libs\runelite-loadout-studio-0.9.1-rc.2-all.jar" (
  echo Build the test client first. See README.md.
  pause
  exit /b 1
)
"%STUDIO_JAVA%" -ea -jar "build\libs\runelite-loadout-studio-0.9.1-rc.2-all.jar" --developer-mode --debug
pause
