package com.alumniconnect.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.alumniconnect.app.R;
import com.alumniconnect.app.fragments.AlumniFragment;
import com.alumniconnect.app.fragments.EventsFragment;
import com.alumniconnect.app.fragments.HomeFragment;
import com.alumniconnect.app.fragments.MentorshipFragment;
import com.alumniconnect.app.fragments.OpportunitiesFragment;
import com.alumniconnect.app.fragments.PlaceholderFragment;
import com.alumniconnect.app.fragments.ProfileFragment;
import com.alumniconnect.app.models.UnreadCountResponse;
import com.alumniconnect.app.repositories.NotificationRepository;
import com.alumniconnect.app.utils.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.badge.BadgeUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private SessionManager sessionManager;
    private BottomNavigationView bottomNav;

    // Notification Unread Badge
    private NotificationRepository notificationRepository;
    private BadgeDrawable notificationBadge;
    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private Runnable pollRunnable;
    private static final int POLL_INTERVAL_MS = 30000; // 30s matching React Navbar.jsx

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sessionManager = new SessionManager(this);
        notificationRepository = new NotificationRepository(this);

        // Setup Toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setSubtitle(sessionManager.getUserRole().toUpperCase());
        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_logout) {
                performLogout();
                return true;
            } else if (id == R.id.action_profile) {
                navigateToTab(R.id.nav_profile);
                return true;
            } else if (id == R.id.action_opportunities) {
                loadFragment(new OpportunitiesFragment());
                return true;
            } else if (id == R.id.action_notifications) {
                Intent intent = new Intent(MainActivity.this, NotificationsActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        });

        // Bottom Navigation
        bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                loadFragment(new HomeFragment());
                return true;
            } else if (id == R.id.nav_alumni) {
                loadFragment(new AlumniFragment());
                return true;
            } else if (id == R.id.nav_events) {
                loadFragment(new EventsFragment());
                return true;
            } else if (id == R.id.nav_mentorship) {
                loadFragment(new MentorshipFragment());
                return true;
            } else if (id == R.id.nav_profile) {
                loadFragment(new ProfileFragment());
                return true;
            }
            return false;
        });

        // Default fragment on open
        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        }

        // Handle target deep links if any
        handleDeepLinkIntent(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        startUnreadPolling();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopUnreadPolling();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleDeepLinkIntent(intent);
    }

    private void handleDeepLinkIntent(Intent intent) {
        if (intent != null && intent.hasExtra("target_fragment")) {
            String target = intent.getStringExtra("target_fragment");
            if ("mentorship".equals(target)) {
                navigateToTab(R.id.nav_mentorship);
            } else if ("events".equals(target)) {
                navigateToTab(R.id.nav_events);
            } else if ("opportunities".equals(target)) {
                loadFragment(new OpportunitiesFragment());
            }
            intent.removeExtra("target_fragment");
        }
    }

    private void startUnreadPolling() {
        stopUnreadPolling();
        pollRunnable = new Runnable() {
            @Override
            public void run() {
                fetchUnreadCount();
                pollHandler.postDelayed(this, POLL_INTERVAL_MS);
            }
        };
        pollHandler.post(pollRunnable);
    }

    private void stopUnreadPolling() {
        if (pollRunnable != null) {
            pollHandler.removeCallbacks(pollRunnable);
            pollRunnable = null;
        }
    }

    private void fetchUnreadCount() {
        if (sessionManager.getToken().isEmpty()) return;
        notificationRepository.getUnreadNotificationCount().enqueue(new Callback<UnreadCountResponse>() {
            @Override
            @com.google.android.material.badge.ExperimentalBadgeUtils
            public void onResponse(Call<UnreadCountResponse> call, Response<UnreadCountResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int count = response.body().getUnreadCount();
                    updateNotificationBadge(count);
                }
            }

            @Override
            public void onFailure(Call<UnreadCountResponse> call, Throwable t) {
                // fail silently for background polling
            }
        });
    }

    @com.google.android.material.badge.ExperimentalBadgeUtils
    private void updateNotificationBadge(int count) {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar == null) return;

        if (notificationBadge == null) {
            notificationBadge = BadgeDrawable.create(this);
        }

        notificationBadge.setNumber(count);
        notificationBadge.setVisible(count > 0);

        try {
            BadgeUtils.detachBadgeDrawable(notificationBadge, toolbar, R.id.action_notifications);
        } catch (Exception ignored) {}

        if (count > 0) {
            BadgeUtils.attachBadgeDrawable(notificationBadge, toolbar, R.id.action_notifications);
        }
    }

    public void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    /** Called by fragments to switch to a specific bottom nav tab */
    public void navigateToTab(int navItemId) {
        bottomNav.setSelectedItemId(navItemId);
    }

    private void performLogout() {
        sessionManager.clearSession();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
