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

public class ConnectionsAdapter extends RecyclerView.Adapter<ConnectionsAdapter.ViewHolder> {

    public interface OnConnectionClickListener {
        void onRemove(Connection connection);
    }

    private List<Connection> connectionList = new ArrayList<>();
    private final OnConnectionClickListener listener;

    public ConnectionsAdapter(OnConnectionClickListener listener) {
        this.listener = listener;
    }

    public void setList(List<Connection> list) {
        this.connectionList = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_connection, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Connection conn = connectionList.get(position);
        holder.bind(conn, listener);
    }

    @Override
    public int getItemCount() {
        return connectionList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvAvatar;
        private final TextView tvName;
        private final TextView tvHeadline;
        private final TextView tvDate;
        private final MaterialButton btnRemove;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tv_conn_avatar);
            tvName = itemView.findViewById(R.id.tv_conn_name);
            tvHeadline = itemView.findViewById(R.id.tv_conn_headline);
            tvDate = itemView.findViewById(R.id.tv_conn_date);
            btnRemove = itemView.findViewById(R.id.btn_conn_remove);
        }

        void bind(Connection conn, OnConnectionClickListener listener) {
            ConnectionUserSummary other = conn.getOtherUser();
            String name = other != null ? other.getDisplayName() : "Unknown User";
            String initial = name.length() > 0 ? name.substring(0, 1).toUpperCase() : "?";
            tvAvatar.setText(initial);
            tvName.setText(name);

            StringBuilder sub = new StringBuilder();
            if (other != null) {
                if (other.getRole() != null) {
                    sub.append(other.getRole().toUpperCase());
                }
                if (other.getCompany() != null && !other.getCompany().isEmpty()) {
                    sub.append(" • ").append(other.getCompany());
                } else if (other.getDepartment() != null && !other.getDepartment().isEmpty()) {
                    sub.append(" • ").append(other.getDepartment());
                }
            }
            tvHeadline.setText(sub.length() > 0 ? sub.toString() : "Member");

            if (conn.getUpdatedAt() != null && conn.getUpdatedAt().length() >= 10) {
                tvDate.setText("Connected: " + conn.getUpdatedAt().substring(0, 10));
            } else {
                tvDate.setText("Connected");
            }

            btnRemove.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRemove(conn);
                }
            });
        }
    }
}
