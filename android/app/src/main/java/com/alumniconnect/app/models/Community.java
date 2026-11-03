package com.alumniconnect.app.models;

import com.google.gson.annotations.SerializedName;

public class Community {
    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("community_type")
    private String communityType;

    @SerializedName("created_by")
    private Integer createdBy;

    @SerializedName("creator_name")
    private String creatorName;

    @SerializedName("members_count")
    private int membersCount;

    @SerializedName("is_member")
    private boolean isMember;

    @SerializedName("my_role")
    private String myRole;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("is_active")
    private boolean isActive;

    public Community() {}

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCommunityType() { return communityType; }
    public String getType() { return communityType; }
    public Integer getCreatedBy() { return createdBy; }
    public String getCreatorName() { return creatorName; }
    public int getMembersCount() { return membersCount; }
    public Integer getMemberCount() { return membersCount; }
    public String getLocation() { return ""; }
    public boolean isMember() { return isMember; }
    public void setMember(boolean member) { isMember = member; }
    public String getMyRole() { return myRole; }
    public String getCreatedAt() { return createdAt; }
    public boolean isActive() { return isActive; }
}
