package com.alumniconnect.app.models;

import com.google.gson.annotations.SerializedName;

/**
 * Matches backend MentorshipRequestResponse schema exactly.
 */
public class MentorshipRequest {
    @SerializedName("id")
    private int id;

    @SerializedName("student_id")
    private int studentId;

    @SerializedName("mentor_id")
    private int mentorId;

    @SerializedName("message")
    private String message;

    @SerializedName("status")
    private String status;           // PENDING, ACCEPTED, REJECTED, COMPLETED

    @SerializedName("response_note")
    private String responseNote;     // nullable

    @SerializedName("created_at")
    private String createdAt;        // nullable ISO datetime

    @SerializedName("student_name")
    private String studentName;

    @SerializedName("student_email")
    private String studentEmail;

    @SerializedName("mentor_name")
    private String mentorName;

    @SerializedName("mentor_email")
    private String mentorEmail;

    public MentorshipRequest() {}

    // Getters
    public int getId() { return id; }
    public int getStudentId() { return studentId; }
    public int getMentorId() { return mentorId; }
    public String getMessage() { return message != null ? message : ""; }
    public String getStatus() { return status != null ? status : "PENDING"; }
    public String getResponseNote() { return responseNote; }
    public String getCreatedAt() { return createdAt; }
    public String getStudentName() { return studentName != null ? studentName : "Unknown Student"; }
    public String getStudentEmail() { return studentEmail != null ? studentEmail : ""; }
    public String getMentorName() { return mentorName != null ? mentorName : "Unknown Mentor"; }
    public String getMentorEmail() { return mentorEmail != null ? mentorEmail : ""; }

    // Setters (for updating local list item states if needed)
    public void setStatus(String status) { this.status = status; }
    public void setResponseNote(String responseNote) { this.responseNote = responseNote; }

    /** Helper for checking status */
    public boolean isPending() { return "PENDING".equalsIgnoreCase(getStatus()); }
    public boolean isAccepted() { return "ACCEPTED".equalsIgnoreCase(getStatus()); }
    public boolean isRejected() { return "REJECTED".equalsIgnoreCase(getStatus()); }
    public boolean isCompleted() { return "COMPLETED".equalsIgnoreCase(getStatus()); }

    /** Returns formatted status for UI badge */
    public String getStatusText() {
        return getStatus().toUpperCase();
    }
}
