package com.alumniconnect.app.models;

import com.google.gson.annotations.SerializedName;

public class AchievementCreateRequest {
    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("category")
    private String category;

    @SerializedName("issuer")
    private String issuer;

    @SerializedName("issue_date")
    private String issueDate;

    @SerializedName("evidence_url")
    private String evidenceUrl;

    public AchievementCreateRequest(String title, String description, String category, String issuer, String issueDate, String evidenceUrl) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.issuer = issuer;
        this.issueDate = issueDate;
        this.evidenceUrl = evidenceUrl;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public String getIssuer() { return issuer; }
    public String getIssueDate() { return issueDate; }
    public String getEvidenceUrl() { return evidenceUrl; }
}
