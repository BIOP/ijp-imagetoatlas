package ch.epfl.biop.atlas.aligner.gui.message;

import com.google.gson.annotations.SerializedName;
import java.time.Instant;
import java.util.List;

/**
 * Data model for startup messages loaded from JSON
 */
public class StartupMessageData {

    @SerializedName("announcements")
    private List<Announcement> announcements;

    @SerializedName("tips")
    private List<Tip> tips;

    public List<Announcement> getAnnouncements() {
        return announcements;
    }

    public List<Tip> getTips() {
        return tips;
    }

    /**
     * Represents an announcement message
     */
    public static class Announcement {
        @SerializedName("timestamp")
        private String timestamp;

        @SerializedName("title")
        private String title;

        @SerializedName("content")
        private String content;

        public String getTimestamp() {
            return timestamp;
        }

        public Instant getTimestampAsInstant() {
            return Instant.parse(timestamp);
        }

        public String getTitle() {
            return title;
        }

        public String getContent() {
            return content;
        }
    }

    /**
     * Represents a tip message
     */
    public static class Tip {
        @SerializedName("id")
        private int id;

        @SerializedName("title")
        private String title;

        @SerializedName("content")
        private String content;

        public int getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getContent() {
            return content;
        }
    }
}