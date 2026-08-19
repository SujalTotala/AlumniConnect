package com.alumniconnect.app.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
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
import com.alumniconnect.app.adapters.MentorshipRequestAdapter;
import com.alumniconnect.app.models.MentorshipRequest;
import com.alumniconnect.app.repositories.MentorshipRepository;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SentRequestsFragment extends Fragment implements MentorshipRequestAdapter.OnRequestActionListener {

    private MentorshipRepository mentorshipRepository;
    private MentorshipRequestAdapter adapter;

    private ProgressBar progressSent;
    private TextView tvEmpty;
    private RecyclerView rvSent;
    private SwipeRefreshLayout swipeRefresh;
    private View layoutError;
    private TextView tvErrorMsg;
    private View btnRetry;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sent_requests, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mentorshipRepository = new MentorshipRepository(requireContext());

        progressSent = view.findViewById(R.id.progress_sent);
        tvEmpty = view.findViewById(R.id.tv_sent_empty);
        rvSent = view.findViewById(R.id.rv_sent_requests);
        swipeRefresh = view.findViewById(R.id.swipe_refresh_sent);
        layoutError = view.findViewById(R.id.layout_sent_error);
        tvErrorMsg = view.findViewById(R.id.tv_sent_error_msg);
        btnRetry = view.findViewById(R.id.btn_sent_retry);

        adapter = new MentorshipRequestAdapter(true, this);
        rvSent.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSent.setAdapter(adapter);

        swipeRefresh.setColorSchemeResources(R.color.primary);
        swipeRefresh.setOnRefreshListener(() -> loadSentRequests(true));

        btnRetry.setOnClickListener(v -> loadSentRequests(false));

        loadSentRequests(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadSentRequests(false);
    }

    private void loadSentRequests(boolean fromSwipe) {
        if (!fromSwipe) {
            progressSent.setVisibility(View.VISIBLE);
            layoutError.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.GONE);
            rvSent.setVisibility(View.GONE);
        }

        mentorshipRepository.getSentRequests().enqueue(new Callback<List<MentorshipRequest>>() {
            @Override
            public void onResponse(Call<List<MentorshipRequest>> call, Response<List<MentorshipRequest>> response) {
                progressSent.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    List<MentorshipRequest> list = response.body();
                    layoutError.setVisibility(View.GONE);

                    if (list.isEmpty()) {
                        rvSent.setVisibility(View.GONE);
                        tvEmpty.setVisibility(View.VISIBLE);
                    } else {
                        adapter.setList(list);
                        rvSent.setVisibility(View.VISIBLE);
                        tvEmpty.setVisibility(View.GONE);
                    }
                } else {
                    showError("Failed to load sent requests (HTTP " + response.code() + ")");
                }
            }

            @Override
            public void onFailure(Call<List<MentorshipRequest>> call, Throwable t) {
                progressSent.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                showError("Network error. Swipe down to retry.");
            }
        });
    }

    private void showError(String msg) {
        layoutError.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        rvSent.setVisibility(View.GONE);
        tvErrorMsg.setText(msg);
    }

    // Callbacks from Adapter actions
    @Override
    public void onAccept(MentorshipRequest request) {
        // Not used on Sent tab
    }

    @Override
    public void onReject(MentorshipRequest request) {
        // Not used on Sent tab
    }

    @Override
    public void onComplete(MentorshipRequest request) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Complete Mentorship")
                .setMessage("Are you sure you want to mark this mentorship as completed?")
                .setPositiveButton("Complete", (dialog, which) -> {
                    progressSent.setVisibility(View.VISIBLE);
                    mentorshipRepository.completeMentorshipRequest(request.getId()).enqueue(new Callback<MentorshipRequest>() {
                        @Override
                        public void onResponse(Call<MentorshipRequest> call, Response<MentorshipRequest> response) {
                            progressSent.setVisibility(View.GONE);
                            if (response.isSuccessful()) {
                                Toast.makeText(requireContext(), "Mentorship marked as COMPLETED!", Toast.LENGTH_SHORT).show();
                                loadSentRequests(false);
                            } else {
                                Toast.makeText(requireContext(), "Failed to complete request.", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<MentorshipRequest> call, Throwable t) {
                            progressSent.setVisibility(View.GONE);
                            Toast.makeText(requireContext(), "Network error completing request.", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
