# Altair: A mobile application for cellular network coverage assessment through mobile crowdsensing

## 📌 Description

Altair is a mobile application for cellular network coverage assessment through mobile crowdsensing. The software collects radio key performance indicators (KPIs), including **RSRP**, **RSRQ**, and **RSSI**, together with GPS location, timestamp, network technology, operator information, and device metadata.

Altair supports local CSV storage, Firebase Firestore synchronization, map visualization, CSV export, and external KPI analysis using Python. It is intended for academic, experimental, and exploratory studies of cellular network coverage using commercial smartphones.

---

## 🚀 Quick Start

### 📥 Install APK

1. Clone the repository:

```bash
git clone https://github.com/your-user/Altair.git
```

2. Navigate to the APK folder:

```text
Altair/app/release/
```

3. Transfer the APK file to your smartphone.

4. Install and open the application.

5. Grant the required permissions when prompted.

---

## 🛠 Build from Source

1. Open **Android Studio**.

2. Import the Altair project.

3. Let Gradle synchronize the dependencies.

4. Connect a compatible smartphone via USB.

5. Run the application from Android Studio.

---

## 📖 How to Use

1. Open Altair.

2. Grant location and phone state permissions.

3. Start a measurement campaign.

4. Move through the area of interest while the app records samples.

5. Visualize collected points on the map.

6. Export the CSV file or synchronize measurements with Firebase Firestore.

7. Process exported data using the scripts in the `analysis/` folder.

Detailed instructions are available in:

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
    └── kpi_comparison_scripts.py     # KPI processing scripts
```

---

## 📡 Main Features

* Collection of RSRP, RSRQ, and RSSI.
* GPS-based georeferencing of samples.
* Local storage in CSV format.
* Cloud synchronization using Firebase Firestore.
* Map visualization of collected measurements.
* Filtering by network technology and operator.
* Basic statistics and exploratory visualization.
* CSV export for external analysis.
* Python scripts for KPI post-processing.

---

## 📊 Data

Example measurement data are available in:

```text
data/sample_measurements.csv
```

Each sample may include timestamp, latitude, longitude, network technology, operator, RSRP, RSRQ, RSSI, device metadata, battery status, and other technical fields depending on device support.

---

## 🧪 Validation

Altair was evaluated through a controlled LTE measurement campaign comparing commercial smartphone measurements against the professional benchmarking system **Anite NEMO Invex**.

The comparison focused on the spatial coherence of **RSRP**, **RSRQ**, and **RSSI** across multiple measurement points.

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

