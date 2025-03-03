# OS and Dependencies Info

```
OS Windows 11
ABBA Python (installer) v0.10.4
ImageJ 2.16.0
IJ 1.54p
ABBA 0.10.4
BigWarp 9.3.0
Bdv 10.6.1
Bdv Vistools 10.6.1
Bdv Biop Tools 0.11.0
Biop Image Loader 0.9.0
Biop Wrappers 0.11.2
```

# ABBA Benchmark

## 25 sections | 40 Threads | 200 Gb RAM

- Date and Time: 2025-02-28 14:09
- Comments: first run
- Dataset: 25 sections
- Number of threads: 40
- Max Allocatable Memory: 219767 MB

| Step                                   | Time (%) | RAM Usage (MB) |
|----------------------------------------|----------|----------------|
| Download QuPath Project                | 0.3      | 53             |
| Connect to OMERO Server                | 0.6      | 60             |
| Create BDV Dataset                     | 8.8      | 62             |
| Load Atlas and Initialize Positioner   | 2.1      | 144            |
| Import Sources into ABBA               | 1.0      | 113            |
| DeepSlice Registration (Local) Run 1   | 14.9     | 140            |
| DeepSlice Registration (Local) Run 2   | 7.9      | 141            |
| Elastix Registration (Affine)          | 11.4     | 152            |
| Elastix Registration (Spline)          | 37.6     | 225            |
| Export Registrations to QuPath Project | 15.4     | 1443           |

Total Time: 277.6 seconds

## 25 sections | 16 Core | 8 Gb RAM

- Date and Time: 2025-02-28 13:32
- Comments: Local Machine
- Dataset: 25 sections
- Number of threads: 16
- Max Allocatable Memory: 8192 MB

| Step                                   | Time (%) | RAM Usage (MB) |
|----------------------------------------|----------|----------------|
| Download QuPath Project                | 0.3      | 52             |
| Connect to OMERO Server                | 1.0      | 54             |
| Create BDV Dataset                     | 12.4     | 57             |
| Load Atlas and Initialize Positioner   | 2.8      | 144            |
| Import Sources into ABBA               | 1.5      | 105            |
| DeepSlice Registration (Local) Run 1   | 8.7      | 129            |
| DeepSlice Registration (Local) Run 2   | 6.3      | 128            |
| Elastix Registration (Affine)          | 14.2     | 135            |
| Elastix Registration (Spline)          | 43.0     | 241            |
| Export Registrations to QuPath Project | 9.8      | 1410           |

Total Time: 203.5 seconds

## 97 sections | 16 Core | 49 Gb RAM

- Date and Time: 2025-02-28 12:29
- Comments: Local Machine
- Dataset: 97 sections
- Number of threads: 16
- Max Allocatable Memory: 48979 MB

| Step                                   | Time (%) | RAM Usage (MB) |
|----------------------------------------|----------|----------------|
| Download QuPath Project                | 0.1      | 144            |
| Connect to OMERO Server                | 0.3      | 150            |
| Create BDV Dataset                     | 15.2     | 166            |
| Load Atlas and Initialize Positioner   | 0.2      | 172            |
| Import Sources into ABBA               | 0.2      | 176            |
| DeepSlice Registration (Local) Run 1   | 6.2      | 228            |
| DeepSlice Registration (Local) Run 2   | 3.8      | 231            |
| Elastix Registration (Affine)          | 17.7     | 244            |
| Elastix Registration (Spline)          | 47.8     | 355            |
| Export Registrations to QuPath Project | 8.5      | 3567           |

Total Time: 610.6 seconds

## 97 sections | 16 Threads | 8 Gb RAM

- Date and Time: 2025-02-28 14:49
- Comments: Local Machine
- Dataset: 97 sections
- Number of threads: 16
- Max Allocatable Memory: 7282 MB

| Step                                   | Time (%) | RAM Usage (MB) |
|----------------------------------------|----------|----------------|
| Download QuPath Project                | 0.2      | 45             |
| Connect to OMERO Server                | 0.3      | 46             |
| Create BDV Dataset                     | 14.2     | 57             |
| Load Atlas and Initialize Positioner   | 0.2      | 59             |
| Import Sources into ABBA               | 0.3      | 64             |
| DeepSlice Registration (Local) Run 1   | 7.1      | 118            |
| DeepSlice Registration (Local) Run 2   | 3.9      | 114            |
| Elastix Registration (Affine)          | 18.9     | 126            |
| Elastix Registration (Spline)          | 47.8     | 229            |
| Export Registrations to QuPath Project | 7.3      | 3167           |

Total Time: 647.6 seconds

## 97 sections | 40 Threads | 8 Gb RAM

- Date and Time: 2025-02-28 14:43
- Comments: Local Machine
- Dataset: 97 sections
- Number of threads: 40
- Max Allocatable Memory: 7889 MB

