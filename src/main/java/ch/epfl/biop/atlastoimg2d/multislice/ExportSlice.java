package ch.epfl.biop.atlastoimg2d.multislice;


import java.awt.*;
import java.io.File;

public class ExportSlice extends CancelableAction {

    final SliceSources slice;
    String namingChoice;
    File dirOutput;
    boolean erasePreviousFile;

    public ExportSlice(MultiSlicePositioner mp, SliceSources slice, String namingChoice, File dirOutput, boolean erasePreviousFile) {
        super(mp);
        this.slice = slice;
        this.namingChoice = namingChoice;
        this.dirOutput = dirOutput;
        this.erasePreviousFile = erasePreviousFile;
    }

    @Override
    public boolean run() { //
        System.out.println("Exporting slice registration");
        slice.export(namingChoice, dirOutput, erasePreviousFile);
        return true;
    }

    public String toString() {
        return "Export";
    }

    public void drawAction(Graphics2D g, double px, double py, double scale) {
        g.setColor(new Color(255, 0, 0, 200));
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