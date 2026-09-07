package com.alumniconnect.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.alumniconnect.app.R;
import com.alumniconnect.app.models.CommunityPost;
import java.util.ArrayList;
import java.util.List;

public class CommunityPostsAdapter extends RecyclerView.Adapter<CommunityPostsAdapter.ViewHolder> {

    private List<CommunityPost> postList = new ArrayList<>();

    public void setList(List<CommunityPost> list) {
        this.postList = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void addPost(CommunityPost post) {
        if (post != null) {
            postList.add(0, post);
            notifyItemInserted(0);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_community_post, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CommunityPost post = postList.get(position);
        holder.bind(post);
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvAvatar;
        private final TextView tvAuthor;
        private final TextView tvDate;
        private final TextView tvContent;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tv_post_avatar);
            tvAuthor = itemView.findViewById(R.id.tv_post_author);
            tvDate = itemView.findViewById(R.id.tv_post_date);
            tvContent = itemView.findViewById(R.id.tv_post_content);
        }

        void bind(CommunityPost post) {
            String author = post.getAuthorName() != null && !post.getAuthorName().trim().isEmpty()
                    ? post.getAuthorName()
                    : "Member";
            tvAuthor.setText(author);

            String initial = author.length() > 0 ? author.substring(0, 1).toUpperCase() : "U";
            tvAvatar.setText(initial);

            if (post.getCreatedAt() != null) {
                String d = post.getCreatedAt().replace("T", " ");
                if (d.length() > 19) d = d.substring(0, 19);
                tvDate.setText(d);
            } else {
                tvDate.setText("");
            }

            tvContent.setText(post.getContent());
        }
    }
}
