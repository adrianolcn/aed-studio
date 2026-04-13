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

@REM Verifica se Java está disponível
if not "%JAVA_HOME%"=="" goto OkJHome
for %%i in (java.exe) do set "JAVACMD=%%~$PATH:i"
if not "%JAVACMD%"=="" goto OkJava
echo ERRO: variavel JAVA_HOME nao esta definida e 'java' nao foi encontrado no PATH.
echo Instale o Java 17+ e tente novamente.
exit /b 1
:OkJHome
set "JAVACMD=%JAVA_HOME%\bin\java.exe"
if not exist "%JAVACMD%" (
  echo ERRO: JAVA_HOME='%JAVA_HOME%', mas %JAVACMD% nao foi encontrado.
  exit /b 1
)
:OkJava

set MAVEN_PROJECTBASEDIR=%~dp0

set WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar
set WRAPPER_PROPERTIES=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.properties

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

"%JAVACMD%" ^
  -classpath "%WRAPPER_JAR%" ^
  "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" ^
  org.apache.maven.wrapper.MavenWrapperMain %*

if %ERRORLEVEL% neq 0 exit /b %ERRORLEVEL%
