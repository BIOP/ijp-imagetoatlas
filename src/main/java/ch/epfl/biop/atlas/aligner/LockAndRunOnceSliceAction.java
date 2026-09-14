package ch.epfl.biop.atlas.aligner;

import java.awt.Graphics2D;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Runs a task once, when the action queues of several slices have all reached it: one such action is enqueued on
 * each slice, sharing the counter, and nothing else runs on these slices until the task is done.
 * Once run, the action is not kept in the undo stack: undoing it would do nothing.
 */
public class LockAndRunOnceSliceAction extends CancelableAction {

    private final SliceSources sliceSource;
    private final AtomicInteger counter;
    private final int counterTarget;

    private final Supplier<Boolean> runnable;

    private final AtomicBoolean result;

    private volatile boolean done = false;

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
            int counterValue = counter.incrementAndGet();
            if (counterValue == counterTarget) {
                // Run the thing
                result.set(runnable.get());
                synchronized (counter) {
                    counter.incrementAndGet();
                    counter.notifyAll();
                }
            } else {
                synchronized (counter) {
                    // Checked while holding the monitor: the task may end before this thread waits
                    while (counter.get() != counterTarget + 1) {
                        try {
                            counter.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            done = true;
        }
        return result.get();
    }

    @Override
    public boolean isValid() {
        return !done;
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