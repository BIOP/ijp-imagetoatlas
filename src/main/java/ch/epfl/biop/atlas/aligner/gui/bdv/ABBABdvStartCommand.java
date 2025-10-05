package ch.epfl.biop.atlas.aligner.gui.bdv;

import bdv.util.BdvHandle;
import bdv.util.Prefs;
import ch.epfl.biop.atlas.aligner.ABBAHelper;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.command.ABBAStartCommand;
import ch.epfl.biop.atlas.struct.Atlas;
import ij.plugin.frame.Recorder;
import org.scijava.Initializable;
import org.scijava.ItemIO;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;
import sc.fiji.bdvpg.bdv.supplier.DefaultBdvSupplier;
import sc.fiji.bdvpg.bdv.supplier.SerializableBdvOptions;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

@SuppressWarnings("unused")
@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>ABBA - ABBA Start",
        description = "Starts ABBA from an Atlas with a BDV View",
        iconPath = "/graphics/ABBAStart.png")
public class ABBABdvStartCommand implements Command, Initializable {

    @Parameter(visibility = ItemVisibility.MESSAGE)
    String message = "Select the atlas slicing orientation";

    @Parameter(callback = "coronalCB")
    Button coronal;

    @Parameter(callback = "sagittalCB")
    Button sagittal;

    @Parameter(callback = "horizontalCB")
    Button horizontal;

    @Parameter(choices = {
            "AP (Anterior-Posterior)",
            "PA (Posterior-Anterior)",
            "SI (Superior-Inferior)",
            "IS (Inferior-Superior)",
            "RL (Right-Left)",
            "LR (Left-Right)"})
    String x_axis;

    @Parameter(choices = {
            "AP (Anterior-Posterior)",
            "PA (Posterior-Anterior)",
            "SI (Superior-Inferior)",
            "IS (Inferior-Superior)",
            "RL (Right-Left)",
            "LR (Left-Right)"})
    String y_axis;

    @Parameter(choices = {
            "AP (Anterior-Posterior)",
            "PA (Posterior-Anterior)",
            "SI (Superior-Inferior)",
            "IS (Inferior-Superior)",
            "RL (Right-Left)",
            "LR (Left-Right)"})
    String z_axis;

    @Parameter
    public Atlas ba;

    @Parameter
    CommandService cs;

    @Parameter(type = ItemIO.OUTPUT)
    BdvMultislicePositionerView view;

    @Override
    public void run() {
        try {
            // Avoid breaking recorder when running a command within a command
            boolean tmpRecord = Recorder.record;
            Recorder.record = false;
            MultiSlicePositioner mp = (MultiSlicePositioner) cs
                .run(ABBAStartCommand.class, true,
                    "ba", ba,
                        "x_axis", x_axis,
                        "y_axis", y_axis,
                        "z_axis", z_axis
                        )
                    .get()
                    .getOutput("mp");
            Recorder.record = tmpRecord;

            if (mp==null) {
                System.err.println("Error - could not create multislicepositioner.");
                return;
            }

            BdvHandle bdvh = new DefaultBdvSupplier(new SerializableBdvOptions()).get();
            Prefs.showMultibox(false);
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
        ABBAHelper.displayABBALogo(1500); // BDV implies the presence of a GUI
    }

    void coronalCB() {
        this.x_axis = "RL (Right-Left)";
        this.y_axis = "SI (Superior-Inferior)";
        this.z_axis = "AP (Anterior-Posterior)";
    }

    void horizontalCB() {
        this.x_axis = "LR (Left-Right)";
        this.y_axis = "AP (Anterior-Posterior)";
        this.z_axis = "SI (Superior-Inferior)";
    }

    void sagittalCB() {
        this.x_axis = "AP (Anterior-Posterior)";
        this.y_axis = "SI (Superior-Inferior)";
        this.z_axis = "LR (Left-Right)";
    }

}
