package com.alumniconnect.app.repositories;

import android.content.Context;
import com.alumniconnect.app.models.Connection;
import com.alumniconnect.app.models.ConnectionStatusResponse;
import com.alumniconnect.app.models.ConnectionSuggestion;
import com.alumniconnect.app.models.NetworkSummary;
import com.alumniconnect.app.network.ApiClient;
import com.alumniconnect.app.network.ApiService;
import java.util.List;
import java.util.Map;
import retrofit2.Call;

public class ConnectionRepository {
    private final ApiService apiService;

    public ConnectionRepository(Context context) {
        this.apiService = ApiClient.getApiService(context);
    }

    public Call<Connection> sendConnectionRequest(int userId) {
        return apiService.sendConnectionRequest(userId);
    }

    public Call<List<Connection>> getReceivedConnections() {
        return apiService.getReceivedConnections();
    }

    public Call<List<Connection>> getReceivedRequests() {
        return getReceivedConnections();
    }

    public Call<List<Connection>> getSentConnections() {
        return apiService.getSentConnections();
    }

    public Call<List<Connection>> getSentRequests() {
        return getSentConnections();
    }

    public Call<List<Connection>> getMyNetwork() {
        return apiService.getMyNetwork();
    }

    public Call<Connection> acceptConnection(int connectionId) {
        return apiService.acceptConnection(connectionId);
    }

    public Call<Connection> rejectConnection(int connectionId) {
        return apiService.rejectConnection(connectionId);
    }

    public Call<Map<String, Object>> removeConnection(int connectionId) {
        return apiService.removeConnection(connectionId);
    }

    public Call<ConnectionStatusResponse> getConnectionStatus(int userId) {
        return apiService.getConnectionStatus(userId);
    }

    public Call<List<ConnectionSuggestion>> getConnectionSuggestions() {
        return getConnectionSuggestions(null);
    }

    public Call<List<ConnectionSuggestion>> getConnectionSuggestions(Integer limit) {
        return apiService.getConnectionSuggestions(limit);
    }

    public Call<NetworkSummary> getNetworkSummary() {
        return apiService.getNetworkSummary();
    }
}
