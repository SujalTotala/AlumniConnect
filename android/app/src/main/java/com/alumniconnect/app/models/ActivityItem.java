package com.alumniconnect.app.models;

import com.google.gson.annotations.SerializedName;

public class ActivityItem {
    @SerializedName("id")
    private String id;

    @SerializedName("type")
    private String type;

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("timestamp")
    private String timestamp;

    @SerializedName("icon")
    private String icon;

    public ActivityItem() {}

    public String getId() { return id != null ? id : ""; }
    public String getType() { return type != null ? type : ""; }
    public String getTitle() { return title != null ? title : ""; }
    public String getDescription() { return description != null ? description : ""; }
    public String getTimestamp() { return timestamp != null ? timestamp : ""; }
    public String getIcon() { return icon != null ? icon : "📌"; }
}
