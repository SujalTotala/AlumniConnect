package com.alumniconnect.app.models;

import com.google.gson.annotations.SerializedName;

public class CommunityMember {
    @SerializedName("id")
    private int id;

    @SerializedName("community_id")
    private int communityId;

    @SerializedName("user_id")
    private int userId;

    @SerializedName("user_name")
    private String userName;

    @SerializedName("user_email")
    private String userEmail;

    @SerializedName("user_role")
    private String userRole;

    @SerializedName("member_role")
    private String memberRole;

    @SerializedName("joined_at")
    private String joinedAt;

    public CommunityMember() {}

    public int getId() { return id; }
    public int getCommunityId() { return communityId; }
    public int getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getUserEmail() { return userEmail; }
    public String getUserRole() { return userRole; }
    public String getMemberRole() { return memberRole; }
    public String getJoinedAt() { return joinedAt; }
}
