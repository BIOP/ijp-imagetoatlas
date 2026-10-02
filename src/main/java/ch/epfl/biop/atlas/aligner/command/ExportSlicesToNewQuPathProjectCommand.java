package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.QuPathProjectCreator;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.action.ExportSliceRegionsToQuPathProjectAction;
import ch.epfl.biop.atlas.aligner.action.MarkActionSequenceBatchAction;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Export>ABBA - Export Slices To New QuPath Project",
        description = "Creates a QuPath project (QuPath 0.7) containing the images of the selected slices, with their atlas regions "
                + "and their registration (ABBA-RoiSet and ABBA-Transform files), which are imported in QuPath with the ABBA extension. "
                + "Only slices opened from a single series of a 2D file with Bio-Formats can be exported, one slice per image; "
                + "use 'ABBA - Export Registrations To QuPath Project' for slices imported from a QuPath project.",
        iconPath = "/graphics/ExportRegistrationToQuPath.png")
public class ExportSlicesToNewQuPathProjectCommand implements Command {

    @Parameter(label = "ABBA session", description = "The ABBA session the command acts on.")
    MultiSlicePositioner mp;

    @Parameter(label = "QuPath project folder", style = "directory",
            description = "Folder of the new QuPath project. It must be empty or not exist yet.")
    File project_folder;

    @Override
    public void run() {
        List<SliceSources> slices = mp.getSelectedSlices();
        if (slices.isEmpty()) {
            mp.errorMessageForUser.accept("No slice selected", "You did not select any slice.");
            return;
        }

        String[] existingFiles = project_folder.list();
        if (project_folder.exists() && ((existingFiles == null) || (existingFiles.length > 0))) {
            mp.errorMessageForUser.accept("QuPath project folder not empty",
                    "The folder "+project_folder.getAbsolutePath()+" is not empty. Please choose an empty or a new folder.");
            return;
        }

        List<SliceSources> exportedSlices = new ArrayList<>();
        List<String> imageNames = new ArrayList<>();
        List<QuPathProjectCreator.BioFormatsImage> images = new ArrayList<>();
        Map<QuPathProjectCreator.BioFormatsImage, SliceSources> sliceOfImage = new HashMap<>();
        List<String> skipped = new ArrayList<>();
        List<String> sharedImages = new ArrayList<>();

        for (SliceSources slice : slices) {
            try {
                QuPathProjectCreator.BioFormatsImage image = QuPathProjectCreator.getBioFormatsImage(slice);
                if (sliceOfImage.containsKey(image)) {
                    sharedImages.add(slice+" and "+sliceOfImage.get(image)+" both come from "+image+".");
                    continue;
                }
                sliceOfImage.put(image, slice);
                exportedSlices.add(slice);
                // Same convention as QuPath, slice names (often series names) are not unique across files
                imageNames.add(image.file.getName()+" - "+slice);
                images.add(image);
            } catch (IllegalArgumentException e) {
                skipped.add(slice+": "+e.getMessage());
            }
        }

        // A QuPath image holds the regions of a single slice
        if (!sharedImages.isEmpty()) {
            mp.errorMessageForUser.accept("Slices sharing an image",
                    "Each exported slice needs its own image, but:\n"+String.join("\n", sharedImages)
                    + "\nNothing was exported. Please select only one slice per image.");
            return;
        }

        if (exportedSlices.isEmpty()) {
            mp.errorMessageForUser.accept("No slice can be exported to a new QuPath project",
                    "None of the selected slices can be exported:\n"+String.join("\n", skipped));
            return;
        }

        List<File> dataEntryFolders;
        try {
            dataEntryFolders = QuPathProjectCreator.createProject(project_folder, imageNames, images);
            QuPathProjectCreator.writeImportScript(project_folder, mp.getAtlas().getName());
            SliceSources.writeOntotogyIfNotPresent(mp, project_folder.getAbsolutePath());
        } catch (Exception e) {
            mp.errorMessageForUser.accept("QuPath project creation error",
                    "The QuPath project could not be written in "+project_folder.getAbsolutePath()+": "+e.getMessage());
            e.printStackTrace();
            return;
        }

        if (!skipped.isEmpty()) {
            mp.warningMessageForUser.accept("Slices not exported to the QuPath project",
                    skipped.size()+" selected slice(s) were not exported:\n"+String.join("\n", skipped));
        }

        new MarkActionSequenceBatchAction(mp).runRequest();
        for (int i = 0; i < exportedSlices.size(); i++) {
            new ExportSliceRegionsToQuPathProjectAction(mp, exportedSlices.get(i), project_folder, dataEntryFolders.get(i), false).runRequest();
        }
        new MarkActionSequenceBatchAction(mp).runRequest();

        mp.infoMessageForUser.accept("QuPath project created",
                exportedSlices.size()+" slice(s) exported to "+new File(project_folder, "project.qpproj").getAbsolutePath()+".\n"
                + "Open it in QuPath, then import the atlas regions with the ABBA extension, "
                + "for instance with the project script 'Import_ABBA_atlas_regions' (Run > Run for project).");
    }

}
