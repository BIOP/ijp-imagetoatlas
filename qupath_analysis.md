# Using ABBA's registration in QuPath

## Exporting ABBA registration results
When a registration is done is ABBA, and if the slices have been opened from a QuPath project, it is possible to re-export the registration into the QuPath project.

For that, simply select all your slices of interest and click, in the top menu bar `Export > Export Regions To QuPath project`.

When executing this action, ABBA does 2 things:
* export regions of the allen brain atlas as a zip file (ImageJ rois file)
* saves a json file which can be used to compute the transformation between pixels of the original file to the Allen Brain Atlas CCF

These two files are saved into the data folder of each QuPath entry. Also, the Allen Brain Ontology is written next to the QuPath project file (do not erase it!).

## Importing ABBA registration results in QuPath

If you have installed [QuPath BIOP extensions](installation.md), once you have opened a section image, you can click `BIOP > Atlas > Load Atlas Annotations into Open Image`.

![Load annotation](assets/img/qupath_import_atlas_regions.png)

When opening the annotation, you will be able to split the regions between left and right hemi brain, or not.

