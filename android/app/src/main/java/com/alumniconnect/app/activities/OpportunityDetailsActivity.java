package com.alumniconnect.app.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.alumniconnect.app.R;
import com.alumniconnect.app.models.Opportunity;
import com.alumniconnect.app.repositories.OpportunityRepository;
import com.alumniconnect.app.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OpportunityDetailsActivity extends AppCompatActivity {

    private OpportunityRepository opportunityRepository;
    private SessionManager sessionManager;

    private int opportunityId;
    private Opportunity currentOpp;

    // UI
    private TextView tvTitle, tvCompany, tvTypeLabel;
    private TextView tvLocation, tvDeadline, tvPoster, tvDescription;
    private LinearLayout rowLocation, rowDeadline;
    private MaterialButton btnApply, btnDelete;
    private ProgressBar progressDelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opportunity_details);

        opportunityRepository = new OpportunityRepository(this);
        sessionManager = new SessionManager(this);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Opportunity Details");
        }

        tvTitle = findViewById(R.id.tv_od_title);
        tvCompany = findViewById(R.id.tv_od_company);
        tvTypeLabel = findViewById(R.id.tv_od_type_label);

        tvLocation = findViewById(R.id.tv_od_location);
        tvDeadline = findViewById(R.id.tv_od_deadline);
        tvPoster = findViewById(R.id.tv_od_poster);
        tvDescription = findViewById(R.id.tv_od_description);

        rowLocation = findViewById(R.id.row_od_location);
        rowDeadline = findViewById(R.id.row_od_deadline);

        btnApply = findViewById(R.id.btn_opp_apply);
        btnDelete = findViewById(R.id.btn_opp_delete);
        progressDelete = findViewById(R.id.progress_opp_delete);

        opportunityId = getIntent().getIntExtra("opportunity_id", -1);
        String initialTitle = getIntent().getStringExtra("opportunity_title");

        if (initialTitle != null) {
            tvTitle.setText(initialTitle);
        }

        if (opportunityId == -1) {
            Toast.makeText(this, "Invalid Opportunity", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnApply.setOnClickListener(v -> applyToOpportunity());
        btnDelete.setOnClickListener(v -> confirmDeletion());

        fetchOpportunityDetails();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void fetchOpportunityDetails() {
        opportunityRepository.getOpportunityById(opportunityId).enqueue(new Callback<Opportunity>() {
            @Override
            public void onResponse(Call<Opportunity> call, Response<Opportunity> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentOpp = response.body();
                    renderOpportunity(currentOpp);
                } else {
                    Toast.makeText(OpportunityDetailsActivity.this, "Failed to load opportunity details.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Opportunity> call, Throwable t) {
                Toast.makeText(OpportunityDetailsActivity.this, "Network error loading details.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderOpportunity(Opportunity opp) {
        tvTitle.setText(opp.getTitle());
        tvCompany.setText(opp.getCompany());
        tvTypeLabel.setText(opp.getOpportunityType());
        tvDescription.setText(opp.getDescription());

        // Location
        if (Opportunity.hasValue(opp.getLocation())) {
            tvLocation.setText(opp.getLocation());
            rowLocation.setVisibility(View.VISIBLE);
        } else {
            rowLocation.setVisibility(View.GONE);
        }

        // Deadline
        String formattedDeadline = opp.getFormattedDeadline();
        if (Opportunity.hasValue(formattedDeadline)) {
            if (opp.isDeadlinePassed()) {
                tvDeadline.setText("Deadline Passed (" + formattedDeadline + ")");
                tvDeadline.setTextColor(getResources().getColor(R.color.error, null));
            } else {
                tvDeadline.setText(formattedDeadline);
                tvDeadline.setTextColor(getResources().getColor(R.color.text_primary, null));
            }
            rowDeadline.setVisibility(View.VISIBLE);
        } else {
            rowDeadline.setVisibility(View.GONE);
        }

        // Poster
        tvPoster.setText(opp.getPosterName());

        // Apply Button
        if (Opportunity.hasValue(opp.getApplicationUrl())) {
            btnApply.setVisibility(View.VISIBLE);
        } else {
            btnApply.setVisibility(View.GONE);
        }

        // Delete Button (visible if Admin OR posted_by matches current user ID)
        String userRole = sessionManager.getUserRole().toLowerCase();
        int currentUserId = sessionManager.getUserId();
        boolean isCreator = opp.getPostedBy() != null && opp.getPostedBy() == currentUserId;

        if ("admin".equals(userRole) || isCreator) {
            btnDelete.setVisibility(View.VISIBLE);
        } else {
            btnDelete.setVisibility(View.GONE);
        }
    }

    private void applyToOpportunity() {
        if (currentOpp == null || !Opportunity.hasValue(currentOpp.getApplicationUrl())) return;
        try {
            String url = currentOpp.getApplicationUrl().trim();
            String sanitized = url.startsWith("http") ? url : "https://" + url;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(sanitized));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open application link.", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmDeletion() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Opportunity")
                .setMessage("Are you sure you want to delete this career post?")
                .setPositiveButton("Delete", (dialog, which) -> deletePost())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deletePost() {
        btnDelete.setVisibility(View.GONE);
        progressDelete.setVisibility(View.VISIBLE);

        opportunityRepository.deleteOpportunity(opportunityId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                progressDelete.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    Toast.makeText(OpportunityDetailsActivity.this, "Opportunity deleted successfully.", Toast.LENGTH_SHORT).show();
                    finish();
                } else if (response.code() == 403) {
                    Toast.makeText(OpportunityDetailsActivity.this, "You do not have permission to perform this action.", Toast.LENGTH_LONG).show();
                    btnDelete.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(OpportunityDetailsActivity.this, "Failed to delete post. HTTP " + response.code(), Toast.LENGTH_SHORT).show();
                    btnDelete.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                progressDelete.setVisibility(View.GONE);
                btnDelete.setVisibility(View.VISIBLE);
                Toast.makeText(OpportunityDetailsActivity.this, "Network error deleting post.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
