package com.alumniconnect.app.models;

import com.google.gson.annotations.SerializedName;

public class ConnectionStatusResponse {
    @SerializedName("user_id")
    private int userId;

    @SerializedName("status")
    private String status; // "CONNECTED", "PENDING_SENT", "PENDING_RECEIVED", "NOT_CONNECTED", "SELF"

    @SerializedName("connection_id")
    private Integer connectionId;

    public ConnectionStatusResponse() {}

    public int getUserId() { return userId; }
    public String getStatus() { return status != null ? status : "NOT_CONNECTED"; }
    public Integer getConnectionId() { return connectionId; }

    public boolean isConnected() { return "CONNECTED".equalsIgnoreCase(status); }
    public boolean isPendingSent() { return "PENDING_SENT".equalsIgnoreCase(status); }
    public boolean isPendingReceived() { return "PENDING_RECEIVED".equalsIgnoreCase(status); }
    public boolean isSelf() { return "SELF".equalsIgnoreCase(status); }
}
