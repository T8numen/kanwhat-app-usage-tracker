#!/bin/bash

echo "Stopping Gradle daemons..."
./gradlew --stop

echo "Cleaning build..."
./gradlew clean

echo "Building debug APK..."
./gradlew assembleDebug --stacktrace

echo "Build complete!"
echo "APK location: app/build/outputs/apk/debug/app-debug.apk"

