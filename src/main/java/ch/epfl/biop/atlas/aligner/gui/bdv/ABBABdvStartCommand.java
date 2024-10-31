package ch.epfl.biop.atlas.aligner.gui.bdv;

import bdv.util.BdvHandle;
import ch.epfl.biop.atlas.aligner.ABBAHelper;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.command.ABBAStartCommand;
import ch.epfl.biop.atlas.struct.Atlas;
import org.scijava.Initializable;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.bdv.supplier.DefaultBdvSupplier;
import sc.fiji.bdvpg.bdv.supplier.SerializableBdvOptions;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

@SuppressWarnings("unused")
@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>ABBA - Align Big Brains and Atlases (BDV)",
        description = "Starts ABBA from an Atlas with a BDV View")
public class ABBABdvStartCommand implements Command, Initializable {

    @Parameter
    public Atlas ba;

    @Parameter
    CommandService cs;

    @Parameter(type = ItemIO.OUTPUT)
    BdvMultislicePositionerView view;

    @Override
    public void run() {
        try {
            MultiSlicePositioner mp = (MultiSlicePositioner) cs
                .run(ABBAStartCommand.class, true,
                    "ba", ba)
                    .get()
                    .getOutput("mp");

            if (mp==null) {
                System.err.println("Error - could not create multislicepositioner.");
                return;
            }

            BdvHandle bdvh = new DefaultBdvSupplier(new SerializableBdvOptions()).get();
            view = new BdvMultislicePositionerView(mp, bdvh);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Parameter
    ObjectService os;

    public static boolean showWarningMessageWithCheckbox(String title, String message) {
        // Create a panel to hold the checkbox
        JPanel panel = new JPanel();
        JLabel label = new JLabel(message);
        panel.add(label);
        JCheckBox skipCheckbox = new JCheckBox("Skip this warning");
        panel.add(skipCheckbox);

        // Show the option dialog with the warning message and the checkbox
        int result = JOptionPane.showConfirmDialog(null, panel, title, JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE);

        // Return true if the user clicks OK and the checkbox is selected, false otherwise
        return result == JOptionPane.OK_OPTION && skipCheckbox.isSelected();
    }

    public void initialize() {
        /*String keyMessagePref = "ch.epfl.biop.atlas.aligner.startupmessage.skip";
        boolean messageSkip = ij.Prefs.get(keyMessagePref, false);
        if (!messageSkip) {
            messageSkip = showWarningMessageWithCheckbox("Please read!","<html>If you are using ABBA in QuPath, please update to v0.4.4!<br> See https://go.epfl.ch/abba-update for more information.");
            ij.Prefs.set(keyMessagePref, messageSkip);
        }*/
        ABBAHelper.displayABBALogo(1500);
    }

}
