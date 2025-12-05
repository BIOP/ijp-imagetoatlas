package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import ch.epfl.biop.atlas.aligner.SliceSources;
import ij.IJ;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.List;
import java.util.stream.Collectors;

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Edit>ABBA - Set Slices Thickness (fill gaps)",
        description = "Modifies the selected slices thickness in such a way that no space is left between slices. "+
                "This is visible only in the reconstructed volume in BigDataViewer")
public class SetSlicesThicknessMatchNeighborsCommand implements Command {

    @Parameter
    MultiSlicePositioner mp;

    @Override
    public void run() {
        List<SliceSources> slices = mp.getSlices().stream().filter(SliceSources::isSelected).collect(Collectors.toList());
        if (slices.isEmpty()) {
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
                        double currentPosition = currentSlice.getSlicingAxisPosition();
                        if (i==0) {
                            // No slice before -> we need to look at the one after
                            if (i==allSlices.size()-1) {
                                // No slice after -> this command does not make sense, but this was tested before normally
                                mp.errorMessageForUser.accept("Insufficient number of slices", "You need at least two slices.");
                            } else {
                                SliceSources sliceAfter = allSlices.get(i+1);
                                zEnd = (sliceAfter.getSlicingAxisPosition()+currentSlice.getSlicingAxisPosition())/2.0;
                                zBegin = currentPosition-(zEnd-currentPosition);
                                setSliceThicknessWithValidation(mp, currentSlice, zBegin, zEnd);
                            }
                        } else if (i==allSlices.size()-1) {
                            // No slice before -> we need to look at the one after
                            if (i==0) {
                                // No slice before -> this command does not make sense, but this was tested before normally
                                mp.errorMessageForUser.accept("Insufficient number of slices", "You need at least two slices.");
                            } else {
                                SliceSources sliceBefore = allSlices.get(i-1);
                                zBegin = (sliceBefore.getSlicingAxisPosition()+currentSlice.getSlicingAxisPosition())/2.0;
                                zEnd = currentPosition+(currentPosition-zBegin);
                                setSliceThicknessWithValidation(mp, currentSlice, zBegin, zEnd);
                            }
                        } else {
                            SliceSources sliceBefore = allSlices.get(i-1);
                            SliceSources sliceAfter = allSlices.get(i+1);
                            zBegin = (sliceBefore.getSlicingAxisPosition()+currentSlice.getSlicingAxisPosition())/2.0;
                            zEnd = (sliceAfter.getSlicingAxisPosition()+currentSlice.getSlicingAxisPosition())/2.0;
                            setSliceThicknessWithValidation(mp, currentSlice, zBegin, zEnd);
                        }

                    }
                }
            }
        }
    }

    private static void setSliceThicknessWithValidation(MultiSlicePositioner mp, SliceSources currentSlice, double zBegin, double zEnd) {
        double minThickness = mp.getAtlas().getMap().getAtlasPrecisionInMillimeter();
        double thickness = (zEnd-zBegin);
        if (thickness<minThickness) {
            IJ.log("Slice "+currentSlice.getName()+" too thin, thickness set to atlas resolution.");
            double mid = (zEnd+zBegin)/2.0;
            zBegin = mid-minThickness/2.0;
            zEnd = mid+minThickness/2.0;
            currentSlice.setSliceThickness(zBegin, zEnd);
        } else {
            currentSlice.setSliceThickness(zBegin, zEnd);
        }

    }

}
