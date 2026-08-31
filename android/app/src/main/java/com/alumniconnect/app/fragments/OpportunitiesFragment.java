package com.alumniconnect.app.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.alumniconnect.app.R;
import com.alumniconnect.app.activities.CreateOpportunityActivity;
import com.alumniconnect.app.activities.OpportunityDetailsActivity;
import com.alumniconnect.app.adapters.OpportunityAdapter;
import com.alumniconnect.app.models.Opportunity;
import com.alumniconnect.app.repositories.OpportunityRepository;
import com.alumniconnect.app.utils.SessionManager;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OpportunitiesFragment extends Fragment {

    private static final int DEBOUNCE_DELAY_MS = 400;

    private OpportunityRepository opportunityRepository;
    private SessionManager sessionManager;
    private OpportunityAdapter adapter;

    private TextInputEditText etSearch;
    private ChipGroup chipGroupOppType;
    private ProgressBar progressOpps;
    private RecyclerView rvOpportunities;
    private SwipeRefreshLayout swipeRefresh;
    private View layoutError, layoutEmpty;
    private TextView tvErrorMsg, tvResultCount, tvEmptyMsg;
    private View btnRetry;
    private FloatingActionButton fabCreateOpp;

    private String activeSearch = null;
    private String activeType = null;

    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable debounceRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_opportunities, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        opportunityRepository = new OpportunityRepository(requireContext());
        sessionManager = new SessionManager(requireContext());

        etSearch = view.findViewById(R.id.et_opp_search);
        chipGroupOppType = view.findViewById(R.id.chip_group_opp_type);
        progressOpps = view.findViewById(R.id.progress_opps);
        rvOpportunities = view.findViewById(R.id.rv_opportunities);
        swipeRefresh = view.findViewById(R.id.swipe_refresh_opps);
        layoutError = view.findViewById(R.id.layout_opps_error);
        layoutEmpty = view.findViewById(R.id.layout_opps_empty);
        tvErrorMsg = view.findViewById(R.id.tv_opps_error_msg);
        tvResultCount = view.findViewById(R.id.tv_opp_count);
        tvEmptyMsg = view.findViewById(R.id.tv_opps_empty_msg);
        btnRetry = view.findViewById(R.id.btn_opps_retry);
        fabCreateOpp = view.findViewById(R.id.fab_create_opportunity);

        // RecyclerView
        adapter = new OpportunityAdapter(opp -> openOpportunityDetails(opp));
        rvOpportunities.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvOpportunities.setAdapter(adapter);

        // SwipeRefresh
        swipeRefresh.setColorSchemeResources(R.color.primary);
        swipeRefresh.setOnRefreshListener(() -> loadOpportunities(true));

        // Search with debounce
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (debounceRunnable != null) debounceHandler.removeCallbacks(debounceRunnable);
                debounceRunnable = () -> {
                    activeSearch = s.toString().trim().isEmpty() ? null : s.toString().trim();
                    loadOpportunities(false);
                };
                debounceHandler.postDelayed(debounceRunnable, DEBOUNCE_DELAY_MS);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Filter chips group
        chipGroupOppType.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                activeType = null;
            } else {
                int id = checkedIds.get(0);
                if (id == R.id.chip_all_opps) {
                    activeType = null;
                } else if (id == R.id.chip_internship) {
                    activeType = "Internship";
                } else if (id == R.id.chip_full_time) {
                    activeType = "Full-Time Job";
                } else if (id == R.id.chip_referral) {
                    activeType = "Referral";
                } else if (id == R.id.chip_hackathon) {
                    activeType = "Hackathon";
                } else if (id == R.id.chip_scholarship) {
                    activeType = "Scholarship";
                } else if (id == R.id.chip_workshop_opp) {
                    activeType = "Workshop";
                }
            }
            loadOpportunities(false);
        });

        // Retry button
        btnRetry.setOnClickListener(v -> loadOpportunities(false));

        // FAB visibility for Opportunity creation
        setupFAB();

        // Load
        loadOpportunities(false);
    }

    private void setupFAB() {
        String role = sessionManager.getUserRole().toLowerCase();
        if ("admin".equals(role) || "alumni".equals(role)) {
            fabCreateOpp.setVisibility(View.VISIBLE);
            fabCreateOpp.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), CreateOpportunityActivity.class);
                startActivity(intent);
            });
        } else {
            fabCreateOpp.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadOpportunities(false);
    }

    private void loadOpportunities(boolean fromSwipe) {
        if (!fromSwipe) {
            progressOpps.setVisibility(View.VISIBLE);
            layoutError.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.GONE);
            rvOpportunities.setVisibility(View.GONE);
            tvResultCount.setText("");
        }

        // Backend getOpportunities: filter by opportunity_type, search, location
        opportunityRepository.getOpportunities(activeType, activeSearch, null).enqueue(new Callback<List<Opportunity>>() {
            @Override
            public void onResponse(Call<List<Opportunity>> call, Response<List<Opportunity>> response) {
                progressOpps.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    List<Opportunity> list = response.body();
                    layoutError.setVisibility(View.GONE);

                    if (list.isEmpty()) {
                        rvOpportunities.setVisibility(View.GONE);
                        layoutEmpty.setVisibility(View.VISIBLE);
                        tvEmptyMsg.setText(hasActiveFilters()
                                ? "No opportunities match your search/filters."
                                : "No career opportunities available yet.");
                        tvResultCount.setText("");
                    } else {
                        adapter.setOpportunityList(list);
                        rvOpportunities.setVisibility(View.VISIBLE);
                        layoutEmpty.setVisibility(View.GONE);
                        tvResultCount.setText(list.size() + " opportunities found");
                    }
                } else if (response.code() == 401) {
                    showError("Session expired. Please login again.");
                } else {
                    showError("Server error (HTTP " + response.code() + "). Pull to retry.");
                }
            }

            @Override
            public void onFailure(Call<List<Opportunity>> call, Throwable t) {
                progressOpps.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                showError("Network unavailable. Check connection and retry.");
            }
        });
    }

    private void showError(String message) {
        layoutError.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);
        rvOpportunities.setVisibility(View.GONE);
        tvErrorMsg.setText(message);
        tvResultCount.setText("");
    }

    private boolean hasActiveFilters() {
        return (activeSearch != null && !activeSearch.isEmpty())
                || activeType != null;
    }

    private void openOpportunityDetails(Opportunity opp) {
        Intent intent = new Intent(requireContext(), OpportunityDetailsActivity.class);
        intent.putExtra("opportunity_id", opp.getId());
        intent.putExtra("opportunity_title", opp.getTitle());
        startActivity(intent);
    }
}
