package com.alumniconnect.app.models;

import com.google.gson.annotations.SerializedName;

public class SuccessStory {
    @SerializedName("id")
    private int id;

    @SerializedName("author_user_id")
    private int authorUserId;

    @SerializedName("alumni_id")
    private Integer alumniId;

    @SerializedName("author_name")
    private String authorName;

    @SerializedName("author_email")
    private String authorEmail;

    @SerializedName("is_verified")
    private boolean isVerified;

    @SerializedName("title")
    private String title;

    @SerializedName("summary")
    private String summary;

    @SerializedName("content")
    private String content;

    @SerializedName("company")
    private String company;

    @SerializedName("role")
    private String role;

    @SerializedName("achievement_date")
    private String achievementDate;

    @SerializedName("image_url")
    private String imageUrl;

    @SerializedName("status")
    private String status;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    public SuccessStory() {}

    public int getId() { return id; }
    public int getAuthorUserId() { return authorUserId; }
    public Integer getAlumniId() { return alumniId; }
    public String getAuthorName() { return authorName; }
    public String getAuthorEmail() { return authorEmail; }
    public boolean isVerified() { return isVerified; }
    public String getTitle() { return title; }
    public String getSummary() { return summary; }
    public String getContent() { return content; }
    public String getCompany() { return company; }
    public String getRole() { return role; }
    public String getCurrentRole() { return role; }
    public Integer getGraduationYear() { return null; }
    public String getAchievementDate() { return achievementDate; }
    public String getImageUrl() { return imageUrl; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
}
