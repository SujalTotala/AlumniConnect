package com.alumniconnect.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.alumniconnect.app.R;
import com.alumniconnect.app.models.ConnectionSuggestion;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

public class ConnectionSuggestionsAdapter extends RecyclerView.Adapter<ConnectionSuggestionsAdapter.ViewHolder> {

    public interface OnSuggestionConnectListener {
        void onConnect(ConnectionSuggestion suggestion);
    }

    private List<ConnectionSuggestion> suggestionList = new ArrayList<>();
    private final OnSuggestionConnectListener listener;

    public ConnectionSuggestionsAdapter(OnSuggestionConnectListener listener) {
        this.listener = listener;
    }

    public void setList(List<ConnectionSuggestion> list) {
        this.suggestionList = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_connection_suggestion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ConnectionSuggestion item = suggestionList.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return suggestionList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvAvatar;
        private final TextView tvName;
        private final TextView tvHeadline;
        private final TextView tvScore;
        private final TextView tvReasons;
        private final MaterialButton btnConnect;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tv_sug_avatar);
            tvName = itemView.findViewById(R.id.tv_sug_name);
            tvHeadline = itemView.findViewById(R.id.tv_sug_headline);
            tvScore = itemView.findViewById(R.id.tv_sug_score);
            tvReasons = itemView.findViewById(R.id.tv_sug_reasons);
            btnConnect = itemView.findViewById(R.id.btn_sug_connect);
        }

        void bind(ConnectionSuggestion sug, OnSuggestionConnectListener listener) {
            String name = sug != null ? sug.getDisplayName() : "Suggested Member";
            String initial = name.length() > 0 ? name.substring(0, 1).toUpperCase() : "?";
            tvAvatar.setText(initial);
            tvName.setText(name);

            StringBuilder sub = new StringBuilder();
            if (sug != null) {
                if (sug.getRole() != null) {
                    sub.append(sug.getRole().toUpperCase());
                }
                if (sug.getCompany() != null && !sug.getCompany().isEmpty()) {
                    sub.append(" • ").append(sug.getCompany());
                } else if (sug.getDepartment() != null && !sug.getDepartment().isEmpty()) {
                    sub.append(" • ").append(sug.getDepartment());
                }
            }
            tvHeadline.setText(sub.length() > 0 ? sub.toString() : "Member");

            tvScore.setText(sug.getScore() + "% Match");

            List<String> reasons = sug.getReasons();
            if (reasons != null && !reasons.isEmpty()) {
                StringBuilder reasonStr = new StringBuilder();
                for (String r : reasons) {
                    if (reasonStr.length() > 0) reasonStr.append(" • ");
                    reasonStr.append(r);
                }
                tvReasons.setText(reasonStr.toString());
                tvReasons.setVisibility(View.VISIBLE);
            } else {
                tvReasons.setVisibility(View.GONE);
            }

            btnConnect.setOnClickListener(v -> {
                if (listener != null) listener.onConnect(sug);
            });
        }
    }
}
