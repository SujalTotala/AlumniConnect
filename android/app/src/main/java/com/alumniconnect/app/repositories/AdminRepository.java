package com.alumniconnect.app.repositories;

import android.content.Context;
import com.alumniconnect.app.models.AdminStatistics;
import com.alumniconnect.app.network.ApiClient;
import com.alumniconnect.app.network.ApiService;
import retrofit2.Call;

public class AdminRepository {
    private final ApiService apiService;

    public AdminRepository(Context context) {
        this.apiService = ApiClient.getApiService(context);
    }

    public Call<AdminStatistics> getStatistics() {
        return apiService.getAdminStatistics();
    }
}
