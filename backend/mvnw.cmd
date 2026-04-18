@REM ----------------------------------------------------------------------------
@REM Maven Wrapper Script para Windows
@REM Uso: mvnw.cmd [argumentos-maven]
@REM Exemplo: mvnw.cmd spring-boot:run
@REM ----------------------------------------------------------------------------
@REM Se o Maven não estiver instalado localmente, este script baixa
@REM a versão correta automaticamente para %USERPROFILE%\.m2\wrapper\
@REM ----------------------------------------------------------------------------
@echo off
setlocal

set MAVEN_PROJECTBASEDIR=%~dp0
set MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR:~0,-1%

set WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar
set WRAPPER_PROPERTIES=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.properties
set "WRAPPER_URL=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"

@REM Baixa o wrapper jar se não existir
if not exist "%WRAPPER_JAR%" (
  for /f "tokens=2 delims==" %%a in ('findstr /i "wrapperUrl" "%WRAPPER_PROPERTIES%"') do set WRAPPER_URL=%%a
  echo Baixando Maven Wrapper de: %WRAPPER_URL%
  powershell -Command "Invoke-WebRequest -Uri '%WRAPPER_URL%' -OutFile '%WRAPPER_JAR%'" 2>nul
  if not exist "%WRAPPER_JAR%" (
    echo ERRO: Nao foi possivel baixar o Maven Wrapper. Verifique sua conexao.
    exit /b 1
  )
)

java -classpath "%WRAPPER_JAR%" "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" org.apache.maven.wrapper.MavenWrapperMain %*

if %ERRORLEVEL% neq 0 exit /b %ERRORLEVEL%
