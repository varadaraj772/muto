# Muto 🔕

> **Automated, context-aware ringer modes for Android based on device placement and screen state.**

Muto is an open-source, ultra-lightweight Android service written in Kotlin. It continuously evaluates device telemetry (accelerometer, proximity sensor, screen power states) to switch your phone between **Silent**, **Vibrate**, and **Ring** modes depending on where your phone is physically located.

No cloud sync, no tracking, zero third-party dependencies, and minimal APK footprint.

---

## 🎯 The Problem

Modern phones blast audio alerts while you are actively holding them, stay silent while sitting in another room on a desk, or ring loudly inside bags during quiet meetings. Setting manual schedules or toggling Do Not Disturb repeatedly is cumbersome.

Muto automates this by treating **Physical Placement + Screen State** as the source of truth.

---

## ⚡ The 6 Context Scenarios

Muto categorizes phone states into a 6-scenario matrix. You can independently assign **Silent**, **Vibrate**, or **Ring** to each:

| # | Placement | Screen State | Real-World Context | Default Behavior |
|:---:|:---|:---:|:---|:---:|
| **1** | **On Table** | **Screen OFF** | Phone resting flat on a desk, locked and unattended. | **Ring** (hear alerts from across the room) |
| **2** | **On Table** | **Screen ON** | Resting flat on a table with screen awake (AOD, watching videos). | **Vibrate** |
| **3** | **In Hand** | **Screen OFF** | Carrying the phone by your side while walking or holding it locked. | **Vibrate** |
| **4** | **In Hand** | **Screen ON** | Actively typing, browsing, or gaming on the device. | **Silent** (no jarring speaker blasts in your face) |
| **5** | **In Pocket** | **Screen OFF** | Tucked inside pants pocket, jacket, or closed backpack. | **Ring** |
| **6** | **In Pocket** | **Screen ON** | Inside pocket with accidental touch / pocket wake. | **Silent** |

---

## 🛠 How It Works

* **Gravity & Tilt Vectoring:** Low-pass filter ($a = 0.8$) isolates gravitational acceleration from transient shakes. Uses angular tilt hysteresis ($< 14^\circ$ to enter flat, $> 22^\circ$ to exit) to differentiate tables from hands.
* **Proximity Guard:** Proximity sensor checks for covered states (`NEAR` vs `FAR`).
* **Display Telemetry:** Dynamically detects hardware screen states (`PowerManager.isInteractive`).
* **Noise-Debounce Engine:** Sensor state changes are debounced (400ms) to ignore sudden walking jerks and table vibrations while remaining responsive.
* **Native Audio Control:** Adjusts system ringer states directly via `AudioManager` without altering complex Do Not Disturb (DND) filter policies.

---

## 📱 Permissions Required

| Permission | Why It's Needed |
|:---|:---|
| `ACCESS_NOTIFICATION_POLICY` | Required by Android to toggle system ringer modes (`SILENT`, `VIBRATE`, `NORMAL`). |
| `MODIFY_AUDIO_SETTINGS` | Adjusts notification and ringer audio stream volume levels. |
| `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_SPECIAL_USE` | Keeps sensor monitoring persistent across app switches on Android 14+ (API 34+). |
| `RECEIVE_BOOT_COMPLETED` | Restores your configured service automatically when the phone reboots. |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Prevents Android's Doze mode from killing sensor monitoring. |

> **Note on OEM Devices (Xiaomi, Realme, Oppo, Vivo, OnePlus, Samsung):**  
> Aggressive OEM battery killers can freeze background services. Ensure Muto is set to **"No restrictions" / "Unrestricted"** in Battery Settings and has **Auto-start** enabled.

---

## 🚀 Building from Source

### Prerequisites
* Android Studio Ladybug / Meerkat or later
* Android SDK 35 (Target SDK 35, Min SDK 26)
* JDK 17+

### Steps

1. **Clone the repository:**
   ```bash
   git clone https://github.com/varadaraj772/muto.git
   cd muto
   ```

2. **Build the debug APK:**
   ```bash
   ./gradlew assembleDebug
   ```

3. **Install on connected device:**
   ```bash
   ./gradlew installDebug
   ```

4. **Inspect real-time state logs:**
   ```bash
   adb logcat -s Muto
   ```

---

## 📄 License

This project is licensed under the Apache License 2.0.