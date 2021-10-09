package ch.epfl.biop.atlas.aligner.gui;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;

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

    public NavigationPanel(MultiSlicePositioner mp) {
        paneDisplay = new JPanel();

        paneDisplay.setLayout(new BoxLayout(paneDisplay, BoxLayout.X_AXIS));

        reviewMode = new JButton("Review");
        reviewMode.addActionListener(e -> mp.setReviewMode());

        positioningMode = new JButton("Positioning");
        positioningMode.addActionListener(e -> mp.setPositioningMode());

        singleSliceDisplayMode = new JButton("Display One Slice Only (fast)");
        singleSliceDisplayMode.addActionListener(e -> mp.setSliceDisplayMode(MultiSlicePositioner.CURRENT_SLICE_DISPLAY_MODE));

        allSlicesDisplayMode = new JButton("Display All Slices");
        allSlicesDisplayMode.addActionListener(e -> mp.setSliceDisplayMode(MultiSlicePositioner.ALL_SLICES_DISPLAY_MODE));

        changeOverlapMode = new JButton("Change Overlap Mode");
        changeOverlapMode.addActionListener(e -> mp.toggleOverlap());

        gotoPreviousSlice = new JButton("Previous [Left]");
        gotoPreviousSlice.addActionListener(e -> mp.navigatePreviousSlice());

        centerOnCurrentSlice = new JButton("Center Current [C]");
        centerOnCurrentSlice.addActionListener(e -> mp.navigateCurrentSlice());

        gotoNextSlice = new JButton("Next [Right]");
        gotoNextSlice.addActionListener(e -> mp.navigateNextSlice());

        paneDisplay.add(box(false,
                new JLabel("Modes"),
                box(true,reviewMode, positioningMode),
                new JLabel("Slice Display"),
                box(true, singleSliceDisplayMode, allSlicesDisplayMode),
                new JLabel("Overlap (positioning)"),
                changeOverlapMode,
                new JLabel("Navigate Slice"),
                box(true, gotoPreviousSlice, centerOnCurrentSlice, gotoNextSlice)));

    }

    public JPanel box(boolean alongX,JComponent... components) {
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
