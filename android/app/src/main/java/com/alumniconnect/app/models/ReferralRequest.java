package com.alumniconnect.app.models;

import com.google.gson.annotations.SerializedName;

public class ReferralRequest {
    @SerializedName("id")
    private int id;

    @SerializedName("opportunity_id")
    private int opportunityId;

    @SerializedName("requester_id")
    private int requesterId;

    @SerializedName("alumni_id")
    private int alumniId;

    @SerializedName("message")
    private String message;

    @SerializedName("status")
    private String status; // PENDING, ACCEPTED, DECLINED, COMPLETED

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    @SerializedName("opportunity")
    private ReferralOpportunitySummary opportunity;

    @SerializedName("requester")
    private ConnectionUserSummary requester;

    @SerializedName("alumni")
    private ConnectionUserSummary alumni;

    public ReferralRequest() {}

    public int getId() { return id; }
    public int getOpportunityId() { return opportunityId; }
    public int getRequesterId() { return requesterId; }
    public int getAlumniId() { return alumniId; }
    public String getMessage() { return message != null ? message : ""; }
    public String getStatus() { return status != null ? status : "PENDING"; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public ReferralOpportunitySummary getOpportunity() { return opportunity; }
    public ConnectionUserSummary getRequester() { return requester; }
    public ConnectionUserSummary getAlumni() { return alumni; }

    public boolean isPending() { return "PENDING".equalsIgnoreCase(getStatus()); }
    public boolean isAccepted() { return "ACCEPTED".equalsIgnoreCase(getStatus()); }
    public boolean isDeclined() { return "DECLINED".equalsIgnoreCase(getStatus()); }
    public boolean isCompleted() { return "COMPLETED".equalsIgnoreCase(getStatus()); }
}
