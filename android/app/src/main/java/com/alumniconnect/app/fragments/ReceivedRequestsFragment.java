package com.alumniconnect.app.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReceivedRequestsFragment extends Fragment implements MentorshipRequestAdapter.OnRequestActionListener {

    private MentorshipRepository mentorshipRepository;
    private MentorshipRequestAdapter adapter;

    private ProgressBar progressReceived;
    private TextView tvEmpty;
    private RecyclerView rvReceived;
    private SwipeRefreshLayout swipeRefresh;
    private View layoutError;
    private TextView tvErrorMsg;
    private View btnRetry;

    private boolean isActionInProgress = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_received_requests, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mentorshipRepository = new MentorshipRepository(requireContext());

        progressReceived = view.findViewById(R.id.progress_received);
        tvEmpty = view.findViewById(R.id.tv_received_empty);
        rvReceived = view.findViewById(R.id.rv_received_requests);
        swipeRefresh = view.findViewById(R.id.swipe_refresh_received);
        layoutError = view.findViewById(R.id.layout_received_error);
        tvErrorMsg = view.findViewById(R.id.tv_received_error_msg);
        btnRetry = view.findViewById(R.id.btn_received_retry);

        adapter = new MentorshipRequestAdapter(false, this);
        rvReceived.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvReceived.setAdapter(adapter);

        swipeRefresh.setColorSchemeResources(R.color.primary);
        swipeRefresh.setOnRefreshListener(() -> loadReceivedRequests(true));

        btnRetry.setOnClickListener(v -> loadReceivedRequests(false));

        loadReceivedRequests(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadReceivedRequests(false);
    }

    private void loadReceivedRequests(boolean fromSwipe) {
        if (!fromSwipe) {
            progressReceived.setVisibility(View.VISIBLE);
            layoutError.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.GONE);
            rvReceived.setVisibility(View.GONE);
        }

        mentorshipRepository.getReceivedRequests().enqueue(new Callback<List<MentorshipRequest>>() {
            @Override
            public void onResponse(Call<List<MentorshipRequest>> call, Response<List<MentorshipRequest>> response) {
                progressReceived.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    List<MentorshipRequest> list = response.body();
                    layoutError.setVisibility(View.GONE);

                    if (list.isEmpty()) {
                        rvReceived.setVisibility(View.GONE);
                        tvEmpty.setVisibility(View.VISIBLE);
                    } else {
                        adapter.setList(list);
                        rvReceived.setVisibility(View.VISIBLE);
                        tvEmpty.setVisibility(View.GONE);
                    }
                } else {
                    String error = ApiErrorUtils.getErrorMessage(response);
                    showError(error);
                }
            }

            @Override
            public void onFailure(Call<List<MentorshipRequest>> call, Throwable t) {
                progressReceived.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                String error = ApiErrorUtils.getNetworkErrorMessage(t);
                showError(error);
            }
        });
    }

    private void showError(String msg) {
        layoutError.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        rvReceived.setVisibility(View.GONE);
        tvErrorMsg.setText(msg);
    }

    // Callbacks from Adapter actions
    @Override
    public void onAccept(MentorshipRequest request) {
        if (isActionInProgress) return;
        showResponseNoteDialog(request, true);
    }

    @Override
    public void onReject(MentorshipRequest request) {
        if (isActionInProgress) return;
        showResponseNoteDialog(request, false);
    }

    @Override
    public void onComplete(MentorshipRequest request) {
        if (isActionInProgress) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Complete Mentorship")
                .setMessage("Are you sure you want to mark this mentorship request as completed?")
                .setPositiveButton("Complete", (dialog, which) -> {
                    if (isActionInProgress) return;
                    isActionInProgress = true;
                    progressReceived.setVisibility(View.VISIBLE);
                    mentorshipRepository.completeMentorshipRequest(request.getId()).enqueue(new Callback<MentorshipRequest>() {
                        @Override
                        public void onResponse(Call<MentorshipRequest> call, Response<MentorshipRequest> response) {
                            isActionInProgress = false;
                            progressReceived.setVisibility(View.GONE);
                            if (response.isSuccessful()) {
                                Toast.makeText(requireContext(), "Mentorship completed successfully!", Toast.LENGTH_SHORT).show();
                                loadReceivedRequests(false);
                            } else {
                                String error = ApiErrorUtils.getErrorMessage(response);
                                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<MentorshipRequest> call, Throwable t) {
                            isActionInProgress = false;
                            progressReceived.setVisibility(View.GONE);
                            String error = ApiErrorUtils.getNetworkErrorMessage(t);
                            Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showResponseNoteDialog(MentorshipRequest request, boolean isAccept) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(isAccept ? "Accept Mentorship Request" : "Reject Mentorship Request");

        final EditText input = new EditText(requireContext());
        input.setHint("Write a response note (optional)");
        input.setPadding(32, 16, 32, 16);
        builder.setView(input);

        builder.setPositiveButton(isAccept ? "Accept" : "Reject", (dialog, which) -> {
            if (isActionInProgress) return;
            isActionInProgress = true;

            String note = input.getText().toString().trim();
            progressReceived.setVisibility(View.VISIBLE);

            Callback<MentorshipRequest> apiCallback = new Callback<MentorshipRequest>() {
                @Override
                public void onResponse(Call<MentorshipRequest> call, Response<MentorshipRequest> response) {
                    isActionInProgress = false;
                    progressReceived.setVisibility(View.GONE);
                    if (response.isSuccessful()) {
                        Toast.makeText(requireContext(), 
                                isAccept ? "Request accepted!" : "Request rejected.", 
                                Toast.LENGTH_SHORT).show();
                        loadReceivedRequests(false);
                    } else {
                        String error = ApiErrorUtils.getErrorMessage(response);
                        Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<MentorshipRequest> call, Throwable t) {
                    isActionInProgress = false;
                    progressReceived.setVisibility(View.GONE);
                    String error = ApiErrorUtils.getNetworkErrorMessage(t);
                    Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
                }
            };

            if (isAccept) {
                mentorshipRepository.acceptMentorshipRequest(request.getId(), note).enqueue(apiCallback);
            } else {
                mentorshipRepository.rejectMentorshipRequest(request.getId(), note).enqueue(apiCallback);
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}
