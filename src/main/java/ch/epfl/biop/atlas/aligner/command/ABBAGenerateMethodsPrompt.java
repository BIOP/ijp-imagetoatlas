package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.ABBAHelper;
import ch.epfl.biop.atlas.aligner.CancelableAction;
import ch.epfl.biop.atlas.aligner.CreateSliceAction;
import ch.epfl.biop.atlas.aligner.MoveSliceAction;
import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.adapter.AlignerState;
import ch.epfl.biop.atlas.mouse.allen.ccfv3p1.command.AllenBrainAdultMouseAtlasCCF2017v3p1Command;
import ch.epfl.biop.atlas.mouse.allen.ccfv3p1asr.command.AllenBrainAdultMouseAtlasCCF2017v3p1ASRCommand;
import ch.epfl.biop.atlas.rat.waxholm.spraguedawley.v4p2.WaxholmSpragueDawleyRatV4p2Atlas;
import ch.epfl.biop.atlas.rat.waxholm.spraguedawley.v4p2asr.command.WaxholmSpragueDawleyRatV4p2ASRCommand;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Cite>ABBA - Get Prompt r methods writing",
        description = "Outputs a summary of methods used for the registration. Can be copy pasted in the llm of your choice.")
public class ABBAGenerateMethodsPrompt implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Parameter(type = ItemIO.OUTPUT)
    String llm_prompt_for_methods;

    @Override
    public void run() {
        if (mp.getSlices().isEmpty()) {
            mp.errorMessageForUser.accept("You did not have any slice registered", "No slice present");
            return;
        }

        if (mp.getSelectedSlices().isEmpty()) {
            mp.errorMessageForUser.accept("You did not have any select any slice", "Please select slices");
            return;
        }

        mp.infoMessageForUser.accept("Wait for end of all tasks", "Wait for all tasks to finish before generating prompt...");
        mp.waitForTasks();

        DecimalFormat df = new DecimalFormat("00.000");
        DecimalFormat df2 = new DecimalFormat(".0");

        llm_prompt_for_methods =
                "# Goal\n"+
                        "\n"+
                        "Generate a concise methods section describing the registration of 2D histological sections to a 3D atlas. "+
                        "The user has performed this registration using ABBA software (version "+ABBAHelper.getVersion()+"). "+
                        "ABBA integrates various third-party tools (atlases, registration algorithms, and software platforms including Fiji and QuPath) "+
                        "that should be cited when relevant.\n"+
                        "IMPORTANT: Include a disclaimer reminding the user to review the output carefully to ensure accuracy before publication.\n"+
                        "\n";

        llm_prompt_for_methods +=
                "# Atlas Information\n"+
                        "\n"+
                        "The user employed the '"+ mp.getAtlas().getName()+"' atlas.\n"+
                        "Additional information available at:\n"+
                        "* "+ mp.getAtlas().getURL()+"\n";

        if (!mp.getAtlas().getDOIs().isEmpty()) {
            llm_prompt_for_methods += "Related DOIs:\n";
        }

        for (String doi: mp.getAtlas().getDOIs() ) {
            llm_prompt_for_methods +="* "+doi+"\n";
        }

        llm_prompt_for_methods +="\nThe atlas sectioning orientation (coronal, sagittal, or horizontal) cannot be determined from the parameters. "+
                "Insert a clear placeholder for the user to specify this information.\n\n"+
                "Atlas slicing adjustments for rotation:\n"+
                "* X-axis rotation: "+df.format(mp.getReslicedAtlas().getRotateX()/Math.PI*180)+" degrees\n"+
                "* Y-axis rotation: "+df.format(mp.getReslicedAtlas().getRotateY()/Math.PI*180)+" degrees\n\n"+
                "If both values are zero, no adjustment was performed. Note: DeepSlice may have set these values automatically.\n\n";

        llm_prompt_for_methods +="Atlas channels available:\n\n";

        int idx = 0;
        Set<String> uselessKeys = new HashSet<>();
                uselessKeys.add("X");
                uselessKeys.add("Y");
                uselessKeys.add("Z");
                uselessKeys.add("Label");
                uselessKeys.add("Left Right");
        for (String key : mp.getAtlas().getMap().getImagesKeys()) {
            if (!uselessKeys.contains(key)) {
                llm_prompt_for_methods += "* channel " + idx + ": " + key + "\n";
            }
            idx++;
        }

        llm_prompt_for_methods +="\n";

        llm_prompt_for_methods +="# Experimental Data\n\n"+
                "Insufficient information provided about the experimental data. Insert placeholders for:\n"+
                "- Image acquisition method and equipment\n"+
                "- Number and type of channels/stains\n"+
                "- Any preprocessing steps\n\n";

        llm_prompt_for_methods +="# Registration Protocol - Overview\n\n"+
                "Below is the complete list of slices with their registration parameters.\n"+
                "Syntax explanation using this example (not from the current dataset):\n"+
                "```\n"+
                "1 - Slide_04.vsi - 10x_10\n" +
                "2 - Z: 08.162 mm (Thickness: 83.7 um)\n" +
                "3 - Step 1|DeepSlice Affine Id Atlas // Id Section] (done)\n" +
                "4 - Step 2|Elastix 2D Affine [Z0] [Ch0;1] Atlas // [Z0][Ch1;0] Section] (done)\n" +
                "5 - Step 3|Elastix 2D Spline [Z0] [Ch0;1] Atlas // [Z0][Ch0;1] Section] (done)\n" +
                "```\n"+
                "Line 1: Slice identifier (optional `[Key]` suffix indicates where the user positioned its reference/key slice)\n"+
                "Line 2: Position along atlas axis and section thickness\n"+
                "Line 3: DeepSlice registration (automated positioning + initial affine transform + atlas slicing angle correction)\n"+
                "Line 4: Elastix affine registration with channel mapping (atlas Ch0,1 → section Ch1,0). Ignore `[Z0]`\n"+
                "Line 5: Elastix spline registration with channel mapping (atlas Ch0,1 → section Ch0,1). Ignore `[Z0]` and put placeholder to specify how many control points were used.\n"+
                "If DeepSlice is used several times, mention it since running DeepSlice multiple times gives better results.\n"+
                "BigWarp indicates a fully manual spline transformation - include placeholder for number of control points used.\n"+
                "\n";

        llm_prompt_for_methods +="Inter-slice spacing is included to identify regular vs. irregular sampling.\n\n";

        double lastSlicePosition = mp.getSelectedSlices().get(0).getSlicingAxisPosition();

        llm_prompt_for_methods +="# Slices\n\n";

        llm_prompt_for_methods += "There are "+mp.getSelectedSlices().size()+" slices in total. Here is how they have been processed, sorted by Z values:\n\n";

        List<String> slicesProcessingSteps = new ArrayList<>();

        for (SliceSources slice: mp.getSelectedSlices()) {
            List<CancelableAction> actionsArray = mp.getActionsFromSlice(slice);
            String processingStepsForSlice = "";
            if (actionsArray!=null) {
                List<CancelableAction> actions = new ArrayList<>(actionsArray); // Copy useful ?
                actions = AlignerState.filterSerializedActions(actions);
                int idxStep = 1;
                for (CancelableAction action : actions) {
                    if ((!(action instanceof MoveSliceAction)) && (!(action instanceof CreateSliceAction))) {
                        String actionString = action.toString();
                        if (actionString.equals("Affine Id Atlas // Id Section] (done)")|| (actionString.equals("Affine Id.Atlas // Id.Section)] (done)"))) {
                            actionString="DeepSlice Affine Id.Atlas // Id.Section] (done)";
                        }
                        processingStepsForSlice += "Step "+idxStep+"|"+actionString+"\n";
                        idxStep++;
                    }
                }
            }
            slicesProcessingSteps.add(processingStepsForSlice);
        }

        boolean equalProcessing = slicesProcessingSteps.isEmpty() ||
                slicesProcessingSteps.stream().distinct().count() == 1;

        if (!equalProcessing) {
            llm_prompt_for_methods+="\n Not all slices have been processed identically! \n\n";
        }

        int idxSlice = 0;
        for (SliceSources slice: mp.getSelectedSlices()) {
            String name = slice.getName();
            if (slice.isKeySlice()) name += " [Key]";
            llm_prompt_for_methods +=name+"\n";
            llm_prompt_for_methods +="Z: "+df.format(slice.getSlicingAxisPosition()- mp.getReslicedAtlas().getZOffset())+" mm (Thickness: "+df2.format(slice.getThicknessInMm()*1000.0)+" um)\n";
            if (idxSlice!=0) {
                llm_prompt_for_methods +="Dist from previous slice (micrometer): "+df.format((slice.getSlicingAxisPosition()-lastSlicePosition)*1000)+"\n";
                lastSlicePosition = slice.getSlicingAxisPosition();
            }
            if (!equalProcessing) {
                llm_prompt_for_methods+=slicesProcessingSteps.get(idxSlice);
            }
            idxSlice++;
        }

        if (equalProcessing) {
            llm_prompt_for_methods+="\nAll slices have been processed identically with the following registration step(s):\n";
            llm_prompt_for_methods+=slicesProcessingSteps.get(0);
        }


        llm_prompt_for_methods +="\n";
        llm_prompt_for_methods +="# References\n\n";
        llm_prompt_for_methods +="Format citations as numbered references [1] with DOIs only (exclude author lists).\n";
        llm_prompt_for_methods +="Note: QuPath is used for post-processing, not direct registration.\n";
        llm_prompt_for_methods +="- ABBA paper: "+ABBAHelper.URL_ABBA+"\n";
        llm_prompt_for_methods +="- DeepSlice paper: "+ABBAHelper.URL_DeepSlice+"\n";
        llm_prompt_for_methods +="- Fiji paper: "+ABBAHelper.URL_Fiji+"\n";
        llm_prompt_for_methods +="- QuPath paper: "+ABBAHelper.URL_QuPath+"\n";
        llm_prompt_for_methods +="- Elastix paper: "+ABBAHelper.URL_Elastix+"\n";
        llm_prompt_for_methods +="- BigDataViewer paper: "+ABBAHelper.URL_BDV+"\n";
        llm_prompt_for_methods +="- BigWarp paper: "+ABBAHelper.URL_BigWarp+"\n";

        Set<String> javaAtlases = new HashSet<>();
        javaAtlases.add(WaxholmSpragueDawleyRatV4p2Atlas.atlasName);
        javaAtlases.add(AllenBrainAdultMouseAtlasCCF2017v3p1Command.atlasName);
        javaAtlases.add(AllenBrainAdultMouseAtlasCCF2017v3p1ASRCommand.atlasName);
        javaAtlases.add(WaxholmSpragueDawleyRatV4p2ASRCommand.atlasName);

        if (javaAtlases.contains(mp.getAtlas().getName())) {

        } else {
            llm_prompt_for_methods +="- BrainGlobe paper: "+ABBAHelper.URL_BigWarp+" (used to access the atlas data)\n";
        }

        llm_prompt_for_methods+="\n\n";
        llm_prompt_for_methods+="Generate a concise, professional methods section based on the above information.\n";

        llm_prompt_for_methods+="# Example Output Template\n\n";
        llm_prompt_for_methods+="Use this structure as a guide (adapt to the specific data provided):\n\n";
        llm_prompt_for_methods+="IMPORTANT: This is a template only - modify all sections according to the actual data above.\n\n";
        llm_prompt_for_methods+="<START OF TEMPLATE>\n";
        llm_prompt_for_methods+="## Methods: Registration of Histological Sections to 3D Atlas\n" +
                "\n" +
                "## Image Acquisition and Preprocessing\n" +
                "\n" +
                "**[PLACEHOLDER: Describe image acquisition protocol, including microscope/scanner used, magnification, resolution, number of channels, and staining methods employed]**\n" +
                "\n" +
                "## Atlas Selection and Configuration\n" +
                "\n" +
                "Registration of histological sections to a reference atlas was performed using the Aligning Big Brains and Atlas (ABBA) plugin (version "+ABBAHelper.getVersion()+") [1] running within the Fiji image processing platform [2]. ABBA utilizes BigDataViewer [5] for visualization and BigWarp [6] for visualization of spline transformations. The (...) atlas at (...) μm resolution [3] was used as the reference template. **[PLACEHOLDER: Specify the anatomical orientation of atlas sectioning - coronal, sagittal, or horizontal]**. \n" +
                "\n" +
                "The atlas orientation was adjusted to better match the sectioning plane of the experimental tissue, with a rotation of ... along the X-axis and ... along the Y-axis. These values were set by DeepSlice. For registration purposes, two atlas channels were utilized: channel 0 (...) and channel 1 (...).\n" +
                "\n" +
                "## Section Registration Workflow\n" +
                "\n" +
                "A total of (...) serial sections from Slide_04.vsi (sections (...) through (...)) were registered to the atlas, spanning positions from ... mm to ... mm along the atlas axis. All sections were spaced at approximately 80 μm intervals.\n" +
                "\n" +
                "(insert part about registration procedure, common or not depending on whether the slices have been treated identically or not)" +
                "\n" +
                "## Post-Processing\n" +
                "\n" +
                "Registered sections were exported for subsequent analysis in QuPath [7].\n" +
                "\n" +
                "## References\n" +
                "\n" +
                "[1] https://... \n" +
                "[2] https://... \n" +
                "\n" +
                "---\n" +
                "\n" +
                "**IMPORTANT: Please carefully review this methods section and fill in all placeholders (marked with [PLACEHOLDER]) with accurate information specific to your experimental procedures. Verify that all technical details accurately reflect your workflow before including this in any publication.**\n";
        llm_prompt_for_methods+="<END OF TEMPLATE>\n";

    }

}
