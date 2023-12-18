package ch.epfl.biop.atlas.aligner.gui.bdv.card;

import ch.epfl.biop.atlas.aligner.SliceSources;
import ch.epfl.biop.atlas.aligner.gui.bdv.BdvMultislicePositionerView;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
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
        JScrollPane scrollPane = new JScrollPane(sliceInfo);


        bdvView.addCurrentSliceListener(this);

        paneDisplay.add(sliceName);
        JSeparator separator1 = new JSeparator();
        separator1.setAlignmentX( Component.LEFT_ALIGNMENT );
        paneDisplay.add(separator1);
        paneDisplay.add(scrollPane);//sliceInfo);
        JSeparator separator2 = new JSeparator();
        separator2.setAlignmentX( Component.LEFT_ALIGNMENT );
        paneDisplay.add(separator2);
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
