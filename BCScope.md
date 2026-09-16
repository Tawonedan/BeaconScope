# BLE Beacon Logger & Trilateration Testing App — PRD

## Product Name

**BeaconScope** (working title)

---

# 1. Overview

## Purpose

BeaconScope is an Android application designed for logging, monitoring, filtering, and testing BLE beacon signals in real time. The application focuses on improving RSSI stability for trilateration experiments by providing multiple switchable signal filtering methods and detailed diagnostic logging.

The application is intended primarily for:

* BLE beacon experimentation
* indoor positioning system research
* trilateration debugging
* RSSI signal analysis
* filter performance comparison

---

# 2. Goals

## Primary Goals

1. Scan and monitor three BLE beacons simultaneously.
2. Visualize live RSSI signal behavior using real-time graphs.
3. Compare multiple filtering methods interactively.
4. Provide detailed logging for debugging signal instability.
5. Supply filtered signal data to trilateration calculations.
6. Help identify the most stable filtering approach for BLE positioning.

---

# 3. Non-Goals

The MVP will NOT include:

* background service operation
* cloud synchronization
* multi-floor mapping
* production-grade indoor navigation
* beacon provisioning/configuration
* advanced analytics dashboard
* iOS support
* machine learning prediction

---

# 4. Target Users

## Primary Users

* Informatics students
* embedded systems developers
* IoT researchers
* BLE experimentation teams
* indoor positioning researchers

---

# 5. Platform & Tech Stack

## Platform

* Android only

## Tech Stack

* Language: Kotlin
* UI Framework: Jetpack Compose
* Architecture: MVVM
* BLE Communication: Android BLE APIs
* Charts/Graphs:

  * MPAndroidChart (Compose wrapper)
    OR
  * Compose Canvas custom graph

## State Management

* StateFlow
* ViewModel

## Dependency Injection (Optional)

* Hilt

---

# 6. Core Features

---

# 6.1 BLE Beacon Scanner

## Description

The app scans nearby BLE beacon devices and continuously retrieves RSSI values.

## Functional Requirements

* Detect BLE advertisements.
* Identify specific beacon MAC addresses.
* Support exactly 3 monitored beacons for MVP.
* Show beacon connection/visibility status.

## Displayed Information

* Beacon name
* MAC address
* RSSI
* estimated distance
* last seen timestamp

---

# 6.2 Real-Time RSSI Graph

## Description

Each beacon has its own live signal graph.

## Functional Requirements

* Real-time graph updates.
* Separate graph per beacon.
* Smooth scrolling timeline.
* Graph displays:

  * raw RSSI
  * filtered RSSI

## Graph Behavior

* X-axis = time
* Y-axis = RSSI value

## UI Requirements

* Responsive updates
* Stable rendering
* No major UI lag

---

# 6.3 Expandable Log Terminal

## Description

Each beacon panel includes an expandable terminal-like logging section.

## Functional Requirements

* Expand/collapse button.
* Auto-scroll newest logs.
* Preserve previous logs during session.
* Timestamped entries.

## Example Logs

```text
[12:04:21] RSSI: -71
[12:04:21] EMA: -69.4
[12:04:22] Distance: 2.34m
[12:04:22] Beacon lost
```

## Log Categories

* RSSI updates
* filter outputs
* beacon detection/loss
* trilateration results
* errors/warnings

---

# 6.4 Filter System

## Description

Users can switch signal filtering algorithms dynamically.

## Supported Filters

### A. Kalman Filter

Purpose:

* Reduce signal variance.
* Smooth noisy RSSI data.

Adjustable Parameters:

* process noise
* measurement noise

---

### B. Exponential Moving Average (EMA)

Purpose:

* Lightweight smoothing.
* Baseline stability filter.

Adjustable Parameters:

* alpha coefficient

---

### C. Particle Filter

Purpose:

* Experimental probabilistic filtering.
* Advanced state estimation.

Adjustable Parameters:

* particle count
* noise spread

---

## Functional Requirements

* Filter dropdown selector.
* Real-time filter switching.
* Filtering applied without restarting scan.
* Per-beacon filter support (optional MVP+).

---

# 6.5 Trilateration Engine

