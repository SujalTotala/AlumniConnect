package com.alumniconnect.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.alumniconnect.app.R;
import com.alumniconnect.app.models.Event;
import java.util.ArrayList;
import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    public interface OnEventClickListener {
        void onEventClick(Event event);
    }

    private List<Event> eventList = new ArrayList<>();
    private final OnEventClickListener listener;

    public EventAdapter(OnEventClickListener listener) {
        this.listener = listener;
    }

    public void setEventList(List<Event> list) {
        this.eventList = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = eventList.get(position);
        holder.bind(event, listener);
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTypeEmoji;
        private final TextView tvTitle;
        private final TextView tvTypeLabel;
        private final TextView tvRegisteredBadge;
        private final TextView tvDate;
        private final TextView tvTime;
        private final TextView tvLocation;
        private final View rowLocation;
        private final TextView tvOnlineIndicator;
        private final TextView tvRegCount;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTypeEmoji = itemView.findViewById(R.id.tv_event_type_emoji);
            tvTitle = itemView.findViewById(R.id.tv_event_title);
            tvTypeLabel = itemView.findViewById(R.id.tv_event_type_label);
            tvRegisteredBadge = itemView.findViewById(R.id.tv_registered_badge);
            tvDate = itemView.findViewById(R.id.tv_event_date);
            tvTime = itemView.findViewById(R.id.tv_event_time);
            tvLocation = itemView.findViewById(R.id.tv_event_location);
            rowLocation = itemView.findViewById(R.id.row_event_location);
            tvOnlineIndicator = itemView.findViewById(R.id.tv_online_indicator);
            tvRegCount = itemView.findViewById(R.id.tv_reg_count);
        }

        void bind(Event event, OnEventClickListener listener) {
            tvTypeEmoji.setText(event.getTypeEmoji());
            tvTitle.setText(event.getTitle());
            tvTypeLabel.setText(event.getEventType());

            if (event.isRegistered()) {
                tvRegisteredBadge.setVisibility(View.VISIBLE);
            } else {
                tvRegisteredBadge.setVisibility(View.GONE);
            }

            tvDate.setText(event.getFormattedDate());

            String formattedTime = event.getFormattedTime();
            if (Event.hasValue(formattedTime)) {
                tvTime.setText(formattedTime);
                tvTime.setVisibility(View.VISIBLE);
            } else {
                tvTime.setVisibility(View.GONE);
            }

            if (Event.hasValue(event.getLocation())) {
                tvLocation.setText(event.getLocation());
                rowLocation.setVisibility(View.VISIBLE);
            } else {
                rowLocation.setVisibility(View.GONE);
            }

            if (event.isOnline()) {
                tvOnlineIndicator.setVisibility(View.VISIBLE);
            } else {
                tvOnlineIndicator.setVisibility(View.GONE);
            }

            tvRegCount.setText(event.getRegistrationsCount() + " registered");

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onEventClick(event);
            });
        }
    }
}
