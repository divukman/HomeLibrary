#!/bin/bash

# Home Library Application Launcher for Linux/Mac

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "Error: Java is not installed or not in PATH"
    echo "Please install Java 17 or higher"
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | awk -F '.' '{print $1}')
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "Error: Java 17 or higher is required"
    echo "Current version: $(java -version 2>&1 | head -n 1)"
    exit 1
fi

echo "Using Java version: $(java -version 2>&1 | head -n 1)"

# Set paths
JAR_FILE="target/home-library-1.0.0.jar"
LIB_DIR="target/lib"

# Check if JAR exists, if not try to build
if [ ! -f "$JAR_FILE" ]; then
    echo "JAR file not found. Attempting to build..."
    if ! command -v mvn &> /dev/null; then
        echo "Maven not found. Please run: mvn clean package"
        exit 1
    fi
    mvn clean package -DskipTests
    if [ $? -ne 0 ]; then
        echo "Build failed!"
        exit 1
    fi
fi

# Check if lib directory exists
if [ ! -d "$LIB_DIR" ]; then
    echo "Dependencies not found: $LIB_DIR"
    echo "Please run: mvn clean package"
    exit 1
fi

# Build classpath with all dependencies
CLASSPATH="$JAR_FILE"
for jar in "$LIB_DIR"/*.jar; do
    CLASSPATH="$CLASSPATH:$jar"
done

# Run the application
echo "Starting Home Library Application..."
java -cp "$CLASSPATH" com.homelibrary.Launcher
