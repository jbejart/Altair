# Altair: A mobile application for cellular network coverage assessment through mobile crowdsensing

Altair is an Android application for cellular network coverage assessment through mobile crowdsensing. The app collects radio key performance indicators (KPIs), including RSRP, RSRQ, and RSSI, together with GPS location, timestamp, network technology, operator information, and device metadata.

The repository is prepared for publication as companion software for a SoftwareX article. The complete Android Studio project is located in `src/`, and the processed dataset is located in `data/altair_processed_measurements.csv`.

## Repository Structure

```text
Altair/
|-- .git/
|-- .gitignore
|-- README.md
|-- LICENSE.txt
|-- src/
|   |-- app/
|   |-- gradle/
|   |-- build.gradle.kts
|   |-- settings.gradle.kts
|   |-- gradle.properties
|   |-- gradlew
|   `-- gradlew.bat
`-- data/
    |-- README.md
    `-- altair_processed_measurements.csv
```

## Opening the Android Project

Open the `src/` folder in Android Studio. Let Android Studio synchronize the Gradle project, then build and run the application from the IDE.

From a terminal on Windows, the Gradle wrapper can be checked from `src/` with:

```bat
gradlew.bat tasks
```

If local Firebase configuration is available, a debug build can be attempted with:

```bat
gradlew.bat assembleDebug
```

## Firebase Configuration

The repository does not include `google-services.json`. Each user must provide their own Firebase configuration file before building features that require Firebase services.

Place the local Firebase configuration at:

```text
src/app/google-services.json
```

This file is intentionally ignored by Git.

## Data

The processed dataset is available at:

```text
data/altair_processed_measurements.csv
```

The repository does not include raw Anite NEMO Invex data. No Python analysis scripts are published in this repository.

## License

This project is distributed under the MIT License. See `LICENSE.txt`.
