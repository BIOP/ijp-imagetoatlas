package ch.epfl.biop.atlastoimg2d.multislice;

import java.awt.*;
import java.io.File;

public class ExportSliceRegionsToFile extends CancelableAction {

    final SliceSources slice;
    String namingChoice;
    File dirOutput;
    boolean erasePreviousFile;

    public ExportSliceRegionsToFile(MultiSlicePositioner mp, SliceSources slice, String namingChoice, File dirOutput, boolean erasePreviousFile) {
        super(mp);
        this.slice = slice;
        this.namingChoice = namingChoice;
        this.dirOutput = dirOutput;
        this.erasePreviousFile = erasePreviousFile;
    }

    @Override
    public boolean run() { //
        System.out.println("Exporting slice registration");
        slice.exportRegionsToFile(namingChoice, dirOutput, erasePreviousFile);
        return true;
    }

    public String toString() {
        return "Export";
    }

    public void drawAction(Graphics2D g, double px, double py, double scale) {
        switch (slice.getActionState(this)){
            case "(done)":
                g.setColor(new Color(0, 255, 0, 200));
                break;
            case "(locked)":
                g.setColor(new Color(255, 0, 0, 200));
                break;
            case "(pending)":
                g.setColor(new Color(255, 255, 0, 200));
                break;
        }
        g.fillOval((int) (px - 7), (int) (py - 7), 14, 14);
        g.setColor(new Color(255, 255, 255, 200));
        g.drawString("E", (int) px - 4, (int) py + 5);
    }

    @Override
    public boolean cancel() {
        mp.log.accept("Export cancel : no action");
        return false;
    }

    @Override
    public SliceSources getSliceSources() {
        return slice;
    }

}