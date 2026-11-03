package com.alumniconnect.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.alumniconnect.app.R;
import com.alumniconnect.app.models.Connection;
import com.alumniconnect.app.models.ConnectionUserSummary;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

public class ConnectionRequestsAdapter extends RecyclerView.Adapter<ConnectionRequestsAdapter.ViewHolder> {

    public interface OnRequestActionListener {
        void onAccept(Connection connection);
        void onDecline(Connection connection);
        void onCancel(Connection connection);
    }

    private List<Connection> requestList = new ArrayList<>();
    private final boolean isSentTab;
    private final OnRequestActionListener listener;

    public ConnectionRequestsAdapter(boolean isSentTab, OnRequestActionListener listener) {
        this.isSentTab = isSentTab;
        this.listener = listener;
    }

    public void setList(List<Connection> list) {
        this.requestList = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_connection_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Connection req = requestList.get(position);
        holder.bind(req, isSentTab, listener);
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvAvatar;
        private final TextView tvName;
        private final TextView tvHeadline;
        private final TextView tvDate;
        private final TextView tvStatusBadge;
        private final MaterialButton btnCancel;
        private final MaterialButton btnDecline;
        private final MaterialButton btnAccept;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tv_req_avatar);
            tvName = itemView.findViewById(R.id.tv_req_name);
            tvHeadline = itemView.findViewById(R.id.tv_req_headline);
            tvDate = itemView.findViewById(R.id.tv_req_date);
            tvStatusBadge = itemView.findViewById(R.id.tv_req_status_badge);
            btnCancel = itemView.findViewById(R.id.btn_req_cancel);
            btnDecline = itemView.findViewById(R.id.btn_req_decline);
            btnAccept = itemView.findViewById(R.id.btn_req_accept);
        }

        void bind(Connection req, boolean isSent, OnRequestActionListener listener) {
            ConnectionUserSummary user = isSent ? req.getReceiver() : req.getSender();
            String name = user != null ? user.getDisplayName() : "Unknown User";
            String initial = name.length() > 0 ? name.substring(0, 1).toUpperCase() : "?";
            tvAvatar.setText(initial);
            tvName.setText(name);

            StringBuilder sub = new StringBuilder();
            if (user != null) {
                if (user.getRole() != null) {
                    sub.append(user.getRole().toUpperCase());
                }
                if (user.getCompany() != null && !user.getCompany().isEmpty()) {
                    sub.append(" • ").append(user.getCompany());
                } else if (user.getDepartment() != null && !user.getDepartment().isEmpty()) {
                    sub.append(" • ").append(user.getDepartment());
                }
            }
            tvHeadline.setText(sub.length() > 0 ? sub.toString() : "Member");

            if (req.getCreatedAt() != null && req.getCreatedAt().length() >= 10) {
                tvDate.setText((isSent ? "Sent: " : "Received: ") + req.getCreatedAt().substring(0, 10));
            } else {
                tvDate.setText(isSent ? "Sent" : "Received");
            }

            tvStatusBadge.setText(req.getStatus() != null ? req.getStatus().toUpperCase() : "PENDING");

            if (isSent) {
                btnAccept.setVisibility(View.GONE);
                btnDecline.setVisibility(View.GONE);
                btnCancel.setVisibility(View.VISIBLE);
                btnCancel.setOnClickListener(v -> {
                    if (listener != null) listener.onCancel(req);
                });
            } else {
                btnCancel.setVisibility(View.GONE);
                btnAccept.setVisibility(View.VISIBLE);
                btnDecline.setVisibility(View.VISIBLE);
                btnAccept.setOnClickListener(v -> {
                    if (listener != null) listener.onAccept(req);
                });
                btnDecline.setOnClickListener(v -> {
                    if (listener != null) listener.onDecline(req);
                });
            }
        }
    }
}
