package com.alumniconnect.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.alumniconnect.app.R;
import com.alumniconnect.app.models.Community;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

public class CommunitiesAdapter extends RecyclerView.Adapter<CommunitiesAdapter.ViewHolder> {

    public interface OnCommunityClickListener {
        void onCommunityClick(Community community);
        void onJoinLeaveClick(Community community);
    }

    private List<Community> communityList = new ArrayList<>();
    private final OnCommunityClickListener listener;

    public CommunitiesAdapter(OnCommunityClickListener listener) {
        this.listener = listener;
    }

    public void setList(List<Community> list) {
        this.communityList = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_community, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Community community = communityList.get(position);
        holder.bind(community, listener);
    }

    @Override
    public int getItemCount() {
        return communityList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTypeBadge;
        private final TextView tvMembersCount;
        private final TextView tvName;
        private final TextView tvDesc;
        private final TextView tvStatusBadge;
        private final MaterialButton btnAction;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTypeBadge = itemView.findViewById(R.id.tv_comm_type_badge);
            tvMembersCount = itemView.findViewById(R.id.tv_comm_members_count);
            tvName = itemView.findViewById(R.id.tv_comm_name);
            tvDesc = itemView.findViewById(R.id.tv_comm_desc);
            tvStatusBadge = itemView.findViewById(R.id.tv_comm_status_badge);
            btnAction = itemView.findViewById(R.id.btn_comm_action);
        }

        void bind(Community community, OnCommunityClickListener listener) {
            String type = community.getType() != null ? community.getType().toUpperCase() : "CHAPTER";
            tvTypeBadge.setText(type.replace("_", " "));

            int count = community.getMemberCount() != null ? community.getMemberCount() : 0;
            tvMembersCount.setText("👥 " + count + " " + (count == 1 ? "member" : "members"));

            tvName.setText(community.getName());

            if (community.getDescription() != null && !community.getDescription().trim().isEmpty()) {
                tvDesc.setText(community.getDescription());
                tvDesc.setVisibility(View.VISIBLE);
            } else {
                tvDesc.setVisibility(View.GONE);
            }

            boolean isMember = Boolean.TRUE.equals(community.isMember());
            if (isMember) {
                tvStatusBadge.setVisibility(View.VISIBLE);
                btnAction.setText("Leave");
                btnAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        itemView.getContext().getResources().getColor(R.color.error)));
            } else {
                tvStatusBadge.setVisibility(View.GONE);
                btnAction.setText("Join Hub");
                btnAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        itemView.getContext().getResources().getColor(R.color.primary)));
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onCommunityClick(community);
            });

            btnAction.setOnClickListener(v -> {
                if (listener != null) listener.onJoinLeaveClick(community);
            });
        }
    }
}
