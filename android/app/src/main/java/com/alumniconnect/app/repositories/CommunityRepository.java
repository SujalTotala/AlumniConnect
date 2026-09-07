package com.alumniconnect.app.repositories;

import android.content.Context;
import com.alumniconnect.app.models.Community;
import com.alumniconnect.app.models.CommunityCreateRequest;
import com.alumniconnect.app.models.CommunityMember;
import com.alumniconnect.app.models.CommunityPost;
import com.alumniconnect.app.models.CommunityPostCreateRequest;
import com.alumniconnect.app.network.ApiClient;
import com.alumniconnect.app.network.ApiService;
import java.util.List;
import java.util.Map;
import retrofit2.Call;

public class CommunityRepository {
    private final ApiService apiService;

    public CommunityRepository(Context context) {
        this.apiService = ApiClient.getApiService(context);
    }

    public Call<List<Community>> getCommunities(String type, String search) {
        return apiService.getCommunities(type, search);
    }

    public Call<List<Community>> getMyCommunities() {
        return apiService.getMyCommunities();
    }

    public Call<Community> getCommunityById(int id) {
        return apiService.getCommunityById(id);
    }

    public Call<Community> createCommunity(CommunityCreateRequest request) {
        return apiService.createCommunity(request);
    }

    public Call<Map<String, Object>> joinCommunity(int id) {
        return apiService.joinCommunity(id);
    }

    public Call<Map<String, Object>> leaveCommunity(int id) {
        return apiService.leaveCommunity(id);
    }

    public Call<List<CommunityMember>> getCommunityMembers(int id) {
        return apiService.getCommunityMembers(id);
    }

    public Call<List<CommunityPost>> getCommunityPosts(int id) {
        return apiService.getCommunityPosts(id);
    }

    public Call<CommunityPost> createCommunityPost(int id, String content) {
        return apiService.createCommunityPost(id, new CommunityPostCreateRequest(content));
    }
}
