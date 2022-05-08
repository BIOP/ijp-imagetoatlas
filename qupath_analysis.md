# Using ABBA's registration in QuPath

## Exporting ABBA registration results
When a registration is done in ABBA, and if the slices have been opened from a QuPath project, it is possible to re-export the registration results into the original QuPath project.

For that, simply select all your slices of interest and click, in the top menu bar `Export > ABBA - Export Regions To QuPath project`.

When executing this action, ABBA exports, for each slice:
* regions of the allen brain atlas as a zip file (ImageJ rois file)
* a json file which can be used to compute the transformation between pixels coordinates of the original file to the Allen Brain Atlas CCFv3 (and vice versa)

These two files are saved into each QuPath entry folder. Additionally, the Allen Brain Ontology is written next to the QuPath project file (do not erase it!).

## Importing ABBA registration results in QuPath

In QuPath, provided you have correctly installed the [required extensions](installation.md), you can click `Extensions > ABBA > Load Atlas Annotations into Open Image`.

![Load annotation](assets/img/qupath_import_atlas_regions.png)

When opening the annotation, you will be able to split the regions between left and right hemi-brain, or not.

If you go to the workflow tab of QuPath, you will see that a worflow step is present and thus you can create a script out of it, such as:

```
setImageType('FLUORESCENCE');
clearAllObjects();
qupath.ext.biop.abba.AtlasTools.loadWarpedAtlasAnnotations(getCurrentImageData(), "acronym", false);
```

This script can be ran in batch to import the regions for all slices present in the QuPath project.

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

## Compute the location of detections into the Atlas coordinates

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
    ml.addMeasurement("Atlas_X", atlasCoordinates.getDoublePosition(0) )
    ml.addMeasurement("Atlas_Y", atlasCoordinates.getDoublePosition(1) )
    ml.addMeasurement("Atlas_Z", atlasCoordinates.getDoublePosition(2) )
})

import qupath.ext.biop.warpy.Warpy
import net.imglib2.RealPoint
import qupath.lib.measurements.MeasurementList
import qupath.ext.biop.abba.AtlasTools

import static qupath.lib.gui.scripting.QPEx.* // For intellij editor autocompletion
```

This script can also be run in batch.

### Allen Brain CCFv3 coordinates

For the particular case of the Adult Mouse Allen Brain Atlas CCFv3, coordinates are oriented like this: [(source)](http://help.brain-map.org/download/attachments/5308472/3DOrientation.png?version=1&modificationDate=1368132564812&api=v2):

![img.png](assets/img/ccfv3.png)

Mind the axes names and orientations!

## Analysis in QuPath

You can use the scripts developed by [@enassar](https://github.com/enassar) and [@nickdelgrosso](https://github.com/nickdelgrosso) in this repository: https://github.com/nickdelgrosso/ABBA-QuPath-utility-scripts in order to automate cell detection and export.

## Display results in 3D

Some analysis scripts are also available in https://github.com/nickdelgrosso/ABBA-QuPath-RegistrationAnalysis

To be done... combining [PaQuo](https://paquo.readthedocs.io/en/latest/quickstart.html) and [BrainRender](https://github.com/brainglobe/brainrender) looks like the best option for this task. 

[**Back to step by step tutorial**](usage.md)
