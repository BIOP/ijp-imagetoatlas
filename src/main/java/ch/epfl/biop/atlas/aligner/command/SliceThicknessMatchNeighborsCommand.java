package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.List;
import java.util.stream.Collectors;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Edit>ABBA - Set Slices Thickness (fill gaps)",
        description = "Modifies the selected slices thickness in such a way that no space is left between slices. "+
                "This is visible only in the reconstructed volume in BigDataViewer")
public class SliceThicknessMatchNeighborsCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Override
    public void run() {
        List<SliceSources> slices = mp.getSlices().stream().filter(SliceSources::isSelected).collect(Collectors.toList());
        if (slices.size()==0) {
            mp.errorMessageForUser.accept("No slice selected", "You did not select any slice to edit");
        } else {
            List<SliceSources> allSlices = mp.getSlices();
            if (allSlices.size()<2) {
                mp.errorMessageForUser.accept("Insufficient number of slices", "You need at least two slices.");
            } else {
                for (int i=0;i<allSlices.size();i++) {
                    SliceSources currentSlice = allSlices.get(i);
                    if (currentSlice.isSelected()) {
                        double zBegin, zEnd;
                        double currentPosition = currentSlice.getZAxisPosition();
                        if (i==0) {
                            // No slice before -> we need to look at the one after
                            if (i==allSlices.size()-1) {
                                // No slice after -> this command does not make sense, but this was tested before normally
                                mp.errorMessageForUser.accept("Insufficient number of slices", "You need at least two slices.");
                            } else {
                                SliceSources sliceAfter = allSlices.get(i+1);
                                zEnd = (sliceAfter.getZAxisPosition()+currentSlice.getZAxisPosition())/2.0;
                                zBegin = currentPosition-(zEnd-currentPosition);
                                currentSlice.setSliceThickness(zBegin, zEnd);
                            }
                        } else if (i==allSlices.size()-1) {
                            // No slice before -> we need to look at the one after
                            if (i==0) {
                                // No slice before -> this command does not make sense, but this was tested before normally
                                mp.errorMessageForUser.accept("Insufficient number of slices", "You need at least two slices.");
                            } else {
                                SliceSources sliceBefore = allSlices.get(i-1);
                                zBegin = (sliceBefore.getZAxisPosition()+currentSlice.getZAxisPosition())/2.0;
                                zEnd = currentPosition+(currentPosition-zBegin);
                                currentSlice.setSliceThickness(zBegin, zEnd);
                            }
                        } else {
                            SliceSources sliceBefore = allSlices.get(i-1);
                            SliceSources sliceAfter = allSlices.get(i+1);
                            zBegin = (sliceBefore.getZAxisPosition()+currentSlice.getZAxisPosition())/2.0;
                            zEnd = (sliceAfter.getZAxisPosition()+currentSlice.getZAxisPosition())/2.0;
                            currentSlice.setSliceThickness(zBegin, zEnd);
                        }

                    }
                }
            }
        }
    }
}
