# Using ABBA's registration in QuPath

-----
[**Back to documentation main page**](index.md)

-----

<!-- TOC -->
* [Using ABBA's registration in QuPath](#using-abbas-registration-in-qupath)
  * [Exporting ABBA registration results](#exporting-abba-registration-results)
  * [Importing ABBA registration results: creating Atlas regions as QuPath annotations](#importing-abba-registration-results--creating-atlas-regions-as-qupath-annotations)
    * [Display tips](#display-tips)
    * [Automating the import for all slices](#automating-the-import-for-all-slices)
  * [Importing ABBA registration results: adding Atlas coordinates to QuPath detections measurements](#importing-abba-registration-results--adding-atlas-coordinates-to-qupath-detections-measurements)
    * [Allen Brain CCFv3 coordinates](#allen-brain-ccfv3-coordinates)
  * [Analysis in QuPath](#analysis-in-qupath)
    * [Basic QuPath scripts](#basic-qupath-scripts)
      * [1. Clear all objects (cells, annotations)](#1-clear-all-objects--cells-annotations-)
      * [2. Clear annotations only (Atlas regions)](#2-clear-annotations-only--atlas-regions-)
      * [3. Import atlas regions (take care to not import it several times: clear the objects before)](#3-import-atlas-regions--take-care-to-not-import-it-several-times--clear-the-objects-before-)
      * [4. Collect and select a subregion (example with the acronym ‘CTXpl’)](#4-collect-and-select-a-subregion--example-with-the-acronym-ctxpl-)
      * [5. same as 4., but restricted to the left part of the brain](#5-same-as-4-but-restricted-to-the-left-part-of-the-brain)
      * [6. to collect and select subregions from a list](#6-to-collect-and-select-subregions-from-a-list)
      * [7. to collect all regions except the ones on a list](#7-to-collect-all-regions-except-the-ones-on-a-list)
      * [8. Removing objects](#8-removing-objects)
  * [Display results in 3D](#display-results-in-3d)
<!-- TOC -->

-----

## Exporting ABBA registration results
When a registration is done in ABBA, and if the slices have been opened from a QuPath project, it is possible to re-export the registration results into the original QuPath project.

For that, simply select all your slices of interest and click, in the top menu bar `Export > ABBA - Export Regions To QuPath project`.

When executing this action, ABBA exports, for each slice:
* regions of the allen brain atlas as a zip file (ImageJ rois file)
* a json file which can be used to compute the transformation between pixels coordinates of the original file to the Atlas (and vice versa)

These two files are saved into each QuPath entry folder. Additionally, the Atlas Ontology is written next to the QuPath project file (do not erase it!).

## Importing ABBA registration results: creating Atlas regions as QuPath annotations

In QuPath, provided you have correctly installed the [required extensions](installation.md), you can click `Extensions > ABBA > Load Atlas Annotations into Open Image`.

![Load annotation](assets/img/qupath_import_atlas_regions.png)

### Display tips
The region boundaries are displayed. If you do not split between the left and right hemi-brain, the annotation labels are all along the middle of the image and overlapping

If you split the regions between left and right hemi-brain, the display will be less cluttered. Moreover, you can press `N` to hide the name of the imported regions.

You can also use the transparency slider to change the opacity of the regions.



    Is there a way to get the text label to display on the actual region?

If you put a low transparency, double-click on a region and have the hierarchy tab activated, you will see in yellow the region selected, and the region selected will be synchronized on the hierarchy tab:

![Sync between hierarchy tab and annotations](assets/qupath_sync_hierarchy_annotations.png)

### Automating the import for all slices
If you go to the workflow tab of QuPath, you will see that a workflow step is present. You can create a script out of it, such as:

```
setImageType('FLUORESCENCE');
clearAllObjects();
qupath.ext.biop.abba.AtlasTools.loadWarpedAtlasAnnotations(getCurrentImageData(), "acronym", false);
```

This script can be run in batch to import the regions for all slices present in the QuPath project.


## Importing ABBA registration results: adding Atlas coordinates to QuPath detections measurements

A convenient way to pool analysis from several animals, is to combine the result of the analysis into a common coordinate space.

For that, we provide the following script which, for all detections of images in QuPath, appends the coordinate of each centroid detection as extra measurements:

```
/**
 * Computes the centroid coordinates of each detection within the atlas
 * then adds these coordinates onto the measurement list.
 * Measurements names: "Atlas_X", "Atlas_Y", "Atlas_Z"
 */

def pixelToAtlasTransform = 
    AtlasTools
    .getAtlasToPixelTransform(getCurrentImageData())
    .inverse() // pixel to atlas = inverse of atlas to pixel

getDetectionObjects().forEach(detection -> {
    RealPoint atlasCoordinates = new RealPoint(3);
    MeasurementList ml = detection.getMeasurementList();
    atlasCoordinates.setPosition([detection.getROI().getCentroidX(),detection.getROI().getCentroidY(),0] as double[]);
    pixelToAtlasTransform.apply(atlasCoordinates, atlasCoordinates);
    ml.putMeasurement("Atlas_X", atlasCoordinates.getDoublePosition(0) )
    ml.putMeasurement("Atlas_Y", atlasCoordinates.getDoublePosition(1) )
    ml.putMeasurement("Atlas_Z", atlasCoordinates.getDoublePosition(2) )
})

import qupath.ext.biop.warpy.Warpy
import net.imglib2.RealPoint
import qupath.lib.measurements.MeasurementList
import qupath.ext.biop.abba.AtlasTools

import static qupath.lib.gui.scripting.QPEx.* // For intellij editor autocompletion
```

This script can also be run in batch.

----
:bulb: It is not necessary to have imported the regions for this script to run. 

----


### Allen Brain CCFv3 coordinates

For the particular case of the Adult Mouse Allen Brain Atlas CCFv3, coordinates are oriented like this: [(source)](http://help.brain-map.org/download/attachments/5308472/3DOrientation.png?version=1&modificationDate=1368132564812&api=v2):

![CCF v3](assets/img/ccfv3.png)

Mind the axes names and orientations!


## Analysis in QuPath

A typical workflow will consist of detecting cells in a particular region of the brain and exporting these results for all slices.

You can find the full QuPath documentation at [https://qupath.readthedocs.io/en/0.4/](https://qupath.readthedocs.io/en/0.4/), and get some support in [https://forum.image.sc](https://forum.image.sc). But here are a few basic scripts to get you started:

### Basic QuPath scripts

To write a bit of code, click in QuPath: `Automate > Show script editor`.

![show qupath script editor](assets/img/qupath_show_script_editor.png)

#### 1. Clear all objects (cells, annotations)

```
// To clear all objects (take care, it also clear cells (detection objects)!)
clearAllObjects()
```

#### 2. Clear annotations only (Atlas regions)

```
// To clear all annotations objects:
removeObjects(getAnnotationObjects(), false) // last argument = keep child objects ?
```

#### 3. Import atlas regions (take care to not import it several times: clear the objects before)

```
// Load atlas and name all regions according with their acronym
// Last argument = split left and right regions
qupath.ext.biop.abba.AtlasTools.loadWarpedAtlasAnnotations(getCurrentImageData(), "acronym",true);
```

Here's the result:

![Regions just imported in QuPath](assets/img/qupath_regions_just_imported.png)

#### 4. Collect and select a subregion (example with the acronym ‘CTXpl’)

```
// Gets all annotations (=regions) named CTXpl (left and right)
def myObjects = getAllObjects().findAll{it.getName() == 'CTXpl'} // replace 'CTXpl' by any region acronym existing in the atlas

// Then select them
selectObjects(myObjects)
```

![Collect and select regions](assets/img/qupath_collect_select.png)

#### 5. same as 4., but restricted to the left part of the brain

```
// Gets all annotations named CTXpl in the left region:
def myLeftObjects = getAnnotationObjects()
.findAll{it.getName() == 'CTXpl'} // replace 'CTXpl' by any region acronym existing in the atlas
.findAll{it.getPathClass().isDerivedFrom(getPathClass('Left'))} // select only the ones in the left regions
// Then select them      
selectObjects(myLeftObjects)
```

![Collect and select regions left](assets/img/qupath_select_left.png)

#### 6. to collect and select subregions from a list

```
// Gets all annotations which name is contained within a list:
listOfRegionsToSelect=['MPN', 'CTXsp', 'ACAd']


def myObjectsWithinAList = getAnnotationObjects()
.findAll{it.getName() in listOfRegionsToSelect}
//.findAll{it.getPathClass().isDerivedFrom(getPathClass('Left'))} // Uncomment this line to get only the objects in the left region

// Then select them             
selectObjects(myObjectsWithinAList)
```

![Select sub regions](assets/img/qupath_select_subregions.png)

#### 7. to collect all regions except the ones on a list

```
def myObjectsWithinAList = getAnnotationObjects()
.findAll{!(it.getPathClass() == null)} // removes null objects
.findAll{it.getPathClass().isDerivedFrom(getPathClass('Left'))}

// Gets all annotations except the ones of a list
def objectsOtherThan = getAnnotationObjects() - myObjectsWithinAList
selectObjects(objectsOtherThan)
```

![Select except list](assets/img/qupath_select_except_list.png)

#### 8. Removing objects

In the example below, we first selected the objects of interest, then use that to collect the other objects, that we remove with removeObjects

```
// Gets all annotations which name is contained within a list:
listOfRegionsToSelect=['MPN', 'CTXsp', 'ACAd']


def myObjectsWithinAList = getAnnotationObjects()
.findAll{it.getName() in listOfRegionsToSelect}

// Gets all annotations except the ones of a list
def objectsOtherThan = getAnnotationObjects() - myObjectsWithinAList


removeObjects(objectsOtherThan, true)
```

![QuPath remove objects](assets/img/qupath_remove_objects.png)

## Display results in 3D

Some analysis scripts are also available in https://github.com/nickdelgrosso/ABBA-QuPath-RegistrationAnalysis

To be done... combining [PaQuo](https://paquo.readthedocs.io/en/latest/quickstart.html) and [BrainRender](https://github.com/brainglobe/brainrender) looks like the best option for this task. 

-----
[**Back to documentation main page**](index.md)

-----
