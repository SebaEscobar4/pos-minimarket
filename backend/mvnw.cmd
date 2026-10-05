@echo off
setlocal

set "BASE_DIR=%~dp0"
set "MAVEN_VERSION=3.9.16"
set "ARCHIVE_NAME=apache-maven-%MAVEN_VERSION%-bin.zip"
set "DIST_ROOT=%BASE_DIR%.mvn\wrapper\dists\apache-maven-%MAVEN_VERSION%"
set "MAVEN_HOME=%DIST_ROOT%\apache-maven-%MAVEN_VERSION%"
set "ARCHIVE_PATH=%DIST_ROOT%\%ARCHIVE_NAME%"
set "DOWNLOAD_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/%ARCHIVE_NAME%"
set "EXPECTED_SHA512=ed41650d42485cfc243fad22158caf9cbb5dc408ce7a09ddb94dd42a019de929ca43065bfa450612cf12bf78b5cafa3884b96c090de326ff590448c933454af3"

if exist "%MAVEN_HOME%\bin\mvn.cmd" goto run_maven

if not exist "%DIST_ROOT%" mkdir "%DIST_ROOT%"

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference = 'Stop';" ^
  "if (-not (Test-Path '%ARCHIVE_PATH%')) { Invoke-WebRequest -UseBasicParsing -Uri '%DOWNLOAD_URL%' -OutFile '%ARCHIVE_PATH%' };" ^
  "$actual = (Get-FileHash -Algorithm SHA512 '%ARCHIVE_PATH%').Hash.ToLowerInvariant();" ^
  "if ($actual -ne '%EXPECTED_SHA512%') { throw 'La descarga de Maven no coincide con el SHA-512 oficial.' };" ^
  "Expand-Archive -Path '%ARCHIVE_PATH%' -DestinationPath '%DIST_ROOT%' -Force"

if errorlevel 1 exit /b 1

:run_maven
call "%MAVEN_HOME%\bin\mvn.cmd" %*
exit /b %errorlevel%
