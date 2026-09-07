package com.alumniconnect.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.alumniconnect.app.R;
import com.alumniconnect.app.models.SuccessStory;
import java.util.ArrayList;
import java.util.List;

public class SuccessStoriesAdapter extends RecyclerView.Adapter<SuccessStoriesAdapter.ViewHolder> {

    private List<SuccessStory> storyList = new ArrayList<>();

    public void setList(List<SuccessStory> list) {
        this.storyList = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_success_story, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SuccessStory story = storyList.get(position);
        holder.bind(story);
    }

    @Override
    public int getItemCount() {
        return storyList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle;
        private final TextView tvAuthor;
        private final TextView tvBatch;
        private final TextView tvRoleCompany;
        private final TextView tvDate;
        private final TextView tvSummary;
        private final TextView tvContent;
        private boolean isExpanded = false;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_story_title);
            tvAuthor = itemView.findViewById(R.id.tv_story_author);
            tvBatch = itemView.findViewById(R.id.tv_story_batch);
            tvRoleCompany = itemView.findViewById(R.id.tv_story_role_company);
            tvDate = itemView.findViewById(R.id.tv_story_date);
            tvSummary = itemView.findViewById(R.id.tv_story_summary);
            tvContent = itemView.findViewById(R.id.tv_story_content);
        }

        void bind(SuccessStory story) {
            tvTitle.setText(story.getTitle());
            
            String author = story.getAuthorName() != null ? story.getAuthorName() : "Alumni";
            tvAuthor.setText(author);

            if (story.getGraduationYear() != null && story.getGraduationYear() > 0) {
                tvBatch.setText("('" + (story.getGraduationYear() % 100) + ")");
                tvBatch.setVisibility(View.VISIBLE);
            } else {
                tvBatch.setVisibility(View.GONE);
            }

            StringBuilder rc = new StringBuilder();
            if (story.getCurrentRole() != null && !story.getCurrentRole().trim().isEmpty()) {
                rc.append(story.getCurrentRole());
            }
            if (story.getCompany() != null && !story.getCompany().trim().isEmpty()) {
                if (rc.length() > 0) rc.append(" @ ");
                rc.append(story.getCompany());
            }
            if (rc.length() > 0) {
                tvRoleCompany.setText(rc.toString());
                tvRoleCompany.setVisibility(View.VISIBLE);
            } else {
                tvRoleCompany.setVisibility(View.GONE);
            }

            if (story.getCreatedAt() != null) {
                String d = story.getCreatedAt();
                if (d.contains("T")) d = d.substring(0, d.indexOf("T"));
                tvDate.setText(d);
            } else {
                tvDate.setText("");
            }

            if (story.getSummary() != null && !story.getSummary().trim().isEmpty()) {
                tvSummary.setText(story.getSummary());
                tvSummary.setVisibility(View.VISIBLE);
            } else {
                tvSummary.setVisibility(View.GONE);
            }

            if (story.getContent() != null && !story.getContent().trim().isEmpty()) {
                tvContent.setText(story.getContent());
                tvContent.setVisibility(View.VISIBLE);
            } else {
                tvContent.setVisibility(View.GONE);
            }

            // Click to toggle full story
            itemView.setOnClickListener(v -> {
                isExpanded = !isExpanded;
                if (isExpanded) {
                    tvContent.setMaxLines(Integer.MAX_VALUE);
                    tvContent.setEllipsize(null);
                } else {
                    tvContent.setMaxLines(3);
                    tvContent.setEllipsize(android.text.TextUtils.TruncateAt.END);
                }
            });
        }
    }
}
