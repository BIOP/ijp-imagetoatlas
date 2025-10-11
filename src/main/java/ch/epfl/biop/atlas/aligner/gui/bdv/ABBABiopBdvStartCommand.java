package ch.epfl.biop.atlas.aligner.gui.bdv;

import bdv.util.*;
import bdv.util.source.alpha.AlphaConverter;
import bdv.util.source.alpha.AlphaSourceRAI;
import bdv.util.source.alpha.IAlphaSource;
import bdv.viewer.SourceAndConverter;
import bdv.viewer.SourceGroup;
import ch.epfl.biop.atlas.aligner.ABBAHelper;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.command.ABBAStartCommand;
import ch.epfl.biop.atlas.struct.Atlas;
import ij.plugin.frame.Recorder;
import net.imglib2.FinalInterval;
import net.imglib2.position.FunctionRealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.real.FloatType;
import org.scijava.Initializable;
import org.scijava.ItemIO;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;
import sc.fiji.bdvpg.bdv.supplier.alpha.AlphaBdvSupplier;
import sc.fiji.bdvpg.bdv.supplier.alpha.AlphaSerializableBdvOptions;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.util.List;

import static bdv.util.source.alpha.AlphaSourceHelper.ALPHA_SOURCE_KEY;

@SuppressWarnings("unused")
@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>ABBA - ABBA Start (alpha)",
        description = "Starts ABBA from an Atlas with a BDV View",
        iconPath = "/graphics/ABBAStart.png")
public class ABBABiopBdvStartCommand implements Command, Initializable {

    @Parameter(visibility = ItemVisibility.MESSAGE)
    String message = "Select the atlas slicing orientation";

    @Parameter(callback = "coronalCB")
    Button coronal;

    @Parameter(callback = "sagittalCB")
    Button sagittal;

    @Parameter(callback = "horizontalCB")
    Button horizontal;

    @Parameter
    boolean white_background;

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

            AlphaSerializableBdvOptions options = new AlphaSerializableBdvOptions();
            options.white_bg = white_background;
            options.showCenterCross = false;
            options.showSourcesNames = false;
            options.showRayCastSlider = false;
            options.showSourceNavigatorSlider = false;
            options.numGroups = 3;
            options.showEditorCard = false;
            BdvHandle bdvh = new AlphaBdvSupplier(options).get();

            List<SourceGroup> groups = bdvh.getViewerPanel().state().getGroups();
            bdvh.getViewerPanel().state().setGroupName(groups.get(0), "Atlas");
            bdvh.getViewerPanel().state().setGroupName(groups.get(1), "Slices");
            bdvh.getViewerPanel().state().setGroupName(groups.get(2), "ROI Mask");

            if (white_background) {
                // Bdv bg color is not configurable: https://github.com/bigdataviewer/bigdataviewer-core/issues/211
                // So I add a fake white source, but that cost a little bit of computation.
                FunctionRealRandomAccessible<UnsignedByteType> whiteBG =
                        new FunctionRealRandomAccessible<>(3,
                                (p,v) -> v.set(0), UnsignedByteType::new);

                long boxSizeUm = 1_000_000; // 1 cm
                RealRandomAccessibleIntervalSource<UnsignedByteType> whiteBGSource = new RealRandomAccessibleIntervalSource<>(whiteBG,
                        new FinalInterval(new long[]{-boxSizeUm, -boxSizeUm, -boxSizeUm}, new long[]{boxSizeUm, boxSizeUm, boxSizeUm}),
                        new UnsignedByteType(), new AffineTransform3D(), "Background");

                SourceAndConverter<UnsignedByteType> whiteBGSAC = SourceAndConverterHelper.createSourceAndConverter(whiteBGSource);
                SourceAndConverterServices.getSourceAndConverterService().register(whiteBGSAC);

                IAlphaSource alpha = new AlphaSourceRAI(whiteBGSAC.getSpimSource());
                SourceAndConverter<FloatType> alpha_sac = new SourceAndConverter<>(alpha, new AlphaConverter());
                SourceAndConverterServices.getSourceAndConverterService().setMetadata(whiteBGSAC, ALPHA_SOURCE_KEY, alpha_sac);

                SourceAndConverterServices.getBdvDisplayService().show(bdvh, whiteBGSAC);//.setMetadata(whiteBGSAC, ALPHA_SOURCE_KEY, alpha_sac);

                ABBATheme.setTheme(ABBATheme.createLightTheme());
            } else {
                ABBATheme.setTheme(ABBATheme.createDarkTheme());
            }

            Prefs.showMultibox(false);
            Prefs.showScaleBar(false);
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
        ABBAHelper.displayABBALogo(2000); // BDV implies the presence of a GUI
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
