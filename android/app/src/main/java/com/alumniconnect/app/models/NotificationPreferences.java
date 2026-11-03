package com.alumniconnect.app.models;

import com.google.gson.annotations.SerializedName;

public class NotificationPreferences {
    @SerializedName("user_id")
    private Integer userId;

    @SerializedName("events")
    private boolean events = true;

    @SerializedName("mentorship")
    private boolean mentorship = true;

    @SerializedName("opportunities")
    private boolean opportunities = true;

    @SerializedName("announcements")
    private boolean announcements = true;

    @SerializedName("updated_at")
    private String updatedAt;

    public NotificationPreferences() {}

    public NotificationPreferences(boolean events, boolean mentorship, boolean opportunities, boolean announcements) {
        this.events = events;
        this.mentorship = mentorship;
        this.opportunities = opportunities;
        this.announcements = announcements;
    }

    public Integer getUserId() { return userId; }
    public boolean isEvents() { return events; }
    public void setEvents(boolean events) { this.events = events; }
    public boolean isMentorship() { return mentorship; }
    public void setMentorship(boolean mentorship) { this.mentorship = mentorship; }
    public boolean isOpportunities() { return opportunities; }
    public void setOpportunities(boolean opportunities) { this.opportunities = opportunities; }
    public boolean isAnnouncements() { return announcements; }
    public void setAnnouncements(boolean announcements) { this.announcements = announcements; }
    public String getUpdatedAt() { return updatedAt; }
}
