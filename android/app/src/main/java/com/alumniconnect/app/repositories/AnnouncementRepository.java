package com.alumniconnect.app.repositories;

import android.content.Context;
import com.alumniconnect.app.models.ActivityItem;
import com.alumniconnect.app.models.Announcement;
import com.alumniconnect.app.network.ApiClient;
import com.alumniconnect.app.network.ApiService;
import java.util.List;
import retrofit2.Call;

public class AnnouncementRepository {
    private final ApiService apiService;

    public AnnouncementRepository(Context context) {
        this.apiService = ApiClient.getApiService(context);
    }

    public Call<List<Announcement>> getActiveAnnouncements() {
        return apiService.getAnnouncements(true);
    }

    public Call<List<ActivityItem>> getActivityFeed(int limit) {
        return apiService.getActivityFeed(limit);
    }
}
