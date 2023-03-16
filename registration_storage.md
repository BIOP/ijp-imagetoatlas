# ABBA state file and registration formats 

-----
[**Back to documentation main page**](index.md)

-----

<!-- TOC -->
* [ABBA state file and registration formats](#abba-state-file-and-registration-formats)
  * [ABBA state files](#abba-state-files)
    * [File(s) `_bdvdataset_{i}.xml`](#file--s--bdvdatasetixml)
    * [File `sources.json`](#file-sourcesjson)
    * [File `state.json`](#file-statejson)
      * [CreateSliceAction:](#createsliceaction-)
      * [MoveSliceAction:](#movesliceaction-)
      * [RegisterSliceAction:](#registersliceaction-)
      * [KeySliceOnAction:](#keysliceonaction-)
      * [KeySliceOffAction:](#keysliceoffaction-)
      * [Other actions](#other-actions)
  * [How transformations are saved with JSON](#how-transformations-are-saved-with-json)
<!-- TOC -->

## ABBA state files

First of all, state files with the `.abba` extensions are zipped files. You can unzip them to explore what's inside (debug an issue and potentially edit them).

Here are the files you will usually see:

* `_bdvdataset_0.xml`
* `sources.json`
* `state.json`

### File(s) `_bdvdataset_{i}.xml`
The file `_bdvdataset_0.xml` contains the definition of a BigDataViewer dataset. See more explanation here: [https://imagej.net/plugins/bdv/](https://imagej.net/plugins/bdv/). This xml dataset can points towards [many different backends](dataset_prerequisite.md#more-technicalities). In brief this xml file specifies the `ImageLoader` (how to get array data), and metadata: how to convert pixels indices to physical space, display settings, and some other tags to specify multi-series dataset (what is a tile, a channel, etc.). In this file, each "source" is a 4D image which is indexed by a "ViewSetup" using a unique number for identification.

Because an ABBA state file can combine several bdv dataset, you may find several bdv dataset files, each with a different index (`_bdvdataset_0.xml`, `_bdvdataset_1.xml`, etc.).

----
:warning: This file may contain references to absolute file paths. As of ABBA v0.5+, depending on the BDV backend and if used with a GUI, the user will be asked for updated file paths if the absolute path is not valid anymore. In some rare cases (old state file, non standard backend), you may still have to edit this file manually to fix state opening issues

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

## How transformations are saved with JSON

When a slice registration is exported to QuPath, it's not the state file which is read by QuPath, but a more compact json file describing how to transform the pixel coordinates of an image entry to the atlas and vice-versa. This json file is stored directly in the QuPath project image entry. The pattern naming is `ABBA-Transform-{name-of-the-atlas}.json`, for instance for the Allen CCFv3 atlas: `ABBA-Transform-Adult Mouse Brain - Allen Brain Atlas V3.json`

In practice, this json file is created by serializing a Java [`InvertibleRealTransformSequence`](https://github.com/imglib/imglib2-realtransform/blob/master/src/main/java/net/imglib2/realtransform/InvertibleRealTransformSequence.java) object thanks to a series of RunTime adapters and the gson library.

The `InvertibleRealTransformSequence` object contains a sequence of `InvertibleRealTransform` object, which can be of several kinds. In most cases, they will be either:
* `AffineTransform3D` for affine 3D transformation
* `ThinplateSplineTransform` for splines transformation

----
:bulb: There are other kinds of transformation which are 'helper' transformation or wrappers. For instance, while spline `ThinplateSplineTransform` transformations can be 3D, they non-invertible 2D transformations in ABBA. To make them invertible, they have to be wrapped within `WrappedIterativeInvertibleRealTransform` objects. A `WrappedIterativeInvertibleRealTransform` contains an optimizer that can invert its inner transform (with a target precision). Since ABBA expects 3D transformation where the third is usually unchanged, 2D transformations have to be wrapped in a `Wrapped2DTransformAs3D` objects that apply the inner 2D transformation for the first two dimensions and that lets the third dimension unchanged.

----

A json transform file will fit this structure:

* `transform_0`: an affine transform which compensates for the non-perfectly orthogonal cutting angle and atlas offset
* `transform_{1}`: last transform applied by ABBA
* `transform_{i+1}`, `transform_{i+2}`, ...: .. transform performed by ABBA, reverse order
* `transform_{n-4}`: first transform applied by ABBA
* `transform_{n-3}`: atlas Z axis offset (serves for along the atals axis positioning)
* `transform_{n-2}`: the transform that serves for interactive transform, as well as slice flip and rotate 90/180/270 degrees
* `transform_{n-1}`: centering transform, typically compensates for microscope stage offset
* `transform_{n}`: from micrometer to pixel, with a (0,0) original

In most cases you will not need to understand and reverse engineer this file, but directly use java or python functions that know how to read this file.

(see also [this issue](https://github.com/BIOP/ijp-imagetoatlas/issues/160) for a discussion about the transformation sequence)

-----
[**Back to documentation main page**](index.md)

-----