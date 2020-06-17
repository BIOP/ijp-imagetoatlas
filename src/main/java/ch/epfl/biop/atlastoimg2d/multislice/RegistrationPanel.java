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
    //JLabel emailLabel = new JLabel("Email:");
    //JLabel avatarLabel = new JLabel("Avatar Image:");
    JTextField chAtlasField = new JTextField(10);
    JTextField chSliceField = new JTextField(1);

    //JTextField lNameField = new JTextField(15);
    //JTextField zipField = new JTextField(5);
    //JTextField emailField = new JTextField(20);
    //JTextField avatarField = new JTextField(30);
    JButton okBttn = new JButton("Apply to selected slices");


    //JButton cancelBttn = new JButton("Cancel");
    //JButton helpBttn = new JButton("Help");

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

        //panel.add(mNameField);

        //wrap keyword starts a new row
        /*panel.add(lNameField, "wrap");

        //align label triggers platform-specific label alignment
        panel.add(zipLabel, "align label");
        panel.add(zipField, "wrap");
        panel.add(emailLabel,"align label");

        //span keyword lets emailField use the rest of the row
        panel.add(emailField, "span");
        panel.add(avatarLabel, "align label");
        panel.add(avatarField, "span");

        //tag identifies the type of button

        //sizegroups set all members to the size of the biggest member
        panel.add(cancelBttn, "tag cancel, sizegroup bttn");
        panel.add(helpBttn, "tag help, sizegroup bttn");*/
    }

    public JPanel getPanel() {
        return panel;
    }


}
