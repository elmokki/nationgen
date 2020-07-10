@echo off
setlocal enabledelayedexpansion

call release %1 %1 windows %2
if %errorlevel% neq 0 exit /b %errorlevel%

if exist "local/linux-java-home.txt" (
	set /p LINUXJAVAHOME=<local/linux-java-home.txt
	call release %1 "!LINUXJAVAHOME!" linux %2
) else (
	echo No local java home for linux configured. To create a linux runtime image, create 'local/linux-java-home.txt' containing the path to the linux jdk home.
)
if %errorlevel% neq 0 exit /b %errorlevel%

if exist "local/osx-java-home.txt" (
	set /p OSXJAVAHOME=<local/osx-java-home.txt
	call release %1 "!OSXJAVAHOME!" osx %2
) else (
	echo No local java home for osx configured. To create an osx runtime image, create 'local/linux-java-home.txt' containing the path to the osx jdk home.
)
if %errorlevel% neq 0 exit /b %errorlevel%