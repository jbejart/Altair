# Validation campaign measurements

This directory contains the processed smartphone measurements obtained during the controlled LTE validation campaign conducted with Altair, as well as the processed reference measurements collected with the Keysight Anite NEMO Invex system.

## Files

- `Altair_measurements_aligned.csv`: Processed crowdsensing measurements grouped by smartphone and measurement interval.
- `nemo_measurements_aligned.csv`: Processed reference measurements collected by the Anite NEMO Invex system, aligned to the same time intervals for direct comparison.
- `FILTERING.ipynb`: Jupyter Notebook containing the data filtering and processing steps for the Altair and NEMO measurements.

## Raw data samples

- `raw data Brand C model 1.csv`: Campaign sample collected with Altair v0.9.0 (34 columns).
- `raw data Brand C model 2.csv`: Additional sample captured with Altair v1.1.0 (90 fields per record), not used in the experimental results.

Direct user identifiers in the Brand C model 1 sample have been replaced with `anon`; measurement values are unchanged.

## Dataset structure

In the processed datasets, each row represents the measurements collected by one smartphone or the reference system during a specific acquisition interval.

The files have slightly different headers: `deviceModel` appears only in the smartphone dataset, and `N°_Samples` appears only in the NEMO dataset. The remaining columns are shared:

| Column | Description | Unit |
|---|---|---|
| deviceModel | Anonymized smartphone label (Altair dataset only) | — |
| N°_Samples | Number of reference observations in the interval (NEMO dataset only) | Samples |
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

The datasets contain processed measurements grouped by device (smartphone or reference system) and acquisition interval. 

For each group, the data includes the calculated mean and standard deviation for the RSRQ, RSRP, and RSSI metrics, alongside the mean geographic coordinates representing the approximate measurement location. 

Both files provide aggregated results strictly aligned for benchmarking purposes. They are not the original raw CSV files exported directly by the Android application or the NEMO system.

## Device anonymization

Smartphone brands and commercial model names were replaced with anonymized labels such as Brand A model 1, Brand B model 1, and Brand C model 1. 
The anonymized labels remain consistent throughout the dataset so that the measurements of each device can be analyzed separately.

## Scope

The dataset contains processed measurements obtained with smartphones running Altair, alongside the aligned reference measurements collected with the Anite NEMO Invex system to evaluate the accuracy of the mobile crowdsensing approach.

## License

The dataset is distributed under the same MIT License as the Altair project. See the LICENSE.txt file in the root directory of the repository.
