# Using ABBA's registration in QuPath

## Exporting ABBA registration results
When a registration is done in ABBA, and if the slices have been opened from a QuPath project, it is possible to re-export the registration results into the original QuPath project.

For that, simply select all your slices of interest and click, in the top menu bar `Export > Export Regions To QuPath project`.

When executing this action, ABBA exports, for each slice:
* regions of the allen brain atlas as a zip file (ImageJ rois file)
* a json file which can be used to compute the transformation between pixels coordinates of the original file to the Allen Brain Atlas CCFv3

These two files are saved into the data folder of each QuPath entry. Also, the Allen Brain Ontology is written next to the QuPath project file (do not erase it!).

## Importing ABBA registration results in QuPath

In QuPath, provided you have correctly installed the [QuPath BIOP extensions](installation.md), you can click `BIOP > Atlas > Load Atlas Annotations into Open Image`.

![Load annotation](assets/img/qupath_import_atlas_regions.png)

When opening the annotation, you will be able to split the regions between left and right hemi-brain, or not.

The following script can also be used and ran in batch to import the regions for all slices of the dataset:

```
// Necessary import, requires biop-tools-2.0.7, see: https://github.com/BIOP/qupath-biop-extensions
import ch.epfl.biop.qupath.atlas.allen.api.AtlasTools
import qupath.lib.images.ImageData

ImageData imageData = getCurrentImageData();
AtlasTools.loadWarpedAtlasAnnotations(imageData, true); // true means regions are split between left and right hemi brain
```

If you need to keep only certain regions, you can modify and reuse the script below:

```
// Modify the sequence of functions according to your need:
// - reImportAbbaRegions()
// - clearAllExcept(['Left: TH', 'Left: MB']) 
// - clearRight()
// - clearLeft()
//

reImportAbbaRegions() // Erase and re import regions
clearAllExcept(['Left: TH', 'Left: MB']) // Modify and or expand the list according to your needs
 

// ------------- FUNCTIONS ------------------------
// 0 - Re-import regions
// Necessary import, requires biop-tools-2.0.7, see: https://github.com/BIOP/qupath-biop-extensions
def reImportAbbaRegions() {
    clearAllObjects();
    ImageData imageData = getCurrentImageData();
    AtlasTools.loadWarpedAtlasAnnotations(imageData, true);
}

// 1 - Remove right region
def clearRight() {
    removeObjects(getAnnotationObjects().findAll{it.getName().equals('Root')}, true) 
    removeObjects(getAnnotationObjects().findAll{it.getPathClass().toString().contains('Right:')}, false) 
}

// 2 - Remove left region
def clearLeft() {
    removeObjects(getAnnotationObjects().findAll{it.getName().equals('Root')}, true) 
    removeObjects(getAnnotationObjects().findAll{it.getPathClass().toString().contains('Left:')}, false) 
}

// 3 - Delete All Regions except the ones on the list
def clearAllExcept(regionsToKeep) {
    removeObjects(
        getAnnotationObjects()
        .findAll{!(regionsToKeep.contains(it.getPathClass().toString()))}, true) 
}
  
// 4 - Print all regions
//getAnnotationObjects().each{println(it.getPathClass().toString())}   

import ch.epfl.biop.qupath.atlas.allen.api.AtlasTools
import qupath.lib.images.ImageData
```

## Analysis in QuPath

A typical workflow will consist of detecting cells in a particular region of the brain and exporting these results for all slices.

The following draft script can be used to restrict cell detection to a particular region of the brain, and if run in batch, to make this detection for all slices:

```
import static qupath.lib.gui.scripting.QPEx.* // For intellij editor autocompletion

def regionClassPath = "Right: SSp-ul" // for instance, provided regions have been splitted before import. 
// Check imported region to know what can be chosen as a String

selectObjectsByClassification(regionClassPath);

//TODO : cell detection, etc.

```

## Export result into common coordinates of the Allen Brain Atlas (CCFv3)

A convenient way to pool analysis from several animals, is to combine the result of the analysis into a common coordinate space.

For that, we provide the following script which, for all detections of images in QuPath, appends the coordinate of each centroid detection as extra measurements ("CCFx","CCFy", "CCFz"):

```
/**
 * Adds to the detections results which are coordinates of the detection
 * in the CCFv3 (tagged "CCFx", "CCFy", "CCFz" in measurement list)
 *
 */

// Necessary import, requires biop-tools-2.0.7, see: https://github.com/BIOP/qupath-biop-extensions
import ch.epfl.biop.qupath.transform.*
import net.imglib2.RealPoint
import qupath.lib.measurements.MeasurementList

import static qupath.lib.gui.scripting.QPEx.* // For intellij editor autocompletion

// Get ABBA transform file located in entry path +
def targetEntry = getProjectEntry()
def targetEntryPath = targetEntry.getEntryPath();

def fTransform = new File (targetEntryPath.toString(),"ABBA-Transform.json")

if (!fTransform.exists()) {
    System.err.println("ABBA transformation file not found for entry "+targetEntry);
    return ;
}

def pixelToCCFTransform = Warpy.getRealTransform(fTransform).inverse(); // Needs the inverse transform

getDetectionObjects().forEach(detection -> {
    RealPoint ccfCoordinates = new RealPoint(3);
    MeasurementList ml = detection.getMeasurementList();
    ccfCoordinates.setPosition([detection.getROI().getCentroidX(),detection.getROI().getCentroidY(),0] as double[]);
    pixelToCCFTransform.apply(ccfCoordinates, ccfCoordinates);
    ml.addMeasurement("CCFx", ccfCoordinates.getDoublePosition(0) )
    ml.addMeasurement("CCFy", ccfCoordinates.getDoublePosition(1) )
    ml.addMeasurement("CCFz", ccfCoordinates.getDoublePosition(2) )
})
```

This script can also be run in batch.

## Analysis in QuPath

You can use the scripts developed by [@enassar](https://github.com/enassar) and [@nickdelgrosso](https://github.com/nickdelgrosso) in this repository: https://github.com/nickdelgrosso/ABBA-QuPath-utility-scripts in order to automate cell detection and export.

Some analysis scripts are also available in https://github.com/nickdelgrosso/ABBA-QuPath-RegistrationAnalysis 

## Display results in 3D

To be done... combining [PaQuo](https://paquo.readthedocs.io/en/latest/quickstart.html) and [BrainRender](https://github.com/brainglobe/brainrender) looks like the best option for this task. 

[**Back to step by step tutorial**](usage.md)
