package com.alumniconnect.app.models;

import com.google.gson.annotations.SerializedName;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Matches backend NotificationResponse schema exactly.
 */
public class Notification {
    @SerializedName("id")
    private int id;

    @SerializedName("user_id")
    private int userId;

    @SerializedName("title")
    private String title;

    @SerializedName("message")
    private String message;

    @SerializedName("notification_type")
    private String notificationType; // MENTORSHIP, EVENT, OPPORTUNITY, GENERAL

    @SerializedName("is_read")
    private boolean isRead;

    @SerializedName("created_at")
    private String createdAt; // ISO-8601 datetime string

    public Notification() {}

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public String getTitle() { return title != null ? title : ""; }
    public String getMessage() { return message != null ? message : ""; }
    public String getNotificationType() { return notificationType != null ? notificationType : "GENERAL"; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    public String getCreatedAt() { return createdAt; }

    /** Get appropriate emoji for the notification type */
    public String getTypeEmoji() {
        if (notificationType == null) return "🔔";
        switch (notificationType.toUpperCase()) {
            case "MENTORSHIP":
                return "🎓";
            case "EVENT":
                return "📅";
            case "OPPORTUNITY":
                return "💼";
            default:
                return "🔔";
        }
    }

    /** Formats ISO timestamp to relative time (e.g. "5 min ago", "2 hr ago", "Yesterday", "18 Aug 2026") */
    public String getRelativeTime() {
        if (createdAt == null || createdAt.trim().isEmpty()) return "";
        
        // Clean ISO fractional seconds or trailing Z
        String cleaned = createdAt.replace("Z", "+0000");
        
        // Try parsing different common ISO formats
        String[] formats = {
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ",
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSS",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss"
        };
        
        Date date = null;
        for (String format : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
                if (format.contains("Z")) {
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                }
                date = sdf.parse(cleaned);
                if (date != null) break;
            } catch (Exception ignored) {}
        }
        
        if (date == null) {
            // Fallback: parse just YYYY-MM-DD
            try {
                if (createdAt.length() >= 10) {
                    String ymd = createdAt.substring(0, 10);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                    date = sdf.parse(ymd);
                }
            } catch (Exception ignored) {}
        }

        if (date == null) {
            return createdAt; // absolute fallback
        }

        long now = System.currentTimeMillis();
        long diff = now - date.getTime();
        if (diff < 0) {
            diff = 0; // handle slight clock drifts
        }

        long diffSeconds = diff / 1000;
        long diffMinutes = diff / (60 * 1000);
        long diffHours = diff / (60 * 60 * 1000);
        long diffDays = diff / (24 * 60 * 60 * 1000);

        if (diffSeconds < 60) {
            return "Just now";
        } else if (diffMinutes < 60) {
            return diffMinutes + (diffMinutes == 1 ? " min ago" : " min ago");
        } else if (diffHours < 24) {
            return diffHours + (diffHours == 1 ? " hr ago" : " hr ago");
        } else if (diffDays == 1) {
            return "Yesterday";
        } else if (diffDays < 7) {
            return diffDays + " days ago";
        } else {
            try {
                SimpleDateFormat friendlySdf = new SimpleDateFormat("d MMM yyyy", Locale.US);
                return friendlySdf.format(date);
            } catch (Exception e) {
                return createdAt;
            }
        }
    }
}
