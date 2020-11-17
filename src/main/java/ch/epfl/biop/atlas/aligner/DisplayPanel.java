package ch.epfl.biop.atlas.aligner;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class DisplayPanel {

    JPanel paneDisplay;

    public DisplayPanel(MultiSlicePositioner mp) {
        paneDisplay = new JPanel(new FlowLayout());

        JButton toggleDisplayMode = new JButton("Multi/Single Slice");
        toggleDisplayMode.addActionListener(e -> {
            mp.toggle_display_mode();
        });

        paneDisplay.add(toggleDisplayMode);
    }

    public JPanel getPanel() {
        return paneDisplay;
    }

}
