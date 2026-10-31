A 2D ANTs SyN warp between two synthetic images of 64 x 48 pixels, spacing (0.5, 0.4) mm, origin (10, -5) mm,
direction rotated by 0.3 rad (ITK/LPS physical space), made with antspyx 0.6.3:
  o_1Warp.nii.gz   the warp, as ANTs wrote it (NIfTI, intent 1007, dim [5, 64, 48, 1, 1, 2])
  warp.mha         the same warp, rewritten by ITK 5.4.7 as a MetaImage
  points_ants.csv  fixed points p (px, py) and where ANTs itself maps them with the warp alone
                   (q_warp_only, ants.apply_transforms_to_points); rows 0-7 are pixel centres, 8-19 are between pixels
