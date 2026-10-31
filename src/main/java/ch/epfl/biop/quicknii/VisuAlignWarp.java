/*
 * The triangulation and the piecewise affine map of this file are ported from PyNutil's visualign_deformations.py
 * (https://github.com/Neural-Systems-at-UIO/PyNutil), code by Gergely Csucs, Harry Carey and Rembrandt Bakker:
 *
 * MIT License
 *
 * Copyright (c) 2023 Harry Carey
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package ch.epfl.biop.quicknii;

import java.util.ArrayList;
import java.util.List;

/**
 * The nonlinear warp VisuAlign makes from the markers of a slice: a piecewise affine map on a triangulation of the
 * markers, as VisuAlign builds it (incremental Delaunay triangulation, with four fixed corners outside the image).
 * Ported from PyNutil, see the notice above.
 * <p>
 * A marker [x, y, nx, ny] moves the point (x, y) of the linearly anchored atlas plane to the point (nx, ny) of the
 * slice image, both in pixels of the width x height frame of the slice.
 */
public class VisuAlignWarp {

    final List<Triangle> triangles;

    public VisuAlignWarp(double w, double h, double[][] markers) {
        List<double[]> vertices = new ArrayList<>();
        vertices.add(new double[]{-0.1 * w, -0.1 * h, -0.1 * w, -0.1 * h});
        vertices.add(new double[]{1.1 * w, -0.1 * h, 1.1 * w, -0.1 * h});
        vertices.add(new double[]{-0.1 * w, 1.1 * h, -0.1 * w, 1.1 * h});
        vertices.add(new double[]{1.1 * w, 1.1 * h, 1.1 * w, 1.1 * h});
        int[] edges = new int[(markers.length + 4) * (markers.length + 3) / 2];
        List<Triangle> current = new ArrayList<>();
        current.add(new Triangle(0, 1, 2, vertices, edges));
        current.add(new Triangle(1, 2, 3, vertices, edges));
        edges[0] = edges[1] = edges[4] = edges[5] = 2;
        for (double[] marker : markers) {
            double x = marker[2], y = marker[3];
            boolean found = false;
            List<Triangle> keep = new ArrayList<>(), remove = new ArrayList<>();
            for (Triangle t : current) {
                if (!found && t.toAtlas(x, y) != null) found = true;
                if (t.inCircle(x, y)) remove.add(t);
                else keep.add(t);
            }
            if (found) {
                for (Triangle t : remove) t.removeEdges();
            } else {
                keep.addAll(remove);
            }
            int count = vertices.size();
            vertices.add(marker);
            for (int i = 0; i < count - 1; i++) {
                for (int j = i + 1; j < count; j++) {
                    if (edges[edgeIndex(i, j)] == 1) keep.add(new Triangle(i, j, count, vertices, edges));
                }
            }
            current = keep;
        }
        triangles = current;
    }

    /** @return the point (x, y) of the atlas plane drawn at the point (nx, ny) of the slice, or null outside the triangulation */
    public double[] toAtlas(double nx, double ny) {
        for (Triangle t : triangles) {
            double[] p = t.toAtlas(nx, ny);
            if (p != null) return p;
        }
        return null;
    }

    /** @return the point (nx, ny) of the slice where the point (x, y) of the atlas plane is drawn, or null outside the triangulation */
    public double[] toSlice(double x, double y) {
        for (Triangle t : triangles) {
            double[] p = t.toSlice(x, y);
            if (p != null) return p;
        }
        return null;
    }

    static int edgeIndex(int a, int b) {
        int i = Math.min(a, b), j = Math.max(a, b);
        return j * (j - 1) / 2 + i;
    }

    static class Triangle {
        final double[] a, b, c;
        final int[] edgeList;
        int[] edges;
        final double[] toAtlasDecomposition, toSliceDecomposition;
        final double den, mDenX, mDenY, r2Den;

        Triangle(int ia, int ib, int ic, List<double[]> vertices, int[] edgeList) {
            a = vertices.get(ia);
            b = vertices.get(ib);
            c = vertices.get(ic);
            this.edgeList = edgeList;
            edges = new int[]{edgeIndex(ia, ib), edgeIndex(ia, ic), edgeIndex(ib, ic)};
            for (int e : edges) edgeList[e]++;
            toSliceDecomposition = decomposition(0, 1);
            toAtlasDecomposition = decomposition(2, 3);
            // Circumcircle, on the slice side
            double a2 = distSquare(b[2], b[3], c[2], c[3]), b2 = distSquare(a[2], a[3], c[2], c[3]), c2 = distSquare(a[2], a[3], b[2], b[3]);
            double fa = a2 * (b2 + c2 - a2), fb = b2 * (c2 + a2 - b2), fc = c2 * (a2 + b2 - c2);
            den = fa + fb + fc;
            mDenX = fa * a[2] + fb * b[2] + fc * c[2];
            mDenY = fa * a[3] + fb * b[3] + fc * c[3];
            r2Den = distSquare(a[2] * den, a[3] * den, mDenX, mDenY);
        }

        /** The inverse of [[bx - ax, by - ay, 0], [cx - ax, cy - ay, 0], [ax, ay, 1]], first two columns, or null */
        double[] decomposition(int i, int j) {
            double m00 = b[i] - a[i], m01 = b[j] - a[j], m10 = c[i] - a[i], m11 = c[j] - a[j];
            double det = m00 * m11 - m01 * m10;
            if (det == 0) return null;
            double i00 = m11 / det, i01 = -m01 / det, i10 = -m10 / det, i11 = m00 / det;
            return new double[]{i00, i01, i10, i11, -(a[i] * i00 + a[j] * i10), -(a[i] * i01 + a[j] * i11)};
        }

        void removeEdges() {
            for (int e : edges) edgeList[e]--;
        }

        boolean inCircle(double x, double y) {
            return distSquare(x * den, y * den, mDenX, mDenY) < r2Den;
        }

        double[] toAtlas(double x, double y) {
            return barycentric(x, y, toAtlasDecomposition, 0, 1);
        }

        double[] toSlice(double x, double y) {
            return barycentric(x, y, toSliceDecomposition, 2, 3);
        }

        double[] barycentric(double x, double y, double[] d, int xi, int yi) {
            if (d == null) return null;
            double u = x * d[0] + y * d[2] + d[4], v = x * d[1] + y * d[3] + d[5];
            if (u < 0 || u > 1 || v < 0 || v > 1 || u + v > 1) return null;
            return new double[]{a[xi] + (b[xi] - a[xi]) * u + (c[xi] - a[xi]) * v, a[yi] + (b[yi] - a[yi]) * u + (c[yi] - a[yi]) * v};
        }

        static double distSquare(double ax, double ay, double bx, double by) {
            return (ax - bx) * (ax - bx) + (ay - by) * (ay - by);
        }
    }
}
