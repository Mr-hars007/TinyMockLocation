#!/usr/bin/env bash
# install.sh - Build and install Tiny Mock Location onto connected device
# Author: Mr-hars007

set -e

echo "=== Tiny Mock Location Installer ==="

# 1. Check ADB
if ! command -v adb >/dev/null 2>&1; then
    echo "Error: adb command not found in PATH."
    exit 1
fi

# 2. Check Device Connection
echo "Checking connected ADB devices..."
DEVICES=$(adb devices | grep -v "List of devices" | grep "device$" || true)
if [ -z "$DEVICES" ]; then
    echo "Error: No ADB device connected or authorized."
    exit 1
fi
echo "Device detected: $DEVICES"

# 3. Build APK
echo "Building optimized APK..."
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew assembleRelease

# 4. Install APK
APK_PATH="app/build/outputs/apk/release/app-release.apk"
if [ ! -f "$APK_PATH" ]; then
    echo "Error: APK not found at $APK_PATH"
    exit 1
fi

echo "Installing $APK_PATH to device..."
adb install -r "$APK_PATH"

# 5. Verify Installation & Launch
echo "Verifying package installation..."
adb shell pm list packages | grep com.mrhars007.mocklocation || true

echo "Launching application..."
adb shell am start -n com.mrhars007.mocklocation/.MainActivity

echo ""
echo "======================================================================"
echo " SUCCESS! Tiny Mock Location installed and launched."
echo "======================================================================"
echo ""
echo "SETUP INSTRUCTION (Required on first use):"
echo "  1. On your Android tablet, open Settings -> System -> Developer Options."
echo "  2. Scroll to 'Select mock location app'."
echo "  3. Select 'Tiny Mock Location'."
echo ""
echo "AVAILABLE ADB COMMANDS:"
echo "  # Start mock location with coordinates:"
echo "  adb shell am start -n com.mrhars007.mocklocation/.MainActivity --es action \"start\" --es lat \"0.000000\" --es lon \"0.000000\""
echo ""
echo "  # Set / update coordinates while running:"
echo "  adb shell am start -n com.mrhars007.mocklocation/.MainActivity --es action \"set\" --es lat \"0.000000\" --es lon \"0.000000\""
echo ""
echo "  # Stop mock location:"
echo "  adb shell am start -n com.mrhars007.mocklocation/.MainActivity --es action \"stop\""
echo ""
echo "CONVENIENCE SCRIPT USAGE:"
echo "  ./mock-location.sh start <lat> <lon>"
echo "  ./mock-location.sh set <lat> <lon>"
echo "  ./mock-location.sh stop"
echo "  ./mock-location.sh status"
echo "======================================================================"
