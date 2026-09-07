package com.alumniconnect.app.models;

import com.google.gson.annotations.SerializedName;

public class Achievement {
    @SerializedName("id")
    private int id;

    @SerializedName("alumni_id")
    private Integer alumniId;

    @SerializedName("user_id")
    private int userId;

    @SerializedName("user_name")
    private String userName;

    @SerializedName("user_email")
    private String userEmail;

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

    @SerializedName("status")
    private String status;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    public Achievement() {}

    public int getId() { return id; }
    public Integer getAlumniId() { return alumniId; }
    public int getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getUserEmail() { return userEmail; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public String getIssuer() { return issuer; }
    public String getIssueDate() { return issueDate; }
    public String getDate() { return issueDate; }
    public String getEvidenceUrl() { return evidenceUrl; }
    public String getUrl() { return evidenceUrl; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
}
