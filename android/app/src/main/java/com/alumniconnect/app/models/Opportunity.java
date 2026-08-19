package com.alumniconnect.app.models;

import com.google.gson.annotations.SerializedName;

/**
 * Matches backend OpportunityResponse schema exactly.
 */
public class Opportunity {
    @SerializedName("id")
    private int id;

    @SerializedName("title")
    private String title;

    @SerializedName("company")
    private String company;

    @SerializedName("description")
    private String description;

    @SerializedName("opportunity_type")
    private String opportunityType; // Internship, Full-Time Job, Referral, etc.

    @SerializedName("location")
    private String location;         // nullable

    @SerializedName("deadline")
    private String deadline;         // nullable, format "YYYY-MM-DD"

    @SerializedName("application_url")
    private String applicationUrl;   // nullable

    @SerializedName("posted_by")
    private Integer postedBy;        // nullable

    @SerializedName("created_at")
    private String createdAt;        // nullable datetime string

    @SerializedName("poster_name")
    private String posterName;       // nullable

    public Opportunity() {}

    // Getters
    public int getId() { return id; }
    public String getTitle() { return title != null ? title : ""; }
    public String getCompany() { return company != null ? company : ""; }
    public String getDescription() { return description != null ? description : ""; }
    public String getOpportunityType() { return opportunityType != null ? opportunityType : "Job"; }
    public String getLocation() { return location; }
    public String getDeadline() { return deadline; }
    public String getApplicationUrl() { return applicationUrl; }
    public Integer getPostedBy() { return postedBy; }
    public String getCreatedAt() { return createdAt; }
    public String getPosterName() { return posterName != null ? posterName : "Alumni Community"; }

    /** Returns formatted date representation of deadline */
    public String getFormattedDeadline() {
        if (deadline == null || deadline.trim().isEmpty()) return null;
        try {
            String[] parts = deadline.split("-");
            if (parts.length == 3) {
                int year = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                int day = Integer.parseInt(parts[2]);
                String[] months = {"Jan","Feb","Mar","Apr","May","Jun",
                                   "Jul","Aug","Sep","Oct","Nov","Dec"};
                String monthName = (month >= 1 && month <= 12) ? months[month - 1] : parts[1];
                return day + " " + monthName + " " + year;
            }
        } catch (Exception ignored) {}
        return deadline;
    }

    /** Returns true if deadline is non-empty and has passed current system date */
    public boolean isDeadlinePassed() {
        if (deadline == null || deadline.trim().isEmpty()) return false;
        try {
            String[] parts = deadline.split("-");
            if (parts.length == 3) {
                int year = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]) - 1; // Calendar month is 0-indexed
                int day = Integer.parseInt(parts[2]);

                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
                cal.set(java.util.Calendar.MINUTE, 0);
                cal.set(java.util.Calendar.SECOND, 0);
                cal.set(java.util.Calendar.MILLISECOND, 0);
                long todayMs = cal.getTimeInMillis();

                cal.set(year, month, day);
                long deadlineMs = cal.getTimeInMillis();

                return todayMs > deadlineMs;
            }
        } catch (Exception ignored) {}
        return false;
    }

    /** Helper for checking string presence */
    public static boolean hasValue(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
