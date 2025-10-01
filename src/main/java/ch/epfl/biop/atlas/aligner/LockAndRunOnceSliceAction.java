package ch.epfl.biop.atlas.aligner;

import java.awt.Graphics2D;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Moves a slice to a new position along the slicing axis
 */
public class LockAndRunOnceSliceAction extends CancelableAction {

    private final SliceSources sliceSource;
    private final AtomicInteger counter;
    private final int counterTarget;

    private final Supplier<Boolean> runnable;

    private final AtomicBoolean result;

    private boolean done = false;

    public LockAndRunOnceSliceAction(MultiSlicePositioner mp,
                                     SliceSources sliceSource,
                                     AtomicInteger counter,
                                     int counterTarget,
                                     Supplier<Boolean> runnable,
                                     AtomicBoolean result) {
        super(mp);
        this.sliceSource = sliceSource;
        this.counter = counter;
        this.counterTarget = counterTarget;
        this.runnable = runnable;
        this.result = result;
        hide();
    }

    @Override
    public SliceSources getSliceSources() {
        return sliceSource;
    }

    protected boolean run() {
        if (!done) {
            //sliceSource.setSlicingAxisPosition(newSlicingAxisPosition);
            int counterValue = counter.incrementAndGet();
            if (counterValue == counterTarget) {
                // Run the thing
                result.set(runnable.get());
                counter.incrementAndGet();
                synchronized (counter) {
                    counter.notifyAll();
                }
            } else {
                while (counterValue != counterTarget + 1) {
                    synchronized (counter) {
                        try {
                            counter.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        counterValue = counter.get();
                    }
                }
            }
            done = true;
        }
        return result.get();
    }

    public String toString() {
        return "Lock Slice ";
    }

    protected boolean cancel() {
        //sliceSource.setSlicingAxisPosition(oldSlicingAxisPosition);
        return true;
    }

    public void drawAction(Graphics2D g, double px, double py, double scale) {
        g.drawString("L", (int) px - 5, (int) py + 5);//+new DecimalFormat("###.##").format(newSlicingAxisPosition), (int) px-5, (int) py+5);
    }

}