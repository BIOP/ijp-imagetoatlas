package ch.epfl.biop.atlas.aligner.gui.bdv;

import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.action.MarkActionSequenceBatchAction;
import net.imglib2.RealPoint;
import org.scijava.ui.behaviour.DragBehaviour;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SliceDragBehaviour implements DragBehaviour {

    private static final Logger logger = LoggerFactory.getLogger(SliceDragBehaviour.class);

    List<SliceSources> affectedSlices = new ArrayList<>();
    final SliceSources sliceDragged;
    final Map<SliceSources, Double> initialAxisPositions = new HashMap<>();
    double rangeBefore;
    double rangeAfter;
    double lastAxisPos;
    RealPoint iniPointBdv = new RealPoint(3);
    double iniSlicePointing;
    double iniSlicingAxisPosition;
    boolean perform;
    final BdvMultislicePositionerView view;
    boolean keyLeft = false;
    boolean keyRight = false;
    SliceSources keySliceRight;
    SliceSources keySliceLeft;
    int indexOfSliceInDraggedOnes = -1;

    public SliceDragBehaviour(final BdvMultislicePositionerView view, SliceSources slice) {
        this.view = view;
        this.sliceDragged = slice;
    }

    @Override
    public void init(int x, int y) {
        logger.debug(" DragSlice start ("+x+":"+y+")  ? "+ sliceDragged + " perform = "+perform);
        perform = view.startDragAction();

        if ((perform)&&(view.mode != BdvMultislicePositionerView.POSITIONING_MODE_INT)) {
            perform = false;
            view.stopDragAction();
            logger.debug(" DragSlice start ("+x+":"+y+")  Cancelled "+ sliceDragged + " perform = "+perform);
        }

        if (perform) {
            logger.debug(" DragSlice start ("+x+":"+y+")  ! "+ sliceDragged + " perform = "+perform);

            // Collect all selected sources
            affectedSlices = view.msp.getSelectedSlices();

            int indexKeySliceRight = -1;
            int indexKeySliceLeft = -1;
            if (!affectedSlices.contains(sliceDragged)) {
                logger.warn("This should not happen : slice dragged is not selected!");
                perform = false;
                view.stopDragAction();
            } else {

                affectedSlices.forEach(s -> initialAxisPositions.put(s, s.getSlicingAxisPosition()));
                // Now let's look at key slices and filter out the sources which will
                // not be affected by drags because they are 'protected' by key slices

                // Is there any key slice to the Right ?
                int indexCurrentSliceSelected = affectedSlices.indexOf(sliceDragged);


                List<SliceSources> slicesSelected = new ArrayList<>();
                // Is there any key slice to the Left ?
                for (int i = indexCurrentSliceSelected - 1; i >= 0; i--) {
                    //mp.debuglog.accept("Testing Left " + i);
                    if (affectedSlices.get(i).isKeySlice()&&(!keyLeft)) {
                        keyLeft = true;
                        keySliceLeft = affectedSlices.get(i);
                        indexKeySliceLeft = i;
                        logger.debug("Key Slice Left found : " + i + " keysliceleft = " + keySliceLeft + " at p = " + initialAxisPositions.get(keySliceLeft));
                    }
                }

                for (int i = indexCurrentSliceSelected + 1; i < affectedSlices.size(); i++) {
                    //mp.debuglog.accept("Testing Right " + i);
                    if (affectedSlices.get(i).isKeySlice()&&(!keyRight)) {
                        keyRight = true;
                        keySliceRight = affectedSlices.get(i);
                        indexKeySliceRight = i;
                        logger.debug("Key Slice Right found : " + i + " keysliceright = " + keySliceRight + " at p = " + initialAxisPositions.get(keySliceRight));
                    }
                }

                for (int i = 0; i < affectedSlices.size(); i++) {
                    if (i == indexCurrentSliceSelected) {
                        slicesSelected.add(affectedSlices.get(i));
                    }
                    if ((keyLeft) && (i < indexKeySliceLeft)) {
                    } else {
                        if ((keyRight) && (i > indexKeySliceRight)) {
                        } else {
                            slicesSelected.add(affectedSlices.get(i));
                        }
                    }
                }

                affectedSlices = slicesSelected;
                indexOfSliceInDraggedOnes = affectedSlices.indexOf(sliceDragged);

                if (indexOfSliceInDraggedOnes == -1) {
                    logger.debug("This should not happen : index of slice in dragged one not present");
                    perform = false;
                    view.stopDragAction();
                } else {
                    rangeBefore = initialAxisPositions.get(affectedSlices.get(indexOfSliceInDraggedOnes)) - initialAxisPositions.get(affectedSlices.get(0));
                    rangeAfter = initialAxisPositions.get(affectedSlices.get(affectedSlices.size() - 1)) - initialAxisPositions.get(affectedSlices.get(indexOfSliceInDraggedOnes));
                    lastAxisPos = initialAxisPositions.get(affectedSlices.get(indexOfSliceInDraggedOnes));
                }
            }
        }

        logger.debug(" DragSlice start ("+x+":"+y+") "+ sliceDragged +" Drag perform : ("+perform+")");
    }

    @Override
    public void drag(int x, int y) {
        if (perform) {
            //mp.debuglog.accept(" drag (" + x + ":" + y + ")");
            RealPoint currentMousePosition = new RealPoint(3);
            view.getBdvh().getViewerPanel().getGlobalMouseCoordinates(currentMousePosition);

            double currentSlicingAxisPosition = ((currentMousePosition.getDoublePosition(0) - 0.5 * view.msp.sX) / (view.msp.sX)) * view.msp.sizePixZ * (double) view.msp.getReslicedAtlas().getStep();

            double keyLeftPosition = -1;
            if (keyLeft) keyLeftPosition = initialAxisPositions.get(keySliceLeft);

            double keyRightPosition = -1;
            if (keyRight) keyRightPosition = initialAxisPositions.get(keySliceRight);

            for (int i=0; i<= indexOfSliceInDraggedOnes; i++) {
                SliceSources sliceMoved = affectedSlices.get(i);
                double iniSlicePos = initialAxisPositions.get(sliceMoved);
                if (keyLeft) {
                    // The key slice on the left should not move
                    // There's a key to the left, we proportionally stretch the slices
                    if (rangeBefore == 0) {
                        assert i!= 0;
                        double ratio = 1.0 / (i);
                        setDisplayAxisPosition(sliceMoved,keyLeftPosition + (initialAxisPositions.get(sliceMoved) - keyLeftPosition) * ratio );
                    } else {
                        double ratio = (currentSlicingAxisPosition - keyLeftPosition) / rangeBefore;
                        setDisplayAxisPosition(sliceMoved, keyLeftPosition + (initialAxisPositions.get(sliceMoved) - keyLeftPosition) * ratio);
                    }
                } else {
                    if (keyRight) {
                        if (rangeAfter == 0) {
                            assert i!= affectedSlices.size()-1;
                            double ratio = 1.0 / (affectedSlices.size()-1-i);
                            setDisplayAxisPosition(sliceMoved, keyRightPosition + (initialAxisPositions.get(sliceMoved) - keyRightPosition) * ratio);
                        } else {
                            double ratio = (keyRightPosition - currentSlicingAxisPosition) / rangeAfter;
                            setDisplayAxisPosition(sliceMoved, keyRightPosition + (initialAxisPositions.get(sliceMoved) - keyRightPosition) * ratio);
                        }
                    } else {
                        // Simple shift of all slices
                        double shift = currentSlicingAxisPosition - initialAxisPositions.get(affectedSlices.get(indexOfSliceInDraggedOnes));
                        setDisplayAxisPosition(sliceMoved, iniSlicePos + shift);
                    }
                }
            }

            for (int i = indexOfSliceInDraggedOnes+1; i < affectedSlices.size(); i++) {
                SliceSources sliceMoved = affectedSlices.get(i);
                double iniSlicePos = initialAxisPositions.get(sliceMoved);
                if (keyRight) {
                    // The key slice on the left should not move
                    // There's a key to the right, we proportionally stretch the slices
                    if (rangeAfter == 0) {
                        assert i!= affectedSlices.size()-1;
                        double ratio = 1.0 / (affectedSlices.size()-1-i);
                        setDisplayAxisPosition(sliceMoved, keyRightPosition + (initialAxisPositions.get(sliceMoved) - keyRightPosition) * ratio);
                    } else {
                        double ratio = (keyRightPosition - currentSlicingAxisPosition) / rangeAfter;
                        setDisplayAxisPosition(sliceMoved, keyRightPosition + (initialAxisPositions.get(sliceMoved) - keyRightPosition) * ratio);
                    }
                } else {
                    if (keyLeft) {
                        // There's a key to the right, we proportionally stretch the slices
                        if (rangeBefore == 0) {
                            assert i!= 0;
                            double ratio = 1.0 / (i);
                            setDisplayAxisPosition(sliceMoved, keyLeftPosition + (initialAxisPositions.get(sliceMoved) - keyLeftPosition) * ratio);
                        } else {
                            double ratio = (currentSlicingAxisPosition - keyLeftPosition) / rangeBefore;
                            setDisplayAxisPosition(sliceMoved, keyLeftPosition + (initialAxisPositions.get(sliceMoved) - keyLeftPosition) * ratio);
                        }
                    } else {
                        // Simple shift of all slices
                        double shift = currentSlicingAxisPosition - initialAxisPositions.get(affectedSlices.get(indexOfSliceInDraggedOnes));
                        setDisplayAxisPosition(sliceMoved, iniSlicePos + shift);
                    }

                }
            }
            view.getBdvh().getViewerPanel().requestRepaint();
        }
    }

    void setDisplayAxisPosition(SliceSources slice, double displayedAxisPosition) {
        view.guiState.runSlice(slice, sliceGuiState -> sliceGuiState.setDisplayedAxisPosition(displayedAxisPosition));
    }

    @Override
    public void end(int x, int y) {
        if (perform) {
            logger.debug(" DragSlice end (" + x + ":" + y + ") "+sliceDragged);

            // Restores the original displayed atlas position
            for (SliceSources slice : affectedSlices) {
                setDisplayAxisPosition(slice, initialAxisPositions.get(slice));
            }

            if (affectedSlices.size()>1) new MarkActionSequenceBatchAction(view.msp).runRequest();

            RealPoint currentMousePosition = new RealPoint(3);
            view.getBdvh().getViewerPanel().getGlobalMouseCoordinates(currentMousePosition);

            double currentSlicingAxisPosition = ((currentMousePosition.getDoublePosition(0) - 0.5 * view.msp.sX) / (view.msp.sX)) * view.msp.sizePixZ * (double) view.msp.getReslicedAtlas().getStep();

            double keyLeftPosition = -1;
            if (keyLeft) keyLeftPosition = initialAxisPositions.get(keySliceLeft);

            double keyRightPosition = -1;
            if (keyRight) keyRightPosition = initialAxisPositions.get(keySliceRight);

            for (int i=0; i<= indexOfSliceInDraggedOnes; i++) {
                SliceSources sliceMoved = affectedSlices.get(i);
                double iniSlicePos = initialAxisPositions.get(sliceMoved);
                if (keyLeft) {
                    // The key slice on the left should not move
                    // There's a key to the left, we proportionally stretch the slices
                    if (rangeBefore == 0) {
                        assert i!= 0;
                        double ratio = 1.0 / (i);
                        //sliceMoved.setSlicingAxisPosition(keyLeftPosition + (initialAxisPositions.get(sliceMoved) - keyLeftPosition) * ratio);
                        view.msp.moveSlice(sliceMoved, keyLeftPosition + (initialAxisPositions.get(sliceMoved) - keyLeftPosition) * ratio);
                    } else {
                        double ratio = (currentSlicingAxisPosition - keyLeftPosition) / rangeBefore;
                        //sliceMoved.setSlicingAxisPosition(keyLeftPosition + (initialAxisPositions.get(sliceMoved) - keyLeftPosition) * ratio);
                        view.msp.moveSlice(sliceMoved, keyLeftPosition + (initialAxisPositions.get(sliceMoved) - keyLeftPosition) * ratio);
                    }
                } else {
                    if (keyRight) {
                        if (rangeAfter == 0) {
                            assert i!= affectedSlices.size()-1;
                            double ratio = 1.0 / (affectedSlices.size()-1-i);
                            view.msp.moveSlice(sliceMoved,keyRightPosition + (initialAxisPositions.get(sliceMoved) - keyRightPosition) * ratio);
                        } else {
                            double ratio = (keyRightPosition - currentSlicingAxisPosition) / rangeAfter;
                            view.msp.moveSlice(sliceMoved, keyRightPosition + (initialAxisPositions.get(sliceMoved) - keyRightPosition) * ratio);
                        }
                    } else {
                        // Simple shift of all slices
                        double shift = currentSlicingAxisPosition - initialAxisPositions.get(affectedSlices.get(indexOfSliceInDraggedOnes));
                        view.msp.moveSlice(sliceMoved, iniSlicePos + shift);
                    }
                }
            }

            for (int i = indexOfSliceInDraggedOnes+1; i < affectedSlices.size(); i++) {
                SliceSources sliceMoved = affectedSlices.get(i);
                double iniSlicePos = initialAxisPositions.get(sliceMoved);
                if (keyRight) {
                    // The key slice on the left should not move
                    // There's a key to the right, we proportionally stretch the slices
                    if (rangeAfter == 0) {
                        assert i!= affectedSlices.size()-1;
                        double ratio = 1.0 / (affectedSlices.size()-1-i);
                        view.msp.moveSlice(sliceMoved, keyRightPosition + (initialAxisPositions.get(sliceMoved) - keyRightPosition) * ratio);
                    } else {
                        double ratio = (keyRightPosition - currentSlicingAxisPosition) / rangeAfter;
                        view.msp.moveSlice(sliceMoved, keyRightPosition + (initialAxisPositions.get(sliceMoved) - keyRightPosition) * ratio);
                    }
                } else {
                    if (keyLeft) {
                        // There's a key to the right, we proportionally stretch the slices
                        if (rangeBefore == 0) {
                            assert i!= 0;
                            double ratio = 1.0 / (i);
                            view.msp.moveSlice(sliceMoved, keyLeftPosition + (initialAxisPositions.get(sliceMoved) - keyLeftPosition) * ratio);
                        } else {
                            double ratio = (currentSlicingAxisPosition - keyLeftPosition) / rangeBefore;
                            view.msp.moveSlice(sliceMoved, keyLeftPosition + (initialAxisPositions.get(sliceMoved) - keyLeftPosition) * ratio);
                        }
                    } else {
                        // Simple shift of all slices
                        double shift = currentSlicingAxisPosition - initialAxisPositions.get(affectedSlices.get(indexOfSliceInDraggedOnes));
                        view.msp.moveSlice(sliceMoved, iniSlicePos + shift);
                    }

                }
            }

            if (affectedSlices.size()>1) new MarkActionSequenceBatchAction(view.msp).runRequest();
            if (view.overlapMode == 2) {
                view.updateSliceDisplayedPosition(null);
                view.getBdvh().getViewerPanel().requestRepaint();
            }
            perform = false;
            view.stopDragAction();
        }

        // Fields are reused for the next drag action
        affectedSlices.clear();
        initialAxisPositions.clear();
        rangeBefore = -1;
        rangeAfter = -1;
        lastAxisPos = -1;
        iniPointBdv = new RealPoint(3);
        iniSlicePointing = -1;
        iniSlicingAxisPosition = -1;

        keyLeft = false;
        keyRight = false;
        keySliceRight = null;
        keySliceLeft = null;
        indexOfSliceInDraggedOnes = -1;
    }
}
