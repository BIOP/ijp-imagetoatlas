package ch.epfl.biop.atlas.aligner.warp;

import jitk.spline.ThinPlateR2LogRSplineKernelTransform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Landmark pairs whose thin-plate spline follows a warp closely, with a fixed budget of pairs.
 * <p>
 * The warp is known exactly at any point of a rectangular frame (its pixel grid, or its image): each frame point has
 * a fixed (atlas) position and a moving (slice) position, in mm. The spline built from pairs maps fixed to moving,
 * as an ABBA spline registration does. Pairs start on an even grid over the frame, then are added in batches where
 * the spline is the furthest from the warp, measured on the atlas side (where a slice point is drawn under the
 * atlas), at the points of a finer search grid. If a batch makes the spline fold (its Jacobian determinant drops to
 * zero or below at a search point), its pairs next to the fold are dropped and no pair is sought near them again.
 * <p>
 * The result is measured at held-out points of the frame, never used to place pairs: how far the spline, inverted,
 * draws each slice point from where the warp draws it.
 */
public class WarpLandmarks {

    /** A warp known exactly at any point of the frame [0, width] x [0, height] */
    public interface Warp {
        double width();

        double height();

        /** Writes the fixed (atlas) and moving (slice) positions in mm of the frame point (x, y) */
        void pair(double x, double y, double[] fixed, double[] moving);
    }

    /** Most pairs, 33 x 33: solving the spline takes about 2 s at this size and grows with its cube */
    public static final int BUDGET = 33 * 33;
    /** Pairs per side of the start grid: a quarter of the budget, so that most pairs go where the spline errs */
    static final int START_SIDE = 17;
    /** Search points along the longer side of the frame: about 40 um apart on a 10 mm section */
    static final int SEARCH_SIDE = 256;
    /** Pairs added at once: a dozen solves reach the budget, each batch placed after the previous one is solved */
    static final int BATCH = 64;
    /** Smallest distance, in search steps, from a new pair to a placed one: closer pairs add little and fold easily */
    static final double MIN_GAP_STEPS = 1.5;
    /** Smallest distance, in search steps, between the pairs of a batch, so that it does not spend itself on one peak */
    static final double BATCH_GAP_STEPS = 8;
    /**
     * A new pair this many search steps from a fold (twice the smallest gap) is dropped, and no pair is sought this close
     * to it again: dropping the pair alone lets the next batch pick its neighbours on the same crease, which fold too
     */
    static final double DROP_STEPS = 3;
    /** Smallest Jacobian determinant accepted: below it the spline folds or collapses */
    static final double MIN_JACOBIAN = 1e-6;
    /** No pair is added once the largest error at the search points is below 1 um, well below an atlas voxel */
    static final double TOLERANCE_MM = 0.001;

    public final double[][] fixed, moving;
    /** Atlas side distance at the held-out points between the spline and the warp, in mm */
    public final double medianMm, p99Mm, maxMm;
    public final int heldOutPoints;
    /** Times a batch made the spline fold, and pairs were dropped */
    public final int folds;

    WarpLandmarks(List<double[][]> pairs, double[] errors, int folds) {
        this.folds = folds;
        fixed = pairs.stream().map(p -> p[0]).toArray(double[][]::new);
        moving = pairs.stream().map(p -> p[1]).toArray(double[][]::new);
        Arrays.sort(errors);
        medianMm = errors[errors.length / 2];
        p99Mm = errors[(int) Math.ceil(0.99 * errors.length) - 1];
        maxMm = errors[errors.length - 1];
        heldOutPoints = errors.length;
    }

    /** Frame points with their pairs, and the warp's derivative there: how a slice side error shows on the atlas side */
    static class Samples {
        final double[][] points, fixed, moving, toAtlas;

