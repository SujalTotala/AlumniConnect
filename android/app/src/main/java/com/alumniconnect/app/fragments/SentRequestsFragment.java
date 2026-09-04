package com.alumniconnect.app.fragments;

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
import com.alumniconnect.app.utils.ApiErrorUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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

    private boolean isActionInProgress = false;

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
                if (!isAdded()) return;

                progressSent.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    List<MentorshipRequest> list = response.body();
                    layoutError.setVisibility(View.GONE);

                    if (list.isEmpty()) {
                        rvSent.setVisibility(View.GONE);
                        tvEmpty.setVisibility(View.VISIBLE);
                        tvEmpty.setText(R.string.empty_sent_requests);
                    } else {
                        adapter.setList(list);
                        rvSent.setVisibility(View.VISIBLE);
                        tvEmpty.setVisibility(View.GONE);
                    }
                } else {
                    String error = ApiErrorUtils.getErrorMessage(response);
                    showError(error);
                }
            }

            @Override
            public void onFailure(Call<List<MentorshipRequest>> call, Throwable t) {
                if (!isAdded()) return;
                progressSent.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                String error = ApiErrorUtils.getNetworkErrorMessage(t);
                showError(error);
            }
        });
    }

    private void showError(String msg) {
        layoutError.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        rvSent.setVisibility(View.GONE);
        tvErrorMsg.setText(msg);
    }

    @Override
    public void onAccept(MentorshipRequest request) {}

    @Override
    public void onReject(MentorshipRequest request) {}

    @Override
    public void onComplete(MentorshipRequest request) {
        if (isActionInProgress || !isAdded()) return;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_complete_mentorship_title)
                .setMessage(R.string.dialog_complete_mentorship_message)
                .setPositiveButton(R.string.dialog_complete_mentorship_positive, (dialog, which) -> {
                    if (isActionInProgress || !isAdded()) return;
                    isActionInProgress = true;
                    progressSent.setVisibility(View.VISIBLE);

                    mentorshipRepository.completeMentorshipRequest(request.getId()).enqueue(new Callback<MentorshipRequest>() {
                        @Override
                        public void onResponse(Call<MentorshipRequest> call, Response<MentorshipRequest> response) {
                            isActionInProgress = false;
                            if (!isAdded()) return;
                            progressSent.setVisibility(View.GONE);
                            if (response.isSuccessful()) {
                                Toast.makeText(requireContext(), "Mentorship marked as COMPLETED!", Toast.LENGTH_SHORT).show();
                                loadSentRequests(false);
                            } else {
                                String error = ApiErrorUtils.getErrorMessage(response);
                                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<MentorshipRequest> call, Throwable t) {
                            isActionInProgress = false;
                            if (!isAdded()) return;
                            progressSent.setVisibility(View.GONE);
                            String error = ApiErrorUtils.getNetworkErrorMessage(t);
                            Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton(R.string.dialog_cancel_negative, null)
                .show();
    }
}
