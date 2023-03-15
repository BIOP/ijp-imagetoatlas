## [ABBA](https://www.youtube.com/watch?v=8haRfsY4-_s) - Aligning Big Brains & Atlases

-----

<!-- TOC -->
  * [ABBA - Aligning Big Brains & Atlases](#abba---aligning-big-brains--atlases)
  * [Documentation](#documentation)
    * [1. Youtube tutorial](#1-youtube-tutorial)
    * [2. Workshop slides](#2-workshop-slides)
    * [3. This website / reference documentation](#3-this-website--reference-documentation)
* [Troubleshooting](#troubleshooting)
  * [Frequent issues / Frequently asked questions](#frequent-issues--frequently-asked-questions)
    * [The sections are gigantic when opened in ABBA](#the-sections-are-gigantic-when-opened-in-abba)
    * [I only have hemi-brain sections. Can I use ABBA ?](#i-only-have-hemi-brain-sections-can-i-use-abba-)
    * [I can't start any elastix registrations](#i-cant-start-any-elastix-registrations)
    * [I cannot see any image after I import my Qupath project](#i-cannot-see-any-image-after-i-import-my-qupath-project)
    * [I want to use another atlas than the ones available](#i-want-to-use-another-atlas-than-the-ones-available)
* [Javadoc](#javadoc)
<!-- TOC -->

-----

Aligning Big Brains & Atlases or ABBA for short, is a Fiji plugin which allows to register thin serial sections to several atlases, in coronal, sagittal and horizontal orientations.

Within Fiji, you have access to the [3D mouse Allen Brain atlas](http://atlas.brain-map.org/atlas?atlas=602630314), and the [Waxholm Space Atlas of the Sprague Dawley Rat Brain](https://www.nitrc.org/projects/whs-sd-atlas). With [ABBA-Python](https://github.com/NicoKiaru/ABBA-Python), you can access all [BrainGlobe atlases](https://brainglobe.info/).

ABBA is typically used in conjunction with [QuPath](https://qupath.github.io): a QuPath project can serve as an input for ABBA, and the registration results can be imported back into QuPath for downstream processing.

<video autoplay loop muted style="width: 100%;">
  <source src="https://user-images.githubusercontent.com/20223054/149301605-07b27dd0-4010-4ca4-b415-f5a9acc8963d.mp4" type="video/mp4">
  Your browser does not support the video tag.
</video>

-----


ABBA uses [BigDataViewer](https://imagej.net/plugins/bdv/index) and [BigWarp](https://imagej.net/plugins/bigwarp) for the display and on-the-fly computation of spline-transformed multiresolution images (typical output of Whole Slide Imaging).

It has been developed by the [BioImaging & Optics Platform](https://www.epfl.ch/research/facilities/ptbiop/) at EPFL. This page contains the reference documentation of ABBA. If you require additional help, please check the troubleshooting section at the bottom of this page.

## Documentation

There are three forms of documentation:

### 1. Youtube tutorial

A video tutorial is nice to see ABBA in action, and to check the little details which may have been missed in other documentation forms. However, the version presented is not completely up-to-date: a video tutorial is a lot of work. It requires more work to be updated.

* [Youtube video tutorial (March 2022)](https://www.youtube.com/watch?v=sERGONVw4zE).

### 2. Workshop slides

A step by step tutorial that details how to register a demo dataset. Installation instructions are also linked in this presentation.
[Workshop slides](https://docs.google.com/presentation/d/1c5yG-5Rhz5WlR4Hf9TNVkjqb6yD6oukza8P6vHGVZMw)

### 3. This website / reference documentation

* [Installation](installation.md)
* [Using ABBA with Python](abba_python.md)
* [Data source](dataset_prerequisite.md)
  * [File formats requirements](dataset_prerequisite.md)
    * [Bio-Formats readable](dataset_prerequisite.md#1-any-bio-formats-supported-file-format)
    * [Pyramidal/Multi-resolution file format preferred](dataset_prerequisite.md#2-ideally-multi-resolution--bio-formats-supported--)
    * [Calibration required](dataset_prerequisite.md#3-and-calibrated)
  * [Other sources (OMERO, QuPath, n5...)](dataset_prerequisite.md#more-technicalities)
* [Example datasets](example_datasets.md)
* [Creating a QuPath project for ABBA](create_qupath_dataset.md)
* [Registration workflow](usage.md) < the core of ABBA
* Post registration analysis
  * With QuPath
    * [Using ABBA's registration in QuPath](qupath_analysis.md)
      * [Import registration results as QuPath annotations](qupath_analysis.md#importing-abba-registration-results--creating-atlas-regions-as-qupath-annotations)
      * [(Optional) Correct the registration for some slices in ABBA and re-export the new result to QuPath](registration.md#editing-a-registration)
      * [Detect cells in QuPath](qupath_analysis.md#analysis-in-qupath)
      * [Append CCF coordinates in QuPath detected cells measurements](qupath_analysis.md#export-result-into-common-coordinates-of-the-allen-brain-atlas-ccfv3)
      * [Export a table containing, for all cells, their measurements as well as their location in the brain atlas CCF](qupath_analysis.md#compute-the-location-of-detections-into-the-atlas-coordinates)
  * With Python
    * [Using ABBA's registration with Python](python_analysis.md) 
  * [Export modalities](export.md)

* ABBA advanced documentation
  * Headless ABBA
    * Fiji scripting (TODO)
    * Python scripting (TODO)
  * 3D Reconstruction
    * Slice rasterization (TODO)
  * Creating a plugin for ABBA
    * With Java (TODO)
    * With Python (TODO)
  * Creating a registration plugin for ABBA
    * With Java (TODO)
    * With Python (TODO)
  * ABBA state file and registration specs
    * [State file, registration format](registration_storage.md) 
  * Javadoc
    * [ABBA Javadoc](apidocs/index.html)

# Troubleshooting

If you have an issue with ABBA:
1. Check [Frequently Asked Questions](index.md#frequent-issues--frequently-asked-questions), and have a quick look at the documentation if possible.
2. Look if the answer is not present in the list of [ABBA questions in the Image.sc forum](https://forum.image.sc/tag/abba).
3. Ask for help in the [image.sc forum](forum.image.sc/) (add `abba` and `fiji` or `qupath` tags). If you have already installed ABBA, you can also click on `Help > ABBA - Ask for help in the forum` from the plugin (some helpful information from your local installation will be included in your post). You will need to create an account on the forum.
4. You can also [open an issue in GitHub](https://github.com/BIOP/ijp-imagetoatlas/issues)

## Frequent issues / Frequently asked questions
### The sections are gigantic when opened in ABBA

Most probably, your images are either not calibrated, or bio-formats cannot read the calibration. If you are using QuPath, you can [override the pixel size in QuPath **BEFORE** opening the project in ABBA](create_dataset_and_open.md#define-a-dataset-of-brain-sections-in-qupath), or, if your files are not pyramidal, you can convert your files to pyramidal OME-Tiff by using [Kheops](https://github.com/BIOP/ijp-kheops) and set the correct voxel size in the conversion process.

### I only have hemi-brain sections. Can I use ABBA ?

Yes, you can restrict the registration to a certain rectangular region of interest. Please have a look at [this question](https://forum.image.sc/t/abba-experimental-a-fiji-qupath-workflow-for-mouse-brain-slice-registration-to-the-allen-brain-atlas-ccfv3/54345/15) and the [answer just below](https://forum.image.sc/t/abba-experimental-a-fiji-qupath-workflow-for-mouse-brain-slice-registration-to-the-allen-brain-atlas-ccfv3/54345/16). Also, there is the possibility to virtual mirror the hemisection. You can then register the whole 'virtual section', and remove the virtual extra half at the end.

### I can't start any elastix registrations
Either the elastix executable file location is not set, or you are missing a library, or you are missing some access rights. The installation of external dependencies on multiple OS is pain. To narrow down the issue, you can try to [execute this groovy script](https://gist.githubusercontent.com/NicoKiaru/b91f9f3f0069b765a49b5d4629a8b1c7/raw/0744676341b16ee4f37ed203130f0e0b761c08c8/TestRegister.groovy)  in Fiji ([video of how it should look here](https://forum.image.sc/t/abba-experimental-a-fiji-qupath-workflow-for-mouse-brain-slice-registration-to-the-allen-brain-atlas-ccfv3/54345/28)). If this did not help you solve the issue, please report the problem in the forum by using `Help > ABBA - Ask for help in the forum`.

### I cannot see any image after I import my Qupath project

It’s could be because the slices are invisible by default (for faster loading). Select all the slices in the table and click on the header line to make them visible, as well as the channels you want to see, and increa the contrast if necessary. There is a [step by step documentation accessible here](https://docs.google.com/presentation/d/1c5yG-5Rhz5WlR4Hf9TNVkjqb6yD6oukza8P6vHGVZMw/edit#slide=id.p1), check from slice 53 for the display options.

### I want to use another atlas than the ones available

The atlases currently available are:
* the [adult Allen Mouse Brain atlas CCFv3](https://zenodo.org/record/4486659/#.YngkMlRBziE)
* the [Waxholm Space atlas of the Sprague Dawley Rat Brain V4](https://zenodo.org/record/5644162#.YngkTVRBziE)
* through [ABBA-Python](https://github.com/NicoKiaru/ABBA-Python), all [BrainGlobe](https://github.com/brainglobe) atlases

There are other atlases, of course, but adding them in ABBA still requires some work because there is no unified way of accessing labels, properties and hierarchical structure (unless it is implemented within BrainGlobe). This is an effort I can make, but there needs to be:
1. several users needing it - and you can do your request through ABBA `Help > ABBA - Give your feedback`
2. If not implemented in BrainGlobe, the atlas data need to be publicly accessible and shareable. I need to be allowed to repackage it in a different file format and make it accessible through Zenodo like the other ones.
3. I need time.

There's also [a script](https://forum.image.sc/t/custom-atlas-in-abba/77206) that allows to create a fake atlas from an image, but no ontology is imported.

Check also [this forum post](https://forum.image.sc/t/customizing-atlas-labels-of-ccf2017-for-use-in-abba/78523/5) if you want to do modifications to the original Allen Brain CCFv3 atlas.

### I can't open my state file anymore

This issue occurred many times. In most cases, the state can be recovered by editing the ABBA state file(s). Here are the various causes of the issue:
* files being moved around (or drive letter being changed) ([post](https://forum.image.sc/t/issue-loading-saved-states/75223/7))
* entries being deleted from QuPath project after an ABBA state was created ([post](https://forum.image.sc/t/help-for-abba-in-fiji-could-not-load-saved-state/71477/7))
* Bio-Formats memoization [storing the absolute file path of an old location](https://github.com/BIOP/ijp-imagetoatlas/issues/154#issuecomment-1419904570)
* before ABBA v0.5, the abba state was stored in three different files. Any of them missing was a problem 

Any project created after ABBA v0.5+ will be less susceptible to these issues: 
* an ABBA state is not split in three files anymore, but stored [in a zipped file with an `.abba` extension](registration_storage.md#abba-state-files). This also allows to remove some absolute path.
* when required, the absolute path is written as few times as possible, and there's a mechanism that ask users to update their files if their location is not valid anymore.
* if an entry is removed in QuPath, the state file can nonetheless be opened in ABBA.

You should still fix first your file location in QuPath before fixing the QuPath project path in ABBA

Side note: the new state mechanism allow to share much more easily a registration between collaborators, or even with a publication. A few changes in file path, and you're done.

<!---
### Markdown

Markdown is a lightweight and easy-to-use syntax for styling your writing. It includes conventions for

```markdown
Syntax highlighted code block

# Header 1
## Header 2
### Header 3

- Bulleted
- List

1. Numbered
2. List

**Bold** and _Italic_ and `Code` text

[Link](url) and ![Image](src)
```

For more details see [GitHub Flavored Markdown](https://guides.github.com/features/mastering-markdown/).

### Jekyll Themes

Your Pages site will use the layout and styles from the Jekyll theme you have selected in your [repository settings](https://github.com/BIOP/ijp-imagetoatlas/settings/pages). The name of this theme is saved in the Jekyll `_config.yml` configuration file.

### Support or Contact

Having trouble with Pages? Check out our [documentation](https://docs.github.com/categories/github-pages-basics/) or [contact support](https://support.github.com/contact) and we’ll help you sort it out.

-->
