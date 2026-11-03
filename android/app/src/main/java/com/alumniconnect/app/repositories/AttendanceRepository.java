package com.alumniconnect.app.repositories;

import android.content.Context;
import com.alumniconnect.app.models.AttendanceCheckInRequest;
import com.alumniconnect.app.models.AttendanceStats;
import com.alumniconnect.app.models.EventAttendance;
import com.alumniconnect.app.models.QRCodeTokenResponse;
import com.alumniconnect.app.network.ApiClient;
import com.alumniconnect.app.network.ApiService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;

public class AttendanceRepository {
    private final ApiService apiService;

    public AttendanceRepository(Context context) {
        this.apiService = ApiClient.getApiService(context);
    }

    public Call<QRCodeTokenResponse> generateEventQrToken(int eventId) {
        return apiService.generateEventQrToken(eventId);
    }

    public Call<QRCodeTokenResponse> getQRCodeToken(int eventId) {
        return generateEventQrToken(eventId);
    }

    public Call<EventAttendance> checkInAttendee(String qrToken) {
        return apiService.checkInAttendee(new AttendanceCheckInRequest(qrToken));
    }

    public Call<EventAttendance> checkInAttendee(Integer eventId, String qrToken) {
        return checkInAttendee(qrToken);
    }

    public Call<EventAttendance> manualCheckIn(int eventId, int userId, String notes) {
        Map<String, Object> body = new HashMap<>();
        body.put("user_id", userId);
        if (notes != null && !notes.trim().isEmpty()) {
            body.put("notes", notes.trim());
        }
        return apiService.manualCheckIn(eventId, body);
    }

    public Call<EventAttendance> manualCheckIn(int eventId, int userId) {
        return manualCheckIn(eventId, userId, null);
    }

    public Call<List<EventAttendance>> getEventAttendance(int eventId) {
        return apiService.getEventAttendance(eventId);
    }

    public Call<List<EventAttendance>> getEventAttendees(int eventId) {
        return getEventAttendance(eventId);
    }

    public Call<AttendanceStats> getAttendanceStats(int eventId) {
        return apiService.getAttendanceStats(eventId);
    }
}
