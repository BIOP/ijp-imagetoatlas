## Using ABBA

---

:warning: ABBA is still in experimental phase. It is provided as is. **Expect bugs** and **do not expect backward compatibility** when a more stable version will be released (all the code is versioned and stored in a worst case scenario, but do not count on it).

---


It is highly recommended to use [QuPath](https://qupath.github.io/) in order to define the dataset of brain slices. It is possible to use only ImageJ/Fiji, but the analysis capabilities are then limited (no support of multiresolution files in vanilla ImageJ). This documentation will only detail the recommended workflow that uses QuPath and Fiji's ABBA plugin.
 
For each animal:
* [**Define sections dataset:**](create_dataset_and_open.md)
  * [Define the dataset of this animal brain sections into a QuPath project](create_dataset_and_open.md)
  * [Import sections by opening this QuPath project into Fiji's ABBA plugin](create_dataset_and_open.md#abba-navigation)
* [**Register sections to the Allen Brain Atlas:**](registration.md)
  * [Basic slices manipulation and display in ABBA](registration.md)
  * [Position slices along the atlas axis](registration.md#first-coarse-positioning)
  * [Registering slices in 2D (manual, automated affine, automated spline)](registration.md#slices-registration)
  * [Saving / opening an ABBA project](registration.md#saving--opening-registrations-results)
* [**Reuse ABBA's registration results in QuPath:**](qupath_analysis.md)
  * [From ABBA, export registration results to QuPath project](qupath_analysis.md)
  * [Import registration results as QuPath annotations](qupath_analysis.md#importing-abba-registration-results-in-qupath)
  * [(Optional) Correct the registration for some slices into ABBA and re-export the new result to QuPath](registration.md#editing-a-registration)
  * [Detect cells in QuPath](qupath_analysis.md#analysis-in-qupath)
  * [Append cells CCF coordinates in QuPath detected cells](qupath_analysis.md#export-result-into-common-coordinates-of-the-allen-brain-atlas-ccfv3)
  * [Export a table containing, for all cells, their measurements as well as their location in the brain atlas CCF](qupath_analysis.md#display-results)
* **In your prefered data analysis software, combine and or display these results for all your animals**

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

Extra feature:
* [Export registered images to ImageJ](registration.md#exporting-slices-region-as-imagej-stack)


If you have an issue with ABBA, there are 3 ways to hope get it solved:
* Look if the answer is not in the documentation.
* You can ask for help in the image.sc forum
* You can open an issue in GitHub

If you managed to install ABBA, these 3 options are better done directly from within the plugin (top menu bar `Help > Go to documentation` and `Help > Ask for help in the forum`). Asking for help from ABBA allows to pre-fill a form with some hardware and software specifications. There is also a user feedback from if you want to give your opinion about what should be improved or supported.

## A few important words about file formats
```
TL; DR: Use calibrated VSI, CZI, OME-TIFF, a few others (please read), 
or convert to OME-TIFF.
```


First of all, all files need to be properly calibrated (microns, millimeters, etc, but not pixels!). This property is important because ABBA takes advantage of the proper calibration to facilitate display and registration. 

---

 :bulb: It is strongly recommended to work with multiresolution file formats (VSI, OME-TIFF, SVS), since brain slices are usually very big 2d images. ABBA, like QuPath, uses pre-computed downsampled images of these files to speed-up (very significantly) the display and processing of these images. Downsampled images also help for registration, since the registration is made with large scale features (size above  a few cells), which are incorrectly sampled if no downsampled image pre-exists.

---

---

:warning: Because Fiji is used is the workflow, only Bio-Formats supported formats are correctly handled. You can check on  [the Bio-Formats documentation](https://docs.openmicroscopy.org/bio-formats/6.6.1/supported-formats.html) if your file formats will be correctly handled. This will be the case if `Pyramid` is checked. File which can be opened **only** via [`OpenSlide`](https://openslide.org/) are not supported.

Tested file formats for ABBA :

* VSI (Olympus, +++)
* CZI (Zeiss, +++, not fully tested)
* LIF (Leica, +, no multiresolution support)

RGB images as well as 8-bits and 16-bits images have been successfully.

---

If your image can't be loaded in using `Bio-Formats`, you can convert your slides in `ome.tiff` format. Several options are available, for instance by using [bfconvert with Kheops](https://c4science.ch/w/bioimaging_and_optics_platform_biop/image-processing/imagej_tools/ijp-kheops/), or [bioformatsf2raw](https://c4science.ch/w/bioimaging_and_optics_platform_biop/image-processing/qupath/ome-tiff-conversion/) for a fast conversion.