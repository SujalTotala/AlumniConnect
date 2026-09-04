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
import com.alumniconnect.app.repositories.MentorshipRepository;
import com.alumniconnect.app.utils.ApiErrorUtils;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AvailableMentorsFragment extends Fragment {

    private static final int SEARCH_DEBOUNCE_MS = 400;

    private MentorshipRepository mentorshipRepository;
    private AlumniAdapter adapter;

    private TextInputEditText etSearch;
    private ProgressBar progressMentors;
    private TextView tvEmpty;
    private RecyclerView rvMentors;
    private SwipeRefreshLayout swipeRefresh;
    private View layoutError;
    private TextView tvErrorMsg;
    private View btnRetry;

    private String searchKeyword = null;
    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable debounceRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_available_mentors, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mentorshipRepository = new MentorshipRepository(requireContext());

        etSearch = view.findViewById(R.id.et_mentor_search);
        progressMentors = view.findViewById(R.id.progress_mentors);
        tvEmpty = view.findViewById(R.id.tv_mentors_empty);
        rvMentors = view.findViewById(R.id.rv_mentors);
        swipeRefresh = view.findViewById(R.id.swipe_refresh_mentors);
        layoutError = view.findViewById(R.id.layout_mentors_error);
        tvErrorMsg = view.findViewById(R.id.tv_mentors_error_msg);
        btnRetry = view.findViewById(R.id.btn_mentors_retry);

        adapter = new AlumniAdapter(alumni -> openMentorDetails(alumni));
        rvMentors.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvMentors.setAdapter(adapter);

        swipeRefresh.setColorSchemeResources(R.color.primary);
        swipeRefresh.setOnRefreshListener(() -> loadMentors(true));

        if (searchKeyword != null && !searchKeyword.isEmpty()) {
            etSearch.setText(searchKeyword);
        }

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (debounceRunnable != null) debounceHandler.removeCallbacks(debounceRunnable);
                debounceRunnable = () -> {
                    String q = s.toString().trim();
                    searchKeyword = q.isEmpty() ? null : q;
                    loadMentors(false);
                };
                debounceHandler.postDelayed(debounceRunnable, SEARCH_DEBOUNCE_MS);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnRetry.setOnClickListener(v -> loadMentors(false));

        loadMentors(false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (debounceRunnable != null) {
            debounceHandler.removeCallbacks(debounceRunnable);
            debounceRunnable = null;
        }
    }

    private void loadMentors(boolean fromSwipe) {
        if (!fromSwipe) {
            progressMentors.setVisibility(View.VISIBLE);
            layoutError.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.GONE);
            rvMentors.setVisibility(View.GONE);
        }

        mentorshipRepository.getMentors(null, null, null).enqueue(new Callback<List<Alumni>>() {
            @Override
            public void onResponse(Call<List<Alumni>> call, Response<List<Alumni>> response) {
                if (!isAdded()) return;

                progressMentors.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    List<Alumni> rawList = response.body();
                    List<Alumni> filtered = new ArrayList<>();

                    if (searchKeyword != null) {
                        String query = searchKeyword.toLowerCase();
                        for (Alumni alumni : rawList) {
                            boolean matchName = alumni.getDisplayName().toLowerCase().contains(query);
                            boolean matchDept = alumni.getDepartment() != null && alumni.getDepartment().toLowerCase().contains(query);
                            boolean matchCompany = alumni.getCompany() != null && alumni.getCompany().toLowerCase().contains(query);
                            boolean matchSkills = alumni.getSkills() != null && alumni.getSkills().toLowerCase().contains(query);
                            boolean matchLoc = alumni.getLocation() != null && alumni.getLocation().toLowerCase().contains(query);
                            if (matchName || matchDept || matchCompany || matchSkills || matchLoc) {
                                filtered.add(alumni);
                            }
                        }
                    } else {
                        filtered.addAll(rawList);
                    }

                    layoutError.setVisibility(View.GONE);

                    if (filtered.isEmpty()) {
                        rvMentors.setVisibility(View.GONE);
                        tvEmpty.setVisibility(View.VISIBLE);
                        tvEmpty.setText(searchKeyword != null
                                ? getString(R.string.empty_mentors_filtered)
                                : getString(R.string.empty_mentors));
                    } else {
                        adapter.setAlumniList(filtered);
                        rvMentors.setVisibility(View.VISIBLE);
                        tvEmpty.setVisibility(View.GONE);
                    }
                } else {
                    String errorMsg = ApiErrorUtils.getErrorMessage(response);
                    showError(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<List<Alumni>> call, Throwable t) {
                if (!isAdded()) return;
                progressMentors.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                String errorMsg = ApiErrorUtils.getNetworkErrorMessage(t);
                showError(errorMsg);
            }
        });
    }

    private void showError(String msg) {
        layoutError.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        rvMentors.setVisibility(View.GONE);
        tvErrorMsg.setText(msg);
    }

    private void openMentorDetails(Alumni alumni) {
        if (!isAdded() || getContext() == null) return;
        Intent intent = new Intent(requireContext(), AlumniDetailsActivity.class);
        intent.putExtra("alumni_id", alumni.getId());
        intent.putExtra("alumni_name", alumni.getDisplayName());
        startActivity(intent);
    }
}
