package com.alumniconnect.app.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.alumniconnect.app.R;
import com.alumniconnect.app.models.Alumni;
import com.alumniconnect.app.models.MentorshipRequest;
import com.alumniconnect.app.models.MentorshipRequestCreate;
import com.alumniconnect.app.repositories.AlumniRepository;
import com.alumniconnect.app.repositories.MentorshipRepository;
import com.alumniconnect.app.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import android.app.AlertDialog;
import android.widget.EditText;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AlumniDetailsActivity extends AppCompatActivity {

    private AlumniRepository alumniRepository;
    private MentorshipRepository mentorshipRepository;
    private SessionManager sessionManager;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alumni_details);

        alumniRepository = new AlumniRepository(this);
        mentorshipRepository = new MentorshipRepository(this);
        sessionManager = new SessionManager(this);

        // Support back arrow
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Alumni Profile");
        }

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

        // Get the alumni ID from intent
        alumniId = getIntent().getIntExtra("alumni_id", -1);
        String name = getIntent().getStringExtra("alumni_name");

        // Set a placeholder name while loading
        if (Alumni.hasValue(name)) {
            tvDetailName.setText(name);
            tvDetailInitials.setText(getInitials(name));
        }

        if (alumniId == -1) {
            Toast.makeText(this, "Invalid alumni record", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

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
        alumniRepository.getAlumniById(alumniId).enqueue(new Callback<Alumni>() {
            @Override
            public void onResponse(Call<Alumni> call, Response<Alumni> response) {
                if (response.isSuccessful() && response.body() != null) {
                    renderAlumni(response.body());
                } else if (response.code() == 404) {
                    Toast.makeText(AlumniDetailsActivity.this,
                            "Alumni record not found.", Toast.LENGTH_SHORT).show();
                    finish();
                } else if (response.code() == 401) {
                    Toast.makeText(AlumniDetailsActivity.this,
                            "Session expired.", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(AlumniDetailsActivity.this,
                            "Error loading profile (HTTP " + response.code() + ")",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Alumni> call, Throwable t) {
                Toast.makeText(AlumniDetailsActivity.this,
                        "Network error. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderAlumni(Alumni alumni) {
        // Header
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

        // Field rows — only shown when data exists
        showRow(rowEmail, tvDetailEmail, alumni.getEmail());
        showRow(rowDept, tvDetailDept, alumni.getDepartment());
        showRow(rowYear, tvDetailYear, alumni.getGraduationYear());
        showRow(rowCompany, tvDetailCompany, alumni.getCompany());
        showRow(rowJobRole, tvDetailJobRole, alumni.getJobRole());
        showRow(rowLocation, tvDetailLocation, alumni.getLocation());
        showRow(rowSkills, tvDetailSkills, alumni.getSkills());
        showRow(rowBio, tvDetailBio, alumni.getBio());

        // LinkedIn button
        if (Alumni.hasValue(alumni.getLinkedinUrl())) {
            btnLinkedin.setVisibility(View.VISIBLE);
            btnLinkedin.setOnClickListener(v -> openUrl(alumni.getLinkedinUrl()));
        } else {
            btnLinkedin.setVisibility(View.GONE);
        }

        // GitHub button
        if (Alumni.hasValue(alumni.getGithubUrl())) {
            btnGithub.setVisibility(View.VISIBLE);
            btnGithub.setOnClickListener(v -> openUrl(alumni.getGithubUrl()));
        } else {
            btnGithub.setVisibility(View.GONE);
        }

        // Mentorship request button (Student viewing an available mentor)
        String currentRole = sessionManager.getUserRole();
        if ("student".equalsIgnoreCase(currentRole) && alumni.isMentorshipAvailable()) {
            btnRequestMentorship.setVisibility(View.VISIBLE);
            btnRequestMentorship.setOnClickListener(v -> showSendRequestDialog(alumni));
        } else {
            btnRequestMentorship.setVisibility(View.GONE);
        }
    }

    private void showSendRequestDialog(Alumni mentor) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Request Mentorship from " + mentor.getDisplayName());

        final EditText input = new EditText(this);
        input.setHint("Introduce yourself and write your message...");
        input.setPadding(32, 16, 32, 16);
        builder.setView(input);

        builder.setPositiveButton("Send", (dialog, which) -> {
            String msg = input.getText().toString().trim();
            if (msg.isEmpty()) {
                Toast.makeText(this, "Message cannot be empty.", Toast.LENGTH_SHORT).show();
                return;
            }

            int targetId = mentor.getUserId() != null ? mentor.getUserId() : mentor.getId();
            MentorshipRequestCreate req = new MentorshipRequestCreate(targetId, msg);

            mentorshipRepository.sendMentorshipRequest(req).enqueue(new Callback<MentorshipRequest>() {
                @Override
                public void onResponse(Call<MentorshipRequest> call, Response<MentorshipRequest> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(AlumniDetailsActivity.this, "Mentorship request sent successfully!", Toast.LENGTH_LONG).show();
                    } else if (response.code() == 400) {
                        String detail = parseErrorDetail(response);
                        if (detail.contains("already exists")) {
                            Toast.makeText(AlumniDetailsActivity.this, "You already have an active mentorship request with this mentor.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(AlumniDetailsActivity.this, detail, Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(AlumniDetailsActivity.this, "Failed to send request. HTTP " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<MentorshipRequest> call, Throwable t) {
                    Toast.makeText(AlumniDetailsActivity.this, "Network error sending request.", Toast.LENGTH_SHORT).show();
                }
            });
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private String parseErrorDetail(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String json = response.errorBody().string();
                JSONObject obj = new JSONObject(json);
                if (obj.has("detail")) return obj.getString("detail");
            }
        } catch (Exception ignored) {}
        return "Request rejected by server.";
    }

    /** Show a field row only when value is non-null and non-empty */
    private void showRow(LinearLayout row, TextView valueView, String value) {
        if (Alumni.hasValue(value)) {
            valueView.setText(value);
            row.setVisibility(View.VISIBLE);
        } else {
            row.setVisibility(View.GONE);
        }
    }

    /** Open a URL with Intent.ACTION_VIEW — validates before launching */
    private void openUrl(String url) {
        if (url == null || url.trim().isEmpty()) return;
        try {
            String sanitized = url.startsWith("http") ? url : "https://" + url;
            Uri uri = Uri.parse(sanitized);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open URL.", Toast.LENGTH_SHORT).show();
        }
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return String.valueOf(parts[0].charAt(0)).toUpperCase();
        return (String.valueOf(parts[0].charAt(0)) + String.valueOf(parts[parts.length - 1].charAt(0))).toUpperCase();
    }
}
