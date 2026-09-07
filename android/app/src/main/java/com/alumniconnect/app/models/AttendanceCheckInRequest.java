package com.alumniconnect.app.models;

import com.google.gson.annotations.SerializedName;

public class AttendanceCheckInRequest {
    @SerializedName("qr_token")
    private String qrToken;

    public AttendanceCheckInRequest(String qrToken) {
        this.qrToken = qrToken;
    }

    public String getQrToken() { return qrToken; }
    public void setQrToken(String qrToken) { this.qrToken = qrToken; }
}