| Step                                   | Time (%) | RAM Usage (MB) |
|----------------------------------------|----------|----------------|
| Download QuPath Project                | 0.3      | 49             |
| Connect to OMERO Server                | 0.2      | 47             |
| Create BDV Dataset                     | 13.2     | 59             |
| Load Atlas and Initialize Positioner   | 0.5      | 59             |
| Import Sources into ABBA               | 0.4      | 62             |
| DeepSlice Registration (Local) Run 1   | 9.5      | 114            |
| DeepSlice Registration (Local) Run 2   | 6.2      | 115            |
| Elastix Registration (Affine)          | 13.8     | 128            |
| Elastix Registration (Spline)          | 44.7     | 232            |
| Export Registrations to QuPath Project | 11.2     | 3168           |

Total Time: 696.0 seconds

## 97 sections | 16 Threads | 1.5 Gb RAM

- Date and Time: 2025-02-28 16:27
- Comments: Low RAM
- Dataset: 97 sections
- Number of threads: 16
- Max Allocatable Memory: 1450 MB

| Step                                   | Time (%) | RAM Usage (MB) |
|----------------------------------------|----------|----------------|
| Download QuPath Project                | 0.0      | 57             |
| Connect to OMERO Server                | 0.1      | 63             |
| Create BDV Dataset                     | 2.2      | 72             |
| Load Atlas and Initialize Positioner   | 0.0      | 78             |
| Import Sources into ABBA               | 0.0      | 80             |
| DeepSlice Registration (Local) Run 1   | 0.8      | 131            |
| DeepSlice Registration (Local) Run 2   | 0.6      | 132            |
| Elastix Registration (Affine)          | 2.6      | 144            |
| Elastix Registration (Spline)          | 7.0      | 248            |
| Export Registrations to QuPath Project | 86.7     | 1372           |

Total Time: 4242.8 seconds

## 97 sections | 48 Threads | 8 Gb RAM

- Date and Time: 2025-03-03 11:59
- Comments: Orcinus Orca
- Dataset: 97 sections
- Number of threads: 48
- Max Allocatable Memory: 7671 MB

| Step                                   | Time (%) | RAM Usage (MB) |
|----------------------------------------|----------|----------------|
| Download QuPath Project                | 0.3      | 48             |
| Connect to OMERO Server                | 0.5      | 47             |
| Create BDV Dataset                     | 29.6     | 57             |
| Load Atlas and Initialize Positioner   | 0.7      | 60             |
| Import Sources into ABBA               | 0.4      | 62             |
| DeepSlice Registration (Local) Run 1   | 15.2     | 114            |
| DeepSlice Registration (Local) Run 2   | 8.1      | 116            |
| Elastix Registration (Affine)          | 10.1     | 128            |
| Elastix Registration (Spline)          | 25.4     | 231            |
| Export Registrations to QuPath Project | 9.7      | 3167           |

Total Time: 307.7 seconds

## 97 sections | 20 Threads | 8 Gb RAM

- Date and Time: 2025-03-03 12:09
- Comments: Fluorescent Platypus
- Dataset: 97 sections
- Number of threads: 20
- Max Allocatable Memory: 7901 MB

| Step                                   | Time (%) | RAM Usage (MB) |
|----------------------------------------|----------|----------------|
| Download QuPath Project                | 0.2      | 45             |
| Connect to OMERO Server                | 0.3      | 47             |
| Create BDV Dataset                     | 15.4     | 57             |
| Load Atlas and Initialize Positioner   | 0.4      | 60             |
| Import Sources into ABBA               | 0.3      | 62             |
| DeepSlice Registration (Local) Run 1   | 7.9      | 113            |
| DeepSlice Registration (Local) Run 2   | 3.1      | 115            |
| Elastix Registration (Affine)          | 18.5     | 126            |
| Elastix Registration (Spline)          | 46.0     | 230            |
| Export Registrations to QuPath Project | 7.9      | 3166           |

Total Time: 565.3 seconds

## 97 sections | 28 Threads | 8 Gb RAM

- Date and Time: 2025-03-03 12:21
- Comments: Peacock Spider
- Dataset: 97 sections
- Number of threads: 28
- Max Allocatable Memory: 7766 MB

| Step                                   | Time (%) | RAM Usage (MB) |
|----------------------------------------|----------|----------------|
| Download QuPath Project                | 0.5      | 48             |
| Connect to OMERO Server                | 0.3      | 45             |
| Create BDV Dataset                     | 21.0     | 56             |
| Load Atlas and Initialize Positioner   | 1.1      | 60             |
| Import Sources into ABBA               | 0.8      | 61             |
| DeepSlice Registration (Local) Run 1   | 14.6     | 113            |
| DeepSlice Registration (Local) Run 2   | 7.7      | 114            |
| Elastix Registration (Affine)          | 10.7     | 126            |
| Elastix Registration (Spline)          | 33.3     | 230            |
| Export Registrations to QuPath Project | 10.1     | 3166           |

Total Time: 672.9 seconds
 



