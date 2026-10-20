#!/bin/sh
# matchString 的 JS(Rhino) vs Java 对比基准
#
# 需要 rhino-runtime 的 jar。默认从 Gradle 缓存里找；找不到可以手动放一份到
# caaccel/test/rhino-runtime.jar。
set -e
cd "$(dirname "$0")"

if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/javac" ]; then
    JAVAC="$JAVA_HOME/bin/javac"; JAVA="$JAVA_HOME/bin/java"
else
    JAVAC=javac; JAVA=java
fi

RHINO="$(find "$HOME/.gradle/caches" -name 'rhino-runtime-1.7.15.1.jar' 2>/dev/null | head -1)"
[ -z "$RHINO" ] && RHINO="test/rhino-runtime.jar"
if [ ! -f "$RHINO" ]; then
    echo "找不到 rhino-runtime jar，请先跑一次 cadroid 的构建（会下载到 Gradle 缓存）" >&2
    exit 1
fi

./build.sh > /dev/null
rm -rf build/bench-classes && mkdir -p build/bench-classes
"$JAVAC" -encoding UTF-8 -cp "build/caaccel.jar:$RHINO" -d build/bench-classes bench/BenchMatchString.java
"$JAVA" -cp "build/caaccel.jar:$RHINO:build/bench-classes" BenchMatchString
