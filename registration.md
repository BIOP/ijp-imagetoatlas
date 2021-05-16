# Registration of mouse brain slices with ABBA

Once your dataset is opened in ABBA. You will be able to position slices first along the slicing axis (position multiple slices along "z"), then performing 2d adjustements for each slice (tilt and roll atlas slicing correction, 2d affine and spline registrations).

When you start ABBA, you will be in the `Positioning mode`, where the allen brain atlas is displayed with slices regularly spaced on top of the slices present on your dataset. 

## Slices selection

The way ABBA works is by acting on selected slices and by performing actions on them. Each slice has a round handle which serve to indicate if it is selected (green) or not (yellow).

![Highlighted green handle of selected slices](assets/img/fiji_selected_slice.png)

There are two ways of selecting slices:
1. by drawing rectangles with the left mouse button. Modifier keys allow to add (`shift`) or remove (`ctrl`) slices to the current selection:
  * `hold and left-click` draw a rectangle to select slices
  * `ctrl + hold and left-click` remove slices from the current selection
  * `shift + hold and left-click` add slices to the current selection

---

:warning:  you need to hold the modifier keys BEFORE drawing the rectangle in order to take their effect into account. This is in contrast to a lot of other software and a limitation of the UI of BigDataViewer

---

Gif below : adding slices to selection using `shift+rectangle`, removing from the selection with `ctrl+rectangle`, finally selecting all slices with `ctrl+a`.

![Selecting slices in bdv](assets/gif/fiji_select_slices.gif)

2. by selecting slices in the `Slices Display` card table:

![Selecting slices in bdv](assets/gif/fiji_select_slices_table.gif)

`ctrl+a` allows to select all slices, `ctrl+shift+a` allows to deselect all slices.

## Slices display options

It will be convenient for the registration to have your slices properly displayed. Depending on your use case, you may want to display a subset of the available channels, and adjust the min and max value displayed for a good contrast.

But first, in multi series files like vsi files it could happen that you end up with unwanted images (label or macro image). In this case, you will need to remove these slices. For vsi files, these unwanted images, because they are rgb images, appear black in the slice display table. It is thus easy to select them. The selected slices can then be removed form ABBA by right clicking in the viewer window of ABBA and selecting `Remove Selected Slices` (also accessible in the menu bar : `Edit > Remove Selected Slices`). 

![Removing slices](assets/gif/fiji_remove_slices.gif)

Usually, slices will have multiple channels, autodetected by bio-formats, but while some channels will be useful for analysis, some will be useful just for the sake of registration to the atlas. In order to display only certain channels, you can activate to deactivate the display of selected slices by clicking on the header of the slice display table:

You can set the color and the min and max display values of these slices:

![Slices display options](assets/gif/fiji_slices_display_options.gif)

If needed, the display of each slice can be customised by modifying the corresponding line in the table.

## Registration to Allen Brain Atlas (2017 CCF v3)

This procedure starts first by a manual step, which has two goals:

* Estimate the position of each slice along the atlas
* Correct the atlas angles of slicing

In order to position each slice approximately along the axis, ABBA tries to provide a convenient interface to manipulate series of slices.

### First coarse positioning

#### Rotate / flip slices

It could happen that the acquired slices were flipped or rotated compared to the atlas. The tab `Edit Selected Slices` provides 4 actions which can be used to correct this:

![Edit selected slices](assets/img/fiji_edit_slices_tab.png)

The first two buttons rotate selected slices by 90 degrees CW or CCW. The next two buttons flip slices vertically or horizontally.

In the top menu bar `Edit>Rotate` can also be used to rotate selected slices along any XYZ axis and with a custom angle in degree (you'll most probably need Z axis rotation).

Contrary to a lot of other actions in ABBA, these actions (flip rotate) are not undone with `ctrl+Z` (and redone with `ctrl+shift+z`). These actions can be easily reversed by applying an opposite rotation / flip.

#### Correct location of slices along the axis

Before any registration can be started, you will need to position the slices along the Z axis, and also correct the atlas slicing angles to match those of your dataset. The atlas slicing angle will be the same for all  slices, which is the reason why it's convenient to register one animal at a time.

##### Drag selected slices
First of all, it may happen that your slices are not sorted correctly along the atlas axis. If this is the case, you can select slices which are not at their correct position and drag them along the axis. You can create some interval if necessary in between two slices in order to let others in between if needed.

You can select one or several slices and then, by dragging the rectangles located below the atlas, you can shift selected slices along the slicing axis:

![Drag slices along axis](assets/gif/fiji_slices_drag.gif)

##### Distribute spacing between selected slices

In order to apply the same spacing between each selected slices, you can either click the button `distribute spacing` in the card `Edit Selected Slices` or press the shortcut key `d`. This action, as many other, can be cancelled by pressing `ctrl+z` and redone by pressing `ctrl+shift+z`:  

![Distribute slices](assets/gif/fiji_slices_distribute.gif)

When no slice is a key slice ( see next section ), `distribute spacing` keeps constant the position of the first and last selected slices.

##### Lock the position of slices by setting "key slices"

By using ABBA interface, you will be able (and will need), using pan and zooms to switch between a zoomed-out overview and a zoomed-in view where you look precisely at how a particular slice matches the atlas.

Generally, to align the position of slices along the atlas, a convenient workflow is the following, you will need to follow these steps:

* as mentioned before, sort slices correctly along the axis
* rotate / flip slices to match the orientation of the atlas  
* display the atlas sliced with the largest slices thickness ( one slice every 500 microns ):
* approximately shift slices along the atlas
* match precisely a slice of your choice (usually one with easily recognizable features) with the atlas by zooming in
* set this slice as `key slice`. As long as you don't drag this slice, each slice which is a key slice has its z position locked in the atlas
* set a few others slices precisely and set them as key slices
* adjust the angles of the atlas slicing and check all slices

![Coarsening atlas slicing](assets/gif/fiji_atlas_coarse_slicing.gif)

Automatically, the position of the slices within the dataset will be adjusted to the new atlas slicing. It's important to understand that the slicing being displayed does not affect the registration in any way. Internally, all slices all positioned with 10 microns precision, corresponding to the atlas highest resolution.

This coarse display allows, by dragging slices, to adjust approximately all slices along the atlas.


[**Back to step by step tutorial**](usage.md)
