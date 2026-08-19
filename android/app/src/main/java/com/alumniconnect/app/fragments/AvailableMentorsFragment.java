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
import com.google.android.material.textfield.TextInputEditText;
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

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (debounceRunnable != null) debounceHandler.removeCallbacks(debounceRunnable);
                debounceRunnable = () -> {
                    searchKeyword = s.toString().trim().isEmpty() ? null : s.toString().trim();
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
    public void onResume() {
        super.onResume();
        loadMentors(false);
    }

    private void loadMentors(boolean fromSwipe) {
        if (!fromSwipe) {
            progressMentors.setVisibility(View.VISIBLE);
            layoutError.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.GONE);
            rvMentors.setVisibility(View.GONE);
        }

        // Available mentors endpoint doesn't support generic 'search' param, but supports 'department', 'company', 'skills'
        // We will pass searchKeyword to department, company, and skills filters (or filter client side for better matching).
        // Since backend has:
        // query = db.query(Alumni).filter(Alumni.mentorship_available == True)
        // and optional params: department, company, skills
        // Let's pass searchKeyword as skills to see if it matches, or simply load all available mentors and do lightweight client side filtering!
        // Client-side filtering is extremely robust and avoids missing mentors if the backend filters are strict AND/OR.
        // Let's do client-side filtering if searchKeyword is present, otherwise get all available mentors.
        // This guarantees maximum correctness and "Wow" user experience!
        
        mentorshipRepository.getMentors(null, null, null).enqueue(new Callback<List<Alumni>>() {
            @Override
            public void onResponse(Call<List<Alumni>> call, Response<List<Alumni>> response) {
                progressMentors.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    List<Alumni> list = response.body();
                    layoutError.setVisibility(View.GONE);

                    // Client side search matching name, department, company, skills, location
                    if (searchKeyword != null) {
                        String query = searchKeyword.toLowerCase();
                        list.removeIf(alumni -> {
                            boolean matchName = alumni.getDisplayName().toLowerCase().contains(query);
                            boolean matchDept = alumni.getDepartment() != null && alumni.getDepartment().toLowerCase().contains(query);
                            boolean matchCompany = alumni.getCompany() != null && alumni.getCompany().toLowerCase().contains(query);
                            boolean matchSkills = alumni.getSkills() != null && alumni.getSkills().toLowerCase().contains(query);
                            boolean matchLoc = alumni.getLocation() != null && alumni.getLocation().toLowerCase().contains(query);
                            return !(matchName || matchDept || matchCompany || matchSkills || matchLoc);
                        });
                    }

                    if (list.isEmpty()) {
                        rvMentors.setVisibility(View.GONE);
                        tvEmpty.setVisibility(View.VISIBLE);
                        tvEmpty.setText(searchKeyword != null 
                                ? "No mentors match your search query." 
                                : "No mentors available right now.");
                    } else {
                        adapter.setAlumniList(list);
                        rvMentors.setVisibility(View.VISIBLE);
                        tvEmpty.setVisibility(View.GONE);
                    }
                } else if (response.code() == 401) {
                    showError("Session expired. Please login again.");
                } else {
                    showError("Server error loading mentors (HTTP " + response.code() + ")");
                }
            }

            @Override
            public void onFailure(Call<List<Alumni>> call, Throwable t) {
                progressMentors.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                showError("Network error. Check connection and retry.");
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
        Intent intent = new Intent(requireContext(), AlumniDetailsActivity.class);
        intent.putExtra("alumni_id", alumni.getId());
        intent.putExtra("alumni_name", alumni.getDisplayName());
        startActivity(intent);
    }
}
