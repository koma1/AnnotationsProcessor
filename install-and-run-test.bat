:<<"::CMDLITERAL"
@ECHO OFF
GOTO :CMDSCRIPT
::CMDLITERAL
#linux commands
cd ./core
mvn install
cd ./../runner
mvn test
cd ..

:CMDSCRIPT
rem Windows commands:
cd ./core
cmd /c mvn install
cd ./../runner
cmd /c mvn test
cd ..
