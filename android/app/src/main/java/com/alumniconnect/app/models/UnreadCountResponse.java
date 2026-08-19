package com.alumniconnect.app.models;

import com.google.gson.annotations.SerializedName;

/**
 * Matches backend NotificationCountResponse schema exactly.
 */
public class UnreadCountResponse {
    @SerializedName("unread_count")
    private int unreadCount;

    public UnreadCountResponse() {}

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }
}
