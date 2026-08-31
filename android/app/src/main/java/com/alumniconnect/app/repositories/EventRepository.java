package com.alumniconnect.app.repositories;

import android.content.Context;
import com.alumniconnect.app.models.Event;
import com.alumniconnect.app.models.EventCreateRequest;
import com.alumniconnect.app.models.EventRegistration;
import com.alumniconnect.app.network.ApiClient;
import com.alumniconnect.app.network.ApiService;
import java.util.List;
import java.util.Map;
import retrofit2.Call;

public class EventRepository {
    private final ApiService apiService;

    public EventRepository(Context context) {
        this.apiService = ApiClient.getApiService(context);
    }

    /**
     * Fetch event list. Pass null for any param to omit it.
     * Backend JWT (via AuthInterceptor) automatically computes is_registered.
     */
    public Call<List<Event>> getEvents(String eventType, String search) {
        return apiService.getEvents(eventType, search);
    }

    /** Fetch all events with no filters */
    public Call<List<Event>> getAllEvents() {
        return apiService.getEvents(null, null);
    }

    /** Fetch a single event by ID */
    public Call<Event> getEventById(int eventId) {
        return apiService.getEventById(eventId);
    }

    /** Register authenticated user for event (POST /events/{id}/register) */
    public Call<Map<String, Object>> registerForEvent(int eventId) {
        return apiService.registerForEvent(eventId);
    }

    /** Cancel registration (DELETE /events/{id}/register) */
    public Call<Map<String, Object>> cancelEventRegistration(int eventId) {
        return apiService.cancelEventRegistration(eventId);
    }

    /** Create a new event (admin/alumni only) */
    public Call<Event> createEvent(EventCreateRequest request) {
        return apiService.createEvent(request);
    }

    /** Get event registrations (admin/creator only) */
    public Call<List<EventRegistration>> getEventRegistrations(int eventId) {
        return apiService.getEventRegistrations(eventId);
    }
}
