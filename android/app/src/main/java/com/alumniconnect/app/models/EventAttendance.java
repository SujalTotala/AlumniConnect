package com.alumniconnect.app.models;

import com.google.gson.annotations.SerializedName;

public class EventAttendance {
    @SerializedName("id")
    private int id;

    @SerializedName("event_id")
    private int eventId;

    @SerializedName("user_id")
    private int userId;

    @SerializedName("registration_id")
    private Integer registrationId;

    @SerializedName("checked_in_at")
    private String checkedInAt;

    @SerializedName("checkin_method")
    private String checkinMethod;

    @SerializedName("user_name")
    private String userName;

    @SerializedName("user_email")
    private String userEmail;

    @SerializedName("user_role")
    private String userRole;

    public EventAttendance() {}

    public int getId() { return id; }
    public int getEventId() { return eventId; }
    public int getUserId() { return userId; }
    public Integer getRegistrationId() { return registrationId; }
    public String getCheckedInAt() { return checkedInAt; }
    public String getCheckinMethod() { return checkinMethod; }
    public String getUserName() { return userName; }
    public String getUserFullName() { return userName; }
    public String getUserEmail() { return userEmail; }
    public String getUserRole() { return userRole; }
}
