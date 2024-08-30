package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.MultiSlicePositioner;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;

// Merci chaton gpt

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Help>ABBA - How to cite",
        description = "How to cite ABBA")
public class ABBACiteInfoCommand implements Command {

    @Override
    public void run() {

        JFrame frame = new JFrame("How to cite");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(450, 350);
        frame.setLocationRelativeTo(null); // Center the window
        frame.setIconImage((new ImageIcon(MultiSlicePositioner.class.getResource("/graphics/ABBAFrame.jpg"))).getImage());

        // Create a JPanel to hold the content and set a nice background color
        JPanel panel = new JPanel();
        panel.setBackground(new Color(245, 245, 245)); // Light gray background
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15)); // Padding around the edges

        String htmlContent = "<html><body>" +
                "No network connection, the citation information is not available. Please connect to the internet and retry.</body></html>";
        try {
            URL url = new URL("https://go.epfl.ch/abba-cite");
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(url.openStream()));
            htmlContent = "";
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println(inputLine);
                if (!inputLine.startsWith("#")) {
                    htmlContent += inputLine+"\n";
                }
            }
            in.close();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }


        // Create a JEditorPane to display the HTML content
        JEditorPane editorPane = new JEditorPane("text/html", htmlContent);
        editorPane.setEditable(false);
        editorPane.setOpaque(false);

        // Add an ActionListener to handle the link clicks
        editorPane.addHyperlinkListener(e -> {
            if (e.getEventType() == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    Desktop.getDesktop().browse(new URI(e.getURL().toString()));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        // Add a JScrollPane in case the content overflows
        JScrollPane scrollPane = new JScrollPane(editorPane);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // Add the components to the panel
        panel.add(scrollPane);

        // Add the panel to the frame
        frame.add(panel);

        // Show the window
        frame.setVisible(true);
    }

}
