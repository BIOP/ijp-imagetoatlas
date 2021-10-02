package ch.epfl.biop.atlas.aligner;

import org.scijava.Context;
import org.scijava.platform.PlatformService;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URL;

public class AtlasInfoPanel {

    final JPanel paneDisplay;

    public AtlasInfoPanel(MultiSlicePositioner mp) {
        paneDisplay = new JPanel();

        paneDisplay.setLayout(new BoxLayout(paneDisplay, BoxLayout.X_AXIS));

        JLabel atlasName = new JLabel(mp.getAtlas().toString());

        String modalities = "";

        for (String key : mp.getAtlas().getMap().getImagesKeys()) {
            modalities += "["+key+"]";
        }

        int nPx, nPy, nPz;
        long[] dimensions = {0,0,0};
        mp.getAtlas().getMap().getLabelImage().getSpimSource().getSource(0,0).dimensions(dimensions);

        JLabel atlasResolution = new JLabel("["+dimensions[0]+","+dimensions[1]+","+dimensions[2]+"] - Resolution: "+(int) (mp.getAtlas().getMap().getAtlasPrecisionInMillimeter()*1000)+" um");

        JLabel atlasModalities = new JLabel(modalities);

        JTextPane atlasDOI = new JTextPane();
        //f.setContentType("text/html"); // let the text pane know this is what you want
        atlasDOI.setText("doi: "+mp.getAtlas().getDOIs().get(0)); // showing off
        atlasDOI.setEditable(false); // as before
        atlasDOI.setBackground(null); // this is the same as a JLabel
        atlasDOI.setBorder(null); // remove the border

        JButton gotoInfo = new JButton("WebPage");

        Context ctx = mp.scijavaCtx;

        gotoInfo.addActionListener(e -> {
            try {
                ctx.getService(PlatformService.class).open(new URL(mp.getAtlas().getURL()));
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });

        paneDisplay.add(box(false,
                atlasName,
                atlasModalities,
                atlasResolution,
                atlasDOI,
                gotoInfo));

    }

    public JPanel box(boolean alongX,JComponent... components) {
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

}
