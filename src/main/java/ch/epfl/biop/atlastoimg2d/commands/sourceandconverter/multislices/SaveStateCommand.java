package ch.epfl.biop.atlastoimg2d.commands.sourceandconverter.multislices;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlastoimg2d.multislice.MultiSlicePositioner;
import org.apache.commons.io.FilenameUtils;
import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.services.SourceAndConverterServiceSaver;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>MP Save State")
public class SaveStateCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter
    File stateFile;

    @Parameter
    Context ctx;

    @Override
    public void run() {
        List<SourceAndConverter> allSacs = new ArrayList<>();

        mp.getSortedSlices().forEach(slice -> {
            slice.waitForEndOfTasks();
        });

        synchronized (mp) {
            mp.getSortedSlices().forEach(sliceSource -> {
                sliceSource.getRegistrationSequence().forEach(regAndSacs -> {
                    allSacs.addAll(Arrays.asList(regAndSacs.sacs));
                });
            });
        }

        String fileNoExt = FilenameUtils.removeExtension(stateFile.getAbsolutePath());
        File sacsFile = new File(fileNoExt+"_sources.json");

        if (sacsFile.exists()) {
            System.err.println("File "+sacsFile.getAbsolutePath()+" already exists. Abort command");
            return;
        }

        SourceAndConverterServiceSaver sacss = new SourceAndConverterServiceSaver(sacsFile,ctx,allSacs);
        sacss.run();
        Map<SourceAndConverter, Integer> sacToId = sacss.getSacToId();

        //sacss.getGson()

    }
}
