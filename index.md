## ABBA - Aligning Big Brains & Atlases

ABBA is a set of software component which allows to register images of thin serial biological tissue sections, cut in any orientation (coronal, sagittal or horizontal) to atlases, usually brain atlases. It has been developed by the [BioImaging & Optics Platform](https://www.epfl.ch/research/facilities/ptbiop/) at EPFL

ABBA consists of a [Fiji](https://fiji.sc/) plugin for the registration part, which is best used in conjunction with [QuPath](https://qupath.github.io). Typically, a set of serial sections is defined as a QuPath project, that is registered within Fiji. The registration results are imported back into QuPath for downstream processing (cell detection and classification, cell counting per region, etc.).

Available atlases are the [3D mouse Allen Brain atlas](http://atlas.brain-map.org/atlas?atlas=602630314), and the [Waxholm Space Atlas of the Sprague Dawley Rat Brain](https://www.nitrc.org/projects/whs-sd-atlas). Depending on the way you install ABBA, you may also use all [BrainGlobe atlases](https://brainglobe.info/documentation/bg-atlasapi/index.html).

<video autoplay loop muted style="width: 100%;">
  <source src="https://user-images.githubusercontent.com/20223054/149301605-07b27dd0-4010-4ca4-b415-f5a9acc8963d.mp4" type="video/mp4">
  Your browser does not support the video tag.
</video>

-----

<!-- TOC -->
  * [ABBA - Aligning Big Brains & Atlases](#abba---aligning-big-brains--atlases)
  * [Documentation](#documentation)
    * [Installation](#installation)
    * [Data sources & file formats requirements](#data-sources--file-formats-requirements)
    * [Example datasets](#example-datasets)
    * [Creating a QuPath project for ABBA](#creating-a-qupath-project-for-abba)
    * [Registration workflow & registration export](#registration-workflow--registration-export)
    * [Other export modalities](#other-export-modalities)
    * [Post-registration analysis in QuPath](#post-registration-analysis-in-qupath)
    * [Post-registration analysis in Python](#post-registration-analysis-in-python)
    * [Advanced documentation](#advanced-documentation)
      * [State file & registration format](#state-file--registration-format)
      * [Javadoc](#javadoc)
  * [Other forms of documentation](#other-forms-of-documentation)
    * [1. Youtube tutorial](#1-youtube-tutorial)
    * [2. Workshop slides](#2-workshop-slides)
  * [Troubleshooting](#troubleshooting)
  * [Frequent issues / Frequently asked questions](#frequent-issues--frequently-asked-questions)
    * [The sections are gigantic when opened in ABBA](#the-sections-are-gigantic-when-opened-in-abba)
    * [I only have hemi-brain sections. Can I use ABBA ?](#i-only-have-hemi-brain-sections-can-i-use-abba-)
    * [I can't start any elastix registrations](#i-cant-start-any-elastix-registrations)
    * [I cannot see any image after I import my Qupath project](#i-cannot-see-any-image-after-i-import-my-qupath-project)
    * [I want to use another atlas than the ones available](#i-want-to-use-another-atlas-than-the-ones-available)
    * [I can't open my state file anymore](#i-cant-open-my-state-file-anymore)
<!-- TOC -->

-----

## Documentation

### [Installation](installation.md)
### [Data sources & file formats requirements](dataset_prerequisite.md)
### [Example datasets](example_datasets.md)
### [Creating a QuPath project for ABBA](create_qupath_dataset.md)
### [Registration workflow & registration export](usage.md)
### [Other export modalities](export.md)
### [Post-registration analysis in QuPath](qupath_analysis.md)
### [Post-registration analysis in Python](python_analysis.md)

### Advanced documentation
#### [State file & registration format](registration_storage.md)
#### [Javadoc](apidocs/index.html)

## Other forms of documentation

### 1. [Youtube tutorial](https://www.youtube.com/watch?v=sERGONVw4zE)

A video tutorial is nice to see ABBA in action, and to check the little details which may have been missed in other documentation forms. However, be aware that, while not very different, the version presented in the video tutorial is not up-to-date with this documentation.

Also, there's no information about DeepSlice neither QuPath post-processing in the video.

[Youtube video tutorial (March 2022)](https://www.youtube.com/watch?v=sERGONVw4zE).

- Introduction and installation
- [0:00 Introduction](https://www.youtube.com/watch?v=sERGONVw4zE&t=0s)
- [0:30 Installation](https://www.youtube.com/watch?v=sERGONVw4zE&t=30s)
-  Dataset definition, atlas display, and QuPath project import
- [1:27 Dataset presentation](https://www.youtube.com/watch?v=sERGONVw4zE&t=87s)
- [2:25 Dataset definition in QuPath](https://www.youtube.com/watch?v=sERGONVw4zE&t=145s)
- [4:58 Bdv navigation and Atlas display options](https://www.youtube.com/watch?v=sERGONVw4zE&t=298s)
- [7:52 QuPath project import](https://www.youtube.com/watch?v=sERGONVw4zE&t=472s)
- Description of ABBA's interface
- [9:06 Short description of ABBA's interface (Bdv view, table view)](https://www.youtube.com/watch?v=sERGONVw4zE&t=566s)
- [9:56 How to display the sections + how to remove unwanted sections](https://www.youtube.com/watch?v=sERGONVw4zE&t=594s)
- [13:46 Selecting sections in the Bdv view](https://www.youtube.com/watch?v=sERGONVw4zE&t=826s)
- Manual registration of sections along Z
- [15:20 Translating sections along the Z atlas axis (antero-posterior position)](https://www.youtube.com/watch?v=sERGONVw4zE&t=920s)
- [16:05 Tip: reversing section order](https://www.youtube.com/watch?v=sERGONVw4zE&t=965s)
- [17:14 How to cancel an action](https://www.youtube.com/watch?v=sERGONVw4zE&t=1034s)
- [17:38 First key slice](https://www.youtube.com/watch?v=sERGONVw4zE&t=1058s)
- [19:19 Second key slice (+atlas cutting angle adjustment)](https://www.youtube.com/watch?v=sERGONVw4zE&t=1159s)
- [21:54 Third key slice](https://www.youtube.com/watch?v=sERGONVw4zE&t=1314s)
- [23:00 Distribute spacing between sections](https://www.youtube.com/watch?v=sERGONVw4zE&t=1380s)
- Saving your work
- [23:43 How to save your work (state file)](https://www.youtube.com/watch?v=sERGONVw4zE&t=1423s)
- [24:04 Closing and reopening from a state file](https://www.youtube.com/watch?v=sERGONVw4zE&t=1444s)
- In-plane registrations of the sections (manual & automated, linear & non-linear)
- [24:51 Introducing positioning and review modes](https://www.youtube.com/watch?v=sERGONVw4zE&t=1491s)
- [26:00 Browsing sections in review mode](https://www.youtube.com/watch?v=sERGONVw4zE&t=1560s)
- [27:15 Manual affine in-plane registration](https://www.youtube.com/watch?v=sERGONVw4zE&t=1635s)
- [29:50 Automated affine in-plane registration](https://www.youtube.com/watch?v=sERGONVw4zE&t=1790s)
- [33:28 Automated spline in-plane registration](https://www.youtube.com/watch?v=sERGONVw4zE&t=2008s)
- Review and improve your registrations if necessary
- [35:30 Browsing registration steps](https://www.youtube.com/watch?v=sERGONVw4zE&t=2130s)
- [37:30 What you can attempt to improve the registration quality](https://www.youtube.com/watch?v=sERGONVw4zE&t=2250s)
- Export results to QuPath
- [39:29 Export registration results from Fiji to QuPath](https://www.youtube.com/watch?v=sERGONVw4zE&t=2369s)
- [40:04 Import registration results in QuPath](https://www.youtube.com/watch?v=sERGONVw4zE&t=2404s)
- [42:14 How to correct a registration result with BigWarp](https://www.youtube.com/watch?v=sERGONVw4zE&t=2534s)
- [46:42 Conclusion](https://www.youtube.com/watch?v=sERGONVw4zE&t=2802s)

### 2. Workshop slides

A step by step tutorial that details how to register a demo dataset. Installation instructions are also linked in this presentation.
[Workshop slides](https://docs.google.com/presentation/d/1c5yG-5Rhz5WlR4Hf9TNVkjqb6yD6oukza8P6vHGVZMw)

## Troubleshooting

If you have an issue with ABBA:
1. Check first [Frequently Asked Questions](index.md#frequent-issues--frequently-asked-questions), and have a look at the documentation if possible.
2. Maybe your issue was already posted and fixed ? Please look at the list of [questions about ABBA already asked in the Image.sc forum](https://forum.image.sc/tag/abba).
3. Still no luck? Ask for help in the [image.sc forum](https://forum.image.sc/) (add `abba` and `fiji` or `qupath` tags). If you have already installed ABBA in your Fiji, you can also run the command `ABBA - Ask for help in the forum`. You will find it either in the Help menu of ABBA, or by typing `ask for help`in Fiji's search bar. With this command, some helpful information from your local installation will be included in your post (note that you will need to create an account on the forum if you don't have already one).
4. If you feel your question is more tech / dev oriented, and you're familiar with github, you can also [open an issue.](https://github.com/BIOP/ijp-imagetoatlas/issues)

## Frequent issues / Frequently asked questions
### The sections are gigantic when opened in ABBA

Most probably, your images are either not calibrated, or bio-formats cannot read the calibration. If you are using QuPath, you can [override the pixel size in QuPath **BEFORE** opening the project in Fiji's ABBA plugin](create_qupath_dataset.md#define-a-dataset-of-brain-sections-in-qupath), or, if your files are not pyramidal, you can convert your files to pyramidal OME-Tiff by using [Kheops](https://github.com/BIOP/ijp-kheops) and set the correct voxel size in the conversion process.

### I only have hemi-brain sections. Can I use ABBA ?

Yes, you can restrict the registration to a certain rectangular region of interest. Please have a look at [this question](https://forum.image.sc/t/abba-experimental-a-fiji-qupath-workflow-for-mouse-brain-slice-registration-to-the-allen-brain-atlas-ccfv3/54345/15) and the [answer just below](https://forum.image.sc/t/abba-experimental-a-fiji-qupath-workflow-for-mouse-brain-slice-registration-to-the-allen-brain-atlas-ccfv3/54345/16). Also, there is the possibility to [virtually mirror the hemi-section (check point 3)](https://forum.image.sc/t/abba-aligning-big-brains-and-atlases-v0-5-3-released/80732). You can then register the whole 'virtual section', and remove the virtual extra half at the end.

### I can't start any elastix registrations
Either the elastix executable file location is not set, or you are missing a library, or you are missing some access rights. The installation of external dependencies on multiple OS is pain. To narrow down the issue, you can try to [execute this groovy script](https://gist.githubusercontent.com/NicoKiaru/b91f9f3f0069b765a49b5d4629a8b1c7/raw/0744676341b16ee4f37ed203130f0e0b761c08c8/TestRegister.groovy)  in Fiji ([video of how it should look here](https://forum.image.sc/t/abba-experimental-a-fiji-qupath-workflow-for-mouse-brain-slice-registration-to-the-allen-brain-atlas-ccfv3/54345/28)). If this did not help you solve the issue, please report the problem in the forum by using `Help > ABBA - Ask for help in the forum`.

### I cannot see any image after I import my Qupath project

It could be because the slices are invisible by default (for faster loading). Select all the slices in the table and click on the header line to make them visible, as well as the channels you want to see, and increase the contrast if necessary. There is a [step by step documentation accessible here](https://docs.google.com/presentation/d/1c5yG-5Rhz5WlR4Hf9TNVkjqb6yD6oukza8P6vHGVZMw/edit#slide=id.p1), check from slice 53 for the display options.

### I want to use another atlas than the ones available

The atlases currently available are:
* the [adult Allen Mouse Brain atlas CCFv3](https://zenodo.org/record/4486659/#.YngkMlRBziE)
* the [Waxholm Space atlas of the Sprague Dawley Rat Brain V4](https://zenodo.org/record/5644162#.YngkTVRBziE)
* depending on your installation, all [BrainGlobe](https://brainglobe.info/documentation/bg-atlasapi/index.html) atlases

There are other atlases, of course, but adding them in ABBA still requires some work because there is no unified way of accessing labels, properties and hierarchical structure (unless it is implemented within BrainGlobe). This is an effort I can make, but there needs to be:
1. several users needing it - and you can do your request through ABBA `Help > ABBA - Give your feedback`
2. If not implemented in BrainGlobe, the atlas data need to be publicly accessible and shareable. I need to be allowed to repackage it in a different file format and make it accessible through Zenodo like the other ones.
3. ABBA can't swallow all atlases, there are a number of atlases which consists of well annotated 2d sections, but which are not fully 3D. Such atlases can't be used within ABBA.
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
