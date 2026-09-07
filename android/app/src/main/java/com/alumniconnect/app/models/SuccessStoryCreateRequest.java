package com.alumniconnect.app.models;

import com.google.gson.annotations.SerializedName;

public class SuccessStoryCreateRequest {
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

    public SuccessStoryCreateRequest(String title, String summary, String content, String company, String role, String achievementDate, String imageUrl) {
        this.title = title;
        this.summary = summary;
        this.content = content;
        this.company = company;
        this.role = role;
        this.achievementDate = achievementDate;
        this.imageUrl = imageUrl;
    }

    public SuccessStoryCreateRequest(String title, String content, String summary, String company, String role, Integer gradYear, String imageUrl) {
        this(title, summary, content, company, role, gradYear != null ? String.valueOf(gradYear) : null, imageUrl);
    }

    public String getTitle() { return title; }
    public String getSummary() { return summary; }
    public String getContent() { return content; }
    public String getCompany() { return company; }
    public String getRole() { return role; }
    public String getAchievementDate() { return achievementDate; }
    public String getImageUrl() { return imageUrl; }
}
