#!/bin/bash

# Home Library Manager - Run Script

echo "Starting Home Library Manager..."

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "Error: Maven is not installed or not in PATH"
    echo "Please install Maven from https://maven.apache.org/"
    exit 1
fi

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

# Build if necessary
if [ ! -f "target/home-library-1.0.0.jar" ]; then
    echo "Building application..."
    mvn clean package -DskipTests
fi

# Run the application
echo "Launching application..."
mvn javafx:run
