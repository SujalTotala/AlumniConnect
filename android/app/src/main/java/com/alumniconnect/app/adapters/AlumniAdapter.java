package com.alumniconnect.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.alumniconnect.app.R;
import com.alumniconnect.app.models.Alumni;
import java.util.ArrayList;
import java.util.List;

public class AlumniAdapter extends RecyclerView.Adapter<AlumniAdapter.AlumniViewHolder> {

    public interface OnAlumniClickListener {
        void onAlumniClick(Alumni alumni);
    }

    private List<Alumni> alumniList = new ArrayList<>();
    private final OnAlumniClickListener listener;

    public AlumniAdapter(OnAlumniClickListener listener) {
        this.listener = listener;
    }

    public void setAlumniList(List<Alumni> list) {
        this.alumniList = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    public int getAlumniCount() {
        return alumniList.size();
    }

    @NonNull
    @Override
    public AlumniViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alumni, parent, false);
        return new AlumniViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlumniViewHolder holder, int position) {
        Alumni alumni = alumniList.get(position);
        holder.bind(alumni, listener);
    }

    @Override
    public int getItemCount() {
        return alumniList.size();
    }

    static class AlumniViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvInitials;
        private final TextView tvName;
        private final TextView tvHeadline;
        private final TextView tvMentorBadge;
        private final TextView tvChipDept;
        private final TextView tvChipYear;
        private final TextView tvChipLocation;
        private final TextView tvSkills;

        AlumniViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInitials = itemView.findViewById(R.id.tv_alumni_initials);
            tvName = itemView.findViewById(R.id.tv_alumni_name);
            tvHeadline = itemView.findViewById(R.id.tv_alumni_headline);
            tvMentorBadge = itemView.findViewById(R.id.tv_mentorship_badge);
            tvChipDept = itemView.findViewById(R.id.tv_chip_dept);
            tvChipYear = itemView.findViewById(R.id.tv_chip_year);
            tvChipLocation = itemView.findViewById(R.id.tv_chip_location);
            tvSkills = itemView.findViewById(R.id.tv_alumni_skills);
        }

        void bind(Alumni alumni, OnAlumniClickListener listener) {
            // Avatar initials
            tvInitials.setText(alumni.getInitials());

            // Name
            tvName.setText(alumni.getDisplayName());

            // Headline (Job @ Company or dept)
            String headline = alumni.getHeadline();
            if (Alumni.hasValue(headline)) {
                tvHeadline.setText(headline);
                tvHeadline.setVisibility(View.VISIBLE);
            } else {
                tvHeadline.setVisibility(View.GONE);
            }

            // Mentorship badge
            if (alumni.isMentorshipAvailable()) {
                tvMentorBadge.setVisibility(View.VISIBLE);
            } else {
                tvMentorBadge.setVisibility(View.GONE);
            }

            // Department chip
            if (Alumni.hasValue(alumni.getDepartment())) {
                tvChipDept.setText(alumni.getDepartment());
                tvChipDept.setVisibility(View.VISIBLE);
            } else {
                tvChipDept.setVisibility(View.GONE);
            }

            // Graduation year chip
            if (Alumni.hasValue(alumni.getGraduationYear())) {
                tvChipYear.setText(alumni.getGraduationYear());
                tvChipYear.setVisibility(View.VISIBLE);
            } else {
                tvChipYear.setVisibility(View.GONE);
            }

            // Location chip
            if (Alumni.hasValue(alumni.getLocation())) {
                tvChipLocation.setText("📍 " + alumni.getLocation());
                tvChipLocation.setVisibility(View.VISIBLE);
            } else {
                tvChipLocation.setVisibility(View.GONE);
            }

            // Skills preview
            if (Alumni.hasValue(alumni.getSkills())) {
                tvSkills.setText("Skills: " + alumni.getSkills());
                tvSkills.setVisibility(View.VISIBLE);
            } else {
                tvSkills.setVisibility(View.GONE);
            }

            // Click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onAlumniClick(alumni);
            });
        }
    }
}
