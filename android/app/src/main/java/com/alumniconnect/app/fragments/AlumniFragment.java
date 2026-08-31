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
import com.alumniconnect.app.activities.AlumniDetailsActivity;
import com.alumniconnect.app.adapters.AlumniAdapter;
import com.alumniconnect.app.models.Alumni;
import com.alumniconnect.app.repositories.AlumniRepository;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AlumniFragment extends Fragment {

    // Search debounce delay (ms)
    private static final int DEBOUNCE_DELAY_MS = 400;

    private AlumniRepository alumniRepository;
    private AlumniAdapter adapter;

    // Views
    private TextInputEditText etSearch;
    private Chip chipMentorship, chipDept, chipYear, chipClear;
    private ProgressBar progressAlumni;
    private RecyclerView rvAlumni;
    private SwipeRefreshLayout swipeRefresh;
    private View layoutError, layoutEmpty;
    private TextView tvErrorMsg, tvResultCount, tvEmptyMsg;
    private View btnRetry;

    // Active filter state
    private String activeSearch = null;
    private Boolean activeMentorshipFilter = null;   // null = no filter
    private String activeDeptFilter = null;
    private String activeYearFilter = null;

    // Debounce handler
    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable debounceRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_alumni, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        alumniRepository = new AlumniRepository(requireContext());

        etSearch = view.findViewById(R.id.et_search);
        chipMentorship = view.findViewById(R.id.chip_mentorship);
        chipDept = view.findViewById(R.id.chip_dept);
        chipYear = view.findViewById(R.id.chip_year);
        chipClear = view.findViewById(R.id.chip_clear);
        progressAlumni = view.findViewById(R.id.progress_alumni);
        rvAlumni = view.findViewById(R.id.rv_alumni);
        swipeRefresh = view.findViewById(R.id.swipe_refresh_alumni);
        layoutError = view.findViewById(R.id.layout_error);
        layoutEmpty = view.findViewById(R.id.layout_empty);
        tvErrorMsg = view.findViewById(R.id.tv_error_msg);
        tvResultCount = view.findViewById(R.id.tv_result_count);
        tvEmptyMsg = view.findViewById(R.id.tv_empty_msg);
        btnRetry = view.findViewById(R.id.btn_retry);

        // RecyclerView setup
        adapter = new AlumniAdapter(alumni -> openAlumniDetails(alumni));
        rvAlumni.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvAlumni.setAdapter(adapter);

        // SwipeRefresh
        swipeRefresh.setColorSchemeResources(R.color.primary);
        swipeRefresh.setOnRefreshListener(() -> loadAlumni(true));

        // Search with debounce
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (debounceRunnable != null) debounceHandler.removeCallbacks(debounceRunnable);
                debounceRunnable = () -> {
                    activeSearch = s.toString().trim().isEmpty() ? null : s.toString().trim();
                    updateClearChipVisibility();
                    loadAlumni(false);
                };
                debounceHandler.postDelayed(debounceRunnable, DEBOUNCE_DELAY_MS);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Mentorship filter chip (toggle)
        chipMentorship.setOnCheckedChangeListener((buttonView, isChecked) -> {
            activeMentorshipFilter = isChecked ? true : null;
            updateClearChipVisibility();
            loadAlumni(false);
        });

        // Department filter chip — shows an input dialog
        chipDept.setOnClickListener(v -> showTextFilterDialog("Filter by Department", "e.g. Computer Science",
                current -> {
                    activeDeptFilter = current;
                    chipDept.setChecked(current != null);
                    updateClearChipVisibility();
                    loadAlumni(false);
                }));

        // Graduation Year filter chip — shows an input dialog
        chipYear.setOnClickListener(v -> showTextFilterDialog("Filter by Graduation Year", "e.g. 2022",
                current -> {
                    activeYearFilter = current;
                    chipYear.setChecked(current != null);
                    updateClearChipVisibility();
                    loadAlumni(false);
                }));

        // Clear filters
        chipClear.setOnClickListener(v -> clearAllFilters());

        // Retry button
        btnRetry.setOnClickListener(v -> loadAlumni(false));

        // Initial load
        loadAlumni(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh when user returns to tab
        if (adapter.getAlumniCount() == 0) loadAlumni(false);
    }

    private void loadAlumni(boolean fromSwipe) {
        if (!fromSwipe) {
            progressAlumni.setVisibility(View.VISIBLE);
            layoutError.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.GONE);
            rvAlumni.setVisibility(View.GONE);
            tvResultCount.setText("");
        }

        alumniRepository.getAlumni(activeSearch, activeDeptFilter, activeYearFilter,
                null, null, activeMentorshipFilter)
                .enqueue(new Callback<List<Alumni>>() {
                    @Override
                    public void onResponse(Call<List<Alumni>> call, Response<List<Alumni>> response) {
                        progressAlumni.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);

                        if (response.isSuccessful() && response.body() != null) {
                            List<Alumni> list = response.body();
                            layoutError.setVisibility(View.GONE);

                            if (list.isEmpty()) {
                                rvAlumni.setVisibility(View.GONE);
                                layoutEmpty.setVisibility(View.VISIBLE);
                                tvEmptyMsg.setText(hasActiveFilters()
                                        ? "No alumni match your search/filters."
                                        : "No alumni in the directory yet.");
                                tvResultCount.setText("");
                            } else {
                                adapter.setAlumniList(list);
                                rvAlumni.setVisibility(View.VISIBLE);
                                layoutEmpty.setVisibility(View.GONE);
                                tvResultCount.setText(list.size() + " alumni found");
                            }
                        } else if (response.code() == 401) {
                            showError("Session expired. Please login again.");
                        } else {
                            showError("Server error (HTTP " + response.code() + "). Pull to retry.");
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Alumni>> call, Throwable t) {
                        progressAlumni.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);
                        showError("Network unavailable. Check connection and retry.");
                    }
                });
    }

    private void showError(String message) {
        layoutError.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);
        rvAlumni.setVisibility(View.GONE);
        tvErrorMsg.setText(message);
        tvResultCount.setText("");
    }

    private void clearAllFilters() {
        activeSearch = null;
        activeMentorshipFilter = null;
        activeDeptFilter = null;
        activeYearFilter = null;
        etSearch.setText("");
        chipMentorship.setChecked(false);
        chipDept.setChecked(false);
        chipYear.setChecked(false);
        chipClear.setVisibility(View.GONE);
        loadAlumni(false);
    }

    private void updateClearChipVisibility() {
        chipClear.setVisibility(hasActiveFilters() ? View.VISIBLE : View.GONE);
    }

    private boolean hasActiveFilters() {
        return (activeSearch != null && !activeSearch.isEmpty())
                || activeMentorshipFilter != null
                || activeDeptFilter != null
                || activeYearFilter != null;
    }

    private void openAlumniDetails(Alumni alumni) {
        Intent intent = new Intent(requireContext(), AlumniDetailsActivity.class);
        // Pass ID — details activity will re-fetch fresh data from GET /alumni/{id}
        intent.putExtra("alumni_id", alumni.getId());
        // Also pass name for immediate display while loading
        intent.putExtra("alumni_name", alumni.getDisplayName());
        startActivity(intent);
    }

    /** Minimal inline text-input dialog for dept/year filters */
    private void showTextFilterDialog(String title, String hint, FilterCallback callback) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle(title);

        final android.widget.EditText input = new android.widget.EditText(requireContext());
        input.setHint(hint);
        input.setPadding(32, 16, 32, 16);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        // Pre-fill if active
        if ("Filter by Department".equals(title) && activeDeptFilter != null)
            input.setText(activeDeptFilter);
        if ("Filter by Graduation Year".equals(title) && activeYearFilter != null)
            input.setText(activeYearFilter);

        builder.setView(input);
        builder.setPositiveButton("Apply", (dialog, which) -> {
            String value = input.getText().toString().trim();
            callback.onFilter(value.isEmpty() ? null : value);
        });
        builder.setNeutralButton("Clear", (dialog, which) -> callback.onFilter(null));
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    interface FilterCallback {
        void onFilter(String value);
    }
}
