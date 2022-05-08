## Defining a dataset and importing it in ABBA

### Define a dataset of brain sections in QuPath 

As in the recommended workflow, you first need to create a qupath project that contains all the brain slices that you want to register - usually from one animal.

You can check the [full documentation of QuPath](https://qupath.readthedocs.io/en/latest/).

For a quick setup with the demo dataset:
* create a project by dragging and dropping an empty folder into QuPath
* drag and drop your images to import them into a QuPath project
* select `Bio-Formats builder` and then click Import
* MAKE SURE to UNCHECK `Auto-generate pyramids`

![creating a project with slices in QuPath](assets/gif/qupath_create_project.gif)

---

:warning: All files need to be properly calibrated (microns, millimeters, etc, but not pixels!). Check on the image tab of QuPath that you have a proper physical unit specified for your images, and not pixels! If that's not the case, you should specify the correct pixel size NOW! (= BEFORE importing the project into Fiji's ABBA plugin). Otherwise, the images will look gigantic because 1 pixel is assumed to be 1 millimeter...

Also please make sure to read the message about [pyramidal file formats requirements](usage.md#supported-file-formats).

![img.png](assets/img/qupath_image_calibration.png)

---

You are done for now on the QuPath side. You can let QuPath open while performing Fiji's part of the workflow.

---

:warning: Do not modify the images present in the project once it has been opened and used in ABBA. ABBA has not been tested if the number of images present in the QuPath project are changed.

---

### Getting started with ABBA

Note : you will probably need a mouse, since navigating with a trackpad can be very frustrating.

In Fiji, start the ABBA plugin (type `ABBA` in Fiji's search bar or go to ` Plugins › BIOP › Atlas › ABBA - ABBA Start`). You will then need to chose the atlas (the whole documentation is made with the Allen adult mouse brain atlas) and between 3 ways of slicing the atlas : coronal, sagittal, or horizontal. The documentation is made in the coronal sectioning orientation because it is probably the most frequent orientation, but ABBA works the same way in all orientations.

#### ABBA Navigation
ABBA uses [Fiji's BigDataViewer](https://imagej.github.io/plugins/bdv/index) in order to display multiresolution images in a responsive manner, this comes with a totally different way of visualizing your data than from the standard ImageJ. So let's first go through some basic navigation commands in ABBA:

* `hold and drag right-click` pan
* `mouse wheel`  zoom control
* `up / down key` zoom in / out
* `shift + up / down key` fast zoom in / out

Try to get familiar with these commands in order to go quickly from one end of the atlas to the other end.

#### Allen Brain Atlas display options

The image you see when you start ABBA is the Allen Brain Atlas dataset, sliced regularly along Z. This data consists of a 3 channel image:
* `Nissl (Ch. 0)`
* `Ara (Ch. 1)`
* `Label Borders (Ch. 2)` 
  
You can activate or deactivate each of these channels with checkboxes and emphasize more or less their display with sliders.

![Atlas display options](/assets/gif/fiji_abba_atlas_display.gif)

### Import a QuPath project in ABBA

Once you are familiar with these controls, click, in the ABBA window menu bar: `Import > Import QuPath Project`

![Importing a QuPath Project in ABBA](assets/img/fiji_import_qupath.png)

Select your project file, the initial position of the first slice, and the approximate spacing between slices in mm (the demo dataset has a spacing of 80 microns). These positions are just initial guesses which will be adjusted later on.

![Set initial positions of the slices in the atlas](assets/img/fiji_set_ini_position.png)

A second window shows up which specifies some advanced opening parameters. The default values (shown below) will work in most cases.

![Advanced import options](assets/img/fiji_advanced_import_options.png)

----

:warning: if your dataset contains 16-bits RGB channels (as it is the case for some CZI files), you have to check the `Split RGB channels` checkbox.

----


It can take up to a minute for the first opening, due to bio-formats memoization.

With the downsampled demo dataset, you will get an image similar to this:

![Project newly opened in ABBA](assets/img/fiji_just_opened_project.png)

[**Back to step by step tutorial**](usage.md)