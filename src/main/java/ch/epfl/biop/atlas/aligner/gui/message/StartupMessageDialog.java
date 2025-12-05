package ch.epfl.biop.atlas.aligner.gui.message;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.net.URI;

/**
 * Swing dialog for displaying startup messages with HTML content
 */
public class StartupMessageDialog {

    private final String title;
    private final String htmlContent;
    private final boolean isAnnouncement;
    private final Runnable onSkipCallback;

    private JDialog dialog;
    private JCheckBox skipCheckBox;

    /**
     * Constructor
     * @param title Dialog title
     * @param htmlContent HTML formatted content
     * @param isAnnouncement True if this is an announcement, false if it's a tip
     * @param onSkipCallback Callback to execute when user checks "skip" option
     */
    public StartupMessageDialog(String title, String htmlContent,
                                boolean isAnnouncement, Runnable onSkipCallback) {
        this.title = title;
        this.htmlContent = htmlContent;
        this.isAnnouncement = isAnnouncement;
        this.onSkipCallback = onSkipCallback;
    }

    /**
     * Show the dialog
     */
    public void showDialog() {
        SwingUtilities.invokeLater(this::createAndShowDialog);
    }

    private void createAndShowDialog() {
        dialog = new JDialog((Frame) null, title, true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout(10, 10));

        // Content panel with HTML rendering
        JEditorPane editorPane = new JEditorPane();
        editorPane.setContentType("text/html");
        editorPane.setText(htmlContent);
        editorPane.setEditable(false);
        // Use a neutral light gray background that works well in both light and dark themes
        editorPane.setBackground(new Color(240, 240, 240));
        editorPane.setForeground(Color.BLACK);
        editorPane.setMargin(new Insets(10, 10, 10, 10));

        // Enable hyperlink clicking
        editorPane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    Desktop.getDesktop().browse(new URI(e.getURL().toString()));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(editorPane);
        scrollPane.setPreferredSize(new Dimension(500, 600));
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        dialog.add(scrollPane, BorderLayout.CENTER);

        // Bottom panel with checkbox and buttons
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        // Skip checkbox
        String skipText = isAnnouncement ?
                "Don't show announcements until there's a new one" :
                "Don't show tips again";
        skipCheckBox = new JCheckBox(skipText);
        bottomPanel.add(skipCheckBox, BorderLayout.WEST);

        // Buttons panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> closeDialog());
        buttonsPanel.add(okButton);

        bottomPanel.add(buttonsPanel, BorderLayout.EAST);

        dialog.add(bottomPanel, BorderLayout.SOUTH);

        // Finalize dialog
        dialog.pack();
        dialog.setLocationRelativeTo(null); // Center on screen
        dialog.setVisible(true);
    }

    private void closeDialog() {
        if (skipCheckBox.isSelected() && onSkipCallback != null) {
            onSkipCallback.run();
        }
        dialog.dispose();
    }
}