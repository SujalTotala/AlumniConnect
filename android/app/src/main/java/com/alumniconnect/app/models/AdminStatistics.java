package com.alumniconnect.app.models;

import com.google.gson.annotations.SerializedName;

/**
 * Matches backend AdminStatisticsResponse schema exactly.
 * Used by HomeFragment for admin dashboard KPI display.
 */
public class AdminStatistics {
    @SerializedName("total_users")
    private int totalUsers;

    @SerializedName("total_alumni")
    private int totalAlumni;

    @SerializedName("total_students")
    private int totalStudents;

    @SerializedName("active_mentors")
    private int activeMentors;

    @SerializedName("total_events")
    private int totalEvents;

    @SerializedName("total_event_registrations")
    private int totalEventRegistrations;

    @SerializedName("total_opportunities")
    private int totalOpportunities;

    @SerializedName("pending_mentorship_requests")
    private int pendingMentorshipRequests;

    public AdminStatistics() {}

    public int getTotalUsers() { return totalUsers; }
    public int getTotalAlumni() { return totalAlumni; }
    public int getTotalStudents() { return totalStudents; }
    public int getActiveMentors() { return activeMentors; }
    public int getTotalEvents() { return totalEvents; }
    public int getTotalEventRegistrations() { return totalEventRegistrations; }
    public int getTotalOpportunities() { return totalOpportunities; }
    public int getPendingMentorshipRequests() { return pendingMentorshipRequests; }
}
