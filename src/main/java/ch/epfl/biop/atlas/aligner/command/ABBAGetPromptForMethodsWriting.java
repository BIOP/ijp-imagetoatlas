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
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Help>ABBA - Get a prompt to use for methods writing",
        description = "Outputs a summary of methods used for the registration. Can be copy pasted in the llm of your choice.")
public class ABBAGetPromptForMethodsWriting implements Command {

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

        DecimalFormat df = new DecimalFormat("00.000");
        DecimalFormat df2 = new DecimalFormat(".0");

        llm_prompt_for_methods =
                "# Goal\n"+
                "\n"+
                "Please write a SHORT methods section of what I've done to register my 2D histological sections to a 3D atlas."+
                "I used the software ABBA for this task (version"+ABBAHelper.getVersion()+"). ABBA uses a variety of third party tools (atlas, registration tools, software platform such as Fiji and QuPath)"+
                " that should also be included in the methods if they are used.\n"+
                "Ask please make sure to ask the user to review your output! We don't want incorrect methods to end up in publications.\n"+
                "\n";

        llm_prompt_for_methods +=
                "# Atlas\n"+
                "\n"+
                "For starters, I'm using the atlas named '"+ mp.getAtlas().getName()+"'.\n"+
                "You can find some extra information about it at the following URL:\n"+
                "* "+ mp.getAtlas().getURL()+"\n";

        if (!mp.getAtlas().getDOIs().isEmpty()) {
            llm_prompt_for_methods += "You can also check the DOIs associated to the atlas (if they exist):\n";
        }

        for (String doi: mp.getAtlas().getDOIs() ) {
            llm_prompt_for_methods +="* "+doi+"\n";
        }

        llm_prompt_for_methods +="Since it's not easy to know from the parameters in which orientation the atlas was cut (coronal, sagittal, horizontal), "+
                "insert an obvious placeholder sentence that I will need to fill later.\n"+
                "Now the atlas slicing may have been adjusted for slight rotation along the x and y axis. Here's are the adjustement values:\n"+
                "* Angle X (degrees) = "+df.format(mp.getReslicedAtlas().getRotateX()/Math.PI*180)+"\n"+
                "* Angle Y (degrees) = "+df.format(mp.getReslicedAtlas().getRotateY()/Math.PI*180)+"\n\n"+
                "If the values are both zero, then no adjustement was done. Note that if DeepSlice was used, these values were probably set by DeepSlice and not by the user.\n\n";

        llm_prompt_for_methods +="Here's the list of channels present in the atlas:\n\n";

        int idx = 0;
        Set<String> uselessKeys = new HashSet<>();
                uselessKeys.add("X");
                uselessKeys.add("Y");
                uselessKeys.add("Z");
                uselessKeys.add("Label");
                uselessKeys.add("Left_Right");
        for (String key : mp.getAtlas().getMap().getImagesKeys()) {
            if (!uselessKeys.contains(key)) {
                llm_prompt_for_methods += "* channel " + idx + ": " + key + "\n";
            }
            idx++;
        }

        llm_prompt_for_methods +="\n";

        llm_prompt_for_methods +="# Origin and king of slice data\n\n"+
                "I can't provide you good information about the data. Please add a placeholder sentence that specify what I need "+
                "to fill in these information (how were the image acquired, how many channels and what are they).\n\n";

        llm_prompt_for_methods +="# Registration steps for all slices - Preamble\n\n"+
                "I'll give you a list of all slices of the dataset, and for each one of them, which process has been applied for the registration.\n"+
                "But first let me explain a bit the syntax. For instance, see this example slice (which is not part of my current dataset:+" +
                "```\n"+
                "1 - Slide_04.vsi - 10x_10\n" +
                "2 - Z: 08.162 mm (Thickness: 83.7 um)\n" +
                "3 - Affine Id Atlas // Id Section] (done)\n" +
                "4 - Elastix 2D Affine [Z0] [Ch0;1] Atlas // [Z0][Ch1;0] Section] (done)\n" +
                "5 - Elastix 2D Spline [Z0] [Ch0;1] Atlas // [Z0][Ch0;1] Section] (done)\n" +
                "```"+
                "Line 1: name of the slice (`[Key]` may be indicated if the slice is a key reference slice)\n"+
                "Line 2: its position along the atlas axis\n"+
                "Line 3: It's a DeepSlice registration! Mention it! DeepSlice allows for automated positioning along the atlas axis + a first affine registration - (the word `Deepslice` will appear explicitely in newer versions of ABBA)\n"+
                "Line 4: an elastix affine registration was performed, that registration used multiple channels: the channels 0 and 1 of the atlas were "+
                " used against respectively to the channel 1 and 0 of the experimental section. You can ignore `[Z0]`\n"+
                "Line 5: an elastix spline registration was performed, that registration used multiple channels: the channels 0 and 1 of the atlas were "+
                " used against respectively to the channel 0 and 1 of the experimental section. You can ignore `[Z0]`\n"+
                "BigWarp may be used as well, it's a manual spline transform. Also make sure to add a placeholder that should ask me how many control points I've selected for my job."+
                "\n";

