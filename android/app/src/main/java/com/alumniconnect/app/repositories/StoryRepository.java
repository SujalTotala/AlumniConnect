package com.alumniconnect.app.repositories;

import android.content.Context;
import com.alumniconnect.app.models.SuccessStory;
import com.alumniconnect.app.models.SuccessStoryCreateRequest;
import com.alumniconnect.app.network.ApiClient;
import com.alumniconnect.app.network.ApiService;
import java.util.List;
import java.util.Map;
import retrofit2.Call;

public class StoryRepository {
    private final ApiService apiService;

    public StoryRepository(Context context) {
        this.apiService = ApiClient.getApiService(context);
    }

    public Call<List<SuccessStory>> getApprovedSuccessStories() {
        return apiService.getApprovedSuccessStories();
    }

    public Call<List<SuccessStory>> getApprovedStories() {
        return getApprovedSuccessStories();
    }

    public Call<List<SuccessStory>> getMySuccessStories() {
        return apiService.getMySuccessStories();
    }

    public Call<SuccessStory> getSuccessStoryById(int id) {
        return apiService.getSuccessStoryById(id);
    }

    public Call<SuccessStory> submitSuccessStory(SuccessStoryCreateRequest request) {
        return apiService.submitSuccessStory(request);
    }

    public Call<SuccessStory> submitStory(SuccessStoryCreateRequest request) {
        return submitSuccessStory(request);
    }

    public Call<Map<String, Object>> deleteSuccessStory(int id) {
        return apiService.deleteSuccessStory(id);
    }
}