        Samples(Warp warp, double step, double first) {
            List<double[]> lattice = new ArrayList<>();
            for (double y = first; y <= warp.height(); y += step) {
                for (double x = first; x <= warp.width(); x += step) lattice.add(new double[]{x, y});
            }
            int n = lattice.size();
            points = lattice.toArray(new double[0][]);
            fixed = new double[n][2];
            moving = new double[n][2];
            toAtlas = new double[n][];
            double eps = step / 4;
            double[] f1 = new double[2], m1 = new double[2], f2 = new double[2], m2 = new double[2];
            for (int i = 0; i < n; i++) {
                double x = points[i][0], y = points[i][1];
                warp.pair(x, y, fixed[i], moving[i]);
                warp.pair(x + eps, y, f1, m1);
                warp.pair(x, y + eps, f2, m2);
                // d fixed / d moving = (d fixed / d frame) (d moving / d frame)^-1
                double[] dm = invert(new double[]{m1[0] - moving[i][0], m2[0] - moving[i][0], m1[1] - moving[i][1], m2[1] - moving[i][1]});
                double[] df = {f1[0] - fixed[i][0], f2[0] - fixed[i][0], f1[1] - fixed[i][1], f2[1] - fixed[i][1]};
                toAtlas[i] = new double[]{df[0] * dm[0] + df[1] * dm[2], df[0] * dm[1] + df[1] * dm[3],
                        df[2] * dm[0] + df[3] * dm[2], df[2] * dm[1] + df[3] * dm[3]};
            }
        }
    }

    /**
     * @param warp the warp to follow
     * @param budget the most pairs
     * @throws IllegalArgumentException if the spline of the start grid folds: the warp itself folds
     */
    public static WarpLandmarks fit(Warp warp, int budget) {
        double step = Math.max(warp.width(), warp.height()) / SEARCH_SIDE;
        Samples search = new Samples(warp, step, 0);
        Samples probes = new Samples(warp, 2 * step, step / 2); // between the search points

        List<double[][]> pairs = new ArrayList<>();
        for (int j = 0; j < START_SIDE; j++) {
            for (int i = 0; i < START_SIDE; i++) {
                double[] f = new double[2], m = new double[2], frame = {warp.width() * i / (START_SIDE - 1), warp.height() * j / (START_SIDE - 1)};
                warp.pair(frame[0], frame[1], f, m);
                pairs.add(new double[][]{f, m, frame});
            }
        }
        ThinPlateR2LogRSplineKernelTransform spline = spline(pairs);
        if (!foldPoints(spline, search).isEmpty()) throw new IllegalArgumentException("the warp folds: no spline without folds follows it");
        boolean[] dropped = new boolean[search.points.length];
        int folds = 0;
        while (pairs.size() < budget) {
            double[] error = atlasErrors(spline, search);
            Integer[] order = new Integer[error.length];
            for (int i = 0; i < order.length; i++) order[i] = i;
            Arrays.sort(order, (a, b) -> Double.compare(error[b], error[a]));
            if (error[order[0]] <= TOLERANCE_MM) break;
            List<double[][]> trial = new ArrayList<>(pairs);
            for (int i : order) {
                if (trial.size() == Math.min(pairs.size() + BATCH, budget)) break;
                if (dropped[i]) continue;
                boolean apart = true;
                for (int k = 0; k < trial.size() && apart; k++) {
                    apart = distance(search.points[i], trial.get(k)[2]) > (k < pairs.size() ? MIN_GAP_STEPS : BATCH_GAP_STEPS) * step;
                }
                if (apart) trial.add(new double[][]{search.fixed[i], search.moving[i], search.points[i]});
            }
            if (trial.size() == pairs.size()) break;
            ThinPlateR2LogRSplineKernelTransform candidate = spline(trial);
            List<double[]> foldsAt = foldPoints(candidate, search);
            while (!foldsAt.isEmpty() && trial.size() > pairs.size()) {
                // Drops the new pairs next to a fold (all of them if none is), and the search points around them
                folds++;
                List<double[]> culprits = new ArrayList<>();
                for (int k = trial.size() - 1; k >= pairs.size(); k--) {
                    double[] p = trial.get(k)[2];
                    if (foldsAt.stream().anyMatch(q -> distance(p, q) <= DROP_STEPS * step)) culprits.add(trial.remove(k)[2]);
                }
                if (culprits.isEmpty()) {
                    while (trial.size() > pairs.size()) culprits.add(trial.remove(trial.size() - 1)[2]);
                }
                for (int k = 0; k < dropped.length; k++) {
                    double[] p = search.points[k];
                    dropped[k] |= culprits.stream().anyMatch(q -> distance(p, q) <= DROP_STEPS * step);
                }
                if (trial.size() == pairs.size()) break;
                candidate = spline(trial);
                foldsAt = foldPoints(candidate, search);
            }
            if (trial.size() == pairs.size()) continue;
            pairs = trial;
            spline = candidate;
        }
        return new WarpLandmarks(pairs, heldOutErrors(spline, probes), folds);
    }

