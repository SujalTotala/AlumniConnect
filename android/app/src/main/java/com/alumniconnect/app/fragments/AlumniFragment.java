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
import android.widget.Toast;
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
import com.alumniconnect.app.models.Bookmark;
import com.alumniconnect.app.repositories.AlumniRepository;
import com.alumniconnect.app.repositories.BookmarkRepository;
import com.alumniconnect.app.utils.ApiErrorUtils;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AlumniFragment extends Fragment {

    private static final int DEBOUNCE_DELAY_MS = 400;

    private AlumniRepository alumniRepository;
    private BookmarkRepository bookmarkRepository;
    private AlumniAdapter adapter;

    // Views
    private TextInputEditText etSearch;
    private Chip chipMentorship, chipDept, chipYear, chipVerified, chipMoreFilters, chipClear;
    private ProgressBar progressAlumni;
    private RecyclerView rvAlumni;
    private SwipeRefreshLayout swipeRefresh;
    private View layoutError, layoutEmpty;
    private TextView tvErrorMsg, tvResultCount, tvEmptyMsg;
    private View btnRetry;

    // Active filter state
    private String activeSearch = null;
    private Boolean activeMentorshipFilter = null;
    private Boolean activeVerifiedFilter = null;
    private String activeDeptFilter = null;
    private String activeYearFilter = null;
    private String activeCompanyFilter = null;
    private String activeJobRoleFilter = null;

    private Set<Integer> savedAlumniIds = new HashSet<>();

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
        bookmarkRepository = new BookmarkRepository(requireContext());

        etSearch = view.findViewById(R.id.et_search);
        chipMentorship = view.findViewById(R.id.chip_mentorship);
        chipDept = view.findViewById(R.id.chip_dept);
        chipYear = view.findViewById(R.id.chip_year);
        chipVerified = view.findViewById(R.id.chip_verified);
        chipMoreFilters = view.findViewById(R.id.chip_more_filters);
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
        adapter = new AlumniAdapter(this::openAlumniDetails);
        adapter.setOnAlumniBookmarkClickListener(this::handleToggleBookmark);
        rvAlumni.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvAlumni.setAdapter(adapter);

        // SwipeRefresh
        swipeRefresh.setColorSchemeResources(R.color.primary);
        swipeRefresh.setOnRefreshListener(() -> {
            loadSavedBookmarks();
            loadAlumni(true);
        });

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

        // Verified filter chip
        chipVerified.setOnCheckedChangeListener((buttonView, isChecked) -> {
            activeVerifiedFilter = isChecked ? true : null;
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

        // More filters chip (Company / Role)
        chipMoreFilters.setOnClickListener(v -> showMoreFiltersDialog());

        // Clear filters
        chipClear.setOnClickListener(v -> clearAllFilters());

        // Retry button
        btnRetry.setOnClickListener(v -> {
            loadSavedBookmarks();
            loadAlumni(false);
        });

        // Initial load
        loadSavedBookmarks();
        loadAlumni(false);
    }

    private void loadSavedBookmarks() {
        bookmarkRepository.getBookmarks("alumni").enqueue(new Callback<List<Bookmark>>() {
            @Override
            public void onResponse(Call<List<Bookmark>> call, Response<List<Bookmark>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    savedAlumniIds.clear();
                    for (Bookmark b : response.body()) {
                        savedAlumniIds.add(b.getItemId());
                    }
                    if (adapter != null) adapter.setSavedAlumniIds(savedAlumniIds);
                }
            }

            @Override
            public void onFailure(Call<List<Bookmark>> call, Throwable t) {
                // Silently ignore bookmark sync failure on startup
            }
        });
    }

    private void handleToggleBookmark(Alumni alumni, boolean isCurrentlyBookmarked) {
        if (isCurrentlyBookmarked) {
            bookmarkRepository.deleteBookmarkByItem("alumni", alumni.getId()).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    if (isAdded()) {
                        savedAlumniIds.remove(alumni.getId());
                        adapter.setSavedAlumniIds(savedAlumniIds);
                        Toast.makeText(requireContext(), "Removed from saved items", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                    if (isAdded()) Toast.makeText(requireContext(), "Failed to remove bookmark", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            bookmarkRepository.createBookmark("alumni", alumni.getId()).enqueue(new Callback<Bookmark>() {
                @Override
                public void onResponse(Call<Bookmark> call, Response<Bookmark> response) {
                    if (isAdded() && response.isSuccessful()) {
                        savedAlumniIds.add(alumni.getId());
                        adapter.setSavedAlumniIds(savedAlumniIds);
                        Toast.makeText(requireContext(), "Saved to bookmarks ★", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Bookmark> call, Throwable t) {
                    if (isAdded()) Toast.makeText(requireContext(), "Failed to bookmark alumni", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
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
                activeCompanyFilter, activeJobRoleFilter, null, null,
                activeMentorshipFilter, activeVerifiedFilter)
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
        activeVerifiedFilter = null;
        activeDeptFilter = null;
        activeYearFilter = null;
        activeCompanyFilter = null;
        activeJobRoleFilter = null;
        etSearch.setText("");
        chipMentorship.setChecked(false);
        chipVerified.setChecked(false);
        chipDept.setChecked(false);
        chipDept.setText("Department");
        chipYear.setChecked(false);
        chipYear.setText("Batch Year");
        chipMoreFilters.setText("⚙ Company & Role");
        chipClear.setVisibility(View.GONE);
        loadAlumni(false);
    }

    private void updateClearChipVisibility() {
        chipClear.setVisibility(hasActiveFilters() ? View.VISIBLE : View.GONE);
    }

    private boolean hasActiveFilters() {
        return (activeSearch != null && !activeSearch.isEmpty())
                || activeMentorshipFilter != null
                || activeVerifiedFilter != null
                || activeDeptFilter != null
                || activeYearFilter != null
                || activeCompanyFilter != null
                || activeJobRoleFilter != null;
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

    private void showMoreFiltersDialog() {
        if (!isAdded() || getContext() == null) return;

        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 10);

        final android.widget.EditText etCompany = new android.widget.EditText(requireContext());
        etCompany.setHint("Company (e.g. Google)");
        if (activeCompanyFilter != null) etCompany.setText(activeCompanyFilter);

        final android.widget.EditText etRole = new android.widget.EditText(requireContext());
        etRole.setHint("Job Role (e.g. Engineer)");
        if (activeJobRoleFilter != null) etRole.setText(activeJobRoleFilter);

        layout.addView(etCompany);
        layout.addView(etRole);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Filter by Company & Role")
                .setView(layout)
                .setPositiveButton("Apply", (dialog, which) -> {
                    String comp = etCompany.getText().toString().trim();
                    String role = etRole.getText().toString().trim();
                    activeCompanyFilter = comp.isEmpty() ? null : comp;
                    activeJobRoleFilter = role.isEmpty() ? null : role;

                    if (activeCompanyFilter != null || activeJobRoleFilter != null) {
                        chipMoreFilters.setText("⚙ " + (activeCompanyFilter != null ? activeCompanyFilter : activeJobRoleFilter));
                    } else {
                        chipMoreFilters.setText("⚙ Company & Role");
                    }
                    updateClearChipVisibility();
                    loadAlumni(false);
                })
                .setNeutralButton("Clear", (dialog, which) -> {
                    activeCompanyFilter = null;
                    activeJobRoleFilter = null;
                    chipMoreFilters.setText("⚙ Company & Role");
                    updateClearChipVisibility();
                    loadAlumni(false);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    interface FilterCallback {
        void onFilter(String value);
    }
}
