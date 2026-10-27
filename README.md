# Altair: A mobile application for cellular network coverage assessment through mobile crowdsensing

Altair is an open-source Android application for cellular network coverage assessment through mobile crowdsensing.

The application collects radio key performance indicators (KPIs), including Reference Signal Received Power (RSRP), Reference Signal Received Quality (RSRQ), and Received Signal Strength Indicator (RSSI), together with geographic location, timestamp, network technology, operator information, cell information, and technical device metadata.

The complete Android Studio project is located in the `src/` directory. The processed smartphone and NEMO reference measurements, together with the analysis notebook used for the controlled LTE validation campaign, are available in the `data/` directory.

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
    ├── nemo_measurements_aligned.csv
    ├── Altair_measurements_aligned.csv
    └── FILTERING.ipynb
```

## Requirements

- Android Studio
- Android SDK
- Gradle
- Google Play Services
- Android smartphone with cellular and location capabilities

The availability of individual radio parameters depends on the Android version, chipset, modem, manufacturer, and device implementation.

## Opening the Android project

Clone the repository:

```bash
git clone https://github.com/jbejart/Altair.git
cd Altair/src
```

Open the `src/` directory in Android Studio and allow Gradle to synchronize the project dependencies.

Connect a compatible Android smartphone and run the application from Android Studio.

On Windows, the Gradle project can be checked from the `src/` directory with:

```powershell
.\gradlew.bat tasks
```

A debug build can be generated with:

```powershell
.\gradlew.bat assembleDebug
```

A local Android SDK configuration must be available through `ANDROID_HOME` or a local `src/local.properties` file.

## Firebase configuration

The repository does not include `google-services.json`.

Users who want to enable optional Firebase Firestore uploads must provide their own Firebase project configuration file at:

```text
src/app/google-services.json
```

This file is intentionally excluded from version control.

Altair does not require user registration or authentication and does not store names, email addresses, phone numbers, or other direct user identifiers.

## Measurement record format

Version v1.1.0 uses a shared schema of 90 top-level fields for new LTE and 5G NR measurement records. The local CSV contains the same field names and values as the document prepared for Firestore. Unavailable or technology-specific values are empty in CSV and `null` in Firestore. The `neighbors` field is stored as JSON in CSV and as an array of maps in Firestore.

Existing local CSV files are migrated to the shared header, retaining their recorded values and leaving newly introduced fields empty. Previously stored Firestore documents are not modified. The processed validation datasets in `data/` have their own aggregated format and are not expected to contain these 90 columns.

## Basic usage

1. Install and open Altair on a compatible Android smartphone.
2. Grant the requested location and phone permissions.
3. Start a measurement campaign.
4. Collect measurements along the selected route or measurement area.
5. Review the collected measurements and basic statistics.
6. Export the local CSV file for external analysis.
7. Stop the measurement service when the campaign is complete.

## Validation data

The processed measurements and analysis scripts used for the validation campaign are available in the `data/` directory:

- `Altair_measurements_aligned.csv`: Processed smartphone crowdsensing measurements.
- `nemo_measurements_aligned.csv`: Processed reference measurements from the Anite NEMO Invex system.
- `FILTERING.ipynb`: Jupyter Notebook containing the data filtering steps, the statistical evaluation (Bias, MAE, RMSE, r), and the generation of comparative KPI plots (RSRP, RSRQ, RSSI) between the Altair-enabled smartphones and the NEMO reference system.

The CSV datasets contain measurements grouped by device and acquisition interval, including:

- Mean and standard deviation of RSRP, RSRQ, and RSSI.
- Mean latitude and longitude.
- Anonymized device labels.
- Measurement time intervals.

Both datasets contain processed, aggregated, and aligned results to enable benchmarking. They are not the original raw CSV files exported directly by the Android application or the NEMO system.

Additional information is available in [`data/README.md`](data/README.md).

## Scientific use

Altair is intended for academic, experimental, and exploratory studies of cellular network coverage using commercial Android smartphones.

Altair is not intended to replace calibrated professional drive-test equipment. Measurements may vary between smartphones because of differences in antennas, chipsets, modems, firmware, Android versions, and manufacturer reporting mechanisms.

## Version

The current repository release is:

```text
v1.1.1
```

Release v1.1.1 updates the datasets and documentation; the application code remains unchanged from v1.1.0.

The preceding release, v1.1.0, is permanently archived in [Zenodo](https://doi.org/10.5281/zenodo.23653192). The DOI for v1.1.1 will be added after its Zenodo record is available.

## Citation

If you use Altair in academic work, please cite the software and the associated SoftwareX publication.

Archived software DOI for v1.1.0 (preceding release):

https://doi.org/10.5281/zenodo.23653192

## License

Altair is distributed under the [MIT License](LICENSE.txt).

Copyright © 2026:

- Jairo Jorge Bejar Torreblanca
- Alexandra Judith Benavente Vera
- Alberth Ronal Tamo Calla

## Software authors and contributors

- Jairo Jorge Bejar Torreblanca
- Alexandra Judith Benavente Vera
- Alberth Ronal Tamo Calla

## Associated publication

The software is associated with the SoftwareX manuscript:

**Altair: A mobile crowdsensing application for cellular network coverage assessment**

Authors:

- Jairo Jorge Bejar Torreblanca
- Alexandra Judith Benavente Vera
- Alberth Ronal Tamo Calla
- Alexander Beremiz Hilario Tacuri

## Contact

For questions related to the software or associated publication:

**Corresponding author**

Jairo Jorge Bejar Torreblanca  
Universidad Nacional de San Agustín de Arequipa  
Email: jbejart@unsa.edu.pe

Additional authors:

- Alexandra Judith Benavente Vera — abenaventev@unsa.edu.pe
- Alberth Ronal Tamo Calla — atamo@unsa.edu.pe
- Alexander Beremiz Hilario Tacuri — ahilariot@unsa.edu.pe