## Description

Calculate approximate device position based on filtered beacon distances.

## Inputs

* filtered RSSI values
* beacon coordinates

## Outputs

* estimated X/Y position
* confidence score (optional)

## Distance Estimation Formula

```text
distance = 10 ^ ((txPower - RSSI) / (10 * n))
```

Where:

* txPower = calibrated RSSI at 1 meter
* n = environmental attenuation factor

---

# 6.6 Position Visualization (Optional MVP+)

## Description

Simple 2D visualization of trilateration output.

## Functional Requirements

* show beacon positions
* show calculated device position
* update in real time

---

# 7. UI/UX Requirements

---

# 7.1 Main Screen Layout

## Top App Bar

Contains:

* app title
* scan toggle button
* filter dropdown

---

## Main Content

### Beacon Card Components

Each beacon card contains:

#### Header Section

* beacon label
* connection status
* current RSSI
* estimated distance

#### Graph Section

* live RSSI graph

#### Controls Section

* filter indicator
* statistics

#### Expandable Log Section

* terminal-like view
* expandable/collapsible behavior

---

# 7.2 Visual Design

## Style

* technical monitoring dashboard
* minimal modern UI
* dark mode preferred

## Colors

Suggested:

* green = stable
* yellow = weak
* red = lost/disconnected

---

# 8. Architecture

## Recommended Architecture

MVVM

### Layers

#### UI Layer

* Compose Screens
* Components
* State collectors

#### Domain Layer

* filtering logic
* trilateration calculations

#### Data Layer

* BLE scanning
* beacon repository
* logging repository

---

# 9. Data Models

## BeaconSignal

```kotlin
data class BeaconSignal(
    val macAddress: String,
    val beaconName: String,
    val rawRssi: Int,
    val filteredRssi: Float,
    val estimatedDistance: Float,
    val timestamp: Long
)
```

---

## LogEntry

```kotlin
data class LogEntry(
    val timestamp: Long,
    val beaconId: String,
    val message: String,
    val type: LogType
)
```

---

# 10. Performance Requirements

## Functional Performance

* Graph update interval < 250ms
* BLE scan stability during long sessions
* Smooth scrolling logs
* Minimal dropped frames

## Technical Performance

* Support Android 9+
* Memory efficient logging
* No ANR during continuous scanning

---

# 11. Risks & Technical Challenges

## RSSI Instability

BLE RSSI fluctuates heavily due to:

* reflections
* obstacles
* human body interference
* antenna orientation

Mitigation:

* filtering
* rolling average
* outlier rejection

---

## Android BLE Limitations

Potential issues:

* scan throttling
* manufacturer differences
* permission handling

Mitigation:

* foreground scanning
* proper scan settings
* tested device list

---

# 12. Future Enhancements

## Possible Extensions

* recording/exporting sessions
* CSV log export
* heatmap visualization
* beacon calibration mode
* custom filter plugins
* floor map overlay
* websocket streaming
* multi-device comparison

---

# 13. Success Metrics

## MVP Success Criteria

* Stable simultaneous detection of 3 beacons
* Real-time graph rendering
* Dynamic filter switching works correctly
* Trilateration output updates continuously
* Logs accurately capture signal changes

---

# 14. Development Roadmap

## Phase 1 — BLE Foundation

* BLE scanning
* beacon identification
* RSSI retrieval

---

## Phase 2 — Visualization

* real-time graphs
* signal dashboard
* logging terminal

---

## Phase 3 — Filtering

* EMA implementation
* Kalman implementation
* Particle filter implementation

---

## Phase 4 — Trilateration

* distance estimation
* position calculations
* coordinate visualization

---

# 15. Recommended Libraries

## BLE

* Android BLE Scanner API

## Charts

Possible options:

* MPAndroidChart
* Vico Charts
* Compose Canvas

## Logging

* Timber

## Math/Filtering

* Apache Commons Math (optional)

---

# 16. Open Questions

1. Will all beacons use identical Tx power?
2. Will beacon coordinates be static or configurable?
3. Is trilateration 2D only or future 3D support needed?
4. Should filters apply globally or per beacon?
5. Should raw RSSI also remain visible during filtering?
