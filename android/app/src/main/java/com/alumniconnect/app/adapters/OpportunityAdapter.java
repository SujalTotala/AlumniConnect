package com.alumniconnect.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.alumniconnect.app.R;
import com.alumniconnect.app.models.Opportunity;
import java.util.ArrayList;
import java.util.List;

public class OpportunityAdapter extends RecyclerView.Adapter<OpportunityAdapter.ViewHolder> {

    public interface OnOpportunityClickListener {
        void onOpportunityClick(Opportunity opportunity);
    }

    private List<Opportunity> opportunityList = new ArrayList<>();
    private final OnOpportunityClickListener listener;

    public OpportunityAdapter(OnOpportunityClickListener listener) {
        this.listener = listener;
    }

    public void setOpportunityList(List<Opportunity> list) {
        this.opportunityList = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_opportunity, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Opportunity opportunity = opportunityList.get(position);
        holder.bind(opportunity, listener);
    }

    @Override
    public int getItemCount() {
        return opportunityList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle;
        private final TextView tvCompany;
        private final TextView tvTypeBadge;
        private final TextView tvDescSnippet;
        private final TextView tvLocation;
        private final TextView tvDeadlineLabel;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_opp_title);
            tvCompany = itemView.findViewById(R.id.tv_opp_company);
            tvTypeBadge = itemView.findViewById(R.id.tv_opp_type_badge);
            tvDescSnippet = itemView.findViewById(R.id.tv_opp_desc_snippet);
            tvLocation = itemView.findViewById(R.id.tv_opp_location);
            tvDeadlineLabel = itemView.findViewById(R.id.tv_opp_deadline_label);
        }

        void bind(Opportunity opp, OnOpportunityClickListener listener) {
            tvTitle.setText(opp.getTitle());
            tvCompany.setText(opp.getCompany());
            tvTypeBadge.setText(opp.getOpportunityType());

            // Description snippet
            String desc = opp.getDescription();
            tvDescSnippet.setText(desc);

            // Location
            if (Opportunity.hasValue(opp.getLocation())) {
                tvLocation.setText("📍 " + opp.getLocation());
                tvLocation.setVisibility(View.VISIBLE);
            } else {
                tvLocation.setVisibility(View.GONE);
            }

            // Deadline Formatting
            String formattedDeadline = opp.getFormattedDeadline();
            if (Opportunity.hasValue(formattedDeadline)) {
                if (opp.isDeadlinePassed()) {
                    tvDeadlineLabel.setText("Applications Closed");
                    tvDeadlineLabel.setTextColor(tvDeadlineLabel.getContext().getResources().getColor(R.color.error, null));
                } else {
                    tvDeadlineLabel.setText("Apply by " + formattedDeadline);
                    tvDeadlineLabel.setTextColor(tvDeadlineLabel.getContext().getResources().getColor(R.color.text_muted, null));
                }
                tvDeadlineLabel.setVisibility(View.VISIBLE);
            } else {
                tvDeadlineLabel.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onOpportunityClick(opp);
            });
        }
    }
}
