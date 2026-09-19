#!/usr/bin/env bash
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR_PATH="$SCRIPT_DIR/target/input-mutator-1.0.0-SNAPSHOT.jar"

if [ ! -f "$JAR_PATH" ]; then
    echo "[Error] Target JAR not found at '$JAR_PATH'."
    echo "Please run 'mvn clean package' first."
    exit 1
fi

JAVA_CMD="java"
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA_CMD="$JAVA_HOME/bin/java"
fi

if ! command -v "$JAVA_CMD" >/dev/null 2>&1; then
    echo "[Error] Java runtime not found. Please install Java 21+ or set JAVA_HOME."
    exit 1
fi

"$JAVA_CMD" -jar "$JAR_PATH" "$@"