    static ThinPlateR2LogRSplineKernelTransform spline(List<double[][]> pairs) {
        double[][] src = new double[2][pairs.size()], tgt = new double[2][pairs.size()];
        for (int i = 0; i < pairs.size(); i++) {
            for (int d = 0; d < 2; d++) {
                src[d][i] = pairs.get(i)[0][d];
                tgt[d][i] = pairs.get(i)[1][d];
            }
        }
        return new ThinPlateR2LogRSplineKernelTransform(2, src, tgt);
    }

    /** The frame points of the samples where the spline folds */
    static List<double[]> foldPoints(ThinPlateR2LogRSplineKernelTransform spline, Samples s) {
        return IntStream.range(0, s.points.length).parallel().filter(i -> {
            double[][] a = spline.jacobian(s.fixed[i]);
            if (a[0][0] * a[1][1] - a[0][1] * a[1][0] > MIN_JACOBIAN) return false;
            double[] j = jacobian(spline, s.fixed[i]); // jitk's derivative is wrong on a landmark
            return !(j[0] * j[3] - j[1] * j[2] > MIN_JACOBIAN);
        }).mapToObj(i -> s.points[i]).collect(Collectors.toList());
    }

    /** The slice side error at each search point, carried to the atlas side by the warp's derivative */
    static double[] atlasErrors(ThinPlateR2LogRSplineKernelTransform spline, Samples s) {
        return IntStream.range(0, s.points.length).parallel().mapToDouble(i -> {
            double[] q = spline.apply(s.fixed[i]);
            double ex = q[0] - s.moving[i][0], ey = q[1] - s.moving[i][1];
            double[] a = s.toAtlas[i];
            return Math.hypot(a[0] * ex + a[1] * ey, a[2] * ex + a[3] * ey);
        }).toArray();
    }

    /** Where the spline draws each slice point (its inverse, by Newton's method) against where the warp draws it */
    static double[] heldOutErrors(ThinPlateR2LogRSplineKernelTransform spline, Samples s) {
        return IntStream.range(0, s.points.length).parallel().mapToDouble(i -> {
            double[] x = s.fixed[i].clone();
            for (int k = 0; k < 20; k++) {
                double[] r = spline.apply(x);
                double[] j = invert(jacobian(spline, x));
                double dx = j[0] * (r[0] - s.moving[i][0]) + j[1] * (r[1] - s.moving[i][1]);
                double dy = j[2] * (r[0] - s.moving[i][0]) + j[3] * (r[1] - s.moving[i][1]);
                x[0] -= dx;
                x[1] -= dy;
                if (Math.hypot(dx, dy) < 1e-10) break;
            }
            return distance(x, s.fixed[i]);
        }).toArray();
    }

    /** The derivative of the spline (row major), by finite differences */
    static double[] jacobian(ThinPlateR2LogRSplineKernelTransform spline, double[] p) {
        double h = 1e-6;
        double[] q = spline.apply(p), qx = spline.apply(new double[]{p[0] + h, p[1]}), qy = spline.apply(new double[]{p[0], p[1] + h});
        return new double[]{(qx[0] - q[0]) / h, (qy[0] - q[0]) / h, (qx[1] - q[1]) / h, (qy[1] - q[1]) / h};
    }

    static double[] invert(double[] m) {
        double d = m[0] * m[3] - m[1] * m[2];
        return new double[]{m[3] / d, -m[1] / d, -m[2] / d, m[0] / d};
    }

    static double distance(double[] a, double[] b) {
        return Math.hypot(a[0] - b[0], a[1] - b[1]);
    }
}
