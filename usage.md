## Using ABBA

It is highly recommended to use [QuPath](https://qupath.github.io/) in order to define the dataset of brain slices. It is possible to use only ImageJ/Fiji, but the analysis capabilities are then limited (no support of multiresolution files in vanilla ImageJ). This documentation will only detail the following recommended workflow:

* For each animal:
  * `<QuPath>` - define the dataset of this animal brain sections into a QuPath project
  * `<Fiji>` - import sections by opening this QuPath project into Fiji's ABBA plugin
  * `<Fiji>` - perform various steps of registration (manual, automated or semi automated) for all required slices
  * `<Fiji>` - export registration results to the original QuPath project
  * `<QuPath>` - import registration results as annotations into QuPath
  * (Optional) after inspection, correct the registration for some slices into ABBA and re-export the new result to QuPath
  * `<QuPath>` - detect cells and various measurements in QuPath
  * `<QuPath>` - export a table containing, for all cells, their measurements as well as their location in the brain atlas CCF
*  `<Your prefered data analysis software>` combine and or display these results for all your animals

## A few words about file formats
```
TL; DR: Use calibrated VSI, CZI, OME-TIFF, a few others (please read), 
or convert to OME-TIFF.
```


First of all, all files need to be properly calibrated (microns, millimeters, etc, but not pixels!). This property is important because ABBA takes advantage of the proper calibration to facilitate display and registration. 

---

 :bulb: It is strongly recommended to work with multiresolution file formats (VSI, OME-TIFF, SVS), since brain slices are usually very big 2d images. ABBA, like QuPath, uses pre-computed downsampled images of these files to speed-up (very significantly) the display and processing of these images. Downsampled images also help for registration, since the registration is made with large scale features (size above  a few cells), which are incorrectly sampled if no downsampled image pre-exists.

---

---

:warning: Because Fiji is used is the workflow, only Bio-Formats supported formats are correctly handled. You can check on  [the Bio-Formats documentation](https://docs.openmicroscopy.org/bio-formats/6.6.1/supported-formats.html) if your file formats will be correctly handled. This will be the case if `Pyramid` is checked. File which can be opened only via [`OpenSlide`](https://openslide.org/) are not supported.

Tested file formats for ABBA :

* VSI (Olympus, +++)
* CZI (Zeiss, +++, not fully tested)
* LIF (Leica, +, no multiresolution support)

---

If your image can't be loaded in using `Bio-Formats`, you can convert your slides in `ome.tiff` format. Several options are available, for instance by using [bfconvert with Kheops](https://c4science.ch/w/bioimaging_and_optics_platform_biop/image-processing/imagej_tools/ijp-kheops/), or [bioformatsf2raw](https://c4science.ch/w/bioimaging_and_optics_platform_biop/image-processing/qupath/ome-tiff-conversion/) for a fast conversion.