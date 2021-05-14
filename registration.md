## Registration of mouse brain slices with ABBA

Once your dataset is opened in ABBA. You will be able to position slices first along the slicing axis (position multiple slices along "z"), then performing 2d adjustements for each slice (tilt and roll atlas slicing correction, affine transformations and spline transformations).

### Slices selection

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

### Slices display options

It will be convenient for the registration to have your slices properly displayed. Depending on your use case, you may want to display a subset of the available channels, and adjust the min and max value displayed for a good contrast.

But first, in multi series files like vsi files it could happen that you end up with unwanted images series (label or macro image). In this case, you will need to remove these slices. For vsi files, these unwanted images, because they are rgb images, appear black in the slice display table. It is thus easy to select them. The selected slices can then be removed form ABBA by right clicking in the viewer window of ABBA and selecting `Remove Selected Slices` (also accessible in the menu bar : `Edit > Remove Selected Slices`). 

![Removing slices](assets/gif/fiji_remove_slices.gif)

Usually, slices will have multiple channels, autodetected by bio-formats, but while some channels will be useful for analysis, some will be useful just for the sake of registration to the atlas. In order to display only certain channels, you can activate to deactivate the display fo selected slices by clicking on the header of the slice display option table:

Additionally, you can set the color and the min and max display values of these slices:

GIF TODO

Even if modify display option in batch is what you need, the display option of each slice can be modified individually by clicking on the proper line on the table:

GIF TODO

[**Back to step by step tutorial**](usage.md)
