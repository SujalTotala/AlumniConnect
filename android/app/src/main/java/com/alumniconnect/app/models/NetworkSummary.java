package com.alumniconnect.app.models;

import com.google.gson.annotations.SerializedName;

public class NetworkSummary {
    @SerializedName("total_connections")
    private int totalConnections;

    @SerializedName("pending_requests_received")
    private int pendingRequestsReceived;

    @SerializedName("pending_requests_sent")
    private int pendingRequestsSent;

    @SerializedName("total_referrals_sent")
    private int totalReferralsSent;

    @SerializedName("total_referrals_received")
    private int totalReferralsReceived;

    public NetworkSummary() {}

    public int getTotalConnections() { return totalConnections; }
    public int getPendingRequestsReceived() { return pendingRequestsReceived; }
    public int getPendingRequestsSent() { return pendingRequestsSent; }
    public int getTotalReferralsSent() { return totalReferralsSent; }
    public int getTotalReferralsReceived() { return totalReferralsReceived; }
}
