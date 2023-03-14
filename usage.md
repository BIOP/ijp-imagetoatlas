## ABBA Registration WorkFlow

-----
[**Back to documentation main page**](index.md)

-----

At this point of the documentation, you're supposed to have created a QuPath project, compatible with ABBA as detailed from the documentation. The QuPath project contains sections coming from a single animal.

* [**Getting started with Fiji's ABBA plugin**](import_qupath_project.md)
  * [BigDataViewer navigation](import_qupath_project.md#abba-navigation)
  * [Change Atlas display](import_qupath_project.md#allen-brain-atlas-display-options)
  * [Import data from:](import_qupath_project.md#import-a-qupath-project-in-abba)
    * [a QuPath project](import_qupath_project.md#import-a-qupath-project-in-abba)
    * [the current ImageJ image](import_qupath_project.md#current-imagej-window)
    * [a file](import_qupath_project.md#direct-opening-of-a-file)
    * [BigDataViewer-Playground](import_qupath_project.md#sources-from-bigdataviewer-playground)
* [**Register sections to the Allen Brain Atlas**](registration.md)
  * [Basic slices manipulation and display](registration.md)
  * [Slices positioning along the atlas axis](registration.md#first-coarse-positioning)
  * [Correcting the atlas cutting angle](registration.md#correcting-atlas-slicing-orientation)
  * [In-plane (2D) slices registration (manual, automated affine, automated spline)](registration.md#slices-registration)
  * [Saving / opening an ABBA project](registration.md#saving--opening-registrations-results)
* [**Register sections to the Allen Brain Atlas using DeepSlice**](registration_with_deepslice.md)

You can click on each of the steps above to follow a typical workflow. The user interface is explained progressively through these steps. For convenience, here are links for the various controls which are covered:
* [Atlas display](create_dataset_and_open.md#allen-brain-atlas-display-options)
* [Atlas slicing (rotations)](registration.md#correcting-atlas-slicing-orientation)
* [Slices selection](registration.md#slices-selection)
* [Slices display](registration.md#slices-display-options)
* [Moving slices along the atlas](registration.md)
* [Adjusting slices before registration (flip / rotate)](registration.md#rotate--flip-slices)
* [Affine registration](registration.md#affine-registration-automated)
* [Spline registration](registration.md#spline-registration-automated)
* [Registration with BigWarp](registration.md#bigwarp-registration-manual)
* [Editing a registration](registration.md#editing-a-registration)

In order to be fast, ABBA's workflow is designed to avoid time-expensive computations. To achieve this, the transformed slices are never fully computed. At the end of the workflow, it is the regions of the atlas which are transformed into the original slices coordinates. This has the extra advantage of avoiding any interpolation of the original data for its analysis. Nonetheless, it can be useful to compute the transformed imaged into the atlas coordinates for display purpose. ABBA thus provides a way to export the transformed images into the atlas coordinates:
* [Export transformed registered images](export.md)

-----
[**Back to documentation main page**](index.md)

-----