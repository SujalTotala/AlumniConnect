package com.alumniconnect.app.models;

import com.google.gson.annotations.SerializedName;

public class Announcement {
    @SerializedName("id")
    private int id;

    @SerializedName("title")
    private String title;

    @SerializedName("content")
    private String content;

    @SerializedName("priority")
    private String priority; // 'low', 'normal', 'high', 'urgent'

    @SerializedName("is_active")
    private boolean isActive;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("expires_at")
    private String expiresAt;

    public Announcement() {}

    public int getId() { return id; }
    public String getTitle() { return title != null ? title : ""; }
    public String getContent() { return content != null ? content : ""; }
    public String getPriority() { return priority != null ? priority : "normal"; }
    public boolean isActive() { return isActive; }
    public String getCreatedAt() { return createdAt != null ? createdAt : ""; }
    public String getExpiresAt() { return expiresAt; }

    public boolean isUrgentOrHigh() {
        return "urgent".equalsIgnoreCase(priority) || "high".equalsIgnoreCase(priority);
    }
}
