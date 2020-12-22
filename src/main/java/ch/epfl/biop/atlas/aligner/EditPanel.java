package ch.epfl.biop.atlas.aligner;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class EditPanel {

    JPanel paneEdit;

    public EditPanel(MultiSlicePositioner mp) {
        paneEdit = new JPanel(new FlowLayout());

        // CW
        URL turnCWURLIcon = MultiSlicePositioner.class.getResource("/graphics/TurnCW.png");
        ImageIcon iconCW = new ImageIcon(turnCWURLIcon);
        iconCW = new ImageIcon( iconCW.getImage().getScaledInstance( 35, 35,  java.awt.Image.SCALE_SMOOTH ) );

        JButton turnCW = new JButton(iconCW);
        turnCW.setToolTipText("Rotate selected slices 90 degrees clockwise");
        turnCW.addActionListener(e -> mp.rotateSlices(2,Math.PI/2.0));

        // CCW
        URL turnCCWURLIcon = MultiSlicePositioner.class.getResource("/graphics/TurnCCW.png");
        ImageIcon iconCCW = new ImageIcon(turnCCWURLIcon);
        iconCCW = new ImageIcon( iconCCW.getImage().getScaledInstance( 35, 35,  java.awt.Image.SCALE_SMOOTH ) );

        JButton turnCCW = new JButton(iconCCW);
        turnCCW.setToolTipText("Rotate selected slices 90 degrees counter clockwise");
        turnCCW.addActionListener(e -> mp.rotateSlices(2,-Math.PI/2.0));

        paneEdit.add(turnCW);
        paneEdit.add(turnCCW);
    }

    public JPanel getPanel() {
        return paneEdit;
    }

}
