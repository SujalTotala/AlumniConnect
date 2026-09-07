package com.alumniconnect.app.models;

import com.google.gson.annotations.SerializedName;

public class QRCodeTokenResponse {
    @SerializedName("event_id")
    private int eventId;

    @SerializedName("event_title")
    private String eventTitle;

    @SerializedName("qr_token")
    private String qrToken;

    @SerializedName("expires_at")
    private String expiresAt;

    public QRCodeTokenResponse() {}

    public int getEventId() { return eventId; }
    public String getEventTitle() { return eventTitle; }
    public String getQrToken() { return qrToken; }
    public String getToken() { return qrToken; }
    public String getExpiresAt() { return expiresAt; }
}
