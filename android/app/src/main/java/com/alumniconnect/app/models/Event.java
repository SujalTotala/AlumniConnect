package com.alumniconnect.app.models;

import com.google.gson.annotations.SerializedName;

/**
 * Matches backend EventResponse schema exactly.
 * is_registered and registrations_count come from the backend,
 * which uses the JWT to compute per-user state.
 */
public class Event {
    @SerializedName("id")
    private int id;

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("event_type")
    private String eventType;          // "Alumni Meet", "Webinar", "Workshop", etc.

    @SerializedName("event_date")
    private String eventDate;          // stored as String (e.g. "2026-08-25")

    @SerializedName("start_time")
    private String startTime;          // nullable, e.g. "14:30:00"

    @SerializedName("location")
    private String location;

    @SerializedName("meeting_url")
    private String meetingUrl;         // nullable

    @SerializedName("image_url")
    private String imageUrl;           // nullable

    @SerializedName("created_by")
    private Integer createdBy;         // nullable

    @SerializedName("created_at")
    private String createdAt;          // nullable ISO-8601

    @SerializedName("registrations_count")
    private Integer registrationsCount;  // default 0

    @SerializedName("is_registered")
    private Boolean isRegistered;        // default false — backend computes per JWT

    public Event() {}

    // --- Getters ---
    public int getId() { return id; }
    public String getTitle() { return title != null ? title : ""; }
    public String getDescription() { return description != null ? description : ""; }
    public String getEventType() { return eventType != null ? eventType : "Event"; }
    public String getEventDate() { return eventDate; }
    public String getStartTime() { return startTime; }
    public String getLocation() { return location != null ? location : ""; }
    public String getMeetingUrl() { return meetingUrl; }
    public String getImageUrl() { return imageUrl; }
    public Integer getCreatedBy() { return createdBy; }
    public String getCreatedAt() { return createdAt; }
    public int getRegistrationsCount() {
        return registrationsCount != null ? registrationsCount : 0;
    }
    public boolean isRegistered() {
        return isRegistered != null && isRegistered;
    }

    // --- Setters (needed for local state updates after register/cancel) ---
    public void setIsRegistered(boolean registered) { this.isRegistered = registered; }
    public void setRegistrationsCount(int count) { this.registrationsCount = count; }

    /** Returns a formatted display date e.g. "25 Aug 2026" or raw if parsing fails */
    public String getFormattedDate() {
        if (eventDate == null || eventDate.isEmpty()) return "";
        try {
            // eventDate format: "YYYY-MM-DD"
            String[] parts = eventDate.split("-");
            if (parts.length == 3) {
                int year  = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                int day   = Integer.parseInt(parts[2]);
                String[] months = {"Jan","Feb","Mar","Apr","May","Jun",
                                   "Jul","Aug","Sep","Oct","Nov","Dec"};
                String monthName = (month >= 1 && month <= 12) ? months[month - 1] : parts[1];
                return day + " " + monthName + " " + year;
            }
        } catch (Exception ignored) {}
        return eventDate;
    }

    /** Returns formatted time e.g. "2:30 PM" from "14:30:00" */
    public String getFormattedTime() {
        if (startTime == null || startTime.isEmpty()) return "";
        try {
            String[] parts = startTime.split(":");
            int hour   = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            String ampm = hour >= 12 ? "PM" : "AM";
            int displayHour = hour % 12;
            if (displayHour == 0) displayHour = 12;
            return String.format("%d:%02d %s", displayHour, minute, ampm);
        } catch (Exception ignored) {}
        return startTime;
    }

    /** Whether the event appears to be online (has meeting URL) */
    public boolean isOnline() {
        return meetingUrl != null && !meetingUrl.trim().isEmpty();
    }

    /** Null-safe check */
    public static boolean hasValue(String s) {
        return s != null && !s.trim().isEmpty();
    }

    /** Returns event type emoji for display */
    public String getTypeEmoji() {
        if (eventType == null) return "📅";
        switch (eventType.toLowerCase()) {
            case "webinar":        return "💻";
            case "workshop":       return "🛠️";
            case "networking":     return "🤝";
            case "career guidance":return "🎯";
            case "reunion":        return "🎉";
            case "guest lecture":  return "🎓";
            default:               return "📅"; // Alumni Meet and others
        }
    }
}
