package com.alumniconnect.app.repositories;

import android.content.Context;
import com.alumniconnect.app.models.Alumni;
import com.alumniconnect.app.network.ApiClient;
import com.alumniconnect.app.network.ApiService;
import java.util.List;
import retrofit2.Call;

public class AlumniRepository {
    private final ApiService apiService;

    public AlumniRepository(Context context) {
        this.apiService = ApiClient.getApiService(context);
    }

    /**
     * Fetch alumni list with optional filters.
     * Pass null for any parameter to omit it from the request.
     */
    public Call<List<Alumni>> getAlumni(
            String search,
            String department,
            String graduationYear,
            String company,
            String location,
            Boolean mentorshipAvailable
    ) {
        return apiService.getAlumni(search, department, graduationYear, company, location, mentorshipAvailable);
    }

    /** Fetch all alumni with no filters */
    public Call<List<Alumni>> getAllAlumni() {
        return apiService.getAlumni(null, null, null, null, null, null);
    }

    /** Fetch a single alumni by their database ID */
    public Call<Alumni> getAlumniById(int alumniId) {
        return apiService.getAlumniById(alumniId);
    }
}