        llm_prompt_for_methods +="Here's the list of all slices and their registration protocol. I also added the spacing between two consecutive slices (if there are several), because if they are spaced\n"+
                " with equal distance the methods writing will be easier.\n\n";

        double lastSlicePosition = mp.getSelectedSlices().get(0).getSlicingAxisPosition();

        llm_prompt_for_methods +="# Slices\n\n";

        for (SliceSources slice: mp.getSelectedSlices()) {
            String name = slice.getName();
            if (slice.isKeySlice()) name += " [Key]";
            llm_prompt_for_methods +=name+"\n";
            llm_prompt_for_methods +="Z: "+df.format(slice.getSlicingAxisPosition()- mp.getReslicedAtlas().getZOffset())+" mm (Thickness: "+df2.format(slice.getThicknessInMm()*1000.0)+" um)\n";
            if (slice.getIndex()!=0) {
                llm_prompt_for_methods +="Dist from previous slice (micrometer): "+df.format((slice.getSlicingAxisPosition()-lastSlicePosition)*1000)+"\n";
                lastSlicePosition = slice.getSlicingAxisPosition();
            }
            List<CancelableAction> actionsArray = mp.getActionsFromSlice(slice);
            if (actionsArray!=null) {
                List<CancelableAction> actions = new ArrayList<>(actionsArray); // Copy useful ?
                actions = AlignerState.filterSerializedActions(actions);
                for (CancelableAction action : actions) {
                    if ((!(action instanceof MoveSliceAction)) && (!(action instanceof CreateSliceAction))) {
                        String actionString = action.toString();
                        if (actionString.equals("Affine Id Atlas // Id Section] (done)")) {
                            actionString="DeepSlice Affine Id Atlas // Id Section] (done)";
                        }
                        llm_prompt_for_methods += actionString+"\n";
                    }
                }
            }
        }

        llm_prompt_for_methods +="\n";
        llm_prompt_for_methods +="# References\n\n";
        llm_prompt_for_methods +="Please keep only the DOI in your reference section, do you write the authors. Cite with, for example [1] and then add 1 + a copy of the DOI in your reference section.\n";
        llm_prompt_for_methods +="Also note that QuPath is not used directly but rather for the post processing.\n";
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
        llm_prompt_for_methods+="Good luck for your task! Be concise!\n";

        llm_prompt_for_methods+="# Example output\n\n";

        llm_prompt_for_methods+="Here is an example output that you can take as a template (adjusted to the current conditions of course):\n\n";
        llm_prompt_for_methods+="THIS IS JUST AN EXAMPLE, modify the sections according to the data above.";

        llm_prompt_for_methods+="## Methods: Registration of Histological Sections to 3D Atlas\n" +
                "\n" +
                "## Image Acquisition and Preprocessing\n" +
                "\n" +
                "**[PLACEHOLDER: Describe image acquisition protocol, including microscope/scanner used, magnification, resolution, number of channels, and staining methods employed]**\n" +
                "\n" +
                "## Atlas Selection and Configuration\n" +
                "\n" +
                "Registration of histological sections to a reference atlas was performed using the Aligning Big Brains and Atlas (ABBA) plugin (version "+ABBAHelper.getVersion()+") [1] running within the Fiji image processing platform [2]. ABBA utilizes BigDataViewer [5] for visualization and BigWarp [6] for visualization of spline transformations. The Allen Mouse Common Coordinate Framework (CCF) atlas at 10 μm resolution (allen_mouse_10um_java) [3] was used as the reference template. **[PLACEHOLDER: Specify the anatomical orientation of atlas sectioning - coronal, sagittal, or horizontal]**. \n" +
                "\n" +
                "The atlas orientation was adjusted to better match the sectioning plane of the experimental tissue, with a rotation of ... along the X-axis and ... along the Y-axis. These values were set by DeepSlice. For registration purposes, two atlas channels were utilized: channel 0 (...) and channel 1 (...).\n" +
                "\n" +
                "## Section Registration Workflow\n" +
                "\n" +
                "A total of ... serial sections from Slide_04.vsi (sections 10x_06 through 10x_12) were registered to the atlas, spanning positions from ... mm to ... mm along the atlas axis. All sections were spaced at approximately 80 μm intervals.\n" +
                "\n" +
                "(insert part about registration procedure, common or not depending on whether the slices have been treated identically or not)" +
                "**[PLACEHOLDER: If manual refinement using BigWarp was performed, specify the approximate number of control points used per section]**\n" +
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
                "**IMPORTANT: Please carefully review this methods section and fill in all placeholders (marked with [PLACEHOLDER]) with accurate information specific to your experimental procedures. Verify that all technical details accurately reflect your workflow before including this in any publication.**";


    }

}
