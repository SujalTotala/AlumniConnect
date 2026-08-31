package com.alumniconnect.app.repositories;

import android.content.Context;
import com.alumniconnect.app.models.Opportunity;
import com.alumniconnect.app.models.OpportunityCreateRequest;
import com.alumniconnect.app.models.OpportunityUpdateRequest;
import com.alumniconnect.app.network.ApiClient;
import com.alumniconnect.app.network.ApiService;
import java.util.List;
import java.util.Map;
import retrofit2.Call;

public class OpportunityRepository {
    private final ApiService apiService;

    public OpportunityRepository(Context context) {
        this.apiService = ApiClient.getApiService(context);
    }

    /** GET /opportunities/ with optional filters */
    public Call<List<Opportunity>> getOpportunities(String opportunityType, String search, String location) {
        return apiService.getOpportunities(opportunityType, search, location);
    }

    /** GET /opportunities/{opportunity_id} */
    public Call<Opportunity> getOpportunityById(int id) {
        return apiService.getOpportunityById(id);
    }

    /** POST /opportunities/ */
    public Call<Opportunity> createOpportunity(OpportunityCreateRequest request) {
        return apiService.createOpportunity(request);
    }

    /** PUT /opportunities/{opportunity_id} */
    public Call<Opportunity> updateOpportunity(int id, OpportunityUpdateRequest request) {
        return apiService.updateOpportunity(id, request);
    }

    /** DELETE /opportunities/{opportunity_id} */
    public Call<Map<String, Object>> deleteOpportunity(int id) {
        return apiService.deleteOpportunity(id);
    }
}
