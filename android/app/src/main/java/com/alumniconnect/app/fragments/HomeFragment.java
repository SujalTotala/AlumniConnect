package com.alumniconnect.app.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.alumniconnect.app.R;
import com.alumniconnect.app.activities.MainActivity;
import com.alumniconnect.app.models.AdminStatistics;
import com.alumniconnect.app.models.Event;
import com.alumniconnect.app.models.ProfileResponse;
import com.alumniconnect.app.repositories.AdminRepository;
import com.alumniconnect.app.repositories.EventRepository;
import com.alumniconnect.app.repositories.ProfileRepository;
import com.alumniconnect.app.utils.ApiErrorUtils;
import com.alumniconnect.app.utils.SessionManager;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {
    private SessionManager sessionManager;
    private AdminRepository adminRepository;
    private ProfileRepository profileRepository;
    private EventRepository eventRepository;
    private com.alumniconnect.app.repositories.AnnouncementRepository announcementRepository;

    private SwipeRefreshLayout swipeRefresh;
    private TextView tvWelcome, tvUserEmail, tvRoleBadge, tvHomeStatus;
    private View cardAnnouncement;
    private TextView tvAnnouncementTitle, tvAnnouncementContent;
    private LinearLayout layoutAdminStats, layoutQuickActions;
    private TextView tvTotalUsers, tvTotalAlumni, tvTotalEvents, tvActiveMentors,
            tvTotalOpportunities, tvPendingMentorship;
    private View cardAlumni, cardMentorship, cardEvents, cardProfile, cardOpportunities;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());
        adminRepository = new AdminRepository(requireContext());
        profileRepository = new ProfileRepository(requireContext());
        eventRepository = new EventRepository(requireContext());
        announcementRepository = new com.alumniconnect.app.repositories.AnnouncementRepository(requireContext());

        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        tvWelcome = view.findViewById(R.id.tv_welcome);
        tvUserEmail = view.findViewById(R.id.tv_user_email);
        tvRoleBadge = view.findViewById(R.id.tv_role_badge);
        tvHomeStatus = view.findViewById(R.id.tv_home_status);
        cardAnnouncement = view.findViewById(R.id.card_announcement);
        tvAnnouncementTitle = view.findViewById(R.id.tv_announcement_title);
        tvAnnouncementContent = view.findViewById(R.id.tv_announcement_content);
        layoutAdminStats = view.findViewById(R.id.layout_admin_stats);
        layoutQuickActions = view.findViewById(R.id.layout_quick_actions);
        tvTotalUsers = view.findViewById(R.id.tv_total_users);
        tvTotalAlumni = view.findViewById(R.id.tv_total_alumni);
        tvTotalEvents = view.findViewById(R.id.tv_total_events);
        tvActiveMentors = view.findViewById(R.id.tv_active_mentors);
        tvTotalOpportunities = view.findViewById(R.id.tv_total_opportunities);
        tvPendingMentorship = view.findViewById(R.id.tv_pending_mentorship);
        cardAlumni = view.findViewById(R.id.card_alumni);
        cardMentorship = view.findViewById(R.id.card_mentorship);
        cardEvents = view.findViewById(R.id.card_events);
        cardProfile = view.findViewById(R.id.card_profile);
        cardOpportunities = view.findViewById(R.id.card_opportunities);

        // Populate session-based user info
        populateUserInfo();

        // Navigation card clicks -> navigate to BottomNav tabs
        cardAlumni.setOnClickListener(v -> navigateToTab(R.id.nav_alumni));
        cardMentorship.setOnClickListener(v -> navigateToTab(R.id.nav_mentorship));
        cardEvents.setOnClickListener(v -> navigateToTab(R.id.nav_events));
        cardProfile.setOnClickListener(v -> navigateToTab(R.id.nav_profile));
        if (cardOpportunities != null) {
            cardOpportunities.setOnClickListener(v -> loadOpportunitiesFragment());
        }

        View cardAdminOpportunities = view.findViewById(R.id.card_admin_opportunities);
        if (cardAdminOpportunities != null) {
            cardAdminOpportunities.setOnClickListener(v -> loadOpportunitiesFragment());
        }

        // SwipeRefreshLayout works for ALL users
        swipeRefresh.setColorSchemeResources(R.color.primary);
        swipeRefresh.setOnRefreshListener(() -> refreshDashboardData());

        // Load role-specific content
        loadDashboard();
    }

    @Override
    public void onResume() {
        super.onResume();
        populateUserInfo();
    }

    private void populateUserInfo() {
        String name = sessionManager.getUserName();
        String email = sessionManager.getUserEmail();
        String role = sessionManager.getUserRole();

        tvWelcome.setText("Welcome, " + (name.isEmpty() ? "Member" : name) + "!");
        tvUserEmail.setText(email);
        tvRoleBadge.setText(role.toUpperCase());
    }

    private void loadDashboard() {
        fetchAnnouncements();
        String role = sessionManager.getUserRole().toLowerCase();
        if ("admin".equals(role)) {
            layoutAdminStats.setVisibility(View.VISIBLE);
            fetchAdminStats();
        } else {
            layoutQuickActions.setVisibility(View.VISIBLE);
            refreshUserData();
        }
    }

    private void refreshDashboardData() {
        fetchAnnouncements();
        String role = sessionManager.getUserRole().toLowerCase();
        if ("admin".equals(role)) {
            fetchAdminStats();
        } else {
            refreshUserData();
        }
    }

    private void refreshUserData() {
        // Sync user profile from server to refresh any name/role changes made on web
        profileRepository.getMyProfile().enqueue(new Callback<ProfileResponse>() {
            @Override
            public void onResponse(Call<ProfileResponse> call, Response<ProfileResponse> response) {
                if (!isAdded()) return;
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    ProfileResponse p = response.body();
                    if (p.getName() != null) {
                        com.alumniconnect.app.models.User u = new com.alumniconnect.app.models.User();
                        u.setId(p.getId());
                        u.setName(p.getName());
                        u.setEmail(p.getEmail());
                        u.setRole(p.getRole());
                        sessionManager.saveSession(sessionManager.getToken(), u);
                        populateUserInfo();
                    }
                }
            }

            @Override
            public void onFailure(Call<ProfileResponse> call, Throwable t) {
                if (!isAdded()) return;
                swipeRefresh.setRefreshing(false);
            }
        });
    }

    private void fetchAdminStats() {
        showStatus("Loading statistics...");
        adminRepository.getStatistics().enqueue(new Callback<AdminStatistics>() {
            @Override
            public void onResponse(Call<AdminStatistics> call, Response<AdminStatistics> response) {
                if (!isAdded()) return;
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    AdminStatistics stats = response.body();
                    hideStatus();
                    if (tvTotalUsers != null) tvTotalUsers.setText(String.valueOf(stats.getTotalUsers()));
                    if (tvTotalAlumni != null) tvTotalAlumni.setText(String.valueOf(stats.getTotalAlumni()));
                    if (tvTotalEvents != null) tvTotalEvents.setText(String.valueOf(stats.getTotalEvents()));
                    if (tvActiveMentors != null) tvActiveMentors.setText(String.valueOf(stats.getActiveMentors()));
                    if (tvTotalOpportunities != null) tvTotalOpportunities.setText(String.valueOf(stats.getTotalOpportunities()));
                    if (tvPendingMentorship != null) tvPendingMentorship.setText(String.valueOf(stats.getPendingMentorshipRequests()));
                } else {
                    String error = ApiErrorUtils.getErrorMessage(response);
                    showStatus(error);
                }
            }

            @Override
            public void onFailure(Call<AdminStatistics> call, Throwable t) {
                if (!isAdded()) return;
                swipeRefresh.setRefreshing(false);
                String error = ApiErrorUtils.getNetworkErrorMessage(t);
                showStatus(error);
            }
        });
    }

    private void showStatus(String msg) {
        if (tvHomeStatus != null) {
            tvHomeStatus.setText(msg);
            tvHomeStatus.setVisibility(View.VISIBLE);
        }
    }

    private void hideStatus() {
        if (tvHomeStatus != null) tvHomeStatus.setVisibility(View.GONE);
    }

    private void navigateToTab(int navItemId) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).navigateToTab(navItemId);
        }
    }

    private void loadOpportunitiesFragment() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).loadFragment(new OpportunitiesFragment());
        }
    }

    private void fetchAnnouncements() {
        announcementRepository.getActiveAnnouncements().enqueue(new Callback<List<com.alumniconnect.app.models.Announcement>>() {
            @Override
            public void onResponse(Call<List<com.alumniconnect.app.models.Announcement>> call, Response<List<com.alumniconnect.app.models.Announcement>> response) {
                if (isAdded() && response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    com.alumniconnect.app.models.Announcement top = response.body().get(0);
                    tvAnnouncementTitle.setText(top.getTitle());
                    tvAnnouncementContent.setText(top.getContent());
                    cardAnnouncement.setVisibility(View.VISIBLE);
                } else if (isAdded()) {
                    cardAnnouncement.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Call<List<com.alumniconnect.app.models.Announcement>> call, Throwable t) {
                if (isAdded()) cardAnnouncement.setVisibility(View.GONE);
            }
        });
    }
}
