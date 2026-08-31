package com.alumniconnect.app.models;

import com.google.gson.annotations.SerializedName;

/**
 * Matches backend EventRegistrationResponse schema.
 * Used when admin/creator views attendee list (GET /events/{id}/registrations).
 */
public class EventRegistration {
    @SerializedName("id")
    private int id;

    @SerializedName("event_id")
    private int eventId;

    @SerializedName("user_id")
    private int userId;

    @SerializedName("registration_status")
    private String registrationStatus;

    @SerializedName("registered_at")
    private String registeredAt;       // ISO-8601 datetime, nullable

    @SerializedName("user_name")
    private String userName;

    @SerializedName("user_email")
    private String userEmail;

    @SerializedName("user_role")
    private String userRole;

    public EventRegistration() {}

    public int getId() { return id; }
    public int getEventId() { return eventId; }
    public int getUserId() { return userId; }
    public String getRegistrationStatus() { return registrationStatus != null ? registrationStatus : ""; }
    public String getRegisteredAt() { return registeredAt; }
    public String getUserName() { return userName != null ? userName : "Unknown"; }
    public String getUserEmail() { return userEmail != null ? userEmail : ""; }
    public String getUserRole() { return userRole != null ? userRole : ""; }
}
