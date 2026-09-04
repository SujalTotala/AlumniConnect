package com.alumniconnect.app.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.alumniconnect.app.R;
import com.alumniconnect.app.models.Bookmark;

import java.util.ArrayList;
import java.util.List;

public class SavedItemAdapter extends RecyclerView.Adapter<SavedItemAdapter.ViewHolder> {

    public interface OnSavedItemClickListener {
        void onItemClick(Bookmark bookmark);
        void onUnsaveClick(Bookmark bookmark);
    }

    private final Context context;
    private final List<Bookmark> items = new ArrayList<>();
    private final OnSavedItemClickListener listener;

    public SavedItemAdapter(Context context, OnSavedItemClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setItems(List<Bookmark> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public void removeItem(Bookmark bookmark) {
        int index = -1;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId() == bookmark.getId()) {
                index = i;
                break;
            }
        }
        if (index >= 0) {
            items.remove(index);
            notifyItemRemoved(index);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_saved, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Bookmark item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTypeBadge;
        private final TextView tvTitle;
        private final TextView tvSubtitle;
        private final TextView tvDate;
        private final TextView btnUnsave;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTypeBadge = itemView.findViewById(R.id.tv_saved_type_badge);
            tvTitle = itemView.findViewById(R.id.tv_saved_title);
            tvSubtitle = itemView.findViewById(R.id.tv_saved_subtitle);
            tvDate = itemView.findViewById(R.id.tv_saved_date);
            btnUnsave = itemView.findViewById(R.id.btn_unsave);
        }

        void bind(Bookmark bookmark) {
            String type = bookmark.getItemType().toUpperCase();
            tvTypeBadge.setText(type);

            // Style badge by type
            if ("ALUMNI".equals(type)) {
                tvTypeBadge.setBackgroundResource(R.drawable.bg_badge_blue);
            } else if ("OPPORTUNITY".equals(type)) {
                tvTypeBadge.setBackgroundResource(R.drawable.bg_badge_green);
            } else if ("EVENT".equals(type)) {
                tvTypeBadge.setBackgroundResource(R.drawable.bg_badge_amber);
            } else {
                tvTypeBadge.setBackgroundResource(R.drawable.bg_badge_blue);
            }

            tvTitle.setText(bookmark.getItemTitle());
            String subtitle = bookmark.getItemSubtitle();
            if (subtitle != null && !subtitle.trim().isEmpty()) {
                tvSubtitle.setVisibility(View.VISIBLE);
                tvSubtitle.setText(subtitle);
            } else {
                tvSubtitle.setVisibility(View.GONE);
            }

            String dateStr = bookmark.getCreatedAt();
            if (dateStr != null && dateStr.length() >= 10) {
                tvDate.setText("Saved on " + dateStr.substring(0, 10));
            } else {
                tvDate.setText("Saved");
            }

            btnUnsave.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUnsaveClick(bookmark);
                }
            });

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(bookmark);
                }
            });
        }
    }
}
