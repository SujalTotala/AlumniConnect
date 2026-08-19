package com.alumniconnect.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.alumniconnect.app.R;
import com.alumniconnect.app.models.Alumni;
import com.alumniconnect.app.models.MentorshipRequest;
import java.util.ArrayList;
import java.util.List;

public class MentorshipRequestAdapter extends RecyclerView.Adapter<MentorshipRequestAdapter.ViewHolder> {

    public interface OnRequestActionListener {
        void onAccept(MentorshipRequest request);
        void onReject(MentorshipRequest request);
        void onComplete(MentorshipRequest request);
    }

    private List<MentorshipRequest> requestList = new ArrayList<>();
    private final boolean isSentTab;      // true if outgoing (student viewing sent requests)
    private final OnRequestActionListener listener;

    public MentorshipRequestAdapter(boolean isSentTab, OnRequestActionListener listener) {
        this.isSentTab = isSentTab;
        this.listener = listener;
    }

    public void setList(List<MentorshipRequest> list) {
        this.requestList = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mentorship_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MentorshipRequest request = requestList.get(position);
        holder.bind(request, isSentTab, listener);
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvInitial;
        private final TextView tvRoleLabel;
        private final TextView tvName;
        private final TextView tvStatusBadge;
        private final TextView tvMessage;
        private final View layoutResponseNote;
        private final TextView tvResponseNote;
        private final TextView tvDate;
        private final View layoutActions;
        private final View btnAccept;
        private final View btnReject;
        private final View btnComplete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInitial = itemView.findViewById(R.id.tv_mr_initial);
            tvRoleLabel = itemView.findViewById(R.id.tv_mr_role_label);
            tvName = itemView.findViewById(R.id.tv_mr_name);
            tvStatusBadge = itemView.findViewById(R.id.tv_mr_status_badge);
            tvMessage = itemView.findViewById(R.id.tv_mr_message);
            layoutResponseNote = itemView.findViewById(R.id.layout_mr_response_note);
            tvResponseNote = itemView.findViewById(R.id.tv_mr_response_note);
            tvDate = itemView.findViewById(R.id.tv_mr_date);
            layoutActions = itemView.findViewById(R.id.layout_mr_actions);
            btnAccept = itemView.findViewById(R.id.btn_mr_accept);
            btnReject = itemView.findViewById(R.id.btn_mr_reject);
            btnComplete = itemView.findViewById(R.id.btn_mr_complete);
        }

        void bind(MentorshipRequest req, boolean isSent, OnRequestActionListener listener) {
            // Renders initial
            String nameToUse = isSent ? req.getMentorName() : req.getStudentName();
            String initial = "?";
            if (nameToUse != null && !nameToUse.trim().isEmpty()) {
                initial = String.valueOf(nameToUse.trim().charAt(0)).toUpperCase();
            }
            tvInitial.setText(initial);

            // Labels
            tvRoleLabel.setText(isSent ? "MENTOR" : "STUDENT");
            tvName.setText(nameToUse);

            // Status badge color and label
            String status = req.getStatus().toUpperCase();
            tvStatusBadge.setText(status);

            int bgRes = R.drawable.bg_chip_grey;
            int textColor = tvStatusBadge.getContext().getResources().getColor(R.color.text_secondary, null);

            switch (status) {
                case "PENDING":
                    bgRes = R.drawable.bg_badge_amber;
                    textColor = tvStatusBadge.getContext().getResources().getColor(R.color.white, null);
                    break;
                case "ACCEPTED":
                    bgRes = R.drawable.bg_badge_green;
                    textColor = tvStatusBadge.getContext().getResources().getColor(R.color.white, null);
                    break;
                case "REJECTED":
                    bgRes = R.drawable.bg_badge_red;
                    textColor = tvStatusBadge.getContext().getResources().getColor(R.color.white, null);
                    break;
                case "COMPLETED":
                    bgRes = R.drawable.bg_badge_blue;
                    textColor = tvStatusBadge.getContext().getResources().getColor(R.color.white, null);
                    break;
            }
            tvStatusBadge.setBackgroundResource(bgRes);
            tvStatusBadge.setTextColor(textColor);

            // Request message
            tvMessage.setText(req.getMessage());

            // Response note
            if (Alumni.hasValue(req.getResponseNote())) {
                tvResponseNote.setText(req.getResponseNote());
                layoutResponseNote.setVisibility(View.VISIBLE);
            } else {
                layoutResponseNote.setVisibility(View.GONE);
            }

            // Date
            String dateText = req.getCreatedAt();
            if (dateText != null && dateText.length() >= 10) {
                tvDate.setText("Sent on: " + dateText.substring(0, 10));
            } else {
                tvDate.setText("");
            }

            // Action Buttons
            layoutActions.setVisibility(View.GONE);
            btnAccept.setVisibility(View.GONE);
            btnReject.setVisibility(View.GONE);
            btnComplete.setVisibility(View.GONE);

            if (!isSent) {
                // Incoming request (Alumni role viewing student's request)
                if ("PENDING".equalsIgnoreCase(req.getStatus())) {
                    layoutActions.setVisibility(View.VISIBLE);
                    btnAccept.setVisibility(View.VISIBLE);
                    btnReject.setVisibility(View.VISIBLE);

                    btnAccept.setOnClickListener(v -> {
                        if (listener != null) listener.onAccept(req);
                    });
                    btnReject.setOnClickListener(v -> {
                        if (listener != null) listener.onReject(req);
                    });
                } else if ("ACCEPTED".equalsIgnoreCase(req.getStatus())) {
                    layoutActions.setVisibility(View.VISIBLE);
                    btnComplete.setVisibility(View.VISIBLE);

                    btnComplete.setOnClickListener(v -> {
                        if (listener != null) listener.onComplete(req);
                    });
                }
            } else {
                // Student viewing sent requests.
                // Student may complete an active accepted mentorship too if backend allows.
                if ("ACCEPTED".equalsIgnoreCase(req.getStatus())) {
                    layoutActions.setVisibility(View.VISIBLE);
                    btnComplete.setVisibility(View.VISIBLE);

                    btnComplete.setOnClickListener(v -> {
                        if (listener != null) listener.onComplete(req);
                    });
                }
            }
        }
    }
}
