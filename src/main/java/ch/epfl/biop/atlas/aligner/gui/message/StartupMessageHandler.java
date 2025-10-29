package ch.epfl.biop.atlas.aligner.gui.message;

import com.google.gson.Gson;
import ij.IJ;
import ij.Prefs;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Handles loading and displaying startup messages from GitHub
 */
public class StartupMessageHandler {

    // GitHub raw URL for the messages JSON file
    // Note: Change 'main' to 'master' if your default branch is 'master'
    private static final String MESSAGES_URL =
            "https://raw.githubusercontent.com/BIOP/ijp-imagetoatlas/main/message/messages.json";

    // Preference keys
    private static final String PREF_LAST_ANNOUNCEMENT_TIMESTAMP =
            "ch.epfl.biop.atlas.aligner.startupmessage.last.announcement.timestamp";
    private static final String PREF_SKIP_TIPS =
            "ch.epfl.biop.atlas.aligner.startupmessage.skip.tips";

    private final Gson gson;
    private final Random random;

    public StartupMessageHandler() {
        this.gson = new Gson();
        this.random = new Random();
    }

    /**
     * Main method to check and display startup messages
     * Call this on application startup
     */
    public void checkAndShowMessage() {
        try {
            StartupMessageData data = loadMessagesFromGitHub();

            if (data == null) {
                IJ.log("Could not load startup messages");
                return;
            }

            // Check for new announcements first (priority)
            StartupMessageData.Announcement latestAnnouncement = getLatestAnnouncement(data);

            if (latestAnnouncement != null && isNewAnnouncement(latestAnnouncement)) {
                showAnnouncement(latestAnnouncement);
                return; // Don't show tips if there's a new announcement
            }

            // If no new announcement, check if we should show a tip
            if (!shouldSkipTips() && data.getTips() != null && !data.getTips().isEmpty()) {
                showRandomTip(data.getTips());
            }

        } catch (Exception e) {
            IJ.log("Error checking startup messages: " + e.getMessage());
        }
    }

    /**
     * Load messages from GitHub
     */
    private StartupMessageData loadMessagesFromGitHub() {
        try {
            URL url = new URL(MESSAGES_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                IJ.log("Failed to fetch messages. HTTP response code: " + responseCode);
                return null;
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream())
            );

            StringBuilder jsonContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);
            }
            reader.close();

            return gson.fromJson(jsonContent.toString(), StartupMessageData.class);

        } catch (Exception e) {
            IJ.log("Error loading messages from GitHub: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get the latest announcement (by timestamp)
     */
    private StartupMessageData.Announcement getLatestAnnouncement(StartupMessageData data) {
        if (data.getAnnouncements() == null || data.getAnnouncements().isEmpty()) {
            return null;
        }

        return data.getAnnouncements().stream()
                .max(Comparator.comparing(a -> a.getTimestampAsInstant()))
                .orElse(null);
    }

    /**
     * Check if the announcement is newer than what the user has seen
     */
    private boolean isNewAnnouncement(StartupMessageData.Announcement announcement) {
        String lastSeenTimestamp = Prefs.get(PREF_LAST_ANNOUNCEMENT_TIMESTAMP, "");

        if (lastSeenTimestamp.isEmpty()) {
            return true; // Never seen any announcement
        }

        try {
            Instant lastSeen = Instant.parse(lastSeenTimestamp);
            Instant current = announcement.getTimestampAsInstant();
            return current.isAfter(lastSeen);
        } catch (Exception e) {
            return true; // If we can't parse, show the announcement to be safe
        }
    }

    /**
     * Show an announcement dialog
     */
    private void showAnnouncement(StartupMessageData.Announcement announcement) {
        StartupMessageDialog dialog = new StartupMessageDialog(
                announcement.getTitle(),
                announcement.getContent(),
                true, // This is an announcement
                () -> handleSkipAnnouncements(announcement)
        );
        dialog.showDialog();
    }

    /**
     * Show a random tip dialog
     */
    private void showRandomTip(List<StartupMessageData.Tip> tips) {
        StartupMessageData.Tip randomTip = tips.get(random.nextInt(tips.size()));

        StartupMessageDialog dialog = new StartupMessageDialog(
                randomTip.getTitle(),
                randomTip.getContent(),
                false, // This is a tip
                this::handleSkipTips
        );
        dialog.showDialog();
    }

    /**
     * Handle when user clicks "Don't show announcements again"
     * Save the current announcement timestamp
     */
    private void handleSkipAnnouncements(StartupMessageData.Announcement announcement) {
        Prefs.set(PREF_LAST_ANNOUNCEMENT_TIMESTAMP, announcement.getTimestamp());
        Prefs.savePreferences();
    }

    /**
     * Handle when user clicks "Don't show tips again"
     */
    private void handleSkipTips() {
        Prefs.set(PREF_SKIP_TIPS, true);
        Prefs.savePreferences();
    }

    /**
     * Check if user has chosen to skip tips
     */
    private boolean shouldSkipTips() {
        return Prefs.get(PREF_SKIP_TIPS, false);
    }

    /**
     * Reset preferences (useful for testing or user settings)
     */
    public static void resetPreferences() {
        Prefs.set(PREF_LAST_ANNOUNCEMENT_TIMESTAMP, "");
        Prefs.set(PREF_SKIP_TIPS, false);
        Prefs.savePreferences();
    }
}
