package ch.epfl.biop.atlas.aligner.action;

import ch.epfl.biop.atlas.aligner.CancelableAction;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.gui.bdv.ABBABdvViewPrefs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;

public class ExportSliceRegionsToQuPathProjectAction extends CancelableAction {

    protected static final Logger logger = LoggerFactory.getLogger(ExportSliceRegionsToQuPathProjectAction.class);

    final SliceSources slice;
    final boolean erasePreviousFile;
    final File projectFolder; // null: the project the slice was imported from
    final File dataEntryFolder;

    /**
     * Exports the slice to the QuPath project it was imported from.
     */
    public ExportSliceRegionsToQuPathProjectAction(MultiSlicePositioner mp, SliceSources slice, boolean erasePreviousFile) {
        this(mp, slice, null, null, erasePreviousFile);
    }

    /**
     * Exports the slice to an image of a QuPath project which has the same pixel grid as the slice.
     */
    public ExportSliceRegionsToQuPathProjectAction(MultiSlicePositioner mp, SliceSources slice, File projectFolder, File dataEntryFolder, boolean erasePreviousFile) {
        super(mp);
        this.slice = slice;
        this.projectFolder = projectFolder;
        this.dataEntryFolder = dataEntryFolder;
        this.erasePreviousFile = erasePreviousFile;
    }

    @Override
    protected boolean run() {
        logger.info("Exporting slice "+slice+" registration to QuPath");
        if (projectFolder == null) {
            slice.exportToQuPathProject(erasePreviousFile);
        } else {
            slice.exportToQuPathEntry(projectFolder, dataEntryFolder, erasePreviousFile);
        }
        return true;
    }

    public String toString() {
        return "Export";
    }

    public void drawAction(Graphics2D g, double px, double py, double scale) {
        switch (slice.getActionState(this)){
            case "(done)":
                g.setColor(ABBABdvViewPrefs.done_export);
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
        g.setFont(ABBABdvViewPrefs.action_font);
        g.drawString("E", (int) px - 4, (int) py + 3);
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