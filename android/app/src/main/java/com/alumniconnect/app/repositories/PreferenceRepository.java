package com.alumniconnect.app.repositories;

import android.content.Context;
import com.alumniconnect.app.models.NotificationPreferences;
import com.alumniconnect.app.models.ProfileCompletion;
import com.alumniconnect.app.network.ApiClient;
import com.alumniconnect.app.network.ApiService;
import retrofit2.Call;

public class PreferenceRepository {
    private final ApiService apiService;

    public PreferenceRepository(Context context) {
        this.apiService = ApiClient.getApiService(context);
    }

    public Call<NotificationPreferences> getPreferences() {
        return apiService.getNotificationPreferences();
    }

    public Call<NotificationPreferences> updatePreferences(NotificationPreferences preferences) {
        return apiService.updateNotificationPreferences(preferences);
    }

    public Call<ProfileCompletion> getProfileCompletion() {
        return apiService.getProfileCompletion();
    }
}
