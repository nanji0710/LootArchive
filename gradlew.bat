@rem
@rem Gradle startup script for Windows
@rem

@if "%DEBUG%"=="" @echo off
@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
@rem This is normally unused
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

@rem Resolve any "." and ".." in APP_HOME to make it shorter.
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

@rem Execute Gradle
if not defined JAVA_HOME (
    set JAVA_HOME=C:\Program Files\Amazon Corretto\jdk17.0.19_10
)
set JAVACMD=%JAVA_HOME%\bin\java.exe

if exist "%JAVACMD%" goto execute

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

:execute
@rem Setup the command line
set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"
set GRADLE_OPTS=%GRADLE_OPTS% -Dorg.gradle.appname=%APP_BASE_NAME%

"%JAVACMD%" ^
  %DEFAULT_JVM_OPTS% ^
  %JAVA_OPTS% ^
  %GRADLE_OPTS% ^
  "-Dorg.gradle.wrapper.properties=%APP_HOME%\gradle\wrapper\gradle-wrapper.properties" ^
  -classpath "%CLASSPATH%" ^
  org.gradle.wrapper.GradleWrapperMain ^
  %*
