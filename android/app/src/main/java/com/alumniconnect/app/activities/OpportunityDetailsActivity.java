package com.alumniconnect.app.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import com.alumniconnect.app.R;
import com.alumniconnect.app.models.EligibleAlumni;
import com.alumniconnect.app.models.Opportunity;
import com.alumniconnect.app.models.ReferralRequest;
import com.alumniconnect.app.models.ReferralRequestCreate;
import com.alumniconnect.app.repositories.OpportunityRepository;
import com.alumniconnect.app.repositories.ReferralRepository;
import com.alumniconnect.app.utils.ApiErrorUtils;
import com.alumniconnect.app.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OpportunityDetailsActivity extends AppCompatActivity {

    private OpportunityRepository opportunityRepository;
    private ReferralRepository referralRepository;
    private SessionManager sessionManager;

    private int opportunityId;
    private Opportunity currentOpp;
    private boolean isDeleting = false;

    // Root State Containers
    private ScrollView scrollOppContent;
    private ProgressBar progressOppDetail;
    private LinearLayout layoutOppDetailError;
    private TextView tvOppDetailErrorMsg;
    private MaterialButton btnOppDetailRetry;

    // UI elements
    private TextView tvTitle, tvCompany, tvTypeLabel;
    private TextView tvLocation, tvDeadline, tvPoster, tvDescription;
    private LinearLayout rowLocation, rowDeadline;
    private MaterialButton btnApply, btnDelete, btnRequestReferral;
    private ProgressBar progressDelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opportunity_details);

        opportunityRepository = new OpportunityRepository(this);
        referralRepository = new ReferralRepository(this);
        sessionManager = new SessionManager(this);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Opportunity Details");
        }

        // State views
        scrollOppContent = findViewById(R.id.scroll_opp_content);
        progressOppDetail = findViewById(R.id.progress_opp_detail);
        layoutOppDetailError = findViewById(R.id.layout_opp_detail_error);
        tvOppDetailErrorMsg = findViewById(R.id.tv_opp_detail_error_msg);
        btnOppDetailRetry = findViewById(R.id.btn_opp_detail_retry);

        // Content views
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
        btnRequestReferral = findViewById(R.id.btn_opp_request_referral);
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
        btnRequestReferral.setOnClickListener(v -> openReferralRequestDialog());
        btnOppDetailRetry.setOnClickListener(v -> fetchOpportunityDetails());

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

    private void showLoadingState() {
        if (progressOppDetail != null) progressOppDetail.setVisibility(View.VISIBLE);
        if (scrollOppContent != null) scrollOppContent.setVisibility(View.GONE);
        if (layoutOppDetailError != null) layoutOppDetailError.setVisibility(View.GONE);
    }

    private void showSuccessState() {
        if (progressOppDetail != null) progressOppDetail.setVisibility(View.GONE);
        if (scrollOppContent != null) scrollOppContent.setVisibility(View.VISIBLE);
        if (layoutOppDetailError != null) layoutOppDetailError.setVisibility(View.GONE);
    }

    private void showErrorState(String message) {
        if (progressOppDetail != null) progressOppDetail.setVisibility(View.GONE);
        if (scrollOppContent != null) scrollOppContent.setVisibility(View.GONE);
        if (layoutOppDetailError != null) {
            layoutOppDetailError.setVisibility(View.VISIBLE);
            if (tvOppDetailErrorMsg != null) {
                tvOppDetailErrorMsg.setText(message != null ? message : "Unable to load opportunity details.");
            }
        }
    }

    private void fetchOpportunityDetails() {
        showLoadingState();
        opportunityRepository.getOpportunityById(opportunityId).enqueue(new Callback<Opportunity>() {
            @Override
            public void onResponse(Call<Opportunity> call, Response<Opportunity> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentOpp = response.body();
                    renderOpportunity(currentOpp);
                    showSuccessState();
                } else {
                    String errorMsg = ApiErrorUtils.getErrorMessage(response);
                    showErrorState(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<Opportunity> call, Throwable t) {
                String errorMsg = ApiErrorUtils.getNetworkErrorMessage(t);
                showErrorState(errorMsg);
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
            btnDelete.setEnabled(true);
        } else {
            btnDelete.setVisibility(View.GONE);
        }
    }

    private void applyToOpportunity() {
        if (currentOpp == null || !Opportunity.hasValue(currentOpp.getApplicationUrl())) return;
        com.alumniconnect.app.utils.UrlUtils.openUrlSafely(this, currentOpp.getApplicationUrl());
    }

    private void confirmDeletion() {
        if (isDeleting) return;
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_delete_opportunity_title)
                .setMessage(R.string.dialog_delete_opportunity_message)
                .setPositiveButton(R.string.dialog_delete_opportunity_positive, (dialog, which) -> deletePost())
                .setNegativeButton(R.string.dialog_cancel_negative, null)
                .show();
    }

    private void deletePost() {
        if (isDeleting) return;
        isDeleting = true;

        btnDelete.setEnabled(false);
        btnDelete.setVisibility(View.GONE);
        progressDelete.setVisibility(View.VISIBLE);

        opportunityRepository.deleteOpportunity(opportunityId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                isDeleting = false;
                progressDelete.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    Toast.makeText(OpportunityDetailsActivity.this, "Opportunity deleted successfully.", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    btnDelete.setEnabled(true);
                    btnDelete.setVisibility(View.VISIBLE);
                    String error = ApiErrorUtils.getErrorMessage(response);
                    Toast.makeText(OpportunityDetailsActivity.this, error, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                isDeleting = false;
                progressDelete.setVisibility(View.GONE);
                btnDelete.setEnabled(true);
                btnDelete.setVisibility(View.VISIBLE);
                String error = ApiErrorUtils.getNetworkErrorMessage(t);
                Toast.makeText(OpportunityDetailsActivity.this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void openReferralRequestDialog() {
        btnRequestReferral.setEnabled(false);
        referralRepository.getEligibleAlumniForOpportunity(opportunityId).enqueue(new Callback<List<EligibleAlumni>>() {
            @Override
            public void onResponse(Call<List<EligibleAlumni>> call, Response<List<EligibleAlumni>> response) {
                btnRequestReferral.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    List<EligibleAlumni> eligibleList = response.body();
                    if (eligibleList.isEmpty()) {
                        showNoEligibleAlumniDialog();
                    } else {
                        showReferralFormDialog(eligibleList);
                    }
                } else {
                    Toast.makeText(OpportunityDetailsActivity.this, ApiErrorUtils.parseError(response), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<EligibleAlumni>> call, Throwable t) {
                btnRequestReferral.setEnabled(true);
                Toast.makeText(OpportunityDetailsActivity.this, ApiErrorUtils.parseThrowable(t), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showNoEligibleAlumniDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("No Connected Alumni")
                .setMessage("You must be connected with alumni before requesting a referral for this opportunity. Build your network first through Alumni Directory or Suggestions.")
                .setPositiveButton("Explore Network", (dialog, which) -> {
                    Intent intent = new Intent(OpportunityDetailsActivity.this, MyNetworkActivity.class);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showReferralFormDialog(List<EligibleAlumni> alumniList) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_request_referral, null);
        TextView tvDialogOppTitle = dialogView.findViewById(R.id.tv_dialog_opp_title);
        TextView tvDialogOppCompany = dialogView.findViewById(R.id.tv_dialog_opp_company);
        Spinner spinner = dialogView.findViewById(R.id.spinner_eligible_alumni);
        TextView tvHint = dialogView.findViewById(R.id.tv_dialog_company_match_hint);
        EditText etNote = dialogView.findViewById(R.id.et_referral_note);

        if (currentOpp != null) {
            tvDialogOppTitle.setText("Referral for " + currentOpp.getTitle());
            tvDialogOppCompany.setText(currentOpp.getCompany());
        }

        boolean hasCompanyMatch = false;
        List<String> displayItems = new ArrayList<>();
        for (EligibleAlumni a : alumniList) {
            StringBuilder sb = new StringBuilder();
            sb.append(a.getName());
            if (a.getCompany() != null && !a.getCompany().isEmpty()) {
                sb.append(" (").append(a.getCompany()).append(")");
            }
            if (a.isCompanyMatch()) {
                sb.append(" ⭐ Company Match");
                hasCompanyMatch = true;
            }
            displayItems.add(sb.toString());
        }

        if (hasCompanyMatch) {
            tvHint.setVisibility(View.VISIBLE);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, displayItems);
        spinner.setAdapter(adapter);

        new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setPositiveButton("Send Request", (dialog, which) -> {
                    int selectedPos = spinner.getSelectedItemPosition();
                    if (selectedPos >= 0 && selectedPos < alumniList.size()) {
                        EligibleAlumni selectedAlumni = alumniList.get(selectedPos);
                        String note = etNote.getText() != null ? etNote.getText().toString().trim() : null;
                        submitReferralRequest(selectedAlumni.getUserId(), note);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void submitReferralRequest(int alumniUserId, String note) {
        ReferralRequestCreate requestData = new ReferralRequestCreate(opportunityId, alumniUserId, note);
        referralRepository.createReferralRequest(requestData).enqueue(new Callback<ReferralRequest>() {
            @Override
            public void onResponse(Call<ReferralRequest> call, Response<ReferralRequest> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(OpportunityDetailsActivity.this, "Referral request submitted successfully!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(OpportunityDetailsActivity.this, ApiErrorUtils.parseError(response), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ReferralRequest> call, Throwable t) {
                Toast.makeText(OpportunityDetailsActivity.this, ApiErrorUtils.parseThrowable(t), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
