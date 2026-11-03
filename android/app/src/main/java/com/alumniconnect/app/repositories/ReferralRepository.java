package com.alumniconnect.app.repositories;

import android.content.Context;
import com.alumniconnect.app.models.EligibleAlumni;
import com.alumniconnect.app.models.ReferralRequest;
import com.alumniconnect.app.models.ReferralRequestCreate;
import com.alumniconnect.app.network.ApiClient;
import com.alumniconnect.app.network.ApiService;
import java.util.List;
import java.util.Map;
import retrofit2.Call;

public class ReferralRepository {
    private final ApiService apiService;

    public ReferralRepository(Context context) {
        this.apiService = ApiClient.getApiService(context);
    }

    public Call<ReferralRequest> createReferralRequest(int opportunityId, int alumniId, String message) {
        return apiService.createReferralRequest(new ReferralRequestCreate(opportunityId, alumniId, message));
    }

    public Call<ReferralRequest> createReferralRequest(ReferralRequestCreate request) {
        return apiService.createReferralRequest(request);
    }

    public Call<List<ReferralRequest>> getSentReferrals() {
        return apiService.getSentReferrals();
    }

    public Call<List<ReferralRequest>> getReceivedReferrals() {
        return apiService.getReceivedReferrals();
    }

    public Call<List<EligibleAlumni>> getEligibleAlumniForOpportunity(int opportunityId) {
        return apiService.getEligibleAlumniForOpportunity(opportunityId);
    }

    public Call<ReferralRequest> acceptReferral(int referralId) {
        return apiService.acceptReferral(referralId);
    }

    public Call<ReferralRequest> declineReferral(int referralId) {
        return apiService.declineReferral(referralId);
    }

    public Call<ReferralRequest> completeReferral(int referralId) {
        return apiService.completeReferral(referralId);
    }

    public Call<Map<String, Object>> cancelReferral(int referralId) {
        return apiService.cancelReferral(referralId);
    }
}
