package com.alumniconnect.app.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.alumniconnect.app.R;
import com.alumniconnect.app.adapters.SuccessStoriesAdapter;
import com.alumniconnect.app.models.SuccessStory;
import com.alumniconnect.app.models.SuccessStoryCreateRequest;
import com.alumniconnect.app.repositories.StoryRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SuccessStoriesActivity extends AppCompatActivity {

    private StoryRepository storyRepository;
    private SuccessStoriesAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvStories;
    private ProgressBar progressBar;
    private View layoutEmpty;
    private ExtendedFloatingActionButton fabSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_success_stories);

        storyRepository = new StoryRepository(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_stories);
        toolbar.setNavigationOnClickListener(v -> finish());

        swipeRefresh = findViewById(R.id.swipe_stories);
        rvStories = findViewById(R.id.rv_stories);
        progressBar = findViewById(R.id.progress_stories);
        layoutEmpty = findViewById(R.id.layout_empty_stories);
        fabSubmit = findViewById(R.id.fab_submit_story);

        adapter = new SuccessStoriesAdapter();
        rvStories.setLayoutManager(new LinearLayoutManager(this));
        rvStories.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::loadStories);
        fabSubmit.setOnClickListener(v -> showSubmitStoryDialog());

        loadStories();
    }

    private void loadStories() {
        progressBar.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);

        storyRepository.getApprovedStories().enqueue(new Callback<List<SuccessStory>>() {
            @Override
            public void onResponse(@NonNull Call<List<SuccessStory>> call, @NonNull Response<List<SuccessStory>> response) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<SuccessStory> list = response.body();
                    adapter.setList(list);
                    if (list.isEmpty()) {
                        layoutEmpty.setVisibility(View.VISIBLE);
                    }
                } else {
                    layoutEmpty.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<SuccessStory>> call, @NonNull Throwable t) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                layoutEmpty.setVisibility(View.VISIBLE);
                Toast.makeText(SuccessStoriesActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSubmitStoryDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_submit_story, null);
        TextInputEditText etTitle = dialogView.findViewById(R.id.et_story_title);
        TextInputEditText etCompany = dialogView.findViewById(R.id.et_story_company);
        TextInputEditText etRole = dialogView.findViewById(R.id.et_story_role);
        TextInputEditText etGradYear = dialogView.findViewById(R.id.et_story_grad_year);
        TextInputEditText etSummary = dialogView.findViewById(R.id.et_story_summary);
        TextInputEditText etContent = dialogView.findViewById(R.id.et_story_content);
        ProgressBar dialogProgress = dialogView.findViewById(R.id.progress_dialog_story);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Submit Story", null)
                .setNegativeButton("Cancel", (d, which) -> d.dismiss())
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
                String content = etContent.getText() != null ? etContent.getText().toString().trim() : "";
                String company = etCompany.getText() != null ? etCompany.getText().toString().trim() : "";
                String role = etRole.getText() != null ? etRole.getText().toString().trim() : "";
                String summary = etSummary.getText() != null ? etSummary.getText().toString().trim() : "";
                String gradYearStr = etGradYear.getText() != null ? etGradYear.getText().toString().trim() : "";

                if (title.isEmpty()) {
                    etTitle.setError("Title is required");
                    return;
                }
                if (content.isEmpty()) {
                    etContent.setError("Content is required");
                    return;
                }

                Integer gradYear = null;
                if (!gradYearStr.isEmpty()) {
                    try {
                        gradYear = Integer.parseInt(gradYearStr);
                    } catch (NumberFormatException ignored) {}
                }

                dialogProgress.setVisibility(View.VISIBLE);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

                SuccessStoryCreateRequest request = new SuccessStoryCreateRequest(
                        title, content, summary, company, role, gradYear, null
                );

                storyRepository.submitStory(request).enqueue(new Callback<SuccessStory>() {
                    @Override
                    public void onResponse(@NonNull Call<SuccessStory> call, @NonNull Response<SuccessStory> response) {
                        dialogProgress.setVisibility(View.GONE);
                        if (response.isSuccessful()) {
                            dialog.dismiss();
                            Toast.makeText(SuccessStoriesActivity.this,
                                    "Story submitted! It will appear once approved by an administrator.",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                            Toast.makeText(SuccessStoriesActivity.this, "Submission failed: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<SuccessStory> call, @NonNull Throwable t) {
                        dialogProgress.setVisibility(View.GONE);
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        Toast.makeText(SuccessStoriesActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });

        dialog.show();
    }
}
