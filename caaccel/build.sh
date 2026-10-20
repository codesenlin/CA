#!/bin/sh
# 构建 caaccel.jar：纯 javac + jar，不依赖 Gradle/Android
set -e
cd "$(dirname "$0")"

# 找 JDK：优先 JAVA_HOME，其次 PATH
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/javac" ]; then
    JAVAC="$JAVA_HOME/bin/javac"
    JAR="$JAVA_HOME/bin/jar"
else
    JAVAC=javac
    JAR=jar
fi

rm -rf build/classes build/caaccel.jar
mkdir -p build/classes
find src -name '*.java' > build/sources.txt
"$JAVAC" -source 8 -target 8 -encoding UTF-8 -d build/classes @build/sources.txt
"$JAR" cf build/caaccel.jar -C build/classes .
echo "built: $(pwd)/build/caaccel.jar ($(stat -c%s build/caaccel.jar) 字节)"
