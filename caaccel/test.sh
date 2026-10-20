#!/bin/sh
# 跑 caaccel 的单测
set -e
cd "$(dirname "$0")"
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/javac" ]; then
    JAVAC="$JAVA_HOME/bin/javac"; JAVA="$JAVA_HOME/bin/java"
else
    JAVAC=javac; JAVA=java
fi
./build.sh > /dev/null
rm -rf build/test-classes && mkdir -p build/test-classes
"$JAVAC" -encoding UTF-8 -cp build/caaccel.jar -d build/test-classes test/AccelTest.java
"$JAVA" -cp build/caaccel.jar:build/test-classes AccelTest
