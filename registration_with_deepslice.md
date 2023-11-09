# Using DeepSlice in ABBA

-----
[**Back to registration workflow**](usage.md)

-----

<!-- TOC -->
* [Using DeepSlice in ABBA](#using-deepslice-in-abba)
    * [Using DeepSlice in ABBA: step by step procedure](#using-deepslice-in-abba--step-by-step-procedure)
  * [Using the Web interface](#using-the-web-interface)
<!-- TOC -->

-----

[DeepSlice](https://www.deepslice.com.au/guide) is a deep learning based tool for automatic alignment of whole mouse brain histological sections. It is developed in the McMullan lab group by [Harry Carey](https://github.com/polarbean/) at [Macquarie University](https://www.mq.edu.au/), Sydney, Australia. It was designed to work primarily with [QuickNII](https://www.nitrc.org/projects/quicknii).

A [preprint is available](https://www.biorxiv.org/content/10.1101/2022.04.28.489953v1.full), the tool is [directly accessible via a web interface](https://www.deepslice.com.au/), and [its source code is on github](https://github.com/PolarBean/DeepSlice).

It can work with the Allen mouse brain atlas and the Rat Waxholm atlas, in coronal orientation. However, only the Allen mouse Brain atlas has been tested so far with ABBA. 

Using DeepSlice within ABBA thus gives very fast results and automates many initial steps of the alignment:

* atlas cutting angle estimation
* initial positioning of slices along the axis
* in-plane affine registration (DeepSlice does not deform beyond affine transformation)

After DeepSlice, ABBA can be used to further refine the alignment, for instance by applying an in-plane non-linear step with BigWarp or Elastix.

---

ABBA can facilitate the use of DeepSlice by generating low resolution sections and by reading back the QuickNII json result file. 

---

:warning: make sure that all the slices belong to the same animal

---

---


:warning: Set the slices display settings to avoid oversaturated pixels!

---

DeepSlice works with 8-bits RGB images. ABBA always rescales intensities according to the user display settings. Please make sure that the display settings are not completely off, resulting in an oversaturated image, or in an almost fully black image. When more features are visible, the registration quality will improve.


### Using DeepSlice in ABBA: step by step procedure
* set the slices display settings to avoid oversaturated pixels
* select all the slices you want to register
* click in the top menu bar: `Align > ABBA - DeepSlice Registration`

You get the following window:

![ABBA DeepSlice options](assets/img/fiji_deepslice_options.png)

* `Slices channels, 0-based` - used to select the channels you want to export to DeepSlice. You can for instance export a nuclear channel only. You can export the first and third channel by writing `0,2`.
* `Allow change of atlas slicing angle` - When checked, ABBA will adapt the atlas slicing angle based on the median slicing angles given by DeepSlice. If you don't want to modify the atlas slicing angle, you can uncheck this box.
* `Allow change of position along the slicing axis` - you probably want to let this box checked. If not, the slices will stay at their location along the axis.
* `Maintain the rank of the slices` - if you allow to change the position of slices along the axis (checkbox above), it may occur that deepslice swap some slices position (Slices 1-2-3-4-5 might be reordered  1-2-4-3-5 for instance). If you are sure of your slice order, you may want to avoid such change and let this box checked.
* `Affine transform in plane` - allow to transform the slices in plane. There may be rare cases where you want to avoid it, but I don't know which ones, so let it checked.
* `Local conda env or Web` - if you managed to install a Conda env containing [DeepSlice locally as explained in the installation](installation.md#installing-deepslice-to-run-it-locally), you can run DeepSlice directly. If not, you can use the Web interface.


## Using the Web interface

After pressing ok, you get this window:

![DeepSlice step 0](assets/img/fiji_deepslice_0.png)

After clicking it, a web page will open in your browser with the DeepSlice interface:

![DeepSlice web interface](assets/img/deepslice_web.png)

You can drag and drop the content of your dataset folder into this page, and then submit the task.

---

:warning: Checking `Slower but more accurate results` is advised because DeepSlice is very fast anyway. If your slices are regularly evenly spaced, you can click `Use section numbers`. Check `Normalise section angles` because ABBA forces this normalization anyway afterwards (only one cutting angle allowed).

---

When the registration is done, you can download the result json file.

Put back the json file in the result folder.

Then click ok in the small DeepSlice result window. You will see, if you selected the option, a window stating that slicing angles have been adjusted. After pressing ok again, the slices will be moved and transformed to their new position.

**Before**

![Before deepslice](assets/img/fiji_before_deepslice.png)

**After**

![After deepslice](assets/img/fiji_after_deepslice.png)

You can adjust then, review, regularly space the slices position and perform non linear registrations with the rest of ABBA functionalities.

-----
[**Back to registration workflow**](usage.md)

-----
