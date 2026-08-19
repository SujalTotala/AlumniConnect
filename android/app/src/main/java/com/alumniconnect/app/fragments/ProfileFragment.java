package com.alumniconnect.app.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.alumniconnect.app.R;
import com.alumniconnect.app.activities.EditProfileActivity;
import com.alumniconnect.app.models.ProfileResponse;
import com.alumniconnect.app.repositories.ProfileRepository;
import com.alumniconnect.app.utils.SessionManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {
    private ProfileRepository profileRepository;
    private SessionManager sessionManager;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressProfile;
    private TextView tvProfileError, tvProfileInitial, tvProfileName, tvProfileEmail;
    private TextView tvProfileRoleBadge;
    private LinearLayout layoutProfileFields;
    private View btnEditProfile;
    private ProfileResponse currentProfile;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        profileRepository = new ProfileRepository(requireContext());
        sessionManager = new SessionManager(requireContext());

        swipeRefresh = view.findViewById(R.id.swipe_refresh_profile);
        progressProfile = view.findViewById(R.id.progress_profile);
        tvProfileError = view.findViewById(R.id.tv_profile_error);
        tvProfileInitial = view.findViewById(R.id.tv_profile_initial);
        tvProfileName = view.findViewById(R.id.tv_profile_name);
        tvProfileEmail = view.findViewById(R.id.tv_profile_email);
        tvProfileRoleBadge = view.findViewById(R.id.tv_profile_role_badge);
        layoutProfileFields = view.findViewById(R.id.layout_profile_fields);
        btnEditProfile = view.findViewById(R.id.btn_edit_profile);

        swipeRefresh.setColorSchemeResources(R.color.primary);
        swipeRefresh.setOnRefreshListener(() -> loadProfile(true));

        btnEditProfile.setOnClickListener(v -> openEditProfile());

        loadProfile(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh on return from EditProfileActivity
        loadProfile(false);
    }

    private void loadProfile(boolean fromSwipe) {
        if (!fromSwipe) {
            progressProfile.setVisibility(View.VISIBLE);
            tvProfileError.setVisibility(View.GONE);
        }

        profileRepository.getMyProfile().enqueue(new Callback<ProfileResponse>() {
            @Override
            public void onResponse(Call<ProfileResponse> call, Response<ProfileResponse> response) {
                progressProfile.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    currentProfile = response.body();
                    tvProfileError.setVisibility(View.GONE);
                    renderProfile(currentProfile);
                    // Sync name if changed on web
                    if (currentProfile.getName() != null) {
                        sessionManager.saveSession(sessionManager.getToken(),
                                buildUserFromProfile(currentProfile));
                    }
                } else if (response.code() == 401) {
                    showError("Session expired. Please login again.");
                } else {
                    showError("Failed to load profile (HTTP " + response.code() + "). Pull to retry.");
                }
            }

            @Override
            public void onFailure(Call<ProfileResponse> call, Throwable t) {
                progressProfile.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                showError("Network error. Pull down to retry.");
            }
        });
    }

    private void renderProfile(ProfileResponse profile) {
        String name = profile.getName() != null ? profile.getName() : "Member";
        String email = profile.getEmail() != null ? profile.getEmail() : "";
        String role = profile.getRole() != null ? profile.getRole() : "student";

        if (!name.isEmpty()) {
            tvProfileInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
        }
        tvProfileName.setText(name);
        tvProfileEmail.setText(email);
        tvProfileRoleBadge.setText(role.toUpperCase());

        // Build dynamic profile field rows
        layoutProfileFields.removeAllViews();

        if ("alumni".equalsIgnoreCase(role)) {
            addProfileField("Company", profile.getProfileString("company"));
            addProfileField("Job Role", profile.getProfileString("job_role"));
            addProfileField("Department", profile.getProfileString("department"));
            addProfileField("Graduation Year", profile.getProfileString("graduation_year"));
            addProfileField("Location", profile.getProfileString("location"));
            addProfileField("Skills", profile.getProfileString("skills"));
            addProfileField("Bio", profile.getProfileString("bio"));
            addProfileField("LinkedIn", profile.getProfileString("linkedin_url"));
            addProfileField("GitHub", profile.getProfileString("github_url"));
            boolean mentoring = profile.getProfileBoolean("mentorship_available");
            addProfileField("Available for Mentorship", mentoring ? "Yes ✓" : "No");
        } else if ("student".equalsIgnoreCase(role)) {
            addProfileField("Branch / Program", profile.getProfileString("branch"));
            addProfileField("Academic Year", profile.getProfileString("year"));
            addProfileField("Skills", profile.getProfileString("skills"));
            addProfileField("Career Interests", profile.getProfileString("interests"));
            addProfileField("Bio", profile.getProfileString("bio"));
        } else {
            addProfileField("Account Type", "Administrator");
            addProfileField("Name", name);
            addProfileField("Email", email);
        }
    }

    private void addProfileField(String label, String value) {
        if (value == null || value.trim().isEmpty()) return;
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, 0, 0, 14);

        TextView tvLabel = new TextView(requireContext());
        tvLabel.setText(label);
        tvLabel.setTextSize(10f);
        tvLabel.setTextColor(getResources().getColor(R.color.text_muted, null));
        tvLabel.setAllCaps(true);

        TextView tvValue = new TextView(requireContext());
        tvValue.setText(value);
        tvValue.setTextSize(13f);
        tvValue.setTextColor(getResources().getColor(R.color.text_primary, null));

        row.addView(tvLabel);
        row.addView(tvValue);
        layoutProfileFields.addView(row);
    }

    private void showError(String msg) {
        tvProfileError.setText(msg);
        tvProfileError.setVisibility(View.VISIBLE);
    }

    private void openEditProfile() {
        Intent intent = new Intent(requireContext(), EditProfileActivity.class);
        if (currentProfile != null) {
            intent.putExtra("profile_name", currentProfile.getName());
            intent.putExtra("profile_role", currentProfile.getRole());
            intent.putExtra("profile_skills", currentProfile.getProfileString("skills"));
            intent.putExtra("profile_bio", currentProfile.getProfileString("bio"));
            // Alumni extras
            intent.putExtra("profile_company", currentProfile.getProfileString("company"));
            intent.putExtra("profile_job_role", currentProfile.getProfileString("job_role"));
            intent.putExtra("profile_department", currentProfile.getProfileString("department"));
            intent.putExtra("profile_graduation_year", currentProfile.getProfileString("graduation_year"));
            intent.putExtra("profile_location", currentProfile.getProfileString("location"));
            intent.putExtra("profile_linkedin", currentProfile.getProfileString("linkedin_url"));
            intent.putExtra("profile_github", currentProfile.getProfileString("github_url"));
            intent.putExtra("profile_mentorship", currentProfile.getProfileBoolean("mentorship_available"));
            // Student extras
            intent.putExtra("profile_branch", currentProfile.getProfileString("branch"));
            intent.putExtra("profile_year", currentProfile.getProfileString("year"));
            intent.putExtra("profile_interests", currentProfile.getProfileString("interests"));
        }
        startActivity(intent);
    }

    /** Build minimal User object for SessionManager update */
    private com.alumniconnect.app.models.User buildUserFromProfile(ProfileResponse p) {
        com.alumniconnect.app.models.User u = new com.alumniconnect.app.models.User();
        u.setId(p.getId());
        u.setName(p.getName());
        u.setEmail(p.getEmail());
        u.setRole(p.getRole());
        return u;
    }
}
