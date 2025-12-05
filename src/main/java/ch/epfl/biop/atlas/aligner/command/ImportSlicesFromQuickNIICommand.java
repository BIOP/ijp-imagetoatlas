package ch.epfl.biop.atlas.aligner.command;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.aligner.DeepSliceHelper;
import ch.epfl.biop.atlas.aligner.MoveSliceAction;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.RegisterSliceAction;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.action.MarkActionSequenceBatchAction;
import ch.epfl.biop.bdv.img.bioformats.command.CreateBdvDatasetBioFormatsCommand;
import ch.epfl.biop.quicknii.QuickNIISeries;
import ch.epfl.biop.registration.Registration;
import ch.epfl.biop.registration.plugin.IRegistrationPlugin;
import ch.epfl.biop.registration.sourceandconverter.affine.AffineRegistration;
import ch.epfl.biop.sourceandconverter.processor.SourcesProcessorHelper;
import com.google.gson.Gson;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.sequence.Tile;
import net.imglib2.realtransform.AffineTransform3D;
import org.scijava.Context;
import org.scijava.InstantiableException;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginService;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterService;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterAndTimeRange;
import sc.fiji.bdvpg.sourceandconverter.transform.SourceTransformHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static ch.epfl.biop.atlas.aligner.command.RegisterSlicesDeepSliceAbstractCommand.adjustSlicingAngle;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Import>ABBA - Import QuickNII Project",
        description = "Import images of a QuickNII Project as slices into ABBA",
        iconPath = "/graphics/QNIIToABBA.png")
public class ImportSlicesFromQuickNIICommand implements Command {

    @Parameter(style = "message", visibility = ItemVisibility.MESSAGE)
    String message = "<html><b>WARNING:</b> The QuickNII import is not exact:<br>" +
            "The same slicing angle (median of all slices) will be applied for all slices.";

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(label = "Split RGB channels")
    boolean split_rgb_channels = false;

    @Parameter(label = "QuickNII file (.json)")
    File quicknii_project;

    @Parameter
    CommandService command_service;

    @Parameter
    SourceAndConverterService sac_service;

    @Parameter
    Context ctx;


