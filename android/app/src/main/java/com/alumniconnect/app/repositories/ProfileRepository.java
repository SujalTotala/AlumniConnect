package com.alumniconnect.app.repositories;

import android.content.Context;
import com.alumniconnect.app.models.ProfileResponse;
import com.alumniconnect.app.models.ProfileUpdateRequest;
import com.alumniconnect.app.network.ApiClient;
import com.alumniconnect.app.network.ApiService;
import retrofit2.Call;

public class ProfileRepository {
    private final ApiService apiService;

    public ProfileRepository(Context context) {
        this.apiService = ApiClient.getApiService(context);
    }

    public Call<ProfileResponse> getMyProfile() {
        return apiService.getMyProfile();
    }

    public Call<ProfileResponse> updateMyProfile(ProfileUpdateRequest request) {
        return apiService.updateMyProfile(request);
    }
}
