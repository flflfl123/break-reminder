@rem
@rem Gradle启动脚本 for Windows
@rem

@if "%DEBUG%"=="" @echo off

@rem 设置本地环境变量
set DEFAULT_JAVA_OPTS="-Xmx2048m" "-Dfile.encoding=UTF-8"
set JAVA_OPTS=%JAVA_OPTS% %DEFAULT_JAVA_OPTS%

@rem 查找gradle-wrapper.jar
set CLASSPATH=%~dp0gradle\wrapper\gradle-wrapper.jar

@rem 执行Gradle Wrapper
"%JAVA_HOME%\bin\java.exe" %JAVA_OPTS% -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
