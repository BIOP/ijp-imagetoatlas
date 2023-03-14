# ABBA state file and registration formats 

<!-- TOC -->
* [ABBA state file and registration formats](#abba-state-file-and-registration-formats)
  * [ABBA state files](#abba-state-files)
    * [File(s) `_bdvdataset_{i}.xml`](#file--s--bdvdatasetixml)
    * [File `sources.json`](#file-sourcesjson)
    * [File `state.json`](#file-statejson)
<!-- TOC -->

## ABBA state files

First of all, files with the `.abba` extensions are zipped files. You can unzip them to explore what's inside (debug an issue and potentially edit them).

Here are the files you will usually see:

* `_bdvdataset_0.xml`
* `sources.json`
* `state.json`

### File(s) `_bdvdataset_{i}.xml`
The file `_bdvdataset_0.xml` contains the definition of a BigDataViewer dataset. See more explanation here: [https://imagej.net/plugins/bdv/](https://imagej.net/plugins/bdv/). This xml dataset can points towards [many different backends](dataset_prerequisite.md#more-technicalities). In brief this xml file specifies the `ImageLoader` (how to get array data), and metadata: how to convert pixels indices to physical space, display settings, and some other tags to specify multi-series dataset (what is a tile, a channel, etc.). In this file, each "source" is a 4D image which is indexed by a "ViewSetup" using a unique number for identification.

Because an ABBA state file can combine several bdv dataset, you may find several bdv dataset files, each with a different index (`_bdvdataset_0.xml`, `_bdvdataset_1.xml`, etc.).

----
:warning: This file may contain references to absolute file paths. As of ABBA v0.5+, depending on the BDV backend and if used with a GUI, the user will be asked for updated file paths if the absolute path are not valid anymore. In some rare cases (old state file, non standard backend), you may still have to edit this file manually to fix state opening issues

----

### File `sources.json`

The file `sources.json` just serves to re-index uniquely the sources coming from potentially several bdv dataset. As well, it is used to override some metadata (name, display settings). Each source has an entry looking like this, and the entry order specifies the index of the source.

```
{
"source_name": "Slide_00.vsi - 10x_01-CY3",
"source_class": "bdv.SpimSource",
"converter_class": "class net.imglib2.display.Instances$Imp",
"source_id": 2,
"color": -179,
"converter_setup_min": 0.0,
"converter_setup_max": 255.0,
"sac": {
"spimdata": {
"datalocation": "_bdvdataset_0.xml"
},
"viewsetup": 4
},
"string_metadata": {}
}
```

----
:bulb: in older versions of ABBA, `datalocation` was using absolute path, which was the cause of many issues whereby users couldn't open their state file anymore. This issue is solved now because the `.abba` state file zips together the `sources.json` and `_bdvdataset_{i}.xml` file. These files are thus always expected to be in the same zip (= `.abba`) file.

----

### File `state.json`

This file contains essentially, a serialized form of the sequence of action that was performed on each slice (either programmatically or by the user). The state of the atlas (cutting angle) is also specified in it.

The `actions` that can be performed are of several kinds.
The major kinds are presented in the list below. These actions are Java classes which are automatically serialized by using the gson library.

#### [CreateSliceAction](https://github.com/BIOP/ijp-imagetoatlas/blob/master/src/main/java/ch/epfl/biop/atlas/aligner/CreateSliceAction.java):
```
{
    "type": "CreateSliceAction",
    "original_sources": {
    "source_indexes": [
      0,
      1,
      2
    ]
    },
    "original_location": 2.24,
    "final_thicknessCorrection": 68.98052838572343,
    "final_zShiftCorrection": 0.0
}
```

#### [MoveSliceAction]():

```
{
    "type": "MoveSliceAction",
    "location": 2.4
}
```
#### [RegisterSliceAction]():

```
{
          "type": "RegisterSliceAction",
          "fixed_sources_preprocess": {
            "type": "SourcesProcessComposer",
            "f1": {
              "type": "SourcesChannelsSelect",
              "channels_indices": [
                0,
                1
              ]
            },
            "f2": {
              "type": "SourcesAffineTransformer",
              "affine_transform": {
                "type": "AffineTransform3D",
                "affinetransform3d": [
                  1.0,
                  0.0,
                  0.0,
                  0.0,
                  0.0,
                  1.0,
                  0.0,
                  0.0,
                  0.0,
                  0.0,
                  1.0,
                  -3.4443820086452415
                ]
              }
            }
          },
          "moving_sources_preprocess": {
            "type": "SourcesProcessComposer",
            "f1": {
              "type": "SourcesChannelsSelect",
              "channels_indices": [
                0,
                1
              ]
            },
            "f2": {
              "type": "SourcesAffineTransformer",
              "affine_transform": {
                "type": "AffineTransform3D",
                "affinetransform3d": [
                  1.0,
                  0.0,
                  0.0,
                  0.0,
                  0.0,
                  1.0,
                  0.0,
                  0.0,
                  0.0,
                  0.0,
                  1.0,
                  -3.4443820086452415
                ]
              }
            }
          },
          "registration": {
            "type": "Elastix2DAffineRegistration",
            "transform": "{\n  \"affinetransform3d\": [\n    1.0452432123360618,\n    0.006327564749404507,\n    0.0,\n    -0.04590107118375819,\n    0.01038468179294674,\n    1.021780495450928,\n    0.0,\n    0.06049699130734343,\n    0.0,\n    0.0,\n    0.9999999999999999,\n    0.0\n  ]\n}",
            "parameters": {
              "showImagePlusRegistrationResult": "false",
              "background_offset_value_moving": "0.0",
              "sx": "13.11",
              "sy": "9.19",
              "pxSizeInCurrentUnit": "0.08",
              "px": "-6.555",
              "py": "-4.595",
              "background_offset_value_fixed": "0.0"
            }
          }
        }
```

#### [KeySliceOnAction]():

```
{
  "type": "KeySliceOnAction"
}
```

#### [KeySliceOffAction]():

```
{
  "type": "KeySliceOffAction"
}
```

#### Other actions

Some other actions are not serialized and thus are not part of the history or the state ABBA file.

## Json registration format

When a slice registration is exported to QuPath, a json file describing how to transform the pixel coordinates to the atlas and vice-versa is stored in the QuPath project image entry.

This json file is created by serializing a Java [`InvertibleRealTransformSequence`](https://github.com/imglib/imglib2-realtransform/blob/master/src/main/java/net/imglib2/realtransform/InvertibleRealTransformSequence.java) object thanks to a series of RunTime adapters and the gson library.