package com.alumniconnect.app.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.alumniconnect.app.R;
import com.alumniconnect.app.adapters.ConnectionRequestsAdapter;
import com.alumniconnect.app.adapters.ConnectionSuggestionsAdapter;
import com.alumniconnect.app.adapters.ConnectionsAdapter;
import com.alumniconnect.app.adapters.ReferralRequestsAdapter;
import com.alumniconnect.app.models.Connection;
import com.alumniconnect.app.models.ConnectionSuggestion;
import com.alumniconnect.app.models.NetworkSummary;
import com.alumniconnect.app.models.ReferralRequest;
import com.alumniconnect.app.repositories.ConnectionRepository;
import com.alumniconnect.app.repositories.ReferralRepository;
import com.alumniconnect.app.utils.ApiErrorUtils;
import com.alumniconnect.app.utils.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyNetworkActivity extends AppCompatActivity {

    private ConnectionRepository connectionRepository;
    private ReferralRepository referralRepository;
    private SessionManager sessionManager;

    private MaterialToolbar toolbar;
    private TextView tvSummaryConnections;
    private TextView tvSummaryPending;
    private TextView tvSummaryReferrals;
    private TabLayout tabLayout;
    private LinearLayout layoutReferralFilter;
    private MaterialButton btnRefTabReceived;
    private MaterialButton btnRefTabSent;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private LinearLayout layoutEmpty;
    private TextView tvEmptyIcon;
    private TextView tvEmptyTitle;
    private TextView tvEmptySubtitle;

    // Adapters
    private ConnectionsAdapter connectionsAdapter;
    private ConnectionRequestsAdapter receivedAdapter;
    private ConnectionRequestsAdapter sentAdapter;
    private ConnectionSuggestionsAdapter suggestionsAdapter;
    private ReferralRequestsAdapter referralReceivedAdapter;
    private ReferralRequestsAdapter referralSentAdapter;

    private int currentTab = 0; // 0=Connections, 1=Received, 2=Sent, 3=Suggestions, 4=Referrals
    private boolean isReferralSentSelected = false; // For Referrals tab sub-filter

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_network);

        connectionRepository = new ConnectionRepository(this);
        referralRepository = new ReferralRepository(this);
        sessionManager = new SessionManager(this);

        initViews();
        setupAdapters();
        setupTabs();
        setupListeners();

        loadSummary();
        loadTabData();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar_network);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvSummaryConnections = findViewById(R.id.tv_summary_connections);
        tvSummaryPending = findViewById(R.id.tv_summary_pending);
        tvSummaryReferrals = findViewById(R.id.tv_summary_referrals);

        tabLayout = findViewById(R.id.tab_layout_network);
        layoutReferralFilter = findViewById(R.id.layout_referral_filter);
        btnRefTabReceived = findViewById(R.id.btn_ref_tab_received);
        btnRefTabSent = findViewById(R.id.btn_ref_tab_sent);

        swipeRefresh = findViewById(R.id.swipe_refresh_network);
        recyclerView = findViewById(R.id.recycler_network);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        progressBar = findViewById(R.id.progress_network);
        layoutEmpty = findViewById(R.id.layout_network_empty);
        tvEmptyIcon = findViewById(R.id.tv_empty_network_icon);
        tvEmptyTitle = findViewById(R.id.tv_empty_network_title);
        tvEmptySubtitle = findViewById(R.id.tv_empty_network_subtitle);
    }

    private void setupAdapters() {
        connectionsAdapter = new ConnectionsAdapter(this::confirmRemoveConnection);

        receivedAdapter = new ConnectionRequestsAdapter(false, new ConnectionRequestsAdapter.OnRequestActionListener() {
            @Override
            public void onAccept(Connection connection) {
                acceptConnection(connection);
            }

            @Override
            public void onDecline(Connection connection) {
                rejectConnection(connection);
            }

            @Override
            public void onCancel(Connection connection) {}
        });

        sentAdapter = new ConnectionRequestsAdapter(true, new ConnectionRequestsAdapter.OnRequestActionListener() {
            @Override
            public void onAccept(Connection connection) {}

            @Override
            public void onDecline(Connection connection) {}

            @Override
            public void onCancel(Connection connection) {
                cancelConnectionRequest(connection);
            }
        });

        suggestionsAdapter = new ConnectionSuggestionsAdapter(this::sendConnectionRequest);

        referralReceivedAdapter = new ReferralRequestsAdapter(false, new ReferralRequestsAdapter.OnReferralActionListener() {
            @Override
            public void onAccept(ReferralRequest request) {
                acceptReferral(request);
            }

            @Override
            public void onDecline(ReferralRequest request) {
                declineReferral(request);
            }

            @Override
            public void onComplete(ReferralRequest request) {
                completeReferral(request);
            }

            @Override
            public void onCancel(ReferralRequest request) {}
        });

        referralSentAdapter = new ReferralRequestsAdapter(true, new ReferralRequestsAdapter.OnReferralActionListener() {
            @Override
            public void onAccept(ReferralRequest request) {}

            @Override
            public void onDecline(ReferralRequest request) {}

            @Override
            public void onComplete(ReferralRequest request) {}

            @Override
            public void onCancel(ReferralRequest request) {
                cancelReferral(request);
            }
        });
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Connections"));
        tabLayout.addTab(tabLayout.newTab().setText("Received"));
        tabLayout.addTab(tabLayout.newTab().setText("Sent"));
        tabLayout.addTab(tabLayout.newTab().setText("Suggestions"));
        tabLayout.addTab(tabLayout.newTab().setText("Referrals"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                layoutReferralFilter.setVisibility(currentTab == 4 ? View.VISIBLE : View.GONE);
                loadTabData();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                loadTabData();
            }
        });
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(() -> {
            loadSummary();
            loadTabData();
        });

        btnRefTabReceived.setOnClickListener(v -> {
            if (isReferralSentSelected) {
                isReferralSentSelected = false;
                updateReferralFilterUI();
                loadReferrals();
            }
        });

        btnRefTabSent.setOnClickListener(v -> {
            if (!isReferralSentSelected) {
                isReferralSentSelected = true;
                updateReferralFilterUI();
                loadReferrals();
            }
        });
    }

    private void updateReferralFilterUI() {
        if (!isReferralSentSelected) {
            btnRefTabReceived.setBackgroundTintList(getColorStateList(R.color.primary));
            btnRefTabReceived.setTextColor(getColor(R.color.white));
            btnRefTabSent.setBackgroundTintList(getColorStateList(android.R.color.transparent));
            btnRefTabSent.setTextColor(getColor(R.color.text_secondary));
        } else {
            btnRefTabSent.setBackgroundTintList(getColorStateList(R.color.primary));
            btnRefTabSent.setTextColor(getColor(R.color.white));
            btnRefTabReceived.setBackgroundTintList(getColorStateList(android.R.color.transparent));
            btnRefTabReceived.setTextColor(getColor(R.color.text_secondary));
        }
    }

    private void loadSummary() {
        connectionRepository.getNetworkSummary().enqueue(new Callback<NetworkSummary>() {
            @Override
            public void onResponse(Call<NetworkSummary> call, Response<NetworkSummary> response) {
                if (response.isSuccessful() && response.body() != null) {
                    NetworkSummary s = response.body();
                    tvSummaryConnections.setText(String.valueOf(s.getTotalConnections()));
                    tvSummaryPending.setText(String.valueOf(s.getPendingRequestsReceived()));
                    tvSummaryReferrals.setText(String.valueOf(s.getTotalReferralsReceived() + s.getTotalReferralsSent()));
                }
            }

            @Override
            public void onFailure(Call<NetworkSummary> call, Throwable t) {}
        });
    }

    private void loadTabData() {
        progressBar.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);

        switch (currentTab) {
            case 0:
                recyclerView.setAdapter(connectionsAdapter);
                loadConnections();
                break;
            case 1:
                recyclerView.setAdapter(receivedAdapter);
                loadReceivedRequests();
                break;
            case 2:
                recyclerView.setAdapter(sentAdapter);
                loadSentRequests();
                break;
            case 3:
                recyclerView.setAdapter(suggestionsAdapter);
                loadSuggestions();
                break;
            case 4:
                loadReferrals();
                break;
        }
    }

    private void loadConnections() {
        connectionRepository.getMyNetwork().enqueue(new Callback<List<Connection>>() {
            @Override
            public void onResponse(Call<List<Connection>> call, Response<List<Connection>> response) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<Connection> list = response.body();
                    connectionsAdapter.setList(list);
                    if (list.isEmpty()) {
                        showEmptyState("👥", "No connections yet", "Discover suggested connections or connect with alumni to grow your network.");
                    } else {
                        layoutEmpty.setVisibility(View.GONE);
                    }
                } else {
                    showEmptyState("⚠️", "Could not load network", ApiErrorUtils.parseError(response));
                }
            }

            @Override
            public void onFailure(Call<List<Connection>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                showEmptyState("⚠️", "Network error", ApiErrorUtils.parseThrowable(t));
            }
        });
    }

    private void loadReceivedRequests() {
        connectionRepository.getReceivedRequests().enqueue(new Callback<List<Connection>>() {
            @Override
            public void onResponse(Call<List<Connection>> call, Response<List<Connection>> response) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<Connection> list = response.body();
                    receivedAdapter.setList(list);
                    if (list.isEmpty()) {
                        showEmptyState("📥", "No pending requests", "When other members send you connection requests, they'll appear here.");
                    } else {
                        layoutEmpty.setVisibility(View.GONE);
                    }
                } else {
                    showEmptyState("⚠️", "Could not load requests", ApiErrorUtils.parseError(response));
                }
            }

            @Override
            public void onFailure(Call<List<Connection>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                showEmptyState("⚠️", "Network error", ApiErrorUtils.parseThrowable(t));
            }
        });
    }

    private void loadSentRequests() {
        connectionRepository.getSentRequests().enqueue(new Callback<List<Connection>>() {
            @Override
            public void onResponse(Call<List<Connection>> call, Response<List<Connection>> response) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<Connection> list = response.body();
                    sentAdapter.setList(list);
                    if (list.isEmpty()) {
                        showEmptyState("📤", "No outgoing requests", "Requests you've sent to other members will appear here.");
                    } else {
                        layoutEmpty.setVisibility(View.GONE);
                    }
                } else {
                    showEmptyState("⚠️", "Could not load requests", ApiErrorUtils.parseError(response));
                }
            }

            @Override
            public void onFailure(Call<List<Connection>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                showEmptyState("⚠️", "Network error", ApiErrorUtils.parseThrowable(t));
            }
        });
    }

    private void loadSuggestions() {
        connectionRepository.getConnectionSuggestions().enqueue(new Callback<List<ConnectionSuggestion>>() {
            @Override
            public void onResponse(Call<List<ConnectionSuggestion>> call, Response<List<ConnectionSuggestion>> response) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<ConnectionSuggestion> list = response.body();
                    suggestionsAdapter.setList(list);
                    if (list.isEmpty()) {
                        showEmptyState("✨", "No suggestions found", "Check back later as new alumni and students join the platform.");
                    } else {
                        layoutEmpty.setVisibility(View.GONE);
                    }
                } else {
                    showEmptyState("⚠️", "Could not load suggestions", ApiErrorUtils.parseError(response));
                }
            }

            @Override
            public void onFailure(Call<List<ConnectionSuggestion>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                showEmptyState("⚠️", "Network error", ApiErrorUtils.parseThrowable(t));
            }
        });
    }

    private void loadReferrals() {
        if (!isReferralSentSelected) {
            recyclerView.setAdapter(referralReceivedAdapter);
            referralRepository.getReceivedReferrals().enqueue(new Callback<List<ReferralRequest>>() {
                @Override
                public void onResponse(Call<List<ReferralRequest>> call, Response<List<ReferralRequest>> response) {
                    progressBar.setVisibility(View.GONE);
                    swipeRefresh.setRefreshing(false);
                    if (response.isSuccessful() && response.body() != null) {
                        List<ReferralRequest> list = response.body();
                        referralReceivedAdapter.setList(list);
                        if (list.isEmpty()) {
                            showEmptyState("🤝", "No received referrals", "Referral requests directed to you by connections will appear here.");
                        } else {
                            layoutEmpty.setVisibility(View.GONE);
                        }
                    } else {
                        showEmptyState("⚠️", "Could not load referrals", ApiErrorUtils.parseError(response));
                    }
                }

                @Override
                public void onFailure(Call<List<ReferralRequest>> call, Throwable t) {
                    progressBar.setVisibility(View.GONE);
                    swipeRefresh.setRefreshing(false);
                    showEmptyState("⚠️", "Network error", ApiErrorUtils.parseThrowable(t));
                }
            });
        } else {
            recyclerView.setAdapter(referralSentAdapter);
            referralRepository.getSentReferrals().enqueue(new Callback<List<ReferralRequest>>() {
                @Override
                public void onResponse(Call<List<ReferralRequest>> call, Response<List<ReferralRequest>> response) {
                    progressBar.setVisibility(View.GONE);
                    swipeRefresh.setRefreshing(false);
                    if (response.isSuccessful() && response.body() != null) {
                        List<ReferralRequest> list = response.body();
                        referralSentAdapter.setList(list);
                        if (list.isEmpty()) {
                            showEmptyState("🤝", "No sent referrals", "Request referrals from your alumni connections directly from opportunity listings.");
                        } else {
                            layoutEmpty.setVisibility(View.GONE);
                        }
                    } else {
                        showEmptyState("⚠️", "Could not load referrals", ApiErrorUtils.parseError(response));
                    }
                }

                @Override
                public void onFailure(Call<List<ReferralRequest>> call, Throwable t) {
                    progressBar.setVisibility(View.GONE);
                    swipeRefresh.setRefreshing(false);
                    showEmptyState("⚠️", "Network error", ApiErrorUtils.parseThrowable(t));
                }
            });
        }
    }

    private void showEmptyState(String icon, String title, String subtitle) {
        layoutEmpty.setVisibility(View.VISIBLE);
        tvEmptyIcon.setText(icon);
        tvEmptyTitle.setText(title);
        tvEmptySubtitle.setText(subtitle);
    }

    // Connection Actions
    private void confirmRemoveConnection(Connection connection) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Remove Connection")
                .setMessage("Are you sure you want to remove this connection from your network?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    connectionRepository.removeConnection(connection.getId()).enqueue(new Callback<Map<String, Object>>() {
                        @Override
                        public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(MyNetworkActivity.this, "Connection removed", Toast.LENGTH_SHORT).show();
                                loadSummary();
                                loadConnections();
                            } else {
                                Toast.makeText(MyNetworkActivity.this, ApiErrorUtils.parseError(response), Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                            Toast.makeText(MyNetworkActivity.this, ApiErrorUtils.parseThrowable(t), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void acceptConnection(Connection connection) {
        connectionRepository.acceptConnection(connection.getId()).enqueue(new Callback<Connection>() {
            @Override
            public void onResponse(Call<Connection> call, Response<Connection> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MyNetworkActivity.this, "Connection accepted!", Toast.LENGTH_SHORT).show();
                    loadSummary();
                    loadReceivedRequests();
                } else {
                    Toast.makeText(MyNetworkActivity.this, ApiErrorUtils.parseError(response), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Connection> call, Throwable t) {
                Toast.makeText(MyNetworkActivity.this, ApiErrorUtils.parseThrowable(t), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void rejectConnection(Connection connection) {
        connectionRepository.rejectConnection(connection.getId()).enqueue(new Callback<Connection>() {
            @Override
            public void onResponse(Call<Connection> call, Response<Connection> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MyNetworkActivity.this, "Request declined", Toast.LENGTH_SHORT).show();
                    loadSummary();
                    loadReceivedRequests();
                } else {
                    Toast.makeText(MyNetworkActivity.this, ApiErrorUtils.parseError(response), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Connection> call, Throwable t) {
                Toast.makeText(MyNetworkActivity.this, ApiErrorUtils.parseThrowable(t), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cancelConnectionRequest(Connection connection) {
        connectionRepository.removeConnection(connection.getId()).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MyNetworkActivity.this, "Request cancelled", Toast.LENGTH_SHORT).show();
                    loadSummary();
                    loadSentRequests();
                } else {
                    Toast.makeText(MyNetworkActivity.this, ApiErrorUtils.parseError(response), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(MyNetworkActivity.this, ApiErrorUtils.parseThrowable(t), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendConnectionRequest(ConnectionSuggestion suggestion) {
        if (suggestion.getUser() == null) return;
        connectionRepository.sendConnectionRequest(suggestion.getUser().getId()).enqueue(new Callback<Connection>() {
            @Override
            public void onResponse(Call<Connection> call, Response<Connection> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MyNetworkActivity.this, "Connection request sent!", Toast.LENGTH_SHORT).show();
                    loadSummary();
                    loadSuggestions();
                } else {
                    Toast.makeText(MyNetworkActivity.this, ApiErrorUtils.parseError(response), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Connection> call, Throwable t) {
                Toast.makeText(MyNetworkActivity.this, ApiErrorUtils.parseThrowable(t), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Referral Actions
    private void acceptReferral(ReferralRequest request) {
        referralRepository.acceptReferral(request.getId()).enqueue(new Callback<ReferralRequest>() {
            @Override
            public void onResponse(Call<ReferralRequest> call, Response<ReferralRequest> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MyNetworkActivity.this, "Referral request accepted!", Toast.LENGTH_SHORT).show();
                    loadSummary();
                    loadReferrals();
                } else {
                    Toast.makeText(MyNetworkActivity.this, ApiErrorUtils.parseError(response), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ReferralRequest> call, Throwable t) {
                Toast.makeText(MyNetworkActivity.this, ApiErrorUtils.parseThrowable(t), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void declineReferral(ReferralRequest request) {
        referralRepository.declineReferral(request.getId()).enqueue(new Callback<ReferralRequest>() {
            @Override
            public void onResponse(Call<ReferralRequest> call, Response<ReferralRequest> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MyNetworkActivity.this, "Referral request declined", Toast.LENGTH_SHORT).show();
                    loadSummary();
                    loadReferrals();
                } else {
                    Toast.makeText(MyNetworkActivity.this, ApiErrorUtils.parseError(response), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ReferralRequest> call, Throwable t) {
                Toast.makeText(MyNetworkActivity.this, ApiErrorUtils.parseThrowable(t), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void completeReferral(ReferralRequest request) {
        referralRepository.completeReferral(request.getId()).enqueue(new Callback<ReferralRequest>() {
            @Override
            public void onResponse(Call<ReferralRequest> call, Response<ReferralRequest> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MyNetworkActivity.this, "Referral marked as submitted!", Toast.LENGTH_SHORT).show();
                    loadSummary();
                    loadReferrals();
                } else {
                    Toast.makeText(MyNetworkActivity.this, ApiErrorUtils.parseError(response), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ReferralRequest> call, Throwable t) {
                Toast.makeText(MyNetworkActivity.this, ApiErrorUtils.parseThrowable(t), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cancelReferral(ReferralRequest request) {
        referralRepository.cancelReferral(request.getId()).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MyNetworkActivity.this, "Referral request cancelled", Toast.LENGTH_SHORT).show();
                    loadSummary();
                    loadReferrals();
                } else {
                    Toast.makeText(MyNetworkActivity.this, ApiErrorUtils.parseError(response), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(MyNetworkActivity.this, ApiErrorUtils.parseThrowable(t), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
