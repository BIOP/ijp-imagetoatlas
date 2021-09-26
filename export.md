# Exporting ABBA's registration results

Once your dataset is registered, there are many ways you can export these results. The most useful way is to continue the analysis in QuPath (provided you started on QuPath). For this procedure, please go to the [qupath analysis part](qupath_analysis.md).

However, it is also possible to export the results differently within Fiji. The main use is for probably for display rather than analysis, but other application may exist.

Briefly, in ABBA, the registration that is stored for each slice is a function that's capable to match a point located in the atlas to a pixel coordinates in a slice. (forward transform)

Since the function is invertible by construction (if the deformation is not too weird), it is also possible to have the function that matches a pixel in a slice to the atlas coordinates. (backward transform)

With this invertible transform, the possibilities are  of many type:

1. it is possible to warp the slice onto the atlas
2. it is possible to warp the atlas onto the slice
3. it is possible to export an image of the atlas coordinates onto the (untransformed) slice
4. it is possible to warp an atlas region onto the slice

The point 4 correspond to what happens in the QuPath workflow, and thus won't be detailed here.

To be continued...