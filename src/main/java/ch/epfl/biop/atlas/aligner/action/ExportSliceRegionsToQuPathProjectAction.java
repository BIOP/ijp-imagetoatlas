package ch.epfl.biop.atlas.aligner.action;

import ch.epfl.biop.atlas.aligner.CancelableAction;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.gui.bdv.ABBABdvViewPrefs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;

public class ExportSliceRegionsToQuPathProjectAction extends CancelableAction {

    protected static final Logger logger = LoggerFactory.getLogger(ExportSliceRegionsToQuPathProjectAction.class);

    final SliceSources slice;
    final boolean erasePreviousFile;

    public ExportSliceRegionsToQuPathProjectAction(MultiSlicePositioner mp, SliceSources slice, boolean erasePreviousFile) {
        super(mp);
        this.slice = slice;
        this.erasePreviousFile = erasePreviousFile;
    }

    @Override
    protected boolean run() {
        logger.info("Exporting slice "+slice+" registration to QuPath");
        slice.exportToQuPathProject(erasePreviousFile);
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
        g.setColor(ABBABdvViewPrefs.text_action_export_slice_regions_to_qupath);
        g.drawString("E", (int) px - 4, (int) py + 5);
    }

    @Override
    protected boolean cancel() {
        logger.debug("Export to QuPath cancel : no action");
        return true;
    }

    @Override
    public SliceSources getSliceSources() {
        return slice;
    }

}