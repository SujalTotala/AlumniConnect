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
import com.alumniconnect.app.models.Achievement;
import com.alumniconnect.app.models.AchievementCreateRequest;
import com.alumniconnect.app.models.AttendanceCheckInRequest;
import com.alumniconnect.app.models.AttendanceStats;
import com.alumniconnect.app.models.Community;
import com.alumniconnect.app.models.CommunityCreateRequest;
import com.alumniconnect.app.models.CommunityMember;
import com.alumniconnect.app.models.CommunityPost;
import com.alumniconnect.app.models.CommunityPostCreateRequest;
import com.alumniconnect.app.models.Connection;
import com.alumniconnect.app.models.ConnectionStatusResponse;
import com.alumniconnect.app.models.ConnectionSuggestion;
import com.alumniconnect.app.models.EligibleAlumni;
import com.alumniconnect.app.models.EventAttendance;
import com.alumniconnect.app.models.NetworkSummary;
import com.alumniconnect.app.models.QRCodeTokenResponse;
import com.alumniconnect.app.models.ReferralRequest;
import com.alumniconnect.app.models.ReferralRequestCreate;
import com.alumniconnect.app.models.SuccessStory;
import com.alumniconnect.app.models.SuccessStoryCreateRequest;
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

    // ── Connections & Networking ─────────────────────────────────
    @POST("connections/request/{userId}")
    Call<Connection> sendConnectionRequest(@Path("userId") int userId);

    @GET("connections/received")
    Call<List<Connection>> getReceivedConnections();

    @GET("connections/sent")
    Call<List<Connection>> getSentConnections();

    @GET("connections/my-network")
    Call<List<Connection>> getMyNetwork();

    @PUT("connections/{connectionId}/accept")
    Call<Connection> acceptConnection(@Path("connectionId") int connectionId);

    @PUT("connections/{connectionId}/reject")
    Call<Connection> rejectConnection(@Path("connectionId") int connectionId);

    @DELETE("connections/{connectionId}")
    Call<Map<String, Object>> removeConnection(@Path("connectionId") int connectionId);

    @GET("connections/status/{userId}")
    Call<ConnectionStatusResponse> getConnectionStatus(@Path("userId") int userId);

    @GET("connections/suggestions")
    Call<List<ConnectionSuggestion>> getConnectionSuggestions(@Query("limit") Integer limit);

    @GET("connections/network-summary")
    Call<NetworkSummary> getNetworkSummary();

    // ── Referrals ────────────────────────────────────────────────
    @POST("referrals/")
    Call<ReferralRequest> createReferralRequest(@Body ReferralRequestCreate request);

    @GET("referrals/sent")
    Call<List<ReferralRequest>> getSentReferrals();

    @GET("referrals/received")
    Call<List<ReferralRequest>> getReceivedReferrals();

    @GET("referrals/eligible-alumni/{opportunityId}")
    Call<List<EligibleAlumni>> getEligibleAlumniForOpportunity(@Path("opportunityId") int opportunityId);

    @PUT("referrals/{referralId}/accept")
    Call<ReferralRequest> acceptReferral(@Path("referralId") int referralId);

    @PUT("referrals/{referralId}/decline")
    Call<ReferralRequest> declineReferral(@Path("referralId") int referralId);

    @PUT("referrals/{referralId}/complete")
    Call<ReferralRequest> completeReferral(@Path("referralId") int referralId);

    @DELETE("referrals/{referralId}")
    Call<Map<String, Object>> cancelReferral(@Path("referralId") int referralId);

    // ── Event Attendance & QR (Pass 2B) ──────────────────────────
    @POST("events/{event_id}/attendance/qr-token")
    Call<QRCodeTokenResponse> generateEventQrToken(@Path("event_id") int eventId);

    @POST("events/attendance/check-in")
    Call<EventAttendance> checkInAttendee(@Body AttendanceCheckInRequest request);

    @POST("events/{event_id}/attendance/manual")
    Call<EventAttendance> manualCheckIn(@Path("event_id") int eventId, @Body Map<String, Object> request);

    @GET("events/{event_id}/attendance")
    Call<List<EventAttendance>> getEventAttendance(@Path("event_id") int eventId);

    @GET("events/{event_id}/attendance/stats")
    Call<AttendanceStats> getAttendanceStats(@Path("event_id") int eventId);

    // ── Alumni Success Stories (Pass 2B) ─────────────────────────
    @GET("success-stories/")
    Call<List<SuccessStory>> getApprovedSuccessStories();

    @GET("success-stories/mine")
    Call<List<SuccessStory>> getMySuccessStories();

    @GET("success-stories/{id}")
    Call<SuccessStory> getSuccessStoryById(@Path("id") int id);

    @POST("success-stories/")
    Call<SuccessStory> submitSuccessStory(@Body SuccessStoryCreateRequest request);

    @DELETE("success-stories/{id}")
    Call<Map<String, Object>> deleteSuccessStory(@Path("id") int id);

    // ── Communities & Alumni Chapters (Pass 2B) ──────────────────
    @GET("communities/")
    Call<List<Community>> getCommunities(
            @Query("community_type") String communityType,
            @Query("search") String search
    );

    @GET("communities/my")
    Call<List<Community>> getMyCommunities();

    @GET("communities/{id}")
    Call<Community> getCommunityById(@Path("id") int id);

    @POST("communities/")
    Call<Community> createCommunity(@Body CommunityCreateRequest request);

    @POST("communities/{id}/join")
    Call<Map<String, Object>> joinCommunity(@Path("id") int id);

    @DELETE("communities/{id}/leave")
    Call<Map<String, Object>> leaveCommunity(@Path("id") int id);

    @GET("communities/{id}/members")
    Call<List<CommunityMember>> getCommunityMembers(@Path("id") int id);

    @GET("communities/{id}/posts")
    Call<List<CommunityPost>> getCommunityPosts(@Path("id") int id);

    @POST("communities/{id}/posts")
    Call<CommunityPost> createCommunityPost(@Path("id") int id, @Body CommunityPostCreateRequest request);

    // ── Achievements & Recognition (Pass 2B) ─────────────────────
    @GET("achievements/")
    Call<List<Achievement>> getApprovedAchievements(@Query("category") String category);

    @GET("achievements/mine")
    Call<List<Achievement>> getMyAchievements();

    @GET("achievements/alumni/{alumni_id}")
    Call<List<Achievement>> getAlumniAchievements(@Path("alumni_id") int alumniId);

    @GET("achievements/{id}")
    Call<Achievement> getAchievementById(@Path("id") int id);

    @POST("achievements/")
    Call<Achievement> submitAchievement(@Body AchievementCreateRequest request);

    @DELETE("achievements/{id}")
    Call<Map<String, Object>> deleteAchievement(@Path("id") int id);
}
