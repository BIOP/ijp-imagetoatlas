package ch.epfl.biop.atlas.aligner.gui.bdv.card;

import ch.epfl.biop.atlas.aligner.gui.bdv.BdvMultislicePositionerView;

import javax.swing.*;
import java.awt.*;

public class NavigationPanel {

    final JPanel paneDisplay;

    final JButton singleSliceDisplayMode;
    final JButton allSlicesDisplayMode;

    final JButton reviewMode;
    final JButton positioningMode;

    final JButton gotoNextSlice;
    final JButton centerOnCurrentSlice;
    final JButton gotoPreviousSlice;

    final JButton changeOverlapMode;
    final JSlider overlapFactorSlider;

    public NavigationPanel(BdvMultislicePositionerView view) {
        paneDisplay = new JPanel();

        paneDisplay.setLayout(new BoxLayout(paneDisplay, BoxLayout.X_AXIS));

        reviewMode = new JButton("Review");
        reviewMode.addActionListener(e -> view.setDisplayMode(BdvMultislicePositionerView.REVIEW_MODE_INT));

        positioningMode = new JButton("Positioning");
        positioningMode.addActionListener(e -> view.setDisplayMode(BdvMultislicePositionerView.POSITIONING_MODE_INT));

        singleSliceDisplayMode = new JButton("Display One Slice Only (fast)");
        singleSliceDisplayMode.addActionListener(e -> view.setSliceDisplayMode(BdvMultislicePositionerView.CURRENT_SLICE_DISPLAY_MODE));

        allSlicesDisplayMode = new JButton("Display All Slices");
        allSlicesDisplayMode.addActionListener(e -> view.setSliceDisplayMode(BdvMultislicePositionerView.ALL_SLICES_DISPLAY_MODE));

        changeOverlapMode = new JButton("Change Overlap Mode");
        changeOverlapMode.addActionListener(e -> view.toggleOverlap());

        gotoPreviousSlice = new JButton("Previous [Left]");
        gotoPreviousSlice.addActionListener(e -> view.navigatePreviousSlice());

        centerOnCurrentSlice = new JButton("Center Current [C]");
        centerOnCurrentSlice.addActionListener(e -> view.navigateCurrentSlice());

        gotoNextSlice = new JButton("Next [Right]");
        gotoNextSlice.addActionListener(e -> view.navigateNextSlice());

        overlapFactorSlider = new JSlider();
        overlapFactorSlider.addChangeListener(l -> view.setOverlapFactor(overlapFactorSlider.getValue()));

        paneDisplay.add(box(false,
                new JLabel("Modes"),
                box(true,reviewMode, positioningMode),
                new JLabel("Slice Display"),
                box(true, singleSliceDisplayMode, allSlicesDisplayMode),
                new JLabel("Overlap (positioning)"),
                box(false,changeOverlapMode,box(true,new JLabel("Overlap size"), overlapFactorSlider)),
                new JLabel("Navigate Slice"),
                box(true, gotoPreviousSlice, centerOnCurrentSlice, gotoNextSlice)));

    }

    public static JPanel box(boolean alongX,JComponent... components) {
        JPanel box = new JPanel();
        if (alongX) {
            box.setLayout(new GridLayout(1, components.length));
        } else {
            box.setLayout(new GridLayout(components.length, 1));//new BoxLayout(box, BoxLayout.Y_AXIS));
        }
        for(JComponent component : components) {
            box.add(component);
        }
        return box;
    }

    public JPanel getPanel() {
        return paneDisplay;
    }
}
