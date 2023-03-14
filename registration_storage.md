# ABBA state file and registration formats 

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

The `actions` that can be performed are of several kinds:
* [CreateSliceAction](https://github.com/BIOP/ijp-imagetoatlas/blob/master/src/main/java/ch/epfl/biop/atlas/aligner/CreateSliceAction.java):
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


