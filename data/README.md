# Validation campaign measurements

This directory contains the processed smartphone measurements obtained during the controlled LTE validation campaign conducted with Altair, as well as the reference measurements collected with the Keysight Anite NEMO Invex system.

## Files

- `Altair_measurements_aligned.csv`: Processed crowdsensing measurements grouped by smartphone and measurement interval.
- `nemo_measurements_aligned.csv`: Reference measurements collected by the Anite NEMO Invex system, aligned to the same time intervals for direct comparison.

## Dataset structure

Each row represents the measurements collected by one smartphone or the reference system during a specific acquisition interval.

Both datasets contain the following columns:

| Column | Description | Unit |
|---|---|---|
| deviceModel | Anonymized label assigned to the smartphone, or "NEMO" for the reference system | — |
| interval | Measurement interval identifier and acquisition time range | — |
| rsrq_mean | Mean Reference Signal Received Quality | dB |
| rsrq_std | Standard deviation of RSRQ | dB |
| rsrp_mean | Mean Reference Signal Received Power | dBm |
| rsrp_std | Standard deviation of RSRP | dB |
| rssi_mean | Mean Received Signal Strength Indicator | dBm |
| rssi_std | Standard deviation of RSSI | dB |
| lat_mean | Mean latitude of the samples collected during the interval | Decimal degrees |
| lon_mean | Mean longitude of the samples collected during the interval | Decimal degrees |

## Data processing

The values in these files were obtained by grouping the periodic samples collected by Altair and the NEMO system according to the device and measurement interval. 

For each group, the mean and standard deviation of RSRQ, RSRP, and RSSI were calculated. The mean geographic coordinates were also calculated to represent the approximate measurement location associated with each interval. 

These files contain processed, aligned, and aggregated results to enable benchmarking. They are not the original raw CSV files exported directly by the Android application or the NEMO system.

## Device anonymization

Smartphone brands and commercial model names were replaced with anonymized labels such as Brand A model 1, Brand B model 1, and Brand C model 1. 
The anonymized labels remain consistent throughout the dataset so that the measurements of each device can be analyzed separately.

## Scope

The dataset contains processed measurements obtained with smartphones running Altair, alongside the aligned reference measurements collected with the Anite NEMO Invex system to evaluate the accuracy of the mobile crowdsensing approach.

## License

The dataset is distributed under the same MIT License as the Altair project. See the LICENSE.txt file in the root directory of the repository.
