package ch.epfl.biop.viewer;

import ch.epfl.biop.atlas.BiopAtlas;
import ch.epfl.biop.atlastoimg2d.AtlasToImg2D;
import net.imagej.ui.swing.viewer.EasySwingDisplayViewer;
import org.scijava.plugin.Plugin;
import org.scijava.ui.viewer.DisplayViewer;

import javax.swing.*;

@Plugin(type = DisplayViewer.class)
public class SwingAtlasToImg2DViewer extends EasySwingDisplayViewer<AtlasToImg2D> {

    public SwingAtlasToImg2DViewer()
    {
        super( AtlasToImg2D.class );
    }

    @Override
    protected boolean canView(AtlasToImg2D value) {
        return true;
    }

    @Override
    protected void redoLayout() {

    }

    @Override
    protected void setLabel(String s) {

    }

    @Override
    protected void redraw() {

    }

    AtlasToImg2D atlasToImg = null;

    @Override
    protected JPanel createDisplayPanel(AtlasToImg2D value) {
        atlasToImg = value;

        return new JPanel();
    }

    // Needs a button to add a transformation, or to retry the registration
    // Something to save and retrieve all of these, and which is convenient
    // Also needs to be QuPath compatible
    // Something which deletes the registration
    // Button set Atlas Location to initial place
    // BUtton reset registration
    // Button re-run registration
    // Button put atlas ROI to ROIManager

}
