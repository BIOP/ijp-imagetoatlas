package ch.epfl.biop.atlastoimg2d.commands.sourceandconverter.multislices;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlastoimg2d.multislice.CancelableAction;
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

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>MP Save State [WIP]")
public class SaveStateCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter
    File stateFile;

    @Parameter
    Context ctx;

    @Parameter
    Boolean overwrite;

    @Override
    public void run() {

        mp.saveState(stateFile, overwrite);




        //sacss.getGson()

    }
}
