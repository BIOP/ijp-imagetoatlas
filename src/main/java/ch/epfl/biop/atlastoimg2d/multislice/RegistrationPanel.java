package ch.epfl.biop.atlastoimg2d.multislice;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;

public class RegistrationPanel {

    final MultiSlicePositioner mp;

    JPanel panel;

    JLabel settingsLabel = new JLabel("Channels used for registration (comma separated)");

    JLabel registrationTypeLabel = new JLabel("Registration Type:");
    JComboBox registrationType = new JComboBox(new String[]{"Manual Affine", "Auto Elastix Affine", "Manual BigWarp", "Auto Elastix BigWarp"});

    JLabel chAtlasLabel = new JLabel("Atlas Image:");
    JLabel chSliceLabel = new JLabel("Slice Channel:");
    JTextField chAtlasField = new JTextField(10);
    JTextField chSliceField = new JTextField(1);

    JButton okBttn = new JButton("Apply to selected slices");

    public RegistrationPanel(MultiSlicePositioner mp) {
        this.mp = mp;

        panel = new JPanel();
        panel.setLayout(new MigLayout());
        buildMiGForm(panel);
    }

    private void buildMiGForm(JPanel panel) {

        panel.add(registrationTypeLabel, "align label");
        panel.add(registrationType, "wrap");

        panel.add(settingsLabel, "span, center, gapbottom 15");

        panel.add(chAtlasLabel, "align label");
        panel.add(chAtlasField, "span, wrap");

        panel.add(chSliceLabel, "align label");
        panel.add(chSliceField, "span, grow");

        panel.add(okBttn, "span");
    }

    public JPanel getPanel() {
        return panel;
    }


}
