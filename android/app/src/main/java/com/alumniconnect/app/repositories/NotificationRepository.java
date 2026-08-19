package com.alumniconnect.app.repositories;

import android.content.Context;
import com.alumniconnect.app.models.Notification;
import com.alumniconnect.app.models.UnreadCountResponse;
import com.alumniconnect.app.network.ApiClient;
import com.alumniconnect.app.network.ApiService;
import java.util.List;
import java.util.Map;
import retrofit2.Call;

public class NotificationRepository {
    private final ApiService apiService;

    public NotificationRepository(Context context) {
        this.apiService = ApiClient.getApiService(context);
    }

    /** GET /notifications/ */
    public Call<List<Notification>> getNotifications() {
        return apiService.getNotifications();
    }

    /** GET /notifications/unread-count */
    public Call<UnreadCountResponse> getUnreadNotificationCount() {
        return apiService.getUnreadNotificationCount();
    }

    /** PUT /notifications/{id}/read */
    public Call<Notification> markNotificationRead(int id) {
        return apiService.markNotificationRead(id);
    }

    /** PUT /notifications/read-all */
    public Call<Map<String, Object>> markAllNotificationsRead() {
        return apiService.markAllNotificationsRead();
    }
}
