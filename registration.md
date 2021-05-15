# Registration of mouse brain slices with ABBA

Once your dataset is opened in ABBA. You will be able to position slices first along the slicing axis (position multiple slices along "z"), then performing 2d adjustements for each slice (tilt and roll atlas slicing correction, affine transformations and spline transformations).

## Slices selection

The way ABBA works is by acting on selected slices and by performing actions on them. Each slice has a round handle which serve to indicate if it is selected (green) or not (yellow).

![Highlighted green handle of selected slices](assets/img/fiji_selected_slice.png)

There are two ways of selecting slices:
1. by drawing rectangles with the left mouse button. Modifier keys allow to add or remove slices to the current selection:
  * `hold and left-click` draw a rectangle to select slices
  * `ctrl + hold and left-click` remove slices from the current selection
  * `shift + hold and left-click` add slices to the current selection

---

:warning:  you need to hold the modifier keys BEFORE drawing the rectangle in order to take their effect into account. This is in contrast to a lot of other software and a limitation of the UI of BigDataViewer

---

`ctrl+a` allows to select all slices, `ctrl+shift+a` allows to deselect all slices.

Gif below : adding selection with shift modifier key and rectangle, removing from the selection with `ctrl+rectangle`, then selecting all slices with `ctrl+a`.

![Selecting slices in bdv](assets/gif/fiji_select_slices.gif)

2. by selecting slices in the `Slices Display` card table:

![Selecting slices in bdv](assets/gif/fiji_select_slices_table.gif)

## Slices display options

It will be convenient for the registration to have your slices properly displayed. Depending on your use case, you may want to display a subset of the available channels, and adjust the min and max value displayed for a good contrast.

But first, in multi series files like vsi files it could happen that you end up with unwanted images series (label or macro image). In this case, you will need to remove these slices. For vsi files, these unwanted images, because they are rgb images, appear black in the slice display table. It is thus easy to select them. The selected slices can then be removed form ABBA by right clicking in the viewer window of ABBA and selecting `Remove Selected Slices` (also accessible in the menu bar : `Edit > Remove Selected Slices`). 

![Removing slices](assets/gif/fiji_remove_slices.gif)

Usually, slices will have multiple channels, autodetected by bio-formats, but while some channels will be useful for analysis, some will be useful just for the sake of registration to the atlas. In order to display only certain channels, you can activate to deactivate the display of selected slices by clicking on the header of the slice display table:

You can set the color and the min and max display values of these slices:

![Slices display options](assets/gif/fiji_slices_display_options.gif)

The display of each slice can be customised individually by modify a single line in the table.

Once you have a correct display, you can start the registration.


## Registration to Allen Brain Atlas (2017 CCF v3)

This procedure starts first by a manual step, which has two goals:

* Estimate the position of each slice along the atlas
* Correct the atlas angles of slicing

In order to position each slice approximately along the axis, ABBA tries to provides a conveninent interface to manipulate series of slices.

### First coarse positioning

#### Correct location of slices along the axis

First of all, it may happen that your slices are not sorted correctly along the atlas axis. If this is the case, you can select slices which are not at their correct position and drag them along the axis. You can create some interval if necessary in between two slices in order to let others in between if needed.

You can select one or several slices and then, by dragging the rectangles located below the atlas, you can shift selected slices along the slicing axis:

![Drag slices along axis](assets/gif/fiji_slices_drag.gif)

#### Rotate / flip slices

normal way:

more advanced way:

[**Back to step by step tutorial**](usage.md)
