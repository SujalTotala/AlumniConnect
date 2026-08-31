package com.alumniconnect.app.models;

import com.google.gson.annotations.SerializedName;

/**
 * Request body for POST /events/
 * Matches backend EventCreate schema exactly.
 */
public class EventCreateRequest {
    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("event_type")
    private String eventType;

    @SerializedName("event_date")
    private String eventDate;

    @SerializedName("start_time")
    private String startTime;

    @SerializedName("location")
    private String location;

    @SerializedName("meeting_url")
    private String meetingUrl;

    @SerializedName("image_url")
    private String imageUrl;

    public EventCreateRequest() {}

    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public void setEventDate(String eventDate) { this.eventDate = eventDate; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public void setLocation(String location) { this.location = location; }
    public void setMeetingUrl(String meetingUrl) { this.meetingUrl = meetingUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
