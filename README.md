# Tiny Mock Location

An ultra-lightweight, high-performance, privacy-focused mock location application engineered specifically for Android 9 (API Level 28) low-end and Android Go devices (e.g. 1 GB RAM MediaTek hardware).

Created by [@Mr-hars007](https://github.com/Mr-hars007).

---

## Table of Contents

1. [Key Features](#key-features)
2. [Architecture & Performance](#architecture--performance)
3. [Privacy & Security](#privacy--security)
4. [First-Time Setup (Developer Options)](#first-time-setup-developer-options)
5. [Installation & Build](#installation--build)
6. [GUI Usage Guide](#gui-usage-guide)
7. [ADB & CLI Control](#adb--cli-control)
8. [Shared Map Pin Integration](#shared-map-pin-integration)
9. [Battery Optimization](#battery-optimization)
10. [Troubleshooting](#troubleshooting)
11. [Legal Notice & Disclaimers](#legal-notice--disclaimers)

---

## Key Features

- **Ultra-Compact (~42 KB APK)**: Built purely with native Android framework APIs. Zero external dependencies, no proprietary background services, and single-dex R8 optimization.
- **Space Mission Adaptive Icon**: Scaled to Android's 66 dp keyline safe zone. Seamlessly adapts to all adaptive icon shapes (circle/sphere, squircle, rounded square, teardrop) without mask clipping.
- **Dynamic USB & ADB Detection**: Real-time broadcast tracking of USB physical connection and ADB developer setting state.
- **High-Accuracy Map Link Share Target**: Share any dropped pin from map apps directly to Tiny Mock Location. Prioritizes exact coordinate marker parameters (`!3d<lat>!4d<lon>`) over viewport approximations.
- **Plus Codes Support**: Full support for Open Location Codes (e.g., `7X7X7X7X+7X`) alongside standard decimal Latitude and Longitude.
- **Named Favorites (Up to 5)**: Save frequent locations with custom labels (e.g., "Home", "Office", "Gym"), rename anytime with inline dialogs, and switch locations in one tap.
- **High-Contrast Calm Theme**: Soft, light-themed aesthetic (`#F7F8FA` background) with high-contrast, actionable button states (sage teal Start / warm coral red Stop).
- **Foreground Service**: Persistent background mock provider registration preventing Android Go low-memory killer terminations.
- **Stationary Optimization**: Does not perform busy loops or redundant calculations while stationary.

---

## Architecture & Performance

```text
               ┌──────────────────────────────┐
               │         MainActivity         │
               │   (GUI / Intent Handler)     │
               └──────────────┬───────────────┘
                              │ Starts / Controls
                              ▼
               ┌──────────────────────────────┐
               │     MockLocationService      │
               │ (Foreground Service Handler) │
               └──────────────┬───────────────┘
                              │ Registers & Injects
                              ▼
               ┌──────────────────────────────┐
               │    Android LocationManager   │
               │ (GPS & Network Test Providers│
               └──────────────┬───────────────┘
                              │ Broadcasts to OS
                              ▼
               ┌──────────────────────────────┐
               │    Android System Framework  │
               │ (All Location-Requesting Apps│
               └──────────────────────────────┘
```

- **Memory Footprint**: ~10–15 MB RAM in active background state.
- **CPU Usage**: Near 0.0% while stationary (updates system elapsed time without recalculating).
- **Storage**: < 50 KB on device filesystem.

---

## Privacy & Security

- **Zero Telemetry**: No analytics, crash reporters, tracking SDKs, or background telemetry.
- **Zero External APIs**: No third-party mapping or proprietary lookup APIs. Coordinate and Plus Code conversions are computed locally and offline.
- **No PII / SPII**: No personal identifiable information is stored or collected.
- **No Root Required**: Operates legitimately within standard Android `LocationManager` test provider APIs.

---

## First-Time Setup (Developer Options)

Before Android allows any application to mock GPS locations, you must authorize it:

1. On your Android tablet/device, open **Settings** -> **System** -> **Developer Options**.
   *(If Developer Options is hidden, go to **Settings** -> **About Tablet** and tap **Build Number** 7 times).*
2. Scroll down to the **Debugging** section.
3. Tap **Select mock location app**.
4. Choose **Tiny Mock Location**.

---

## Installation & Build

### Prerequisites

- Android SDK installed with platform `android-28` and build-tools.
- OpenJDK 21 or compatible JDK.
- ADB connected device.

### One-Step Automated Installation

```bash
./install.sh
```

The script verifies ADB connectivity, builds the release APK via Gradle wrapper (`assembleRelease`), installs it to the connected device, and launches `MainActivity`.

### Manual Build Commands

```bash
# Build optimized release APK (~42 KB)
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew assembleRelease

# Install onto connected device
adb install -r app/build/outputs/apk/release/app-release.apk

# Launch application
adb shell am start -n com.mrhars007.mocklocation/.MainActivity
```

---

## GUI Usage Guide

1. **Enter Coordinates or Plus Code**:
   - **Plus Code**: Enter a code like `7X7X7X7X+7X` and tap **Decode**.
   - **Latitude & Longitude**: Enter coordinates into the respective fields (e.g. `12.971600` and `77.594600` or paste comma-separated `lat, lon`).
2. **Start Mocking**:
   - Tap **Start**. The status indicator turns green, the Stop button turns coral red, and a minimal foreground notification appears.
3. **Stop Mocking**:
   - Tap **Stop**. The test providers are deregistered, the service halts, and location control returns to standard system providers.
4. **Favorites Management**:
   - Tap **+ Save** to save the current coordinates. A dialog will prompt you to enter a friendly name.
   - Tap any saved favorite to load and start mocking immediately.
   - Tap the **✎** icon or long-press a favorite to rename it.
   - Tap the **✕** icon to remove it.

---

## ADB & CLI Control

The application can be controlled entirely over USB using ADB without touching the device screen.

### Using the `mock-location.sh` Convenience Script

```bash
# Start mock location with Plus Code
./mock-location.sh start 7X7X7X7X+7X

# Start mock location with Latitude and Longitude
./mock-location.sh start <latitude> <longitude>

# Update location while running (Plus Code)
./mock-location.sh set 7X7X7X7X+7X

# Update location while running (Lat/Lon)
./mock-location.sh set <latitude> <longitude>

# Stop mock location service
./mock-location.sh stop

# Check background service status
./mock-location.sh status
```

### Direct ADB Intent Commands

```bash
# Start with Plus Code
adb shell am start \
  -n com.mrhars007.mocklocation/.MainActivity \
  --es action "start" \
  --es code "7X7X7X7X+7X"

# Start with Latitude and Longitude
adb shell am start \
  -n com.mrhars007.mocklocation/.MainActivity \
  --es action "start" \
  --es lat "<latitude>" \
  --es lon "<longitude>"

# Update coordinates
adb shell am start \
  -n com.mrhars007.mocklocation/.MainActivity \
  --es action "set" \
  --es lat "<latitude>" \
  --es lon "<longitude>"

# Stop service
adb shell am start \
  -n com.mrhars007.mocklocation/.MainActivity \
  --es action "stop"
```

---

## Shared Map Pin Integration

You can send locations directly from map applications to Tiny Mock Location:

1. Open your map app and select any place or drop a pin.
2. Tap the **Share** button.
3. Select **Tiny Mock Location** from the share sheet.
4. The application parses the exact marker coordinates (`!3d!4d` or query params) to guarantee pinpoint accuracy, populates the coordinate fields, and updates the mock location.

---

## Battery Optimization

On Android 9 and Android Go devices, background processes may be restricted by battery saving algorithms.

- The application automatically checks if it is exempt from battery optimizations.
- If not exempt, a **"Disable Battery Optimization"** button appears in the UI.
- Tapping this button opens the system exemption request dialog.

---

## Troubleshooting

| Issue | Cause | Solution |
| :--- | :--- | :--- |
| **"Mock provider unavailable"** | App is not selected as mock provider in Developer Options. | Go to Settings -> Developer Options -> Select mock location app -> Tiny Mock Location. |
| **"USB: Disconnected"** | Tablet is running on battery or USB data cable is disconnected. | Reconnect USB data cable to PC. |
| **"Invalid coordinates"** | Latitude or Longitude outside valid decimal boundaries. | Ensure Lat is within `[-90, 90]` and Lon is within `[-180, 180]`. |
| **Service killed after screen off** | Device battery manager killed background service. | Tap "Disable Battery Optimization" in the app UI. |
| **"Activity not started..." ADB message** | Standard Android ADB notice for singleTop activities. | The intent was successfully delivered to the running instance via `onNewIntent`. |

---

## Legal Notice & Disclaimers

### 1. Intended Purpose & Developer Testing
This software is an open-source development utility engineered **strictly for software testing, quality assurance (QA), educational research, and legitimate location-based application debugging**. It utilizes official, documented Android SDK developer APIs (`android.location.LocationManager` test provider interface) authorized exclusively through user-enabled Android Developer Options.

### 2. Limitation of Liability & Warranty Disclaimer
- **"AS IS" Software**: This software is provided "AS IS" and "AS AVAILABLE", without warranties of any kind, either express or implied, including but not limited to the implied warranties of merchantability, fitness for a particular purpose, and non-infringement.
- **No Liability**: In no event shall the author ([@Mr-hars007](https://github.com/Mr-hars007)), contributors, or copyright holders be liable for any claim, damages, loss of data, hardware malfunction, account action, or other liability, whether in an action of contract, tort, or otherwise, arising from, out of, or in connection with the software or the use or other dealings in the software.

### 3. User Responsibility & Compliance with Third-Party Terms
- The end user is solely responsible for ensuring that their use of this software complies with all applicable local, state, national, and international laws, regulations, and third-party Terms of Service.
- The author does not endorse, encourage, or support the unauthorized manipulation of location data to violate terms of service of third-party platforms, financial services, gaming systems, or safety-critical applications.

### 4. Trademark Notice & Nominative Fair Use
- All product names, logos, and brands referenced in this repository are the property of their respective owners. Any reference to third-party platforms or formats is made strictly for nominative, descriptive identification of compatibility and interoperability under standard fair use principles.

### 5. Open Location Code Attribution
Open Location Code (Plus Codes) technology is an open standard released under the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0).

---

## License

MIT License. Copyright (c) 2026 Mr-hars007. See the [`LICENSE`](./LICENSE) file for complete licensing terms.
