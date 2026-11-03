package com.alumniconnect.app.models;

import com.google.gson.annotations.SerializedName;

public class Connection {
    @SerializedName("id")
    private int id;

    @SerializedName("sender_id")
    private int senderId;

    @SerializedName("receiver_id")
    private int receiverId;

    @SerializedName("status")
    private String status;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    @SerializedName("sender")
    private ConnectionUserSummary sender;

    @SerializedName("receiver")
    private ConnectionUserSummary receiver;

    public Connection() {}

    public int getId() { return id; }
    public int getSenderId() { return senderId; }
    public int getReceiverId() { return receiverId; }
    public String getStatus() { return status != null ? status : "PENDING"; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public ConnectionUserSummary getSender() { return sender; }
    public ConnectionUserSummary getReceiver() { return receiver; }

    public ConnectionUserSummary getOtherUser() {
        return receiver != null ? receiver : sender;
    }

    public ConnectionUserSummary getPartnerUser(int currentUserId) {
        if (senderId == currentUserId) {
            return receiver;
        }
        return sender;
    }

    public boolean isPending() { return "PENDING".equalsIgnoreCase(getStatus()); }
    public boolean isAccepted() { return "ACCEPTED".equalsIgnoreCase(getStatus()); }
    public boolean isRejected() { return "REJECTED".equalsIgnoreCase(getStatus()); }
}
