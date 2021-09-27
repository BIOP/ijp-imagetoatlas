# Exporting ABBA's registration results

Once your dataset is registered, there are many ways you can export these registration results. Probably the most useful way is to continue the analysis in QuPath (provided that you started on QuPath). For this procedure, please go to [analysis in qupath](qupath_analysis.md).

However, other export options are available within Fiji. 

## Principle

Briefly, in ABBA, the registration that is stored for each slice is a function that's capable to match a point located in the atlas to a pixel coordinates in a slice. (forward transform)

If the registration is not too weird, it is possible to inverse this function, and thus have a way to match a pixel of a slice to its corresponding atlas coordinates. (backward transform)

With this invertible transform in hand, the export possibilities are of many types:

1. it is possible to warp a slice onto the atlas
2. it is possible to warp the atlas onto a slice
3. it is possible to export an image of the atlas coordinates onto the slice
4. it is possible to warp an atlas region onto the slice

The point 4 corresponds to what happens in the QuPath workflow, and thus won't be detailed here. Note that it is the fastest way to export the results (you just need to deform the outline of each regions) and, because your original data is not deformed, it is also the best way to perform subsequent analysis.

The other options are detailed below.

## 1. Warping slices onto the atlas

### Why it is not so obvious

During ABBA's workflow, you observe that as you add registration steps, slices become better and better registered to the atlas (the atlas itself is sliced, but not deformed). So we already have warped slices on the atlas, no? In fact, the warped slices that you see in BigDataViewer cannot be used directly for analysis: only the part that needs to be displayed is warped on the fly, and at the resolution needed for a screen display. This allows to interactively navigate registered slices, but won't allow for an easy analysis.

To export warped slices, you will need to first choose a pixel size for the export, before the computation of a warped slice image can take place. This process is also called rasterization.

If you choose a very small pixel size, be aware that the computation can take a long time. So try first an export with a big pixel size (40 microns for instance). Note that diminishing by 2 the pixel size will lead to a multiplication by 4 of the computation time.

### Export warped slices

In ABBA, select the slices you want to export, and click `Export> ABBA - Export registered slices to ImageJ`. 

![Exporting slices options](assets/img/fiji_export_registered_slices_imagej.png)

You can specify the channels that you want to export, give a name to the exported stack. Clicking `interpolate` will lead to a smoother image at the cost of computation speed.

When this process is done, you end up with an ImageJ image stack, which can be saved and handled as any other regular image in ImageJ.

---

:warning: If you choose a really too small pixel size, you may reach ImageJ's limitation of 2 Gpixels per plane.

---

You may want to export only a subregion of the registered slices. This will allow to compute much faster the registered image. If you want to do this, open the `Define region of interest` panel, click `Define interactively` and draw a rectangular region with the mouse. 

Now the exported slices will be restricted to your user defined ROI.

![Exporting registered slices with defined region](assets/gif/fiji_export_registered_slices.gif)

If you want to export the atlas data in the same conditions (sampling and region), you can click `Edit> ABBA - Export Atlas to ImageJ`. 

![Export atlas options](assets/img/fiji_atlas_export_options.png)

![Export atlas gif](assets/gif/fiji_export_atlas.gif)

