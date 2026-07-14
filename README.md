# Altair: A mobile application for cellular network coverage assessment through mobile crowdsensing

Altair is an open-source Android application for cellular network coverage
assessment through mobile crowdsensing.

The application collects radio key performance indicators (KPIs), including
Reference Signal Received Power (RSRP), Reference Signal Received Quality
(RSRQ), and Received Signal Strength Indicator (RSSI), together with geographic
location, timestamp, network technology, operator information, cell information,
and technical device metadata.

The complete Android Studio project is located in the `src/` directory. The
processed smartphone measurements used in the controlled LTE validation
campaign are available in the `data/` directory.

## Main features

- Collection of LTE and supported 5G NR radio measurements.
- Collection of RSRP, RSRQ, RSSI, and other available radio parameters.
- GPS-based georeferencing of measurements.
- Periodic cellular measurement acquisition.
- Local storage and export in CSV format.
- Optional upload to Firebase Firestore.
- Map-based visualization of collected measurements.
- Measurement history and basic statistics.

## Repository structure

```text
Altair/
├── .gitignore
├── README.md
├── LICENSE.txt
├── src/
│   ├── app/
│   ├── gradle/
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   ├── gradle.properties
│   ├── gradlew
│   └── gradlew.bat
└── data/
    ├── README.md
    └── altair_processed_measurements.csv
```

## Requirements

- Android Studio
- Android SDK
- Gradle
- Google Play Services
- Android smartphone with cellular and location capabilities

The availability of individual radio parameters depends on the Android
version, chipset, modem, manufacturer, and device implementation.

## Opening the Android project

Clone the repository:

```bash
git clone https://github.com/jorgebej/Altair.git
cd Altair/src
```

Open the `src/` directory in Android Studio and allow Gradle to synchronize
the project dependencies.

Connect a compatible Android smartphone and run the application from Android
Studio.

On Windows, the Gradle project can be checked from the `src/` directory with:

```powershell
.\gradlew.bat tasks
```

A debug build can be generated with:

```powershell
.\gradlew.bat assembleDebug
```

A local Android SDK configuration must be available through `ANDROID_HOME` or
a local `src/local.properties` file.

## Firebase configuration

The repository does not include `google-services.json`.

Users who want to enable Firebase Authentication and Firestore functionality
must provide their own Firebase project configuration file at:

```text
src/app/google-services.json
```

This file is intentionally excluded from version control.

## Basic usage

1. Install and open Altair on a compatible Android smartphone.
2. Grant the requested location and phone permissions.
3. Start a measurement campaign.
4. Collect measurements along the selected route or measurement area.
5. Review the collected measurements and basic statistics.
6. Export the local CSV file for external analysis.
7. Stop the measurement service when the campaign is complete.

## Validation data

The processed smartphone measurements are available at:

```text
data/altair_processed_measurements.csv
```

The dataset contains measurements grouped by smartphone and acquisition
interval, including:

- Mean and standard deviation of RSRP.
- Mean and standard deviation of RSRQ.
- Mean and standard deviation of RSSI.
- Mean latitude and longitude.
- Anonymized device labels.
- Measurement time intervals.

The dataset contains processed and aggregated smartphone measurements. It is
not the original raw CSV exported directly by the Android application.

Additional information is available in
[`data/README.md`](data/README.md).

The repository does not include raw or processed measurements from the
Anite NEMO Invex reference system. No Python data-processing scripts are
published in this repository.

## Scientific use

Altair is intended for academic, experimental, and exploratory studies of
cellular network coverage using commercial Android smartphones.

Altair is not intended to replace calibrated professional drive-test
equipment. Measurements may vary between smartphones because of differences
in antennas, chipsets, modems, firmware, Android versions, and manufacturer
reporting mechanisms.

## Version

The software version associated with the article is:

```text
v1.0.0
```

## License

Altair is distributed under the
[MIT License](LICENSE.txt).

Copyright © 2026:

- Jairo Jorge Bejar Torreblanca
- Alexandra Judith Benavente Vera
- Alberth Ronal Tamo Calla

## Authors

- Jairo Jorge Bejar Torreblanca
- Alexandra Judith Benavente Vera
- Alberth Ronal Tamo Calla

## Contact

For questions related to the project:

```text
abenaventev@unsa.edu.pe
atamo@unsa.edu.pe
```