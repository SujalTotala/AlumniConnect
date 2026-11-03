package com.alumniconnect.app.models;

import com.google.gson.annotations.SerializedName;

public class CommunityPostCreateRequest {
    @SerializedName("content")
    private String content;

    public CommunityPostCreateRequest(String content) {
        this.content = content;
    }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
