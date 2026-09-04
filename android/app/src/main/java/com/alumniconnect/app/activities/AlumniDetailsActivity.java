package com.alumniconnect.app.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.alumniconnect.app.R;
import com.alumniconnect.app.models.Alumni;
import com.alumniconnect.app.models.MentorshipRequest;
import com.alumniconnect.app.models.MentorshipRequestCreate;
import com.alumniconnect.app.repositories.AlumniRepository;
import com.alumniconnect.app.repositories.MentorshipRepository;
import com.alumniconnect.app.utils.ApiErrorUtils;
import com.alumniconnect.app.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AlumniDetailsActivity extends AppCompatActivity {

    private AlumniRepository alumniRepository;
    private MentorshipRepository mentorshipRepository;
    private SessionManager sessionManager;

    // State Layouts
    private View layoutContent;
    private ProgressBar progressDetail;
    private View layoutError;
    private TextView tvErrorMsg;
    private MaterialButton btnRetry;

    // Header
    private TextView tvDetailInitials, tvDetailName, tvDetailHeadline, tvDetailMentorBadge;
    // Field rows
    private LinearLayout rowEmail, rowDept, rowYear, rowCompany, rowJobRole;
    private LinearLayout rowLocation, rowSkills, rowBio;
    private TextView tvDetailEmail, tvDetailDept, tvDetailYear, tvDetailCompany;
    private TextView tvDetailJobRole, tvDetailLocation, tvDetailSkills, tvDetailBio;
    // Buttons
    private MaterialButton btnLinkedin, btnGithub, btnRequestMentorship;

    private int alumniId;
    private boolean isSendingRequest = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alumni_details);

        alumniRepository = new AlumniRepository(this);
        mentorshipRepository = new MentorshipRepository(this);
        sessionManager = new SessionManager(this);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Alumni Profile");
        }

        // State views
        layoutContent = findViewById(R.id.layout_alumni_content);
        progressDetail = findViewById(R.id.progress_alumni_detail);
        layoutError = findViewById(R.id.layout_alumni_detail_error);
        tvErrorMsg = findViewById(R.id.tv_alumni_detail_error_msg);
        btnRetry = findViewById(R.id.btn_alumni_detail_retry);

        // Bind views
        tvDetailInitials = findViewById(R.id.tv_detail_initials);
        tvDetailName = findViewById(R.id.tv_detail_name);
        tvDetailHeadline = findViewById(R.id.tv_detail_headline);
        tvDetailMentorBadge = findViewById(R.id.tv_detail_mentor_badge);
        rowEmail = findViewById(R.id.row_email);
        rowDept = findViewById(R.id.row_dept);
        rowYear = findViewById(R.id.row_year);
        rowCompany = findViewById(R.id.row_company);
        rowJobRole = findViewById(R.id.row_jobrole);
        rowLocation = findViewById(R.id.row_location);
        rowSkills = findViewById(R.id.row_skills);
        rowBio = findViewById(R.id.row_bio);
        tvDetailEmail = findViewById(R.id.tv_detail_email);
        tvDetailDept = findViewById(R.id.tv_detail_dept);
        tvDetailYear = findViewById(R.id.tv_detail_year);
        tvDetailCompany = findViewById(R.id.tv_detail_company);
        tvDetailJobRole = findViewById(R.id.tv_detail_jobrole);
        tvDetailLocation = findViewById(R.id.tv_detail_location);
        tvDetailSkills = findViewById(R.id.tv_detail_skills);
        tvDetailBio = findViewById(R.id.tv_detail_bio);
        btnLinkedin = findViewById(R.id.btn_linkedin);
        btnGithub = findViewById(R.id.btn_github);
        btnRequestMentorship = findViewById(R.id.btn_request_mentorship);

        alumniId = getIntent().getIntExtra("alumni_id", -1);
        String name = getIntent().getStringExtra("alumni_name");

        if (Alumni.hasValue(name)) {
            tvDetailName.setText(name);
            tvDetailInitials.setText(getInitials(name));
        }

        if (alumniId == -1) {
            Toast.makeText(this, "Invalid alumni record.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnRetry.setOnClickListener(v -> fetchAlumniDetails());

        fetchAlumniDetails();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void fetchAlumniDetails() {
        showLoadingState();

        alumniRepository.getAlumniById(alumniId).enqueue(new Callback<Alumni>() {
            @Override
            public void onResponse(Call<Alumni> call, Response<Alumni> response) {
                if (response.isSuccessful() && response.body() != null) {
                    showSuccessState();
                    renderAlumni(response.body());
                } else if (response.code() == 404) {
                    showErrorState("Alumni record not found.");
                } else {
                    String errorMsg = ApiErrorUtils.parseError(response);
                    showErrorState(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<Alumni> call, Throwable t) {
                showErrorState(ApiErrorUtils.parseThrowable(t));
            }
        });
    }

    private void showLoadingState() {
        if (progressDetail != null) progressDetail.setVisibility(View.VISIBLE);
        if (layoutContent != null) layoutContent.setVisibility(View.GONE);
        if (layoutError != null) layoutError.setVisibility(View.GONE);
    }

    private void showSuccessState() {
        if (progressDetail != null) progressDetail.setVisibility(View.GONE);
        if (layoutContent != null) layoutContent.setVisibility(View.VISIBLE);
        if (layoutError != null) layoutError.setVisibility(View.GONE);
    }

    private void showErrorState(String message) {
        if (progressDetail != null) progressDetail.setVisibility(View.GONE);
        if (layoutContent != null) layoutContent.setVisibility(View.GONE);
        if (layoutError != null) layoutError.setVisibility(View.VISIBLE);
        if (tvErrorMsg != null) tvErrorMsg.setText(message);
    }

    private void renderAlumni(Alumni alumni) {
        tvDetailInitials.setText(alumni.getInitials());
        tvDetailName.setText(alumni.getDisplayName());

        String headline = alumni.getHeadline();
        if (Alumni.hasValue(headline)) {
            tvDetailHeadline.setText(headline);
            tvDetailHeadline.setVisibility(View.VISIBLE);
        } else {
            tvDetailHeadline.setVisibility(View.GONE);
        }

        if (alumni.isMentorshipAvailable()) {
            tvDetailMentorBadge.setVisibility(View.VISIBLE);
        } else {
            tvDetailMentorBadge.setVisibility(View.GONE);
        }

        showRow(rowEmail, tvDetailEmail, alumni.getEmail());
        showRow(rowDept, tvDetailDept, alumni.getDepartment());
        showRow(rowYear, tvDetailYear, alumni.getGraduationYear());
        showRow(rowCompany, tvDetailCompany, alumni.getCompany());
        showRow(rowJobRole, tvDetailJobRole, alumni.getJobRole());
        showRow(rowLocation, tvDetailLocation, alumni.getLocation());
        showRow(rowSkills, tvDetailSkills, alumni.getSkills());
        showRow(rowBio, tvDetailBio, alumni.getBio());

        if (Alumni.hasValue(alumni.getLinkedinUrl())) {
            btnLinkedin.setVisibility(View.VISIBLE);
            btnLinkedin.setOnClickListener(v -> openUrl(alumni.getLinkedinUrl()));
        } else {
            btnLinkedin.setVisibility(View.GONE);
        }

        if (Alumni.hasValue(alumni.getGithubUrl())) {
            btnGithub.setVisibility(View.VISIBLE);
            btnGithub.setOnClickListener(v -> openUrl(alumni.getGithubUrl()));
        } else {
            btnGithub.setVisibility(View.GONE);
        }

        String currentRole = sessionManager.getUserRole();
        if ("student".equalsIgnoreCase(currentRole) && alumni.isMentorshipAvailable()) {
            btnRequestMentorship.setVisibility(View.VISIBLE);
            btnRequestMentorship.setOnClickListener(v -> showSendRequestDialog(alumni));
        } else {
            btnRequestMentorship.setVisibility(View.GONE);
        }
    }

    private void showSendRequestDialog(Alumni mentor) {
        if (isSendingRequest) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Request Mentorship from " + mentor.getDisplayName());

        final EditText input = new EditText(this);
        input.setHint("Introduce yourself and write your message...");
        input.setPadding(36, 24, 36, 24);
        builder.setView(input);

        builder.setPositiveButton("Send", null);
        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Custom click listener to validate and disable button while network call runs
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(v -> {
            String msg = input.getText().toString().trim();
            if (msg.isEmpty()) {
                input.setError("Message cannot be empty.");
                return;
            }

            if (isSendingRequest) return;
            isSendingRequest = true;
            positiveButton.setEnabled(false);
            input.setEnabled(false);

            int targetId = mentor.getUserId() != null ? mentor.getUserId() : mentor.getId();
            MentorshipRequestCreate req = new MentorshipRequestCreate(targetId, msg);

            mentorshipRepository.sendMentorshipRequest(req).enqueue(new Callback<MentorshipRequest>() {
                @Override
                public void onResponse(Call<MentorshipRequest> call, Response<MentorshipRequest> response) {
                    isSendingRequest = false;
                    dialog.dismiss();

                    if (response.isSuccessful()) {
                        Toast.makeText(AlumniDetailsActivity.this, "Mentorship request sent successfully!", Toast.LENGTH_LONG).show();
                    } else {
                        String errorMsg = ApiErrorUtils.parseError(response);
                        Toast.makeText(AlumniDetailsActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<MentorshipRequest> call, Throwable t) {
                    isSendingRequest = false;
                    positiveButton.setEnabled(true);
                    input.setEnabled(true);
                    String errorMsg = ApiErrorUtils.parseThrowable(t);
                    Toast.makeText(AlumniDetailsActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void showRow(LinearLayout row, TextView valueView, String value) {
        if (Alumni.hasValue(value)) {
            valueView.setText(value);
            row.setVisibility(View.VISIBLE);
        } else {
            row.setVisibility(View.GONE);
        }
    }

    private void openUrl(String url) {
        com.alumniconnect.app.utils.UrlUtils.openUrlSafely(this, url);
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return String.valueOf(parts[0].charAt(0)).toUpperCase();
        return (String.valueOf(parts[0].charAt(0)) + String.valueOf(parts[parts.length - 1].charAt(0))).toUpperCase();
    }
}
