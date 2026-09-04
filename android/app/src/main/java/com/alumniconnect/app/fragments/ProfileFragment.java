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
import com.alumniconnect.app.activities.MainActivity;
import com.alumniconnect.app.activities.SavedItemsActivity;
import com.alumniconnect.app.models.NotificationPreferences;
import com.alumniconnect.app.models.ProfileCompletion;
import com.alumniconnect.app.models.ProfileResponse;
import com.alumniconnect.app.repositories.PreferenceRepository;
import com.alumniconnect.app.repositories.ProfileRepository;
import com.alumniconnect.app.utils.ApiErrorUtils;
import com.alumniconnect.app.utils.SessionManager;
import com.alumniconnect.app.utils.UrlUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import android.widget.Toast;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {
    private ProfileRepository profileRepository;
    private PreferenceRepository preferenceRepository;
    private SessionManager sessionManager;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressProfile;
    private TextView tvProfileError, tvProfileInitial, tvProfileName, tvProfileEmail;
    private TextView tvProfileRoleBadge;
    private LinearLayout layoutProfileFields;
    private View btnEditProfile, btnProfileAbout, btnSavedItems, btnNotificationPrefs;
    private ProfileResponse currentProfile;

    // Completion UI
    private TextView tvCompletionPercentage, tvCompletionHint;
    private LinearProgressIndicator progressProfileCompletion;

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
        preferenceRepository = new PreferenceRepository(requireContext());
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
        btnProfileAbout = view.findViewById(R.id.btn_profile_about);
        btnSavedItems = view.findViewById(R.id.btn_saved_items);
        btnNotificationPrefs = view.findViewById(R.id.btn_notification_prefs);

        tvCompletionPercentage = view.findViewById(R.id.tv_completion_percentage);
        tvCompletionHint = view.findViewById(R.id.tv_completion_hint);
        progressProfileCompletion = view.findViewById(R.id.progress_profile_completion);

        swipeRefresh.setColorSchemeResources(R.color.primary);
        swipeRefresh.setOnRefreshListener(() -> loadProfile(true));

        btnEditProfile.setOnClickListener(v -> openEditProfile());

        if (btnSavedItems != null) {
            btnSavedItems.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), SavedItemsActivity.class);
                startActivity(intent);
            });
        }

        if (btnNotificationPrefs != null) {
            btnNotificationPrefs.setOnClickListener(v -> showNotificationPreferencesDialog());
        }

        if (btnProfileAbout != null) {
            btnProfileAbout.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).showAboutDialog();
                }
            });
        }

        loadProfile(false);
    }

    @Override
    public void onResume() {
        super.onResume();
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
                if (!isAdded()) return;

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
                } else {
                    String error = ApiErrorUtils.getErrorMessage(response);
                    showError(error);
                }
            }

            @Override
            public void onFailure(Call<ProfileResponse> call, Throwable t) {
                if (!isAdded()) return;
                progressProfile.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                String error = ApiErrorUtils.getNetworkErrorMessage(t);
                showError(error);
            }
        });
    }

    private void renderProfile(ProfileResponse profile) {
        String name = profile.getName() != null ? profile.getName().trim() : "Member";
        String email = profile.getEmail() != null ? profile.getEmail().trim() : "";
        String role = profile.getRole() != null ? profile.getRole().trim() : "student";

        if (!name.isEmpty()) {
            tvProfileInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
        }
        tvProfileName.setText(name);
        tvUserField(tvProfileEmail, email);
        tvProfileRoleBadge.setText(role.toUpperCase());

        // Build dynamic profile field rows
        layoutProfileFields.removeAllViews();

        int totalFields = 0;
        int filledFields = 0;

        if ("alumni".equalsIgnoreCase(role)) {
            totalFields = 10;
            filledFields += checkAndAddField("Company", profile.getProfileString("company"));
            filledFields += checkAndAddField("Job Role", profile.getProfileString("job_role"));
            filledFields += checkAndAddField("Department", profile.getProfileString("department"));
            filledFields += checkAndAddField("Graduation Year", profile.getProfileString("graduation_year"));
            filledFields += checkAndAddField("Location", profile.getProfileString("location"));
            filledFields += checkAndAddField("Skills", profile.getProfileString("skills"));
            filledFields += checkAndAddField("Bio", profile.getProfileString("bio"));
            filledFields += checkAndAddLinkField("LinkedIn Profile", profile.getProfileString("linkedin_url"));
            filledFields += checkAndAddLinkField("GitHub Profile", profile.getProfileString("github_url"));

            boolean mentoring = profile.getProfileBoolean("mentorship_available");
            addProfileField("Available for Mentorship", mentoring ? "Yes, available to mentor students ✓" : "Not available at this time", null);
            filledFields++; // mentoring choice exists
        } else if ("student".equalsIgnoreCase(role)) {
            totalFields = 6;
            filledFields += checkAndAddField("Branch / Program", profile.getProfileString("branch"));
            filledFields += checkAndAddField("Academic Year", profile.getProfileString("year"));
            filledFields += checkAndAddField("Skills", profile.getProfileString("skills"));
            filledFields += checkAndAddField("Career Interests", profile.getProfileString("interests"));
            filledFields += checkAndAddField("Bio", profile.getProfileString("bio"));
            if (profile.getName() != null && !profile.getName().trim().isEmpty()) filledFields++;
        } else {
            addProfileField("Account Type", "Administrator", null);
            addProfileField("Name", name, null);
            addProfileField("Email", email, null);
            totalFields = 3;
            filledFields = 3;
        }

        // Calculate and display percentage
        if (totalFields > 0) {
            int percentage = Math.min(100, Math.round((filledFields / (float) totalFields) * 100));
            if (tvCompletionPercentage != null) {
                tvCompletionPercentage.setText(percentage + "%");
            }
            if (progressProfileCompletion != null) {
                progressProfileCompletion.setProgress(percentage);
            }
            if (tvCompletionHint != null) {
                if (percentage == 100) {
                    tvCompletionHint.setText("Your profile is complete! Other members can connect easily.");
                } else {
                    tvCompletionHint.setText("Complete remaining fields by tapping 'Edit Profile' below.");
                }
            }
        }
    }

    private int checkAndAddField(String label, String value) {
        if (value != null && !value.trim().isEmpty() && !"null".equalsIgnoreCase(value.trim())) {
            addProfileField(label, value.trim(), null);
            return 1;
        }
        return 0;
    }

    private int checkAndAddLinkField(String label, String url) {
        if (url != null && !url.trim().isEmpty() && !"null".equalsIgnoreCase(url.trim())) {
            addProfileField(label, url.trim(), url.trim());
            return 1;
        }
        return 0;
    }

    private void addProfileField(String label, String value, String clickableUrl) {
        if (!isAdded() || getContext() == null) return;

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

        if (clickableUrl != null) {
            tvValue.setTextColor(getResources().getColor(R.color.primary, null));
            tvValue.setText("🔗 " + value);
            row.setClickable(true);
            row.setFocusable(true);
            row.setOnClickListener(v -> UrlUtils.openUrlSafely(requireContext(), clickableUrl));
        } else {
            tvValue.setTextColor(getResources().getColor(R.color.text_primary, null));
        }

        row.addView(tvLabel);
        row.addView(tvValue);
        layoutProfileFields.addView(row);
    }

    private void tvUserField(TextView tv, String val) {
        if (tv == null) return;
        if (val == null || val.trim().isEmpty() || "null".equalsIgnoreCase(val.trim())) {
            tv.setVisibility(View.GONE);
        } else {
            tv.setText(val.trim());
            tv.setVisibility(View.VISIBLE);
        }
    }

    private void showError(String msg) {
        tvProfileError.setText(msg);
        tvProfileError.setVisibility(View.VISIBLE);
    }

    private void openEditProfile() {
        if (!isAdded() || getContext() == null) return;
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

    private void showNotificationPreferencesDialog() {
        if (!isAdded() || getContext() == null) return;

        preferenceRepository.getPreferences().enqueue(new Callback<NotificationPreferences>() {
            @Override
            public void onResponse(@NonNull Call<NotificationPreferences> call, @NonNull Response<NotificationPreferences> response) {
                if (!isAdded() || getContext() == null) return;

                boolean events = true;
                boolean mentorship = true;
                boolean opportunities = true;
                boolean announcements = true;

                if (response.isSuccessful() && response.body() != null) {
                    NotificationPreferences prefs = response.body();
                    events = prefs.isEvents();
                    mentorship = prefs.isMentorship();
                    opportunities = prefs.isOpportunities();
                    announcements = prefs.isAnnouncements();
                }

                final boolean[] checkedItems = new boolean[]{events, mentorship, opportunities, announcements};
                final CharSequence[] items = new CharSequence[]{
                        "📅 Event Reminders & Updates",
                        "🤝 Mentorship Requests & Updates",
                        "💼 Career & Job Opportunities",
                        "📢 Campus Announcements"
                };

                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Notification Preferences")
                        .setMultiChoiceItems(items, checkedItems, (dialog, which, isChecked) -> {
                            checkedItems[which] = isChecked;
                        })
                        .setPositiveButton("Save", (dialog, which) -> {
                            NotificationPreferences updated = new NotificationPreferences(
                                    checkedItems[0],
                                    checkedItems[1],
                                    checkedItems[2],
                                    checkedItems[3]
                            );
                            saveNotificationPreferences(updated);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }

            @Override
            public void onFailure(@NonNull Call<NotificationPreferences> call, @NonNull Throwable t) {
                if (!isAdded() || getContext() == null) return;
                Toast.makeText(requireContext(), "Failed to load preferences: " + ApiErrorUtils.getNetworkErrorMessage(t), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveNotificationPreferences(NotificationPreferences prefs) {
        preferenceRepository.updatePreferences(prefs).enqueue(new Callback<NotificationPreferences>() {
            @Override
            public void onResponse(@NonNull Call<NotificationPreferences> call, @NonNull Response<NotificationPreferences> response) {
                if (!isAdded() || getContext() == null) return;
                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(), "Preferences updated successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Failed to update: " + ApiErrorUtils.getErrorMessage(response), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<NotificationPreferences> call, @NonNull Throwable t) {
                if (!isAdded() || getContext() == null) return;
                Toast.makeText(requireContext(), ApiErrorUtils.getNetworkErrorMessage(t), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private com.alumniconnect.app.models.User buildUserFromProfile(ProfileResponse p) {
        com.alumniconnect.app.models.User u = new com.alumniconnect.app.models.User();
        u.setId(p.getId());
        u.setName(p.getName());
        u.setEmail(p.getEmail());
        u.setRole(p.getRole());
        return u;
    }
}
