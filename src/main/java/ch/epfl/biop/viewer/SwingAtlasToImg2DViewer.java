package ch.epfl.biop.viewer;

import ch.epfl.biop.atlas.BiopAtlas;
import ch.epfl.biop.atlastoimg2d.AtlasToImg2D;
import net.imagej.ui.swing.viewer.EasySwingDisplayViewer;
import org.scijava.plugin.Plugin;
import org.scijava.ui.viewer.DisplayViewer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
        System.out.println("Redraw from "+this+" called.");
        if (atlasToImg.isRegistrationSet()) {

        }
    }

    AtlasToImg2D atlasToImg = null;

    JPanel mainPanel;

    @Override
    protected JPanel createDisplayPanel(AtlasToImg2D value) {
        atlasToImg = value;
        mainPanel = new JPanel();

        JButton backToAtlasView = new JButton("Set Atlas to Choosen Location");

        backToAtlasView.addActionListener(actionEvent -> atlasToImg.getAtlas().map.setCurrentLocation(
                        atlasToImg.getAtlasLocation())
        );

        mainPanel.add(backToAtlasView);
        return mainPanel;
    }

    // Needs a button to add a transformation, or to retry the registration
    // Something to save and retrieve all of these, and which is convenient
    // Also needs to be QuPath compatible
    // Something which deletes the registration
    // Button set Atlas Location to initial place
    // BUtton reset registration
    // Button re-run registration
    // Button put atlas ROI to ROIManager


    // 1: Image (type T) toString
    // 2: Atlas
    // 3: Atlas location
    // 4: Registration
    // 5: Rois
    // 6: Filter
    // 7: Actions
    //


}
