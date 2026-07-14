\# Altair processed measurements



This directory contains the processed smartphone measurements obtained during

the controlled LTE validation campaign conducted with Altair.



\## File



\- `altair\_processed\_measurements.csv`: Processed measurements grouped by

&#x20; smartphone and measurement interval.



\## Dataset structure



Each row represents the measurements collected by one smartphone during a

specific acquisition interval.



The dataset contains the following columns:



| Column | Description | Unit |

|---|---|---|

| `deviceModel` | Anonymized label assigned to the smartphone | — |

| `intervalo` | Measurement interval identifier and acquisition time range | — |

| `rsrq\_mean` | Mean Reference Signal Received Quality | dB |

| `rsrq\_std` | Standard deviation of RSRQ | dB |

| `rsrp\_mean` | Mean Reference Signal Received Power | dBm |

| `rsrp\_std` | Standard deviation of RSRP | dB |

| `rssi\_mean` | Mean Received Signal Strength Indicator | dBm |

| `rssi\_std` | Standard deviation of RSSI | dB |

| `lat\_mean` | Mean latitude of the samples collected during the interval | Decimal degrees |

| `lon\_mean` | Mean longitude of the samples collected during the interval | Decimal degrees |



\## Data processing



The values in this file were obtained by grouping the periodic samples

collected by Altair according to the smartphone and measurement interval.



For each group, the mean and standard deviation of RSRQ, RSRP, and RSSI were

calculated. The mean geographic coordinates were also calculated to represent

the approximate measurement location associated with each interval.



This file contains processed and aggregated results. It is not the original

raw CSV file exported directly by the Android application.



\## Device anonymization



Smartphone brands and commercial model names were replaced with anonymized

labels such as `Brand A model 1`, `Brand B model 1`, and `Brand C model 1`.



The anonymized labels remain consistent throughout the dataset so that the

measurements of each device can be analyzed separately.



\## Scope



The dataset contains processed measurements obtained with smartphones running

Altair. It does not contain the measurements collected with the Anite NEMO

Invex reference system.



\## License



The dataset is distributed under the same MIT License as the Altair project.

See the `LICENSE.txt` file in the root directory of the repository.

