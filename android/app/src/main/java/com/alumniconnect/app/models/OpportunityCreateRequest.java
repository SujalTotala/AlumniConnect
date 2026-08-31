package com.alumniconnect.app.models;

import com.google.gson.annotations.SerializedName;

/**
 * Matches backend OpportunityCreate schema exactly.
 */
public class OpportunityCreateRequest {
    @SerializedName("title")
    private String title;

    @SerializedName("company")
    private String company;

    @SerializedName("description")
    private String description;

    @SerializedName("opportunity_type")
    private String opportunityType;

    @SerializedName("location")
    private String location;

    @SerializedName("deadline")
    private String deadline;

    @SerializedName("application_url")
    private String applicationUrl;

    public OpportunityCreateRequest() {}

    public void setTitle(String title) { this.title = title; }
    public void setCompany(String company) { this.company = company; }
    public void setDescription(String description) { this.description = description; }
    public void setOpportunityType(String opportunityType) { this.opportunityType = opportunityType; }
    public void setLocation(String location) { this.location = location; }
    public void setDeadline(String deadline) { this.deadline = deadline; }
    public void setApplicationUrl(String applicationUrl) { this.applicationUrl = applicationUrl; }
}
