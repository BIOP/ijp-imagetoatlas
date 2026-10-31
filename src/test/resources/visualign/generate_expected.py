"""Writes expected.csv with PyNutil's own VisuAlign functions: python generate_expected.py PATH_TO_PYNUTIL_CLONE"""
import importlib.util, json, os, sys

spec = importlib.util.spec_from_file_location(
    "v", os.path.join(sys.argv[1], "PyNutil", "processing", "adapters", "visualign_deformations.py"))
v = importlib.util.module_from_spec(spec)
spec.loader.exec_module(v)
here = os.path.dirname(os.path.abspath(__file__))
with open(os.path.join(here, "expected.csv"), "w") as out:
    out.write("slice,x,y,to_atlas_x,to_atlas_y,to_slice_x,to_slice_y\n")
    for i, s in enumerate(json.load(open(os.path.join(here, "alignment.json")))["slices"]):
        if not s.get("markers"):
            continue
        tri = v.triangulate(s["width"], s["height"], s["markers"])
        for x in (150.0, 450.0, 750.0, 1050.0, 1350.0):
            for y in (250.0, 750.0):
                ax, ay = v.transform_vec(tri, [x], [y])
                sx, sy = v.forwardtransform_vec(tri, [x], [y])
                out.write("%d,%g,%g,%.9f,%.9f,%.9f,%.9f\n" % (i, x, y, ax[0], ay[0], sx[0], sy[0]))
