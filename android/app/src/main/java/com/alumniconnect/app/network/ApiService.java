package com.alumniconnect.app.network;

import com.alumniconnect.app.models.ActivityItem;
import com.alumniconnect.app.models.AdminStatistics;
import com.alumniconnect.app.models.Alumni;
import com.alumniconnect.app.models.Announcement;
import com.alumniconnect.app.models.Bookmark;
import com.alumniconnect.app.models.BookmarkCreateRequest;
import com.alumniconnect.app.models.Event;
import com.alumniconnect.app.models.EventCreateRequest;
import com.alumniconnect.app.models.EventRegistration;
import com.alumniconnect.app.models.LoginRequest;
import com.alumniconnect.app.models.LoginResponse;
import com.alumniconnect.app.models.MentorRecommendation;
import com.alumniconnect.app.models.MentorshipRequest;
import com.alumniconnect.app.models.MentorshipRequestCreate;
import com.alumniconnect.app.models.MentorshipStatusUpdate;
import com.alumniconnect.app.models.Notification;
import com.alumniconnect.app.models.NotificationPreferences;
import com.alumniconnect.app.models.Opportunity;
import com.alumniconnect.app.models.OpportunityCreateRequest;
import com.alumniconnect.app.models.OpportunityUpdateRequest;
import com.alumniconnect.app.models.ProfileCompletion;
import com.alumniconnect.app.models.ProfileResponse;
import com.alumniconnect.app.models.ProfileUpdateRequest;
import com.alumniconnect.app.models.RegisterRequest;
import com.alumniconnect.app.models.UnreadCountResponse;
import com.alumniconnect.app.models.User;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // ── Authentication ──────────────────────────────────────────
    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest loginRequest);

    @POST("auth/register")
    Call<LoginResponse> register(@Body RegisterRequest registerRequest);

    @GET("auth/me")
    Call<User> getMe();

    // ── Profile ─────────────────────────────────────────────────
    @GET("profile/me")
    Call<ProfileResponse> getMyProfile();

    @PUT("profile/me")
    Call<ProfileResponse> updateMyProfile(@Body ProfileUpdateRequest updateRequest);

    @GET("profile/completion-suggestions")
    Call<ProfileCompletion> getProfileCompletion();

    // ── Alumni ──────────────────────────────────────────────────
    @GET("alumni/")
    Call<List<Alumni>> getAlumni(
            @Query("search") String search,
            @Query("department") String department,
            @Query("graduation_year") String graduationYear,
            @Query("company") String company,
            @Query("job_role") String jobRole,
            @Query("location") String location,
            @Query("skills") String skills,
            @Query("mentorship_available") Boolean mentorshipAvailable,
            @Query("is_verified") Boolean isVerified
    );

    @GET("alumni/{alumni_id}")
    Call<Alumni> getAlumniById(@Path("alumni_id") int alumniId);

    @GET("alumni/recommendations/mentors")
    Call<List<MentorRecommendation>> getRecommendedMentors();

    // ── Events ───────────────────────────────────────────────────
    @GET("events/")
    Call<List<Event>> getEvents(
            @Query("event_type") String eventType,
            @Query("search") String search
    );

    @GET("events/{event_id}")
    Call<Event> getEventById(@Path("event_id") int eventId);

    @POST("events/{event_id}/register")
    Call<Map<String, Object>> registerForEvent(@Path("event_id") int eventId);

    @DELETE("events/{event_id}/register")
    Call<Map<String, Object>> cancelEventRegistration(@Path("event_id") int eventId);

    @POST("events/")
    Call<Event> createEvent(@Body EventCreateRequest request);

    @GET("events/{event_id}/registrations")
    Call<List<EventRegistration>> getEventRegistrations(@Path("event_id") int eventId);

    // ── Mentorship ───────────────────────────────────────────────
    @GET("mentorship/mentors")
    Call<List<Alumni>> getMentors(
            @Query("department") String department,
            @Query("company") String company,
            @Query("skills") String skills
    );

    @POST("mentorship/requests")
    Call<MentorshipRequest> sendMentorshipRequest(@Body MentorshipRequestCreate request);

    @GET("mentorship/requests/sent")
    Call<List<MentorshipRequest>> getSentRequests();

    @GET("mentorship/requests/received")
    Call<List<MentorshipRequest>> getReceivedRequests();

    @PUT("mentorship/requests/{id}/accept")
    Call<MentorshipRequest> acceptMentorshipRequest(
            @Path("id") int id,
            @Body MentorshipStatusUpdate request
    );

    @PUT("mentorship/requests/{id}/reject")
    Call<MentorshipRequest> rejectMentorshipRequest(
            @Path("id") int id,
            @Body MentorshipStatusUpdate request
    );

    @PUT("mentorship/requests/{id}/complete")
    Call<MentorshipRequest> completeMentorshipRequest(@Path("id") int id);

    // ── Career Opportunities ─────────────────────────────────────
    @GET("opportunities/")
    Call<List<Opportunity>> getOpportunities(
            @Query("opportunity_type") String opportunityType,
            @Query("search") String search,
            @Query("location") String location
    );

    @GET("opportunities/{opportunity_id}")
    Call<Opportunity> getOpportunityById(@Path("opportunity_id") int id);

    @POST("opportunities/")
    Call<Opportunity> createOpportunity(@Body OpportunityCreateRequest request);

    @PUT("opportunities/{opportunity_id}")
    Call<Opportunity> updateOpportunity(
            @Path("opportunity_id") int id,
            @Body OpportunityUpdateRequest request
    );

    @DELETE("opportunities/{opportunity_id}")
    Call<Map<String, Object>> deleteOpportunity(@Path("opportunity_id") int id);

    // ── Notifications ───────────────────────────────────────────
    @GET("notifications/")
    Call<List<Notification>> getNotifications();

    @GET("notifications/unread-count")
    Call<UnreadCountResponse> getUnreadNotificationCount();

    @PUT("notifications/{notification_id}/read")
    Call<Notification> markNotificationRead(@Path("notification_id") int notificationId);

    @PUT("notifications/read-all")
    Call<Map<String, Object>> markAllNotificationsRead();

    // ── Notification Preferences ─────────────────────────────────
    @GET("notification-preferences/")
    Call<NotificationPreferences> getNotificationPreferences();

    @PUT("notification-preferences/")
    Call<NotificationPreferences> updateNotificationPreferences(@Body NotificationPreferences preferences);

    // ── Bookmarks ────────────────────────────────────────────────
    @GET("bookmarks/")
    Call<List<Bookmark>> getBookmarks(@Query("item_type") String itemType);

    @POST("bookmarks/")
    Call<Bookmark> createBookmark(@Body BookmarkCreateRequest request);

    @GET("bookmarks/check/{item_type}/{item_id}")
    Call<Map<String, Object>> checkBookmark(
            @Path("item_type") String itemType,
            @Path("item_id") int itemId
    );

    @DELETE("bookmarks/{id}")
    Call<Map<String, Object>> deleteBookmark(@Path("id") int id);

    @DELETE("bookmarks/{item_type}/{item_id}")
    Call<Map<String, Object>> deleteBookmarkByItem(
            @Path("item_type") String itemType,
            @Path("item_id") int itemId
    );

    // ── Announcements ────────────────────────────────────────────
    @GET("announcements/")
    Call<List<Announcement>> getAnnouncements(@Query("active_only") Boolean activeOnly);

    // ── Activity Feed ────────────────────────────────────────────
    @GET("activity-feed/")
    Call<List<ActivityItem>> getActivityFeed(@Query("limit") Integer limit);

    // ── Admin ────────────────────────────────────────────────────
    @GET("admin/statistics")
    Call<AdminStatistics> getAdminStatistics();
}
