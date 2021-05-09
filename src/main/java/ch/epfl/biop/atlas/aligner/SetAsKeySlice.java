package ch.epfl.biop.atlas.aligner;

import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.ui.behaviour.ClickBehaviour;

import java.awt.*;
import java.util.function.Supplier;

/**
 * Moves a slice to a new position along the slicing axis
 */
public class SetAsKeySlice extends CancelableAction {

    private final SliceSources sliceSource;
    //GraphicalHandle gh;

    public SetAsKeySlice(MultiSlicePositioner mp, SliceSources sliceSource) {
        super(mp);
        this.sliceSource = sliceSource;
    }

    @Override
    public SliceSources getSliceSources() {
        return sliceSource;
    }

    public boolean run() {
        if (!sliceSource.isKeySlice()) {
            sliceSource.keySliceOn();
            /*gh = new SquareGraphicalHandle(mp,
                    (ClickBehaviour) (x, y) -> System.out.println("slice "+sliceSource+" clicked"),
                    "move_key_slice_"+sliceSource,
                    "",
                    mp.getBdvh().getTriggerbindings(),
                    () -> {

                        RealPoint handlePoint = sliceSource.getGUIState().getCenterPositionPMode();
                        handlePoint.setPosition(+mp.sY/2.0, 1);

                        AffineTransform3D bdvAt3D = new AffineTransform3D();
                        mp.getBdvh().getViewerPanel().state().getViewerTransform(bdvAt3D);

                        bdvAt3D.apply(handlePoint, handlePoint);

                        //return new Integer[]{(int) handlePoint.getDoublePosition(0), (int) handlePoint.getDoublePosition(1), 0};
                        return new Integer[]{(int) 250, (int) 250, 0};

                    },
                    () -> 25,
                    () -> new Integer[]{255, 0, 255, 200});
            mp.ghs.add(gh);
            gh.enable();*/
            return true;
        } else return false; // already a key slice
    }

    public String toString() {
        return "Key slice";
    }

    public boolean cancel() {
        sliceSource.keySliceOff();
        //gh.disable();
        //mp.ghs.remove(gh);
        return true;
    }

    public void drawAction(Graphics2D g, double px, double py, double scale) {
        g.drawString("K", (int) px - 5, (int) py + 5);//+new DecimalFormat("###.##").format(newSlicingAxisPosition), (int) px-5, (int) py+5);
    }

}