package ch.epfl.biop.viewer.swing;

import ch.epfl.biop.atlastoimg2d.AtlasToImg2D;
import org.scijava.plugin.Plugin;
import org.scijava.ui.swing.viewer.EasySwingDisplayViewer;
import org.scijava.ui.viewer.DisplayViewer;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;

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
        /*if (atlasToImg.isRegistrationSet()) {

        }*/
    }

    AtlasToImg2D atlasToImg = null;

    JPanel mainPanel;

    HashMap<String, JPanel> panes;

    void updateAllPanes() {
        updateAtlasImageIdPane();
    }

    void updateAtlasImageIdPane() {
        JPanel pane = this.panes.get(AtlasImageId);
        if (pane.getComponentCount()==0) {
            // let's init this
            pane.setLayout(new FlowLayout());
            pane.add(new JTextField("ATLAS ="));
            pane.add(new JTextField("Undef"));
            pane.add(new JTextField("Image ="));
            pane.add(new JTextField("Undef"));
        }
        if (atlasToImg.getAtlas()!=null) {
            // Atlas is set
            ((JTextField)(pane.getComponent(1))).setText(atlasToImg.getAtlas().toString());
        } else {
            // Atlas not set
            ((JTextField)(pane.getComponent(1))).setText("Undef");
        }

    }



    final public static String AtlasImageId = "Image + Atlas Ids";
    final public static String AtlasSliceSelection = "Atlas Slice Selection";
    final public static String Registration = "Registration";
    final public static String ROIDisplayExport ="Atlas Region Display/Export";


    @Override
    protected JPanel createDisplayPanel(AtlasToImg2D value) {
        atlasToImg = value;
        panes = new HashMap<>();
        mainPanel = new JPanel();
        mainPanel.setLayout( new BorderLayout());

        JButton backToAtlasView = new JButton("Set Atlas to Choosen Location");

        backToAtlasView.addActionListener(actionEvent -> {
                if (atlasToImg.getAtlasLocation()!=null) {
                    atlasToImg.getAtlas().map.setCurrentLocation(atlasToImg.getAtlasLocation());
                }
            }
        );

        // WORKFLOW:
        // -> Set Image to be aligned with (User input)
        // -> Get Atlas slice location in 2D (Potential user input)
        // -> Get Atlas structure slice
        // -> Do registration (Potential user input)
        // -> Transform label image according to registration
        // -> Compute ROIs
        // -> Display ROIs from any structure to any image
        JTabbedPane tabbedPane = new JTabbedPane();
        JPanel imageAtlasIdPanel = new JPanel();
        panes.put(AtlasImageId, imageAtlasIdPanel);
        JScrollPane scroll = new JScrollPane(imageAtlasIdPanel);
        tabbedPane.addTab(AtlasImageId, scroll);

        JPanel atlasSliceSelectionPane = new JPanel();
        panes.put(AtlasSliceSelection, atlasSliceSelectionPane);
        scroll = new JScrollPane(atlasSliceSelectionPane);
        tabbedPane.addTab(AtlasSliceSelection, scroll);

        JPanel registrationPane = new JPanel();
        panes.put(Registration, registrationPane);
        scroll = new JScrollPane(registrationPane);
        tabbedPane.addTab(Registration, scroll);

        JPanel displayAtlasRegionPane = new JPanel();
        panes.put(ROIDisplayExport, displayAtlasRegionPane);
        scroll = new JScrollPane(displayAtlasRegionPane);
        tabbedPane.addTab(ROIDisplayExport, scroll);

        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        updateAllPanes();
        //mainPanel.add(backToAtlasView);
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


    // WORKFLOW:
    // -> Set Image to be aligned with (User input)
    // -> Get Atlas slice location in 2D (Potential user input)
    // -> Get Atlas structure slice
    // -> Do registration (Potential user input)
    // -> Transform label image according to registration
    // -> Compute ROIs
    // -> Display ROIs from any structure to any image


    // 1: Image (type T) toString
    // 2: Atlas
    // 3: Atlas location
    // 4: Registration
    // 5: Rois
    // 6: Filter
    // 7: Actions
    //


}
