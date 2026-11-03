package com.alumniconnect.app.repositories;

import android.content.Context;
import com.alumniconnect.app.models.Achievement;
import com.alumniconnect.app.models.AchievementCreateRequest;
import com.alumniconnect.app.network.ApiClient;
import com.alumniconnect.app.network.ApiService;
import java.util.List;
import java.util.Map;
import retrofit2.Call;

public class AchievementRepository {
    private final ApiService apiService;

    public AchievementRepository(Context context) {
        this.apiService = ApiClient.getApiService(context);
    }

    public Call<List<Achievement>> getApprovedAchievements(String category) {
        return apiService.getApprovedAchievements(category);
    }

    public Call<List<Achievement>> getMyAchievements() {
        return apiService.getMyAchievements();
    }

    public Call<List<Achievement>> getAlumniAchievements(int alumniId) {
        return apiService.getAlumniAchievements(alumniId);
    }

    public Call<Achievement> getAchievementById(int id) {
        return apiService.getAchievementById(id);
    }

    public Call<Achievement> submitAchievement(AchievementCreateRequest request) {
        return apiService.submitAchievement(request);
    }

    public Call<Achievement> createAchievement(AchievementCreateRequest request) {
        return submitAchievement(request);
    }

    public Call<Map<String, Object>> deleteAchievement(int id) {
        return apiService.deleteAchievement(id);
    }
}
