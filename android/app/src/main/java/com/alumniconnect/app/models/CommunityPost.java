package com.alumniconnect.app.models;

import com.google.gson.annotations.SerializedName;

public class CommunityPost {
    @SerializedName("id")
    private int id;

    @SerializedName("community_id")
    private int communityId;

    @SerializedName("author_id")
    private int authorId;

    @SerializedName("author_name")
    private String authorName;

    @SerializedName("author_role")
    private String authorRole;

    @SerializedName("content")
    private String content;

    @SerializedName("created_at")
    private String createdAt;

    public CommunityPost() {}

    public int getId() { return id; }
    public int getCommunityId() { return communityId; }
    public int getAuthorId() { return authorId; }
    public String getAuthorName() { return authorName; }
    public String getAuthorRole() { return authorRole; }
    public String getContent() { return content; }
    public String getCreatedAt() { return createdAt; }
}
