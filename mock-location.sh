#!/usr/bin/env bash
# mock-location.sh - Control Tiny Mock Location via ADB (by @Mr-hars007)

PACKAGE="com.mrhars007.mocklocation"
ACTIVITY=".MainActivity"

usage() {
    echo "Usage:"
    echo "  $0 start <lat> <lon>      - Start mock location with lat/lon"
    echo "  $0 start <plus_code>      - Start mock location with Plus Code"
    echo "  $0 set <lat> <lon>        - Update coordinates with lat/lon"
    echo "  $0 set <plus_code>        - Update coordinates with Plus Code"
    echo "  $0 stop                   - Stop mock location service"
    echo "  $0 status                 - Check service and package status"
    exit 1
}

CMD="$1"

case "$CMD" in
    start)
        if [ -z "$2" ]; then
            echo "Error: Coordinates or Plus Code required."
            usage
        fi
        if [[ "$2" == *"+"* ]]; then
            echo "Starting mock location at Plus Code: $2..."
            adb shell am start -n "$PACKAGE/$ACTIVITY" --es action "start" --es code "$2"
        else
            if [ -z "$3" ]; then
                echo "Error: Both Latitude and Longitude are required."
                usage
            fi
            echo "Starting mock location at Lat: $2, Lon: $3..."
            adb shell am start -n "$PACKAGE/$ACTIVITY" --es action "start" --es lat "$2" --es lon "$3"
        fi
        ;;
    set)
        if [ -z "$2" ]; then
            echo "Error: Coordinates or Plus Code required."
            usage
        fi
        if [[ "$2" == *"+"* ]]; then
            echo "Setting mock location to Plus Code: $2..."
            adb shell am start -n "$PACKAGE/$ACTIVITY" --es action "set" --es code "$2"
        else
            if [ -z "$3" ]; then
                echo "Error: Both Latitude and Longitude are required."
                usage
            fi
            echo "Setting mock location to Lat: $2, Lon: $3..."
            adb shell am start -n "$PACKAGE/$ACTIVITY" --es action "set" --es lat "$2" --es lon "$3"
        fi
        ;;
    stop)
        echo "Stopping mock location..."
        adb shell am start -n "$PACKAGE/$ACTIVITY" --es action "stop"
        ;;
    status)
        echo "Checking device connection..."
        if ! adb get-state >/dev/null 2>&1; then
            echo "Device disconnected or ADB offline."
            exit 1
        fi
        echo "Device connected."
        echo "Checking if service is running..."
        SVC_OUT=$(adb shell dumpsys activity services | grep -i "MockLocationService")
        if [ -n "$SVC_OUT" ]; then
            echo "Service Status: RUNNING"
            echo "$SVC_OUT"
        else
            echo "Service Status: STOPPED / IDLE"
        fi
        ;;
    *)
        usage
        ;;
esac
