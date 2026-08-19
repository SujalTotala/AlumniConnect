package com.alumniconnect.app.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.alumniconnect.app.R;
import com.alumniconnect.app.models.Notification;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    public interface OnNotificationInteractionListener {
        void onMarkReadClick(Notification notification);
        void onNotificationClick(Notification notification);
    }

    private List<Notification> notificationList = new ArrayList<>();
    private final OnNotificationInteractionListener listener;

    public NotificationAdapter(OnNotificationInteractionListener listener) {
        this.listener = listener;
    }

    public void setNotificationList(List<Notification> list) {
        this.notificationList = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification notification = notificationList.get(position);
        holder.bind(notification, listener);
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView card;
        private final TextView tvIcon;
        private final TextView tvTitle;
        private final TextView tvMessage;
        private final TextView tvTime;
        private final View viewUnreadDot;
        private final TextView btnMarkRead;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.card_notification);
            tvIcon = itemView.findViewById(R.id.tv_notif_icon);
            tvTitle = itemView.findViewById(R.id.tv_notif_title);
            tvMessage = itemView.findViewById(R.id.tv_notif_message);
            tvTime = itemView.findViewById(R.id.tv_notif_time);
            viewUnreadDot = itemView.findViewById(R.id.view_unread_dot);
            btnMarkRead = itemView.findViewById(R.id.btn_notif_mark_read);
        }

        void bind(Notification notif, OnNotificationInteractionListener listener) {
            tvIcon.setText(notif.getTypeEmoji());
            
            // Handle null strings safely
            String title = notif.getTitle();
            tvTitle.setText(title.isEmpty() ? "Notification" : title);
            
            String msg = notif.getMessage();
            tvMessage.setText(msg.isEmpty() ? "No details provided." : msg);
            
            tvTime.setText(notif.getRelativeTime());

            Context context = card.getContext();

            // Visual Styling: Read vs Unread
            if (notif.isRead()) {
                card.setStrokeColor(ColorStateList.valueOf(Color.TRANSPARENT));
                card.setStrokeWidth(0);
                card.setCardBackgroundColor(ColorStateList.valueOf(Color.WHITE));
                card.setAlpha(0.85f);
                viewUnreadDot.setVisibility(View.GONE);
                btnMarkRead.setVisibility(View.GONE);
            } else {
                int primaryColor = context.getResources().getColor(R.color.primary, null);
                card.setStrokeColor(ColorStateList.valueOf(primaryColor));
                card.setStrokeWidth(dpToPx(context, 1.5f));
                card.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#F5F8FF")));
                card.setAlpha(1.0f);
                viewUnreadDot.setVisibility(View.VISIBLE);
                btnMarkRead.setVisibility(View.VISIBLE);
            }

            // Click listener for entire item
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onNotificationClick(notif);
            });

            // Click listener for mark read text button
            btnMarkRead.setOnClickListener(v -> {
                if (listener != null) listener.onMarkReadClick(notif);
            });
        }

        private int dpToPx(Context context, float dp) {
            return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
        }
    }
}
