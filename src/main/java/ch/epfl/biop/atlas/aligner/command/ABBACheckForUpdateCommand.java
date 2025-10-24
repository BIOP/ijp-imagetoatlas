package ch.epfl.biop.atlas.aligner.command;

import ch.epfl.biop.atlas.aligner.gui.bdv.ABBABdvStartCommand;
import org.scijava.command.Command;
import org.scijava.plugin.Plugin;
import org.scijava.util.VersionUtils;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

// Merci chaton gpt

@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Multi Image To Atlas>Help>ABBA - Check for updates",
        description = "Check for updates",
        iconPath = "/graphics/ABBAUpdate.png")
public class ABBACheckForUpdateCommand implements Command {

    private static final String REPO_API_URL = "https://api.github.com/repos/BIOP/ijp-imagetoatlas/releases/latest";

    @Override
    public void run() {
        String currentVersion = VersionUtils.getVersion(ABBABdvStartCommand.class);

        try {
            String latestVersion = getLatestReleaseTag();
            StringBuilder bodyBuilder = new StringBuilder();
            bodyBuilder.append("<html> Current version: ").append(currentVersion).append("<br>");
            bodyBuilder.append("Latest release: ").append(latestVersion).append("<br>");

            if (isLatestVersion(currentVersion, latestVersion)) {
                showMessage("You are running the latest version.",
                        bodyBuilder.toString(), JOptionPane.INFORMATION_MESSAGE);
            } else {
                showMessage("A newer version is available !",
                        bodyBuilder.toString(), JOptionPane.WARNING_MESSAGE);
            }

        } catch (Exception e) {
            showMessage("Error occurred while checking for updates: ", e.getMessage(), JOptionPane.ERROR_MESSAGE);
        }

    }

    public static void showMessage(String title, String message, int messageType) {
        // Create a panel to hold the checkbox
        JPanel panel = new JPanel();
        JLabel label = new JLabel(message);
        panel.add(label);

        // Show the option dialog with the warning message and the checkbox
        JOptionPane.showConfirmDialog(null, panel, title, JOptionPane.DEFAULT_OPTION, messageType);
    }

    private static String getLatestReleaseTag() throws Exception {
        URL url = new URL(REPO_API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/vnd.github.v3+json");

        if (conn.getResponseCode() != 200) {
            throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
        }

        BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
        String output;
        StringBuilder response = new StringBuilder();

        while ((output = br.readLine()) != null) {
            response.append(output);
        }

        conn.disconnect();

        // Parse the response JSON to get the tag name
        JSONObject jsonResponse = new JSONObject(response.toString());
        return jsonResponse.getString("tag_name");
    }

    private static boolean isLatestVersion(String currentVersion, String latestVersion) {
        String[] currentParts = currentVersion.split("\\.");
        String[] latestParts = latestVersion.split("\\.");
        if (currentParts[2].endsWith("-SNAPSHOT")) {
            currentParts[2] = currentParts[2].substring(0, currentParts[2].length()-9);
        }

        for (int i = 0; i < Math.min(currentParts.length, latestParts.length); i++) {
            int currentPart = Integer.parseInt(currentParts[i]);
            int latestPart = Integer.parseInt(latestParts[i]);

            if (currentPart < latestPart) {
                return false;
            } else if (currentPart > latestPart) {
                return true;
            }
        }

        return currentParts.length >= latestParts.length;
    }
}
