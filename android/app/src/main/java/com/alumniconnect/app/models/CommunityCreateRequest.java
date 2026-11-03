package com.alumniconnect.app.models;

import com.google.gson.annotations.SerializedName;

public class CommunityCreateRequest {
    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("community_type")
    private String communityType;

    public CommunityCreateRequest(String name, String description, String communityType) {
        this.name = name;
        this.description = description;
        this.communityType = communityType;
    }

    public CommunityCreateRequest(String name, String description, String communityType, String location, Object extra) {
        this(name, description, communityType);
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCommunityType() { return communityType; }
}
