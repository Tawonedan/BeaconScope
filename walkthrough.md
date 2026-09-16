# Developer Walkthrough & Architecture Guide: BeaconScope

Welcome to the comprehensive developer walkthrough for **BeaconScope**! The application is fully structured, coded, and ready to be compiled in Android Studio. 

This guide describes the structural blueprint, the mathematical algorithms, the simulation details, and how you can run and calibrate your indoor positioning filters.

---

## 1. Directory & File Mapping

Here is the location and role of every class created for your application under `d:\Neo_Newie\Projects\BeaconScope`:

```text
BeaconScope/
│
├── settings.gradle.kts          # Top-level module settings
├── build.gradle.kts             # Root plugin declarations
├── gradle.properties            # JVM & AndroidX parameters
├── local.properties             # SDK paths (pre-set to your SDK)
│
└── app/
    ├── build.gradle.kts         # Jetpack Compose and material3 configurations
    ├── src/main/
    │   ├── AndroidManifest.xml  # Requests Bluetooth scan/connect & Fine Location
    │   ├── res/values/strings.xml # String resources
    │   └── java/com/orion/beaconscope/
    │       │
    │       ├── MainActivity.kt  # System entry point with runtime permissions
    │       │
    │       ├── data/
    │       │   ├── model/
    │       │   │   ├── BeaconConfig.kt        # Static label, grid (x,y), txPower config
    │       │   │   ├── BeaconSignal.kt        # Scanned & smoothed signal telemetries
    │       │   │   ├── RawBeaconReading.kt    # Scanned BLE packets
    │       │   │   └── LogEntry.kt            # Diagnostic terminal items
    │       │   └── repository/
    │       │       ├── BleScannerRepository.kt # Android BLE scan + High-Fidelity Simulator
    │       │       └── LogRepository.kt        # Circular in-memory logging cache
    │       │
    │       ├── domain/
    │       │   ├── filter/
    │       │   │   ├── SignalFilter.kt        # Filtering boundary contract
    │       │   │   ├── EmaFilter.kt           # Low-pass Exponential smoothing
    │       │   │   ├── KalmanFilter.kt        # Optimal Gaussian state estimation
    │       │   │   └── ParticleFilter.kt      # Sequential Monte Carlo particle cloud
    │       │   └── trilateration/
    │       │       ├── MathUtils.kt           # Path-loss math & direct 2x2 matrix solver
    │       │       └── TrilaterationEngine.kt  # Least-squares trilateration solver
    │       │
    │       └── ui/
    │           ├── MainViewModel.kt           # Binds scanner, filters, logs & trilateration
    │           ├── MainScreen.kt              # Top bars, sliders, custom canvases grid
    │           ├── theme/
    │           │   ├── Color.kt               # Technical dark neon hex values
    │           │   ├── Type.kt                # Typography parameters
    │           │   └── Theme.kt               # DarkColorScheme configuration
    │           └── components/
    │               ├── LiveRssiGraph.kt       # High-performance custom Canvas scrolling chart
    │               ├── LogTerminal.kt         # Custom monospace diagnostic scrolling panel
    │               ├── RadarPositionView.kt   # Concentric sweeps radar Canvas drawing
    │               └── BeaconCard.kt          # Collapsible card containing graphs and terminal
```

---

## 2. High-Fidelity Signal Simulation

To enable immediate testing and filter comparisons without needing physical BLE beacons, we built a **Dynamic Telemetry Simulator** inside `BleScannerRepository`:

1.  **Simulated Trajectory**: It models a simulated user moving along a continuous, dynamic path (figure-8 / Lissajous trajectory) in a $6\text{m} \times 6\text{m}$ grid room.
2.  **True Distance Calculations**: For every iteration, it computes the exact geometric Euclidean distance from the moving user to the coordinates of the 3 configured beacons.
3.  **Path Loss RSSI Conversion**: It converts the true distance to an ideal RSSI using the inverse Log-Distance Path Loss model:
    $$\text{RSSI}_{\text{ideal}} = \text{TxPower} - 10 \cdot n \cdot \log_{10}(d)$$
4.  **Gaussian Shadowing Noise**: To mimic real-world indoor interference (reflections, absorption, scattering), it injects random Gaussian noise with a standard deviation of $\sigma = 4.5\text{ dBm}$:
    $$\text{RSSI}_{\text{noisy}} = \text{RSSI}_{\text{ideal}} + \mathcal{N}(0, 4.5^2)$$
5.  **Telemetry Flow**: These noisy values are packaged as standard BLE advertisements and emitted through the `SharedFlow` pipeline.

When you switch **"SIGNAL MODE"** to **"SIMULATOR"** on the dashboard, the entire filtering and trilateration pipeline processes these noisy signals! The user node on your radar will move along a smooth path based on your filter smoothing settings.

---

## 3. Telemetry Visualizer details

To keep the application highly responsive, memory-efficient, and free from external library compile conflicts, we avoided complex third-party plotting APIs and implemented custom drawings:

*   **`LiveRssiGraph`**: Built using a custom `Canvas` drawing frame, it scales the raw history (transparent red line) and the filtered history (solid cyan line) in real time over a custom telemetry grid.
*   **`RadarPositionView`**: Plots coordinates using a custom canvas coordinate system mapper, rendering standard axes, concentric rings, and static nodes. It also draws **dotted geometric circles around the beacons matching their estimated distance equations**. This visually displays how the three circles intersect at the estimated user location!

---

## 4. How to Compile and Run

1.  Open Android Studio.
2.  Select **Open Project** and navigate to `d:\Neo_Newie\Projects\BeaconScope`.
3.  Ensure your SDK coordinates in `local.properties` are recognized (pre-configured for standard Windows pathing).
4.  Sync Gradle. All dependencies (Compose BOM, Activity Compose, Material3, Coroutines) are pre-loaded using standard versioning.
5.  Run on an Emulator or physical phone!
6.  **Simulation Mode**: Toggle "SIGNAL MODE" to "SIMULATOR" and press **"TELEMETRY ON"** at the top right to watch the real-time filter smoothing and coordinate matrix solving in action!
