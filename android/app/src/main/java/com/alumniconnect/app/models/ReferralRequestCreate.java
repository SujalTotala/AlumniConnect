package com.alumniconnect.app.models;

import com.google.gson.annotations.SerializedName;

public class ReferralRequestCreate {
    @SerializedName("opportunity_id")
    private int opportunityId;

    @SerializedName("alumni_id")
    private int alumniId;

    @SerializedName("message")
    private String message;

    public ReferralRequestCreate(int opportunityId, int alumniId, String message) {
        this.opportunityId = opportunityId;
        this.alumniId = alumniId;
        this.message = message;
    }

    public int getOpportunityId() { return opportunityId; }
    public int getAlumniId() { return alumniId; }
    public String getMessage() { return message; }
}
