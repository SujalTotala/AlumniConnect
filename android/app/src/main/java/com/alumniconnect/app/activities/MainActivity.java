package com.alumniconnect.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.alumniconnect.app.R;
import com.alumniconnect.app.fragments.AlumniFragment;
import com.alumniconnect.app.fragments.EventsFragment;
import com.alumniconnect.app.fragments.HomeFragment;
import com.alumniconnect.app.fragments.MentorshipFragment;
import com.alumniconnect.app.fragments.OpportunitiesFragment;
import com.alumniconnect.app.fragments.ProfileFragment;
import com.alumniconnect.app.models.UnreadCountResponse;
import com.alumniconnect.app.repositories.NotificationRepository;
import com.alumniconnect.app.utils.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.badge.BadgeUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private SessionManager sessionManager;
    private BottomNavigationView bottomNav;

    // Fragment instance state retention tags
    private static final String TAG_HOME = "tab_home";
    private static final String TAG_ALUMNI = "tab_alumni";
    private static final String TAG_EVENTS = "tab_events";
    private static final String TAG_MENTORSHIP = "tab_mentorship";
    private static final String TAG_PROFILE = "tab_profile";
    private static final String TAG_OPPORTUNITIES = "screen_opportunities";

    private String currentTag = TAG_HOME;

    // Notification Unread Badge & Polling
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
                confirmLogout();
                return true;
            } else if (id == R.id.action_about) {
                showAboutDialog();
                return true;
            } else if (id == R.id.action_profile) {
                navigateToTab(R.id.nav_profile);
                return true;
            } else if (id == R.id.action_opportunities) {
                switchTabFragment(TAG_OPPORTUNITIES);
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
                switchTabFragment(TAG_HOME);
                return true;
            } else if (id == R.id.nav_alumni) {
                switchTabFragment(TAG_ALUMNI);
                return true;
            } else if (id == R.id.nav_events) {
                switchTabFragment(TAG_EVENTS);
                return true;
            } else if (id == R.id.nav_mentorship) {
                switchTabFragment(TAG_MENTORSHIP);
                return true;
            } else if (id == R.id.nav_profile) {
                switchTabFragment(TAG_PROFILE);
                return true;
            }
            return false;
        });

        // Restore active tag or default to Home
        if (savedInstanceState != null) {
            currentTag = savedInstanceState.getString("active_tag", TAG_HOME);
        } else {
            switchTabFragment(TAG_HOME);
        }

        // Handle target deep links if any
        handleDeepLinkIntent(getIntent());
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("active_tag", currentTag);
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
    protected void onStop() {
        super.onStop();
        stopUnreadPolling();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
                switchTabFragment(TAG_OPPORTUNITIES);
            }
            intent.removeExtra("target_fragment");
        }
    }

    /**
     * Preserves fragment instances, loaded data, scroll positions, and search/filter states
     * by using show/hide instead of repeatedly recreating fragments on tab switches.
     */
    public void switchTabFragment(String targetTag) {
        FragmentManager fm = getSupportFragmentManager();
        Fragment currentFrag = fm.findFragmentByTag(currentTag);
        Fragment targetFrag = fm.findFragmentByTag(targetTag);

        FragmentTransaction ft = fm.beginTransaction();

        // Hide current fragment if visible
        if (currentFrag != null && currentFrag.isAdded()) {
            ft.hide(currentFrag);
        }

        // Target fragment: show if already added, instantiate and add if new
        if (targetFrag != null && targetFrag.isAdded()) {
            ft.show(targetFrag);
        } else {
            targetFrag = createFragmentByTag(targetTag);
            ft.add(R.id.fragment_container, targetFrag, targetTag);
        }

        ft.commitAllowingStateLoss();
        currentTag = targetTag;
    }

    private Fragment createFragmentByTag(String tag) {
        switch (tag) {
            case TAG_ALUMNI:
                return new AlumniFragment();
            case TAG_EVENTS:
                return new EventsFragment();
            case TAG_MENTORSHIP:
                return new MentorshipFragment();
            case TAG_PROFILE:
                return new ProfileFragment();
            case TAG_OPPORTUNITIES:
                return new OpportunitiesFragment();
            case TAG_HOME:
            default:
                return new HomeFragment();
        }
    }

    /** Legacy adapter helper */
    public void loadFragment(Fragment fragment) {
        if (fragment instanceof OpportunitiesFragment) {
            switchTabFragment(TAG_OPPORTUNITIES);
        } else {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commitAllowingStateLoss();
        }
    }

    /** Called by child fragments to switch to a specific bottom nav tab */
    public void navigateToTab(int navItemId) {
        bottomNav.setSelectedItemId(navItemId);
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
        String token = sessionManager.getToken();
        if (token == null || token.isEmpty()) return;
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

    private void confirmLogout() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_logout_title)
                .setMessage(R.string.dialog_logout_message)
                .setPositiveButton(R.string.dialog_logout_positive, (dialog, which) -> performLogout())
                .setNegativeButton(R.string.dialog_cancel_negative, null)
                .show();
    }

    public void showAboutDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("About AlumniConnect")
                .setMessage("AlumniConnect — Version 1.0\n\n"
                        + "Digital Platform for Centralized Alumni Data Management and Engagement.\n\n"
                        + "Production Service:\nhttps://alumniconnect-bwoi.onrender.com/\n\n"
                        + "Status: Connected & Operational")
                .setPositiveButton("OK", null)
                .setNeutralButton("Sign Out", (dialog, which) -> confirmLogout())
                .show();
    }

    private void performLogout() {
        stopUnreadPolling();
        sessionManager.clearSession();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