    @Override
    public void run() {
        try {
            // First, let's open the image files and package them into a bdv dataset
            QuickNIISeries series = new Gson().fromJson(new FileReader(quicknii_project.getAbsolutePath()), QuickNIISeries.class);
            File parentFolder = new File(quicknii_project.getParent()); // We assume the files are at the same folder hierarchy level as the images.
            File[] imageFiles = series.slices.stream().map(si -> new File(parentFolder, si.filename)).toArray(File[]::new);

            AbstractSpimData<?> spimdata = (AbstractSpimData<?>)
                    command_service.run(
                                    CreateBdvDatasetBioFormatsCommand.class,
                                    true, "files", imageFiles,
                                    "datasetname", quicknii_project.getName(),
                                    "unit", "MILLIMETER",
                                    "split_rgb_channels", split_rgb_channels,
                                    "plane_origin_convention", "TOP LEFT",
                                    "auto_pyramidize", true,
                                    "disable_memo", false
                            )
                            .get()
                            .getOutput("spimdata");

            SourceAndConverter[] sacs =
                    sac_service.getSourceAndConverterFromSpimdata(spimdata)
                            .toArray(new SourceAndConverter[0]);

            // Remove potential original calibration - pixel size will be 1mm
            sac_service.getSourceAndConverterFromSpimdata(spimdata)
                            .forEach(source -> {
                                AffineTransform3D reverseCalibration = new AffineTransform3D();
                                source.getSpimSource().getSourceTransform(0,0,reverseCalibration);
                                SourceTransformHelper.append(reverseCalibration.inverse(),
                                        new SourceAndConverterAndTimeRange(source,0));
                            });

            List<SliceSources> slices = mp.createSlice(sacs, 0, 1, Tile.class, new Tile(-1));

            mp.selectSlice(mp.getSlices());

            Map<SliceSources, DeepSliceHelper.Holder<Double>> newAxisPosition = new HashMap<>();
            Map<SliceSources, DeepSliceHelper.Holder<Registration<SourceAndConverter<?>[]>>> newSliceRegistration = new HashMap<>();

            for (SliceSources slice: slices) {
                newAxisPosition.put(slice, new DeepSliceHelper.Holder<>());
                newSliceRegistration.put(slice, new DeepSliceHelper.Holder<>());
            }

            adjustSlicingAngle(mp, series, 10, slices);

            Map<SliceSources, Double> slicesNewPosition = new HashMap<>();

            AffineTransform3D toABBA = mp.getReslicedAtlas().getSlicingTransformToAtlas().inverse();

            for (int i = 0; i < slices.size(); i++) {
                QuickNIISeries.SliceInfo slice = series.slices.get(i);

                AffineTransform3D toCCFv3 = QuickNIISeries.getTransform(mp.getReslicedAtlas().ba.getName(), slice, slice.width, slice.height);

                AffineTransform3D nonFlat = toCCFv3.preConcatenate(toABBA);

                double zLocation = nonFlat.get(2,3);
                slicesNewPosition.put(slices.get(i), zLocation);
            }

            for (SliceSources slice : slices) {
                newAxisPosition.get(slice).accept(slicesNewPosition.get(slice));
            }

            //Transform sources according to anchoring

            for (int i = 0; i < slices.size(); i++) {
                QuickNIISeries.SliceInfo slice = series.slices.get(i);

                AffineTransform3D toCCFv3 = QuickNIISeries.getTransform(mp.getReslicedAtlas().ba.getName(), slice, slice.width, slice.height);

                AffineTransform3D flat = toCCFv3.preConcatenate(toABBA);

                // Removes any z transformation -> in plane transformation
                flat.set(0,2,0);
                flat.set(0,2,1);
                flat.set(1,2,2);
                flat.set(0,2,3);

                // flat gives the good registration result for an image which is located at 0,0,0, and
                // which has a pixel size of 1
                // We need to transform the original image this way

                AffineTransform3D preTransform = new AffineTransform3D();
                preTransform.scale(1);
                preTransform.set(1,2,2);
                preTransform.set(slices.get(i).getOriginalSources()[0].getSpimSource().getSource(0,0).dimension(0)/2.0, 0, 3);
                preTransform.set(slices.get(i).getOriginalSources()[0].getSpimSource().getSource(0,0).dimension(1)/2.0, 1, 3);

                IRegistrationPlugin registration = (IRegistrationPlugin)
                        ctx.getService(PluginService.class).getPlugin(AffineRegistration.class).createInstance();
                registration.setScijavaContext(ctx);
                Map<String,Object> parameters = new HashMap<>();

                AffineTransform3D inPlaneTransform = new AffineTransform3D();
                inPlaneTransform.set(flat);
                inPlaneTransform.concatenate(preTransform);

                parameters.put("transform", AffineRegistration.affineTransform3DToString(inPlaneTransform));
                // Always set slice at zero position for registration
                parameters.put("pz", 0);
                AffineTransform3D at3d = new AffineTransform3D();
                at3d.translate(0,0,-slices.get(i).getSlicingAxisPosition());

                // Sends parameters to the registration
                registration.setRegistrationParameters(MultiSlicePositioner.convertToString(ctx,parameters));
                newSliceRegistration.get(slices.get(i)).accept( registration );
            }

            new MarkActionSequenceBatchAction(mp).runRequest();
            for (SliceSources slice: slices) {
                new MoveSliceAction(mp, slice, newAxisPosition.get(slice)).runRequest();
                DeepSliceHelper.Holder<Registration<SourceAndConverter<?>[]>> regSupplier = newSliceRegistration.get(slice);
                new RegisterSliceAction(mp, slice, regSupplier,
                        SourcesProcessorHelper.Identity(),
                        SourcesProcessorHelper.Identity(), "QuickNII Import Affine").runRequest();
            }
            new MarkActionSequenceBatchAction(mp).runRequest();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            mp.errorMessageForUser.accept("QuickNII Import Error",
                    "QuickNII project couldn't be imported.");
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            mp.errorMessageForUser.accept("QuickNII Import Error",
                    "QuickNII project file not found.");
            e.printStackTrace();
        } catch (InstantiableException e) {
            throw new RuntimeException(e);
        }
    }

}
