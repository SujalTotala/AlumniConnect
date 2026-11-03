package com.alumniconnect.app.models;

import com.google.gson.annotations.SerializedName;

public class AttendanceStats {
    @SerializedName("event_id")
    private int eventId;

    @SerializedName("event_title")
    private String eventTitle;

    @SerializedName("total_registered")
    private int totalRegistered;

    @SerializedName("total_attended")
    private int totalAttended;

    @SerializedName("attendance_percentage")
    private double attendancePercentage;

    public AttendanceStats() {}

    public int getEventId() { return eventId; }
    public String getEventTitle() { return eventTitle; }
    public int getTotalRegistered() { return totalRegistered; }
    public int getTotalAttended() { return totalAttended; }
    public double getAttendancePercentage() { return attendancePercentage; }
}
