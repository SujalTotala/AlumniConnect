package com.alumniconnect.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.alumniconnect.app.R;
import com.alumniconnect.app.models.EventAttendance;
import java.util.ArrayList;
import java.util.List;

public class EventAttendanceAdapter extends RecyclerView.Adapter<EventAttendanceAdapter.ViewHolder> {

    private List<EventAttendance> attendanceList = new ArrayList<>();

    public void setList(List<EventAttendance> list) {
        this.attendanceList = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event_attendance, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EventAttendance attendance = attendanceList.get(position);
        holder.bind(attendance);
    }

    @Override
    public int getItemCount() {
        return attendanceList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvAvatar;
        private final TextView tvName;
        private final TextView tvEmail;
        private final TextView tvTime;
        private final TextView tvMethod;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tv_att_avatar);
            tvName = itemView.findViewById(R.id.tv_att_name);
            tvEmail = itemView.findViewById(R.id.tv_att_email);
            tvTime = itemView.findViewById(R.id.tv_att_time);
            tvMethod = itemView.findViewById(R.id.tv_att_method);
        }

        void bind(EventAttendance item) {
            String name = item.getUserFullName() != null && !item.getUserFullName().trim().isEmpty()
                    ? item.getUserFullName()
                    : "User #" + item.getUserId();
            tvName.setText(name);

            if (item.getUserEmail() != null && !item.getUserEmail().trim().isEmpty()) {
                tvEmail.setText(item.getUserEmail());
                tvEmail.setVisibility(View.VISIBLE);
            } else {
                tvEmail.setVisibility(View.GONE);
            }

            String firstChar = name.length() > 0 ? name.substring(0, 1).toUpperCase() : "A";
            tvAvatar.setText(firstChar);

            String timeStr = item.getCheckedInAt() != null ? item.getCheckedInAt().replace("T", " ") : "--";
            if (timeStr.length() > 19) {
                timeStr = timeStr.substring(0, 19);
            }
            tvTime.setText("Checked in at: " + timeStr);

            String method = item.getCheckinMethod() != null ? item.getCheckinMethod() : "MANUAL";
            tvMethod.setText(method.replace("_", " "));
            if ("QR_SCAN".equalsIgnoreCase(method)) {
                tvMethod.setBackgroundResource(R.drawable.bg_badge_green);
            } else {
                tvMethod.setBackgroundResource(R.drawable.bg_badge_blue);
            }
        }
    }
}
