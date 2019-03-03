
set IMAGE=target\%2

RMDIR /S /Q "%IMAGE%" 2>nul

"%~1\bin\jlink" --no-header-files --no-man-pages --compress=2 --strip-debug --module-path "%~1\jmods";./bin --add-modules java.base,java.desktop --output %IMAGE%

if %errorlevel% neq 0 exit /b %errorlevel%

xcopy target\%2.jar "%~dp0%IMAGE%\lib\"

xcopy data "%~dp0%IMAGE%\data\" /i /s /e
xcopy db "%~dp0%IMAGE%\db\" /i /s /e
xcopy db_conversion "%~dp0%IMAGE%\db_conversion\" /i /s /e
xcopy documentation "%~dp0%IMAGE%\documentation\" /i /s /e
xcopy graphics "%~dp0%IMAGE%\graphics\" /i /s /e
xcopy changelog.txt "%~dp0%IMAGE%\"
xcopy forbidden_ids.txt "%~dp0%IMAGE%\"
xcopy settings.txt "%~dp0%IMAGE%\"

echo @start bin\javaw -cp lib\%2.jar nationGen.GUI.GUI > ./%IMAGE%/NationGen.bat
echo @start bin\javaw -cp lib\%2.jar nationGen.GUI.SpriteGen > ./%IMAGE%/SpriteGen.bat