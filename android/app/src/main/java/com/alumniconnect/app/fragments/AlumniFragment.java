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
import com.alumniconnect.app.utils.ApiErrorUtils;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AlumniFragment extends Fragment {

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

    // Active filter state (preserved across tab switches)
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

        // Restore filter views if previously active
        if (activeSearch != null && !activeSearch.isEmpty()) {
            etSearch.setText(activeSearch);
        }
        if (Boolean.TRUE.equals(activeMentorshipFilter)) {
            chipMentorship.setChecked(true);
        }
        if (activeDeptFilter != null) {
            chipDept.setChecked(true);
            chipDept.setText("Dept: " + activeDeptFilter);
        }
        if (activeYearFilter != null) {
            chipYear.setChecked(true);
            chipYear.setText("Year: " + activeYearFilter);
        }
        updateClearChipVisibility();

        // Search with debounce
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (debounceRunnable != null) debounceHandler.removeCallbacks(debounceRunnable);
                debounceRunnable = () -> {
                    String query = s.toString().trim();
                    activeSearch = query.isEmpty() ? null : query;
                    updateClearChipVisibility();
                    loadAlumni(false);
                };
                debounceHandler.postDelayed(debounceRunnable, DEBOUNCE_DELAY_MS);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Mentorship filter chip
        chipMentorship.setOnCheckedChangeListener((buttonView, isChecked) -> {
            activeMentorshipFilter = isChecked ? true : null;
            updateClearChipVisibility();
            loadAlumni(false);
        });

        // Department filter chip
        chipDept.setOnClickListener(v -> showTextFilterDialog("Filter by Department", "e.g. Computer Science",
                current -> {
                    activeDeptFilter = current;
                    chipDept.setChecked(current != null);
                    chipDept.setText(current != null ? "Dept: " + current : "Department");
                    updateClearChipVisibility();
                    loadAlumni(false);
                }));

        // Graduation Year filter chip
        chipYear.setOnClickListener(v -> showTextFilterDialog("Filter by Graduation Year", "e.g. 2022",
                current -> {
                    activeYearFilter = current;
                    chipYear.setChecked(current != null);
                    chipYear.setText(current != null ? "Year: " + current : "Batch Year");
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
    public void onDestroyView() {
        super.onDestroyView();
        // Prevent background callback execution when view is destroyed
        if (debounceRunnable != null) {
            debounceHandler.removeCallbacks(debounceRunnable);
            debounceRunnable = null;
        }
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
                        if (!isAdded()) return;

                        progressAlumni.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);

                        if (response.isSuccessful() && response.body() != null) {
                            List<Alumni> list = response.body();
                            layoutError.setVisibility(View.GONE);

                            if (list.isEmpty()) {
                                rvAlumni.setVisibility(View.GONE);
                                layoutEmpty.setVisibility(View.VISIBLE);
                                tvEmptyMsg.setText(hasActiveFilters()
                                        ? getString(R.string.empty_alumni_filtered)
                                        : getString(R.string.empty_alumni));
                                tvResultCount.setText("");
                            } else {
                                adapter.setAlumniList(list);
                                rvAlumni.setVisibility(View.VISIBLE);
                                layoutEmpty.setVisibility(View.GONE);
                                tvResultCount.setText(list.size() + " alumni found");
                            }
                        } else {
                            String errorMsg = ApiErrorUtils.getErrorMessage(response);
                            showError(errorMsg);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Alumni>> call, Throwable t) {
                        if (!isAdded()) return;
                        progressAlumni.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);
                        String errorMsg = ApiErrorUtils.getNetworkErrorMessage(t);
                        showError(errorMsg);
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
        chipDept.setText("Department");
        chipYear.setChecked(false);
        chipYear.setText("Batch Year");
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
        if (!isAdded() || getContext() == null) return;
        Intent intent = new Intent(requireContext(), AlumniDetailsActivity.class);
        intent.putExtra("alumni_id", alumni.getId());
        intent.putExtra("alumni_name", alumni.getDisplayName());
        startActivity(intent);
    }

    private void showTextFilterDialog(String title, String hint, FilterCallback callback) {
        if (!isAdded() || getContext() == null) return;

        final android.widget.EditText input = new android.widget.EditText(requireContext());
        input.setHint(hint);
        input.setPadding(40, 24, 40, 24);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);

        if ("Filter by Department".equals(title) && activeDeptFilter != null) {
            input.setText(activeDeptFilter);
        }
        if ("Filter by Graduation Year".equals(title) && activeYearFilter != null) {
            input.setText(activeYearFilter);
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setView(input)
                .setPositiveButton(R.string.btn_apply_filter, (dialog, which) -> {
                    String value = input.getText().toString().trim();
                    callback.onFilter(value.isEmpty() ? null : value);
                })
                .setNeutralButton(R.string.btn_clear_filter, (dialog, which) -> callback.onFilter(null))
                .setNegativeButton(R.string.dialog_cancel_negative, null)
                .show();
    }

    interface FilterCallback {
        void onFilter(String value);
    }
}
