package com.alumniconnect.app.adapters;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.alumniconnect.app.R;
import com.alumniconnect.app.models.Achievement;
import java.util.ArrayList;
import java.util.List;

public class AchievementsAdapter extends RecyclerView.Adapter<AchievementsAdapter.ViewHolder> {

    private List<Achievement> achievementList = new ArrayList<>();

    public void setList(List<Achievement> list) {
        this.achievementList = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_achievement, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Achievement item = achievementList.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return achievementList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvCategory;
        private final TextView tvStatus;
        private final TextView tvTitle;
        private final TextView tvIssuer;
        private final TextView tvDate;
        private final TextView tvDesc;
        private final TextView tvLink;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tv_ach_category);
            tvStatus = itemView.findViewById(R.id.tv_ach_status);
            tvTitle = itemView.findViewById(R.id.tv_ach_title);
            tvIssuer = itemView.findViewById(R.id.tv_ach_issuer);
            tvDate = itemView.findViewById(R.id.tv_ach_date);
            tvDesc = itemView.findViewById(R.id.tv_ach_desc);
            tvLink = itemView.findViewById(R.id.tv_ach_link);
        }

        void bind(Achievement item) {
            String cat = item.getCategory() != null ? item.getCategory().toUpperCase() : "HONOR";
            tvCategory.setText(cat.replace("_", " "));

            String status = item.getStatus() != null ? item.getStatus().toUpperCase() : "APPROVED";
            tvStatus.setText(status);
            if ("APPROVED".equals(status)) {
                tvStatus.setBackgroundResource(R.drawable.bg_badge_green);
            } else if ("PENDING".equals(status)) {
                tvStatus.setBackgroundResource(R.drawable.bg_badge_amber);
            } else {
                tvStatus.setBackgroundResource(R.drawable.bg_badge_red);
            }

            tvTitle.setText(item.getTitle());

            if (item.getIssuer() != null && !item.getIssuer().trim().isEmpty()) {
                tvIssuer.setText("Issued by: " + item.getIssuer());
                tvIssuer.setVisibility(View.VISIBLE);
            } else {
                tvIssuer.setVisibility(View.GONE);
            }

            if (item.getDate() != null && !item.getDate().trim().isEmpty()) {
                tvDate.setText("• " + item.getDate());
                tvDate.setVisibility(View.VISIBLE);
            } else {
                tvDate.setVisibility(View.GONE);
            }

            if (item.getDescription() != null && !item.getDescription().trim().isEmpty()) {
                tvDesc.setText(item.getDescription());
                tvDesc.setVisibility(View.VISIBLE);
            } else {
                tvDesc.setVisibility(View.GONE);
            }

            if (item.getUrl() != null && !item.getUrl().trim().isEmpty()) {
                tvLink.setVisibility(View.VISIBLE);
                tvLink.setOnClickListener(v -> {
                    try {
                        String url = item.getUrl();
                        if (!url.startsWith("http://") && !url.startsWith("https://")) {
                            url = "https://" + url;
                        }
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        itemView.getContext().startActivity(browserIntent);
                    } catch (Exception ignored) {
                    }
                });
            } else {
                tvLink.setVisibility(View.GONE);
            }
        }
    }
}
