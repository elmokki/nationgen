
if "%3" == "" (
	echo No platform supplied! >&2
	exit /b 1
)

if "%4" == "" (
	echo No image name supplied! >&2
	exit /b 1
)

set IMAGE=target\runtime-images\%3\%4

RMDIR /S /Q "%IMAGE%" 2>nul

"%~1\bin\jlink" --no-header-files --no-man-pages --compress=2 --strip-debug --module-path "%~2\jmods" --add-modules java.base,java.desktop --output %IMAGE%

if %errorlevel% neq 0 exit /b %errorlevel%

xcopy target\%4.jar "%~dp0%IMAGE%\lib\"

xcopy data "%~dp0%IMAGE%\data\" /i /s /e
xcopy db "%~dp0%IMAGE%\db\" /i /s /e
xcopy db_conversion "%~dp0%IMAGE%\db_conversion\" /i /s /e
xcopy documentation "%~dp0%IMAGE%\documentation\" /i /s /e
xcopy graphics "%~dp0%IMAGE%\graphics\" /i /s /e
xcopy changelog.txt "%~dp0%IMAGE%\"
xcopy forbidden_ids.txt "%~dp0%IMAGE%\"
xcopy settings.txt "%~dp0%IMAGE%\"

goto %3
echo Unknown platform name %3! >&2
exit /b 1

:windows

echo @start bin\javaw -cp lib\%4.jar nationGen.GUI.GUI > ./%IMAGE%/NationGen.bat
echo @start bin\javaw -cp lib\%4.jar nationGen.GUI.SpriteGen > ./%IMAGE%/SpriteGen.bat

goto done

:linux
:osx

echo #!/bin/sh > ./%IMAGE%/NationGen
echo DIR=`dirname $0` >> ./%IMAGE%/NationGen
echo $DIR/bin/java -cp lib/%4.jar nationGen.GUI.GUI >> ./%IMAGE%/NationGen

echo #!/bin/sh > ./%IMAGE%/SpriteGen
echo DIR=`dirname $0` >> ./%IMAGE%/SpriteGen
echo $DIR/bin/java -cp lib/%4.jar nationGen.GUI.SpriteGen >> ./%IMAGE%/SpriteGen

:done