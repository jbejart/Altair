# Altair: A mobile application for cellular network coverage assessment through mobile crowdsensing

## 📌 Description

**Altair** is an open-source mobile application designed for cellular network coverage assessment through **mobile crowdsensing**. The software collects radio key performance indicators (KPIs), including **Reference Signal Received Power (RSRP)**, **Reference Signal Received Quality (RSRQ)**, and **Received Signal Strength Indicator (RSSI)**, together with GPS location, timestamp, network technology, operator information, and device metadata.

Altair supports local CSV storage, cloud synchronization using Firebase Firestore, in-app map visualization, and external post-processing of exported measurements. It is intended for academic, experimental, and exploratory studies of cellular network coverage using commercial smartphones.

The current implementation is deployed as a mobile application using native device APIs for cellular and location data acquisition.

---

## 🚀 Quick Start

### 📥 Install APK

1. Clone the repository:

```bash
git clone https://github.com/your-user/Altair.git
```

2. Navigate to the APK release folder:

```text
Altair/app/release/
```

3. Transfer the APK file to your smartphone.

4. Install and open the application.

5. Grant the required permissions when prompted, such as location access and phone state access.

---

## 🛠 Build from Source

1. Open **Android Studio**.

2. Import the Altair project.

3. Let Gradle synchronize the project dependencies.

4. Connect a compatible smartphone via USB.

5. Use the **Run** option in Android Studio to deploy and launch the application.

Typical requirements include:

* Android Studio
* Gradle
* Android SDK
* A smartphone with cellular and location capabilities
* Google Play Services, when required by the location provider
* Firebase project configuration, only if cloud synchronization is used

---

## 📖 Usage

1. Open Altair on the smartphone.

2. Grant the required permissions.

3. Start a measurement campaign from the main interface.

4. Move through the area of interest while the application records samples.

5. Visualize the collected points on the map.

6. Export the local CSV file or synchronize measurements with Firebase Firestore.

7. Use the scripts in the `analysis/` folder for external KPI processing and comparison.

For detailed instructions, please refer to:

```text
docs/user_manual.md
```

---

## 📂 Directory Structure

```text
Altair/
├── README.md                         # Project documentation
├── LICENSE.txt                       # Project license
├── CITATION.cff                      # Citation metadata
├── app/                              # Main application source code
├── gradle/                           # Gradle wrapper files
├── build.gradle.kts                  # Project build configuration
├── settings.gradle.kts               # Gradle settings
├── docs/                             # Documentation
│   ├── installation.md               # Installation guide
│   ├── user_manual.md                # User manual
│   └── screenshots/                  # Application screenshots
├── data/                             # Example datasets
│   └── sample_measurements.csv       # Sample exported measurements
└── analysis/                         # External analysis scripts
    └── kpi_comparison_scripts.py     # KPI processing and comparison scripts
```

---

## 📡 Main Features

* Cellular coverage measurement using commercial smartphones.
* Collection of RSRP, RSRQ, and RSSI.
* GPS-based georeferencing of measurement samples.
* Timestamped records for field measurement campaigns.
* Local storage in CSV format.
* Cloud synchronization using Firebase Firestore.
* Map visualization of collected measurements.
* Visualization of imported measurement datasets.
* Filtering by network technology and operator.
* Basic statistics and exploratory visualization.
* CSV export for external analysis.
* Python scripts for KPI post-processing and comparison.

---

## 🧩 Software Architecture

Altair follows a modular workflow that separates:

* user interface and visualization,
* cellular and location data acquisition,
* measurement processing and validation,
* continuous foreground measurement services,
* local CSV storage,
* cloud synchronization,
* data export,
* and external post-processing.

The measurement workflow includes:

1. Acquisition of cellular and location information from the mobile device.
2. Extraction of radio KPIs such as RSRP, RSRQ, and RSSI.
3. Association of each sample with GPS location, timestamp, operator, network technology, and device metadata.
4. Validation and packaging of measurement samples.
5. Local storage in CSV format or synchronization with Firebase Firestore.
6. Visualization inside the application and external analysis using Python.

---

## 📊 Data Format

Each measurement sample may include the following fields, depending on device support and network technology:

* Timestamp
* Latitude and longitude
* Location accuracy
* Speed
* Network technology
* Operator
* RSRP
* RSRQ
* RSSI
* Cell identity information
* Neighboring cell information, when available
* Device brand and model
* Operating system version
* Battery percentage
* Charging status
* RAM usage

Some fields may not be available on all devices because reported cellular parameters depend on hardware, chipset, firmware, operating system version, and API support.

---

## 💾 Local Storage

Altair can store measurements locally in CSV format. The default local output file is:

```text
track_local.csv
```

This mode is useful for offline campaigns, field measurements without connectivity, and direct export for external processing.

---

## 🔥 Firebase Synchronization

When connectivity is available, enriched measurement records can be synchronized with Firebase Firestore. The default Firestore collection used by the application is:

```text
measurements
```

To use cloud synchronization, a Firebase project must be configured and the corresponding configuration file must be added to the mobile application project.

> **Important:** Do not upload private Firebase credentials, service account files, or sensitive configuration files to the public repository.

---

## 📁 Example Dataset

A sample dataset is available in:

```text
data/sample_measurements.csv
```

This file illustrates the expected structure of exported measurement records and can be used to test the analysis scripts.

---

## 📈 Data Analysis

The `analysis/` folder contains scripts for processing exported measurements. These scripts can be used to group samples by measurement point, time interval, or device, and to compare KPIs such as RSRP, RSRQ, and RSSI.

Example script:

```text
analysis/kpi_comparison_scripts.py
```

---

## 🧪 Validation

Altair was evaluated through a controlled LTE measurement campaign comparing measurements obtained from commercial smartphones against the professional benchmarking system **Anite NEMO Invex**.

The comparison focused on the spatial coherence of RSRP, RSRQ, and RSSI measurements across multiple measurement points.

Altair is not intended to replace professional drive-test systems. Instead, it provides a complementary, scalable, and low-cost tool for academic and experimental cellular coverage studies.

---

## 🔒 Privacy and Data Handling

Altair is designed to collect technical measurement data for cellular coverage analysis. The application does not require personal information to perform measurement campaigns.

Before sharing datasets publicly, unnecessary identifiers should be removed or anonymized. Shared datasets should retain only the technical information required for traceability, reproducibility, and KPI analysis.

---

## 🗺️ Roadmap

Planned improvements include:

* Extension of measurement campaigns to additional operators and locations.
* Additional support for imported datasets and comparative visualization.
* Improvement of post-processing scripts for multi-device and multi-operator analysis.
* Expansion of validation campaigns in LTE and 5G scenarios.
* Refinement of documentation and installation guides.

---

## 🤝 Contributing

Contributions, suggestions, and bug reports are welcome. To contribute, please open an issue or submit a pull request.

For academic collaboration or questions, please contact the project maintainer.

---

## 📜 License

This project is distributed under the **GNU General Public License v3.0 (GPL-3.0)**.

See:

```text
LICENSE.txt
```

---

## 📚 Citation

If you use Altair in academic work, please cite the software using the metadata provided in:

```text
CITATION.cff
```

---

## 👥 Authors and Acknowledgment

Developed at the **Universidad Nacional de San Agustin de Arequipa**, Peru.

Main contact:

```text
abenaventev@unsa.edu.pe
```

---

## 📌 Project Status

Altair is under active academic development. The current version is intended for controlled measurement campaigns, exploratory cellular coverage studies, and validation against professional benchmarking equipment.
