package ch.epfl.biop.atlas.aligner.action;

import ch.epfl.biop.atlas.aligner.CancelableAction;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.gui.bdv.ABBABdvViewPrefs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;

public class ExportSliceRegionsToFileAction extends CancelableAction {

    protected static final Logger logger = LoggerFactory.getLogger(ExportSliceRegionsToFileAction.class);

    final SliceSources slice;
    final String namingChoice;
    final File dirOutput;
    final boolean erasePreviousFile;

    public ExportSliceRegionsToFileAction(MultiSlicePositioner mp, SliceSources slice, String namingChoice, File dirOutput, boolean erasePreviousFile) {
        super(mp);
        this.slice = slice;
        this.namingChoice = namingChoice;
        this.dirOutput = dirOutput;
        this.erasePreviousFile = erasePreviousFile;
    }

    @Override
    protected boolean run() {
        logger.info("Exporting slice registration of slice "+slice);
        slice.exportRegionsToFile(namingChoice, dirOutput, erasePreviousFile);
        return true;
    }

    public String toString() {
        return "Export";
    }

    public void drawAction(Graphics2D g, double px, double py, double scale) {
        switch (slice.getActionState(this)){
            case "(done)":
                g.setColor(ABBABdvViewPrefs.done);
                break;
            case "(locked)":
                g.setColor(ABBABdvViewPrefs.locked);
                break;
            case "(pending)":
                g.setColor(ABBABdvViewPrefs.pending);
                break;
        }
        g.fillOval((int) (px - 7), (int) (py - 7), 14, 14);
        g.setColor(ABBABdvViewPrefs.text_action_export_slice_region_to_file);
        g.drawString("E", (int) px - 4, (int) py + 5);
    }

    @Override
    protected boolean cancel() {
        logger.debug("Cancelling export action : no action");
        return false;
    }

    @Override
    public SliceSources getSliceSources() {
        return slice;
    }

}