package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.DeepSliceHelper;
import ch.epfl.biop.atlas.brainglobe.BrainGlobeAppose;
import ch.epfl.biop.wrappers.elastix.ApposeElastixTask;
import org.scijava.ItemIO;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the Python environments ABBA otherwise builds on first use (DeepSlice, Elastix, BrainGlobe), through
 * Appose. For a workshop, a machine that will be offline, or an installer: the downloads happen now rather than in
 * the middle of a registration. Every environment asked is attempted, and the command fails at the end if one did
 * not build. Runs headless.
 */
@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Help>ABBA - Prepare Python Environments",
        description = "Downloads and builds the Python environments of DeepSlice, Elastix and BrainGlobe now, "
                + "instead of on their first use. Needs an internet connection; DeepSlice alone is several GB.")
public class ABBAPrepareEnvironmentsCommand implements Command {

    @Parameter(visibility = ItemVisibility.MESSAGE)
    String message = "Needs an internet connection. Environments already built are reused.";

    @Parameter(label = "DeepSlice (TensorFlow, several GB)")
    boolean deepslice = true;

    @Parameter(label = "Elastix (itk-elastix)")
    boolean elastix = true;

    @Parameter(label = "BrainGlobe atlas API")
    boolean brainglobe = true;

    @Parameter(type = ItemIO.OUTPUT, label = "Report")
    String report;

    @Parameter
    LogService log;

    @Override
    public void run() {
        List<String> lines = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        if (deepslice) prepare("DeepSlice v" + DeepSliceHelper.ApposeDeepSliceTask.DS_VERSION,
                () -> DeepSliceHelper.ApposeDeepSliceTask.environment(log::info), lines, failed);
        if (elastix) prepare("Elastix (itk-elastix v" + ApposeElastixTask.ITK_ELASTIX_VERSION + ")",
                ApposeElastixTask::getElastixApposeService, lines, failed);
        if (brainglobe) prepare("BrainGlobe atlas API v" + BrainGlobeAppose.BG_VERSION,
                () -> new BrainGlobeAppose().getOrCreateEnvironment(), lines, failed);
        report = String.join("\n", lines);
        if (!failed.isEmpty()) throw new RuntimeException("Could not prepare: " + String.join(", ", failed)
                + "\n" + report);
    }

    private interface Build {
        Object build() throws Exception;
    }

    private void prepare(String name, Build build, List<String> lines, List<String> failed) {
        log.info("Preparing " + name + "...");
        long start = System.currentTimeMillis();
        try {
            build.build();
            String line = name + ": ready (" + (System.currentTimeMillis() - start) / 1000 + " s)";
            log.info(line);
            lines.add(line);
        } catch (Throwable e) {
            String line = name + ": FAILED - " + e.getMessage();
            log.error(line, e);
            lines.add(line);
            failed.add(name);
        }
    }
}
