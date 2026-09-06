package com.alumniconnect.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.alumniconnect.app.R;
import com.alumniconnect.app.models.ConnectionUserSummary;
import com.alumniconnect.app.models.ReferralOpportunitySummary;
import com.alumniconnect.app.models.ReferralRequest;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

public class ReferralRequestsAdapter extends RecyclerView.Adapter<ReferralRequestsAdapter.ViewHolder> {

    public interface OnReferralActionListener {
        void onAccept(ReferralRequest request);
        void onDecline(ReferralRequest request);
        void onComplete(ReferralRequest request);
        void onCancel(ReferralRequest request);
    }

    private List<ReferralRequest> requestList = new ArrayList<>();
    private final boolean isSentTab;
    private final OnReferralActionListener listener;

    public ReferralRequestsAdapter(boolean isSentTab, OnReferralActionListener listener) {
        this.isSentTab = isSentTab;
        this.listener = listener;
    }

    public void setList(List<ReferralRequest> list) {
        this.requestList = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_referral_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ReferralRequest req = requestList.get(position);
        holder.bind(req, isSentTab, listener);
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvOppTitle;
        private final TextView tvOppCompany;
        private final TextView tvStatusBadge;
        private final TextView tvPersonAvatar;
        private final TextView tvPersonRoleLabel;
        private final TextView tvPersonName;
        private final TextView tvDate;
        private final View layoutNote;
        private final TextView tvNote;
        private final MaterialButton btnCancel;
        private final MaterialButton btnDecline;
        private final MaterialButton btnAccept;
        private final MaterialButton btnComplete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOppTitle = itemView.findViewById(R.id.tv_ref_opp_title);
            tvOppCompany = itemView.findViewById(R.id.tv_ref_opp_company);
            tvStatusBadge = itemView.findViewById(R.id.tv_ref_status_badge);
            tvPersonAvatar = itemView.findViewById(R.id.tv_ref_person_avatar);
            tvPersonRoleLabel = itemView.findViewById(R.id.tv_ref_person_role_label);
            tvPersonName = itemView.findViewById(R.id.tv_ref_person_name);
            tvDate = itemView.findViewById(R.id.tv_ref_date);
            layoutNote = itemView.findViewById(R.id.layout_ref_note);
            tvNote = itemView.findViewById(R.id.tv_ref_note);
            btnCancel = itemView.findViewById(R.id.btn_ref_cancel);
            btnDecline = itemView.findViewById(R.id.btn_ref_decline);
            btnAccept = itemView.findViewById(R.id.btn_ref_accept);
            btnComplete = itemView.findViewById(R.id.btn_ref_complete);
        }

        void bind(ReferralRequest req, boolean isSent, OnReferralActionListener listener) {
            ReferralOpportunitySummary opp = req.getOpportunity();
            if (opp != null) {
                tvOppTitle.setText(opp.getTitle() != null ? opp.getTitle() : "Opportunity");
                String company = opp.getCompany() != null ? opp.getCompany() : "";
                String location = opp.getLocation() != null ? " • " + opp.getLocation() : "";
                tvOppCompany.setText(company + location);
            } else {
                tvOppTitle.setText("Opportunity #" + req.getOpportunityId());
                tvOppCompany.setText("");
            }

            ConnectionUserSummary person = isSent ? req.getAlumni() : req.getRequester();
            String name = person != null ? person.getDisplayName() : "Unknown User";
            String initial = name.length() > 0 ? name.substring(0, 1).toUpperCase() : "?";
            tvPersonAvatar.setText(initial);
            tvPersonRoleLabel.setText(isSent ? "ALUMNI / REFERRER" : "REQUESTED BY");
            tvPersonName.setText(name + (person != null && person.getRole() != null ? " (" + person.getRole() + ")" : ""));

            if (req.getCreatedAt() != null && req.getCreatedAt().length() >= 10) {
                tvDate.setText(req.getCreatedAt().substring(0, 10));
            } else {
                tvDate.setText("");
            }

            String status = req.getStatus() != null ? req.getStatus().toUpperCase() : "PENDING";
            tvStatusBadge.setText(status);
            if ("ACCEPTED".equals(status)) {
                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_blue);
            } else if ("COMPLETED".equals(status)) {
                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_green);
            } else if ("DECLINED".equals(status)) {
                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_red);
            } else {
                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_amber);
            }

            if (req.getMessage() != null && !req.getMessage().trim().isEmpty()) {
                layoutNote.setVisibility(View.VISIBLE);
                tvNote.setText(req.getMessage());
            } else {
                layoutNote.setVisibility(View.GONE);
            }

            // Button visibility based on role & status
            btnCancel.setVisibility(View.GONE);
            btnDecline.setVisibility(View.GONE);
            btnAccept.setVisibility(View.GONE);
            btnComplete.setVisibility(View.GONE);

            if (isSent) {
                if ("PENDING".equals(status)) {
                    btnCancel.setVisibility(View.VISIBLE);
                    btnCancel.setOnClickListener(v -> {
                        if (listener != null) listener.onCancel(req);
                    });
                }
            } else {
                // Alumni received request
                if ("PENDING".equals(status)) {
                    btnAccept.setVisibility(View.VISIBLE);
                    btnDecline.setVisibility(View.VISIBLE);
                    btnAccept.setOnClickListener(v -> {
                        if (listener != null) listener.onAccept(req);
                    });
                    btnDecline.setOnClickListener(v -> {
                        if (listener != null) listener.onDecline(req);
                    });
                } else if ("ACCEPTED".equals(status)) {
                    btnComplete.setVisibility(View.VISIBLE);
                    btnComplete.setOnClickListener(v -> {
                        if (listener != null) listener.onComplete(req);
                    });
                }
            }
        }
    }
}
