package ch.epfl.biop.atlas.aligner.gui;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;

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
        paneEdit.add(turnCW);

        // CCW
        URL turnCCWURLIcon = MultiSlicePositioner.class.getResource("/graphics/TurnCCW.png");
        ImageIcon iconCCW = new ImageIcon(turnCCWURLIcon);
        iconCCW = new ImageIcon( iconCCW.getImage().getScaledInstance( 35, 35,  java.awt.Image.SCALE_SMOOTH ) );

        JButton turnCCW = new JButton(iconCCW);
        turnCCW.setToolTipText("Rotate selected slices 90 degrees counter clockwise");
        turnCCW.addActionListener(e -> mp.rotateSlices(2,-Math.PI/2.0));
        paneEdit.add(turnCCW);

        // Rotate X (/flipX)
        URL turnRotXURLIcon = MultiSlicePositioner.class.getResource("/graphics/RotX.png");
        ImageIcon iconRotX = new ImageIcon(turnRotXURLIcon);
        iconRotX = new ImageIcon( iconRotX.getImage().getScaledInstance( 35, 35,  java.awt.Image.SCALE_SMOOTH ) );

        JButton rotX = new JButton(iconRotX);
        rotX.setToolTipText("Rotate around X axis (~ flip vertically)");
        rotX.addActionListener(e -> mp.rotateSlices(0,Math.PI));
        paneEdit.add(rotX);

        // Rotate X (/flipX)
        URL turnRotYURLIcon = MultiSlicePositioner.class.getResource("/graphics/RotY.png");
        ImageIcon iconRotY = new ImageIcon(turnRotYURLIcon);
        iconRotY = new ImageIcon( iconRotY.getImage().getScaledInstance( 35, 35,  java.awt.Image.SCALE_SMOOTH ) );

        JButton rotY = new JButton(iconRotY);
        rotY.setToolTipText("Rotate around Y axis (~ flip horizontally)");
        rotY.addActionListener(e -> mp.rotateSlices(1,Math.PI));
        paneEdit.add(rotY);

        JButton distribute = new JButton("Distribute Spacing");
        distribute.addActionListener((e) -> mp.equalSpacingSelectedSlices());
        paneEdit.add(distribute);

    }

    public JPanel getPanel() {
        return paneEdit;
    }

}
