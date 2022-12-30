package ch.epfl.biop.atlas.aligner.gui.bdv.card;

import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.gui.bdv.BdvMultislicePositionerView;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import java.awt.Component;
import java.awt.GridLayout;

public class SliceInformationPanel implements BdvMultislicePositionerView.CurrentSliceListener {

    final JPanel paneDisplay;

    final JLabel sliceName;
    final JTextArea sliceInfo;

    public SliceInformationPanel(BdvMultislicePositionerView bdvView) {
        paneDisplay = new JPanel();
        paneDisplay.setLayout(new BoxLayout(paneDisplay, BoxLayout.Y_AXIS));

        sliceName = new JLabel("Slice Name");
        sliceName.setAlignmentX( Component.LEFT_ALIGNMENT );
        sliceInfo = new JTextArea("Slice Info");
        sliceInfo.setAlignmentX( Component.LEFT_ALIGNMENT );
        sliceInfo.setEditable(false);


        bdvView.addCurrentSliceListener(this);

        paneDisplay.add(sliceName);
        JSeparator separator1 = new JSeparator();
        separator1.setAlignmentX( Component.LEFT_ALIGNMENT );
        paneDisplay.add(separator1);
        paneDisplay.add(sliceInfo);
        JSeparator separator2 = new JSeparator();
        separator2.setAlignmentX( Component.LEFT_ALIGNMENT );
        paneDisplay.add(separator2);

    }

    public static JPanel box(boolean alongX,JComponent... components) {
        JPanel box = new JPanel();
        if (alongX) {
            box.setLayout(new GridLayout(1, components.length));
        } else {
            box.setLayout(new GridLayout(components.length, 1));//new BoxLayout(box, BoxLayout.Y_AXIS));
        }
        for(JComponent component : components) {
            box.add(component);
        }
        return box;
    }

    public JPanel getPanel() {
        return paneDisplay;
    }

    @Override
    public void currentSliceChanged(SliceSources slice) {
        sliceName.setText("Current slice: "+slice.getName());
        sliceInfo.setText(slice.getInfo());
    }
}
