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

    @SerializedName("events_registered")
    private int eventsRegistered;

    @SerializedName("event_attendance_total")
    private int eventAttendanceTotal;

    @SerializedName("event_attendance_rate")
    private double eventAttendanceRate;

    @SerializedName("success_stories_total")
    private int successStoriesTotal;

    @SerializedName("success_stories_pending")
    private int successStoriesPending;

    @SerializedName("communities_total")
    private int communitiesTotal;

    @SerializedName("community_memberships")
    private int communityMemberships;

    @SerializedName("achievements_total")
    private int achievementsTotal;

    @SerializedName("achievements_pending")
    private int achievementsPending;

    public AdminStatistics() {}

    public int getTotalUsers() { return totalUsers; }
    public int getTotalAlumni() { return totalAlumni; }
    public int getTotalStudents() { return totalStudents; }
    public int getActiveMentors() { return activeMentors; }
    public int getTotalEvents() { return totalEvents; }
    public int getTotalEventRegistrations() { return totalEventRegistrations; }
    public int getTotalOpportunities() { return totalOpportunities; }
    public int getPendingMentorshipRequests() { return pendingMentorshipRequests; }
    public int getEventsRegistered() { return eventsRegistered; }
    public int getEventAttendanceTotal() { return eventAttendanceTotal; }
    public double getEventAttendanceRate() { return eventAttendanceRate; }
    public int getSuccessStoriesTotal() { return successStoriesTotal; }
    public int getSuccessStoriesPending() { return successStoriesPending; }
    public int getCommunitiesTotal() { return communitiesTotal; }
    public int getCommunityMemberships() { return communityMemberships; }
    public int getAchievementsTotal() { return achievementsTotal; }
    public int getAchievementsPending() { return achievementsPending; }
}
