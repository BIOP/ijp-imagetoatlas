## Using ABBA

---

:warning: ABBA is still in an experimental phase. **Expect (and report) bugs** and **do not expect backward compatibility** when a more stable version will be released (all the code is versioned and stored in a worst case scenario).

---

### Recommended workflow
It is highly recommended to use [QuPath](https://qupath.github.io/) in order to define the dataset of brain slices. It is possible to use only Fiji, but the analysis capabilities are then limited (no support of multiresolution files in vanilla ImageJ). This documentation will only detail the recommended workflow that uses QuPath and Fiji's ABBA plugin.
 
For each animal:
* [**Define sections dataset**](create_dataset_and_open.md)
  * [Define the serial sections dataset into a QuPath project](create_dataset_and_open.md)
  * [Open these sections by importing this QuPath project into Fiji's ABBA plugin](create_dataset_and_open.md#import-a-qupath-project-in-abba)
* [**Register sections to the Allen Brain Atlas**](registration.md)
  * [Basic slices manipulation and display](registration.md)
  * [Slices positioning along the atlas axis](registration.md#first-coarse-positioning)
  * [Correcting the atlas cutting angle](registration.md#correcting-atlas-slicing-orientation)
  * [In-plane (2D) slices registration (manual, automated affine, automated spline)](registration.md#slices-registration)
  * [Saving / opening an ABBA project](registration.md#saving--opening-registrations-results)
* [**Register sections to the Allen Brain Atlas using DeepSlice**](registration_with_deepslice.md)
* [**Reuse ABBA's registration results in QuPath**](qupath_analysis.md)
  * [Export ABBA's registration results to the QuPath project](qupath_analysis.md)
  * [Import registration results as QuPath annotations](qupath_analysis.md#importing-abba-registration-results-in-qupath)
  * [(Optional) Correct the registration for some slices in ABBA and re-export the new result to QuPath](registration.md#editing-a-registration)
  * [Detect cells in QuPath](qupath_analysis.md#analysis-in-qupath)
  * [Append CCF coordinates in QuPath detected cells measurements](qupath_analysis.md#export-result-into-common-coordinates-of-the-allen-brain-atlas-ccfv3)
  * [Export a table containing, for all cells, their measurements as well as their location in the brain atlas CCF](qupath_analysis.md#display-results)
* [**Other export modalities (warped slices, etc.)**](export.md)
* **In your prefered data analysis software, combine and/or display these results for all animals**

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

# Troubleshooting

If you have an issue with ABBA:
* Look if the answer is not in the documentation.
* You can ask for help in the [image.sc forum](forum.image.sc/) (add `abba` and `fiji` or `qupath` tags)
* You can [open an issue in GitHub](https://github.com/BIOP/ijp-imagetoatlas/issues)

If you managed to install ABBA, these 3 options are better done directly from the plugin (top menu bar `Help > Go to documentation` and `Help > Ask for help in the forum`). Asking for help from ABBA allows to pre-fill a form with some hardware and software information. There is also a user feedback from if you want to give your opinion about what should be improved or supported.

## Frequent issues / Frequently asked questions

* The sections look gigantic when opened in ABBA, how to fix that ?

Most probably, your images are either not calibrated, or bio-formats cannot read the calibration. You can either [override the pixel size in QuPath before opening the project in ABBA](create_dataset_and_open.md#define-a-dataset-of-brain-sections-in-qupath), or, if your files are not pyramidal, you can convert your files to pyramidal OME-Tiff by using [Kheops](https://github.com/BIOP/ijp-kheops) and set the correct voxel size in the conversion process.

* I only have hemi-brain sections. Can I use ABBA ?

Yes, you can restrict the registration to a certain rectangular region of interest. Please have a look at [this question](https://forum.image.sc/t/abba-experimental-a-fiji-qupath-workflow-for-mouse-brain-slice-registration-to-the-allen-brain-atlas-ccfv3/54345/15) and the [answer just below](https://forum.image.sc/t/abba-experimental-a-fiji-qupath-workflow-for-mouse-brain-slice-registration-to-the-allen-brain-atlas-ccfv3/54345/16).

* The registrations with elastix do not work.

Yes, the installation of external dependencies on multiple OS is pain. First make sure that you followed all the steps in the installation instructions, and then [execute this script](https://gist.githubusercontent.com/NicoKiaru/b91f9f3f0069b765a49b5d4629a8b1c7/raw/571954a443d1e1f0597022f6c19f042aefbc0f5a/TestRegister.groovy) as a groovy script in Fiji ([video of how it should look here](https://forum.image.sc/t/abba-experimental-a-fiji-qupath-workflow-for-mouse-brain-slice-registration-to-the-allen-brain-atlas-ccfv3/54345/28)), finally report the issue to the forum by using `[Help > ABBA - Ask for help in the forum]`. This will bring useful information to help fix the issue.

* I cannot see any picture after I import my Qupath project

It’s because the slices are invisible by default for a faster loading. Select all the slices in the table and click on the header to make them visible, as well as the channels you want to see. There is a [step by step documentation accessible here](https://docs.google.com/presentation/d/1c5yG-5Rhz5WlR4Hf9TNVkjqb6yD6oukza8P6vHGVZMw/edit#slide=id.p1), check from slice 54 for the display options.

* I want to use another atlas than the ones available currently. How can I import the atlas ?

The atlases currently available are:
* the [adult Allen Mouse Brain atlas CCFv3](https://zenodo.org/record/4486659/#.YngkMlRBziE)
* the [Waxholm Space atlas of the Sprague Dawley Rat Brain V4](https://zenodo.org/record/5644162#.YngkTVRBziE)
* through [ABBA-Python](https://github.com/NicoKiaru/ABBA-Python), all [BrainGlobe](https://github.com/brainglobe) atlases

There are other atlases, of course, but adding them in ABBA still requires some work because there is no unified way of accessing labels, properties and hierarchical structure (unless it is implemented within BrainGlobe). This is an effort I can make, but there needs to be:
1. several users needing it - and you can do your request through ABBA `[Help > ABBA - Give your feedback]`
2. If not implemented in BrainGlobe, the atlas data need to be publicly accessible and shareable. I need to be allowed to repackage it in a different file format and make it accessible through Zenodo like the other ones.
3. I need a bit of time... 