package com.alumniconnect.app.models;

import com.google.gson.annotations.SerializedName;

public class ReferralOpportunitySummary {
    @SerializedName("id")
    private int id;

    @SerializedName("title")
    private String title;

    @SerializedName("company")
    private String company;

    @SerializedName("opportunity_type")
    private String opportunityType;

    @SerializedName("location")
    private String location;

    @SerializedName("application_url")
    private String applicationUrl;

    public ReferralOpportunitySummary() {}

    public int getId() { return id; }
    public String getTitle() { return title != null ? title : ""; }
    public String getCompany() { return company != null ? company : ""; }
    public String getOpportunityType() { return opportunityType != null ? opportunityType : "Job"; }
    public String getLocation() { return location; }
    public String getApplicationUrl() { return applicationUrl; }
}
