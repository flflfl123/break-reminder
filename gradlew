#!/bin/sh
# Gradle启动脚本 for Linux/Mac

# 设置JAVA_HOME（如果未设置）
if [ -z "$JAVA_HOME" ]; then
    echo "错误：请设置JAVA_HOME环境变量"
    exit 1
fi

# 设置JVM参数
DEFAULT_JAVA_OPTS="-Xmx2048m -Dfile.encoding=UTF-8"
JAVA_OPTS="${JAVA_OPTS} ${DEFAULT_JAVA_OPTS}"

# 查找gradle-wrapper.jar
APP_HOME=$(cd "$(dirname "$0")" && pwd)
CLASSPATH="${APP_HOME}/gradle/wrapper/gradle-wrapper.jar"

# 执行Gradle Wrapper
exec "$JAVA_HOME/bin/java" $JAVA_OPTS -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
