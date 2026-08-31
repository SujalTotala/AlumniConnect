package com.alumniconnect.app.models;

import com.google.gson.annotations.SerializedName;

/**
 * Matches backend MentorshipRequestCreate schema exactly.
 */
public class MentorshipRequestCreate {
    @SerializedName("mentor_id")
    private int mentorId;

    @SerializedName("message")
    private String message;

    public MentorshipRequestCreate(int mentorId, String message) {
        this.mentorId = mentorId;
        this.message = message;
    }

    public int getMentorId() { return mentorId; }
    public String getMessage() { return message; }
}
