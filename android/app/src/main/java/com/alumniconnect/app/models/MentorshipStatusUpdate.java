package com.alumniconnect.app.models;

import com.google.gson.annotations.SerializedName;

/**
 * Matches backend MentorshipStatusUpdate schema exactly.
 */
public class MentorshipStatusUpdate {
    @SerializedName("response_note")
    private String responseNote;

    public MentorshipStatusUpdate(String responseNote) {
        this.responseNote = responseNote;
    }

    public String getResponseNote() { return responseNote; }
}
