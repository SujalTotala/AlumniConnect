package com.alumniconnect.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.alumniconnect.app.R;
import com.alumniconnect.app.adapters.NotificationAdapter;
import com.alumniconnect.app.models.Notification;
import com.alumniconnect.app.repositories.NotificationRepository;
import com.alumniconnect.app.utils.ApiErrorUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationsActivity extends AppCompatActivity implements NotificationAdapter.OnNotificationInteractionListener {

    private NotificationRepository notificationRepository;
    private NotificationAdapter adapter;

    // UI
    private ProgressBar progressNotifications;
    private RecyclerView rvNotifications;
    private SwipeRefreshLayout swipeRefresh;
    private View layoutError, layoutEmpty;
    private TextView tvErrorMsg, tvEmptyEmoji, tvEmptyMsg, tvEmptyDesc;
    private View btnRetry;
    private ChipGroup chipGroupFilter;
    private Chip chipFilterAll, chipFilterUnread;

    // Cache list & duplicate protection
    private final List<Notification> allNotifications = new ArrayList<>();
    private final Set<Integer> pendingMarkReadIds = new HashSet<>();
    private boolean isMarkingAllRead = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        notificationRepository = new NotificationRepository(this);

        // Setup Toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar_notifications);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Notifications");
        }

        progressNotifications = findViewById(R.id.progress_notifications);
        rvNotifications = findViewById(R.id.rv_notifications);
        swipeRefresh = findViewById(R.id.swipe_refresh_notifications);
        layoutError = findViewById(R.id.layout_notif_error);
        layoutEmpty = findViewById(R.id.layout_notif_empty);
        tvErrorMsg = findViewById(R.id.tv_notif_error_msg);
        tvEmptyEmoji = findViewById(R.id.tv_notif_empty_emoji);
        tvEmptyMsg = findViewById(R.id.tv_notif_empty_msg);
        tvEmptyDesc = findViewById(R.id.tv_notif_empty_desc);
        btnRetry = findViewById(R.id.btn_notif_retry);
        chipGroupFilter = findViewById(R.id.chip_group_notif_filter);
        chipFilterAll = findViewById(R.id.chip_filter_all);
        chipFilterUnread = findViewById(R.id.chip_filter_unread);

        // RecyclerView
        adapter = new NotificationAdapter(this);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(adapter);

        // SwipeRefresh
        swipeRefresh.setColorSchemeResources(R.color.primary);
        swipeRefresh.setOnRefreshListener(() -> fetchNotifications(true));

        // Filter chips toggles
        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> applyFilter());

        // Retry
        btnRetry.setOnClickListener(v -> fetchNotifications(false));

        // Initial Load
        fetchNotifications(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Add Mark All Read dynamically
        menu.add(0, 1, 0, "Mark All as Read")
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == 1) {
            markAllAsRead();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void fetchNotifications(boolean fromSwipe) {
        if (!fromSwipe) {
            progressNotifications.setVisibility(View.VISIBLE);
            layoutError.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.GONE);
            rvNotifications.setVisibility(View.GONE);
        }

        notificationRepository.getNotifications().enqueue(new Callback<List<Notification>>() {
            @Override
            public void onResponse(Call<List<Notification>> call, Response<List<Notification>> response) {
                progressNotifications.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    allNotifications.clear();
                    allNotifications.addAll(response.body());
                    layoutError.setVisibility(View.GONE);
                    applyFilter();
                } else {
                    String error = ApiErrorUtils.getErrorMessage(response);
                    showError(error);
                }
            }

            @Override
            public void onFailure(Call<List<Notification>> call, Throwable t) {
                progressNotifications.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                String error = ApiErrorUtils.getNetworkErrorMessage(t);
                showError(error);
            }
        });
    }

    private void showError(String msg) {
        layoutError.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);
        rvNotifications.setVisibility(View.GONE);
        tvErrorMsg.setText(msg);
    }

    private void applyFilter() {
        List<Notification> filtered = new ArrayList<>();
        boolean unreadOnly = chipFilterUnread.isChecked();

        for (Notification n : allNotifications) {
            if (!unreadOnly || !n.isRead()) {
                filtered.add(n);
            }
        }

        adapter.setNotificationList(filtered);

        if (filtered.isEmpty()) {
            rvNotifications.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
            if (unreadOnly) {
                tvEmptyEmoji.setText("🎉");
                tvEmptyMsg.setText("You're all caught up.");
                tvEmptyDesc.setText("No unread notifications at the moment.");
            } else {
                tvEmptyEmoji.setText("📭");
                tvEmptyMsg.setText("No notifications yet.");
                tvEmptyDesc.setText("Updates about mentorship, events, opportunities and activity will appear here.");
            }
        } else {
            rvNotifications.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
        }
    }

    @Override
    public void onMarkReadClick(Notification notif) {
        if (notif == null || notif.isRead()) return;
        if (pendingMarkReadIds.contains(notif.getId())) return; // Duplicate click guard

        pendingMarkReadIds.add(notif.getId());

        // Mark Single Read
        notificationRepository.markNotificationRead(notif.getId()).enqueue(new Callback<Notification>() {
            @Override
            public void onResponse(Call<Notification> call, Response<Notification> response) {
                pendingMarkReadIds.remove(notif.getId());
                if (response.isSuccessful() && response.body() != null) {
                    // Update cache state
                    for (Notification n : allNotifications) {
                        if (n.getId() == notif.getId()) {
                            n.setRead(true);
                            break;
                        }
                    }
                    applyFilter();
                } else {
                    String error = ApiErrorUtils.getErrorMessage(response);
                    Toast.makeText(NotificationsActivity.this, error, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Notification> call, Throwable t) {
                pendingMarkReadIds.remove(notif.getId());
                String error = ApiErrorUtils.getNetworkErrorMessage(t);
                Toast.makeText(NotificationsActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onNotificationClick(Notification notif) {
        if (notif == null) return;

        // Click -> mark read then navigate
        if (!notif.isRead()) {
            onMarkReadClick(notif);
        }
        
        // Deep linking
        String type = notif.getNotificationType() != null ? notif.getNotificationType().toUpperCase() : "";
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        
        if ("MENTORSHIP".equals(type)) {
            intent.putExtra("target_fragment", "mentorship");
            startActivity(intent);
        } else if ("EVENT".equals(type)) {
            intent.putExtra("target_fragment", "events");
            startActivity(intent);
        } else if ("OPPORTUNITY".equals(type)) {
            intent.putExtra("target_fragment", "opportunities");
            startActivity(intent);
        }
    }

    private void markAllAsRead() {
        if (isMarkingAllRead) return;

        boolean hasUnread = false;
        for (Notification n : allNotifications) {
            if (!n.isRead()) {
                hasUnread = true;
                break;
            }
        }
        if (!hasUnread) return;

        isMarkingAllRead = true;
        progressNotifications.setVisibility(View.VISIBLE);
        notificationRepository.markAllNotificationsRead().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                isMarkingAllRead = false;
                progressNotifications.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    for (Notification n : allNotifications) {
                        n.setRead(true);
                    }
                    applyFilter();
                    Toast.makeText(NotificationsActivity.this, "All notifications marked as read.", Toast.LENGTH_SHORT).show();
                } else {
                    String error = ApiErrorUtils.getErrorMessage(response);
                    Toast.makeText(NotificationsActivity.this, error, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                isMarkingAllRead = false;
                progressNotifications.setVisibility(View.GONE);
                String error = ApiErrorUtils.getNetworkErrorMessage(t);
                Toast.makeText(NotificationsActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
