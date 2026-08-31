package com.alumniconnect.app.repositories;

import android.content.Context;
import com.alumniconnect.app.models.Alumni;
import com.alumniconnect.app.models.MentorshipRequest;
import com.alumniconnect.app.models.MentorshipRequestCreate;
import com.alumniconnect.app.models.MentorshipStatusUpdate;
import com.alumniconnect.app.network.ApiClient;
import com.alumniconnect.app.network.ApiService;
import java.util.List;
import retrofit2.Call;

public class MentorshipRepository {
    private final ApiService apiService;

    public MentorshipRepository(Context context) {
        this.apiService = ApiClient.getApiService(context);
    }

    /** GET /mentorship/mentors */
    public Call<List<Alumni>> getMentors(String department, String company, String skills) {
        return apiService.getMentors(department, company, skills);
    }

    /** POST /mentorship/requests */
    public Call<MentorshipRequest> sendMentorshipRequest(MentorshipRequestCreate request) {
        return apiService.sendMentorshipRequest(request);
    }

    /** GET /mentorship/requests/sent */
    public Call<List<MentorshipRequest>> getSentRequests() {
        return apiService.getSentRequests();
    }

    /** GET /mentorship/requests/received */
    public Call<List<MentorshipRequest>> getReceivedRequests() {
        return apiService.getReceivedRequests();
    }

    /** PUT /mentorship/requests/{id}/accept */
    public Call<MentorshipRequest> acceptMentorshipRequest(int requestId, String responseNote) {
        return apiService.acceptMentorshipRequest(requestId, new MentorshipStatusUpdate(responseNote));
    }

    /** PUT /mentorship/requests/{id}/reject */
    public Call<MentorshipRequest> rejectMentorshipRequest(int requestId, String responseNote) {
        return apiService.rejectMentorshipRequest(requestId, new MentorshipStatusUpdate(responseNote));
    }

    /** PUT /mentorship/requests/{id}/complete */
    public Call<MentorshipRequest> completeMentorshipRequest(int requestId) {
        return apiService.completeMentorshipRequest(requestId);
    }
}
