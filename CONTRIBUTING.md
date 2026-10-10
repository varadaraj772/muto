# Contributing to Muto

Thank you for your interest in contributing to Muto! Muto is an Android-only open-source project written entirely in idiomatic Kotlin.

---

## 🧭 Guiding Principles

1. **Zero Unnecessary Dependencies:** Keep the APK footprint minimal. Avoid heavy third-party UI libraries or dependency injection frameworks unless strictly justified.
2. **Battery Efficiency First:** Background sensors can drain batteries rapidly if misconfigured. All sensor listeners, sample rates, and debounces must balance instantaneous response with zero-idle CPU overhead.
3. **No Intrusive Permissions:** We strictly avoid requesting runtime permissions beyond what is functionally required for audio and foreground sensor evaluation.

---

## 🛠 Development Workflow

### 1. Fork & Branch
* Fork the repository on GitHub.
* Clone your fork locally:
  ```bash
  git clone https://github.com/<your-username>/muto.git
  cd muto
  ```
* Create a focused feature branch:
  ```bash
  git checkout -b feature/your-feature-name
  ```

### 2. Code Standards & Style
* **Language:** Kotlin 2.0+ targeting JVM 17.
* **Architecture:** 
  * `Placement.kt` — Enum definitions and scenario keys.
  * `ContextDetector.kt` — Sensor fusion and screen power receiver logic.
  * `RingerController.kt` — Native `AudioManager` wrapper.
  * `MutoService.kt` — Foreground lifecycle service and boot receiver.
  * `MainActivity.kt` — Programmatic, lightweight settings UI.
* Follow standard Android Kotlin Style Guides.
* Do not submit compiled artifacts (`.apk`, `.aar`, `.class`), IDE settings (`.idea/workspace.xml`), or `local.properties`.

### 3. Testing Changes
Before opening a pull request:
1. Verify clean compilation:
   ```bash
   ./gradlew clean compileDebugKotlin
   ```
2. Test on a physical Android device (emulators often fail to simulate proximity or realistic accelerometer noise):
   ```bash
   adb logcat -s Muto
   ```
3. Test edge-cases:
   * Screen transition (Locked vs Unlocked vs Ambient display).
   * Device restart (`adb shell am broadcast -a android.intent.action.BOOT_COMPLETED`).
   * Transitioning across all 6 scenarios in physical sequence.

### 4. Submitting a Pull Request
1. Commit with clear, descriptive messages:
   ```bash
   git commit -m "fix(detector): increase motion threshold to eliminate desk jitter"
   ```
2. Push your branch to GitHub:
   ```bash
   git push origin feature/your-feature-name
   ```
3. Open a Pull Request against the `main` branch.
4. Describe the specific problem your PR solves and provide logcat snippets or physical device test results.

---

## 🐛 Reporting Bugs

When filing an issue, please include:
* **Device Manufacturer & Model** (e.g., Nothing Phone (2), Pixel 8, Samsung S23).
* **Android OS Version** (e.g., Android 14, Android 15).
* **Logcat output** captured during the failure:
  ```bash
  adb logcat -s Muto
  ```
* Steps to reproduce the physical scenario (e.g., "Placed on wooden table face up, screen locked, but registered as IN_HAND").