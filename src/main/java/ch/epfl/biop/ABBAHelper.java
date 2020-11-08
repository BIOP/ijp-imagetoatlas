package ch.epfl.biop;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.URL;

public class ABBAHelper {

    public static void displayABBALogo(int ms) {
        JFrame frameStart = new JFrame();
        frameStart.setUndecorated(true);
        frameStart.setBackground(new Color(1.0f, 1.0f, 1.0f, 0.5f));
        frameStart.setPreferredSize(new Dimension(960,540));
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gd = ge.getScreenDevices();
        frameStart.setLocation(gd[0].getDefaultConfiguration().getBounds().width/2-960/2, gd[0].getDefaultConfiguration().getBounds().height/2-540/2);
        frameStart.pack();
        URL openImage = MultiSlicePositioner.class.getResource("/graphics/ABBA.png");
        try {
            BufferedImage myPicture = ImageIO.read(openImage);
            frameStart.add(new JLabel(new ImageIcon(myPicture)));
            frameStart.setVisible(true);
            Thread.sleep(ms);
            frameStart.setVisible(false);
            frameStart.dispose();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
