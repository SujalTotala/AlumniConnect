package com.alumniconnect.app.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.alumniconnect.app.R;
import com.alumniconnect.app.models.ProfileResponse;
import com.alumniconnect.app.models.ProfileUpdateRequest;
import com.alumniconnect.app.repositories.ProfileRepository;
import com.alumniconnect.app.utils.ApiErrorUtils;
import com.alumniconnect.app.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileActivity extends AppCompatActivity {
    private ProfileRepository profileRepository;
    private SessionManager sessionManager;
    private String role;
    private boolean isSaving = false;

    // Common fields
    private TextInputEditText etName, etSkills, etBio;
    // Alumni fields
    private TextInputEditText etCompany, etJobRole, etDepartment, etGraduationYear,
            etLocation, etLinkedin, etGithub;
    private CheckBox cbMentorship;
    private LinearLayout layoutAlumniFields;
    // Student fields
    private TextInputEditText etBranch, etYear, etInterests;
    private LinearLayout layoutStudentFields;
    // UI
    private TextView tvError, tvSuccess;
    private MaterialButton btnSave;
    private ProgressBar progressSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        profileRepository = new ProfileRepository(this);
        sessionManager = new SessionManager(this);
        role = getIntent().getStringExtra("profile_role");
        if (role == null) role = sessionManager.getUserRole();

        // Common
        etName = findViewById(R.id.et_edit_name);
        etSkills = findViewById(R.id.et_edit_skills);
        etBio = findViewById(R.id.et_edit_bio);
        // Alumni
        etCompany = findViewById(R.id.et_edit_company);
        etJobRole = findViewById(R.id.et_edit_job_role);
        etDepartment = findViewById(R.id.et_edit_department);
        etGraduationYear = findViewById(R.id.et_edit_graduation_year);
        etLocation = findViewById(R.id.et_edit_location);
        etLinkedin = findViewById(R.id.et_edit_linkedin);
        etGithub = findViewById(R.id.et_edit_github);
        cbMentorship = findViewById(R.id.cb_mentorship);
        layoutAlumniFields = findViewById(R.id.layout_alumni_fields);
        // Student
        etBranch = findViewById(R.id.et_edit_branch);
        etYear = findViewById(R.id.et_edit_year);
        etInterests = findViewById(R.id.et_edit_interests);
        layoutStudentFields = findViewById(R.id.layout_student_fields);
        // UI
        tvError = findViewById(R.id.tv_edit_error);
        tvSuccess = findViewById(R.id.tv_edit_success);
        btnSave = findViewById(R.id.btn_save_profile);
        progressSave = findViewById(R.id.progress_save);

        // Show role-specific fields
        if ("alumni".equalsIgnoreCase(role)) {
            layoutAlumniFields.setVisibility(View.VISIBLE);
        } else if ("student".equalsIgnoreCase(role)) {
            layoutStudentFields.setVisibility(View.VISIBLE);
        }

        // Pre-fill from Intent extras
        preFillFields();

        btnSave.setOnClickListener(v -> saveProfile());
    }

    private void preFillFields() {
        setEditText(etName, getIntent().getStringExtra("profile_name"));
        setEditText(etSkills, getIntent().getStringExtra("profile_skills"));
        setEditText(etBio, getIntent().getStringExtra("profile_bio"));

        if ("alumni".equalsIgnoreCase(role)) {
            setEditText(etCompany, getIntent().getStringExtra("profile_company"));
            setEditText(etJobRole, getIntent().getStringExtra("profile_job_role"));
            setEditText(etDepartment, getIntent().getStringExtra("profile_department"));
            setEditText(etGraduationYear, getIntent().getStringExtra("profile_graduation_year"));
            setEditText(etLocation, getIntent().getStringExtra("profile_location"));
            setEditText(etLinkedin, getIntent().getStringExtra("profile_linkedin"));
            setEditText(etGithub, getIntent().getStringExtra("profile_github"));
            cbMentorship.setChecked(getIntent().getBooleanExtra("profile_mentorship", false));
        } else if ("student".equalsIgnoreCase(role)) {
            setEditText(etBranch, getIntent().getStringExtra("profile_branch"));
            setEditText(etYear, getIntent().getStringExtra("profile_year"));
            setEditText(etInterests, getIntent().getStringExtra("profile_interests"));
        }
    }

    private void setEditText(TextInputEditText et, String value) {
        if (et != null && value != null) et.setText(value);
    }

    private String getText(TextInputEditText et) {
        if (et == null || et.getText() == null) return null;
        String s = et.getText().toString().trim();
        return s.isEmpty() ? null : s;
    }

    private void saveProfile() {
        if (isSaving) return;

        String name = getText(etName);
        if (TextUtils.isEmpty(name)) {
            showError("Full name cannot be empty.");
            return;
        }

        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setName(name);
        request.setSkills(getText(etSkills));
        request.setBio(getText(etBio));

        if ("alumni".equalsIgnoreCase(role)) {
            request.setCompany(getText(etCompany));
            request.setJobRole(getText(etJobRole));
            request.setDepartment(getText(etDepartment));
            request.setGraduationYear(getText(etGraduationYear));
            request.setLocation(getText(etLocation));
            request.setLinkedinUrl(getText(etLinkedin));
            request.setGithubUrl(getText(etGithub));
            request.setMentorshipAvailable(cbMentorship.isChecked());
        } else if ("student".equalsIgnoreCase(role)) {
            request.setBranch(getText(etBranch));
            request.setYear(getText(etYear));
            request.setInterests(getText(etInterests));
        }

        isSaving = true;
        setLoading(true);
        hideMessages();

        profileRepository.updateMyProfile(request).enqueue(new Callback<ProfileResponse>() {
            @Override
            public void onResponse(Call<ProfileResponse> call, Response<ProfileResponse> response) {
                isSaving = false;
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    ProfileResponse updated = response.body();
                    // Update local session name
                    if (updated.getName() != null) {
                        com.alumniconnect.app.models.User u = new com.alumniconnect.app.models.User();
                        u.setId(updated.getId());
                        u.setName(updated.getName());
                        u.setEmail(updated.getEmail());
                        u.setRole(updated.getRole());
                        sessionManager.saveSession(sessionManager.getToken(), u);
                    }
                    com.alumniconnect.app.utils.KeyboardUtils.hideKeyboard(EditProfileActivity.this);
                    showSuccess("Profile updated successfully!");
                } else {
                    String msg = ApiErrorUtils.getErrorMessage(response);
                    showError(msg);
                }
            }

            @Override
            public void onFailure(Call<ProfileResponse> call, Throwable t) {
                isSaving = false;
                setLoading(false);
                String msg = ApiErrorUtils.getNetworkErrorMessage(t);
                showError(msg);
            }
        });
    }

    private void setLoading(boolean isLoading) {
        progressSave.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!isLoading);
    }

    private void showError(String msg) {
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
        tvSuccess.setVisibility(View.GONE);
    }

    private void showSuccess(String msg) {
        tvSuccess.setText(msg);
        tvSuccess.setVisibility(View.VISIBLE);
        tvError.setVisibility(View.GONE);
    }

    private void hideMessages() {
        tvError.setVisibility(View.GONE);
        tvSuccess.setVisibility(View.GONE);
    }
}
