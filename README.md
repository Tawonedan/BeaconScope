# BeaconScope 📡

> **Real-Time BLE Beacon Signal Filtering, Diagnostic Logging & Trilateration Indoor Positioning System**

BeaconScope is an Android application built with **Kotlin** and **Jetpack Compose** designed for real-time monitoring, filtering, and positioning analysis using Bluetooth Low Energy (BLE) beacons. It provides an interactive testing workbench for comparing signal smoothing filters (EMA, Kalman, Particle Filter) and observing their impact on 2D least-squares trilateration in real time.

---

## 🚀 Key Features

- **Dual Operation Modes**:
  - **Live BLE Scanner**: Scans and parses physical BLE beacon advertisements using Android BLE APIs.
  - **High-Fidelity Telemetry Simulator**: Built-in Lissajous trajectory simulator injecting Log-Distance Path Loss and Gaussian shadowing noise ($\sigma = 4.5\text{ dBm}$) for testing without physical hardware.
- **Dynamic Signal Filtering Comparison**:
  - **Raw**: Unfiltered RSSI data.
  - **EMA (Exponential Moving Average)**: Low-pass filter with tunable smoothing factor ($\alpha$).
  - **1D Kalman Filter**: Optimal recursive Gaussian state estimator with adjustable process noise ($Q$) and measurement noise ($R$).
  - **Particle Filter (Sequential Monte Carlo)**: Non-linear/non-Gaussian probability distribution tracking with resampled particle clouds.
- **Real-Time Visualizations**:
  - **Live RSSI Graph**: Custom Compose `Canvas` rendering raw vs. filtered signal trends.
  - **Radar Position View**: 2D coordinate space mapping beacon coordinates, estimated distance intersection circles, and the calculated position.
- **Embedded Diagnostic Terminal**:
  - In-memory circular log buffer displaying timestamped RSSI variances, filter latencies, and matrix solution statuses.

---

## 📋 Prerequisites

Before running the project, make sure you have:

1. **Android Studio**: Android Studio Hedgehog (2023.1.1) or newer (e.g., Iguana, Jellyfish, Koala, Ladybug).
2. **Java Development Kit (JDK)**: **JDK 17** (configured as Gradle JDK).
3. **Android SDK**:
   - `compileSdk`: **34** (Android 14)
   - `minSdk`: **26** (Android 8.0 Oreo or higher)
4. **Testing Device**:
   - **Physical Android Device**: Recommended for real BLE beacon scanning (requires Bluetooth LE support).
   - **Android Emulator**: Supported for running **Simulator Mode** (BLE scanning requires a physical device).

---

## 🛠️ Getting Started & How to Run

### Method 1: Using Android Studio (Recommended)

1. **Open the Project**:
   - Launch Android Studio.
   - Click **File > Open...** and select the `BeaconScope` directory.

2. **Configure SDK & JDK**:
   - Ensure `local.properties` contains your Android SDK path (e.g., `sdk.dir=C\:\\Users\\<YourUsername>\\AppData\\Local\\Android\\Sdk`).
   - Verify Gradle JDK is set to **Java 17** under **Settings / Preferences > Build, Execution, Deployment > Build Tools > Gradle**.

3. **Sync Gradle**:
   - Click **Sync Project with Gradle Files** (elephant icon in the toolbar).

4. **Run the App**:
   - Connect your Android device via USB (with **USB Debugging** enabled) or start an emulator.
   - Select the `app` run configuration and click **Run** (green play button) or press `Shift + F10`.

---

### Method 2: Using the Command Line (Gradle Wrapper)

Open PowerShell or your terminal in the root directory of the project:

#### Build Debug APK:
- **Windows (PowerShell/CMD)**:
  ```powershell
  .\gradlew.bat assembleDebug
  ```
- **Linux / macOS**:
  ```bash
  chmod +x gradlew
  ./gradlew assembleDebug
  ```

The generated APK will be located at:
```text
app/build/outputs/apk/debug/app-debug.apk
```

#### Install Directly to Connected Device:
```powershell
.\gradlew.bat installDebug
```
*Or install via ADB:*
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 📱 How to Use the App

1. **Permissions**:
   - Upon first launch, grant the required permissions: **Bluetooth Scan**, **Bluetooth Connect**, and **Fine Location** (mandatory for Android BLE discovery).
2. **Select Mode**:
   - Toggle **SIGNAL MODE** between **LIVE SCANNER** (for real beacons) and **SIMULATOR** (virtual user moving in a 6m x 6m room).
3. **Start Telemetry**:
   - Tap **START TELEMETRY** in the top bar to begin receiving packets and streaming updates.
4. **Compare Filters**:
   - Switch between **Raw**, **EMA**, **Kalman**, and **Particle** to immediately see noise suppression in the live graph and coordinate stability on the radar map.
5. **Inspect Diagnostics**:
   - Expand the diagnostic terminal at the bottom to inspect raw/filtered RSSI telemetry, computed distances, and trilateration residuals.

---

## 📁 Project Architecture

```text
com.orion.beaconscope/
├── data/
│   ├── model/           # BeaconConfig, BeaconSignal, RawBeaconReading, LogEntry
│   └── repository/      # BleScannerRepository (BLE + Simulator), LogRepository
├── domain/
│   ├── filter/          # SignalFilter contract, EmaFilter, KalmanFilter, ParticleFilter
│   └── trilateration/   # MathUtils (path-loss model), TrilaterationEngine
└── ui/
    ├── components/      # LiveRssiGraph, RadarPositionView, LogTerminal, BeaconCard
    ├── theme/           # Color palette, Material3 Theme, Typography
    ├── MainScreen.kt    # Main dashboard UI & controls
    └── MainViewModel.kt # StateFlow telemetry pipeline & event handling
```

---

## 🧪 Tech Stack & Dependencies

- **Language**: Kotlin 1.9.22
- **UI Framework**: Jetpack Compose (BOM 2024.01.00) + Material 3
- **Architecture**: MVVM with Kotlin Coroutines & `StateFlow`
- **Target Platform**: Android API 26+ (compiled against API 34)
- **Math & Solvers**: Custom 2D matrix solver & inverse log-distance path loss equations
