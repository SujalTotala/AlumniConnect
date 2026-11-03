package com.alumniconnect.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.alumniconnect.app.R;
import com.alumniconnect.app.adapters.CommunitiesAdapter;
import com.alumniconnect.app.models.Community;
import com.alumniconnect.app.models.CommunityCreateRequest;
import com.alumniconnect.app.repositories.CommunityRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CommunitiesActivity extends AppCompatActivity implements CommunitiesAdapter.OnCommunityClickListener {

    private CommunityRepository communityRepository;
    private CommunitiesAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvCommunities;
    private ProgressBar progressBar;
    private View layoutEmpty;
    private TextInputEditText etSearch;
    private ChipGroup chipGroup;
    private ExtendedFloatingActionButton fabCreate;

    private String selectedType = null;
    private String currentSearch = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_communities);

        communityRepository = new CommunityRepository(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_communities);
        toolbar.setNavigationOnClickListener(v -> finish());

        swipeRefresh = findViewById(R.id.swipe_communities);
        rvCommunities = findViewById(R.id.rv_communities);
        progressBar = findViewById(R.id.progress_communities);
        layoutEmpty = findViewById(R.id.layout_empty_communities);
        etSearch = findViewById(R.id.et_search_communities);
        chipGroup = findViewById(R.id.chip_group_comm_types);
        fabCreate = findViewById(R.id.fab_create_community);

        adapter = new CommunitiesAdapter(this);
        rvCommunities.setLayoutManager(new LinearLayoutManager(this));
        rvCommunities.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::loadCommunities);

        chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chip_type_city) {
                selectedType = "city";
            } else if (checkedId == R.id.chip_type_industry) {
                selectedType = "industry";
            } else if (checkedId == R.id.chip_type_batch) {
                selectedType = "batch";
            } else if (checkedId == R.id.chip_type_dept) {
                selectedType = "department";
            } else if (checkedId == R.id.chip_type_general) {
                selectedType = "general";
            } else {
                selectedType = null;
            }
            loadCommunities();
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearch = (s != null && s.toString().trim().length() > 0) ? s.toString().trim() : null;
                loadCommunities();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        fabCreate.setOnClickListener(v -> showCreateCommunityDialog());

        loadCommunities();
    }

    private void loadCommunities() {
        progressBar.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);

        communityRepository.getCommunities(selectedType, currentSearch).enqueue(new Callback<List<Community>>() {
            @Override
            public void onResponse(@NonNull Call<List<Community>> call, @NonNull Response<List<Community>> response) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<Community> list = response.body();
                    adapter.setList(list);
                    if (list.isEmpty()) {
                        layoutEmpty.setVisibility(View.VISIBLE);
                    }
                } else {
                    layoutEmpty.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Community>> call, @NonNull Throwable t) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                layoutEmpty.setVisibility(View.VISIBLE);
                Toast.makeText(CommunitiesActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onCommunityClick(Community community) {
        Intent intent = new Intent(this, CommunityDetailsActivity.class);
        intent.putExtra("community_id", community.getId());
        intent.putExtra("community_name", community.getName());
        intent.putExtra("community_desc", community.getDescription());
        intent.putExtra("community_type", community.getType());
        intent.putExtra("community_location", community.getLocation());
        intent.putExtra("community_member_count", community.getMemberCount() != null ? community.getMemberCount() : 0);
        intent.putExtra("community_is_member", Boolean.TRUE.equals(community.isMember()));
        startActivity(intent);
    }

    @Override
    public void onJoinLeaveClick(Community community) {
        boolean isMember = Boolean.TRUE.equals(community.isMember());
        if (isMember) {
            communityRepository.leaveCommunity(community.getId()).enqueue(new Callback<java.util.Map<String, Object>>() {
                @Override
                public void onResponse(@NonNull Call<java.util.Map<String, Object>> call, @NonNull Response<java.util.Map<String, Object>> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(CommunitiesActivity.this, "Left chapter", Toast.LENGTH_SHORT).show();
                        loadCommunities();
                    } else {
                        Toast.makeText(CommunitiesActivity.this, "Action failed", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<java.util.Map<String, Object>> call, @NonNull Throwable t) {
                    Toast.makeText(CommunitiesActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            communityRepository.joinCommunity(community.getId()).enqueue(new Callback<java.util.Map<String, Object>>() {
                @Override
                public void onResponse(@NonNull Call<java.util.Map<String, Object>> call, @NonNull Response<java.util.Map<String, Object>> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(CommunitiesActivity.this, "Joined chapter!", Toast.LENGTH_SHORT).show();
                        loadCommunities();
                    } else {
                        Toast.makeText(CommunitiesActivity.this, "Action failed", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<java.util.Map<String, Object>> call, @NonNull Throwable t) {
                    Toast.makeText(CommunitiesActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void showCreateCommunityDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_community, null);
        TextInputEditText etName = dialogView.findViewById(R.id.et_comm_name);
        Spinner spinnerType = dialogView.findViewById(R.id.spinner_comm_type);
        TextInputEditText etLocation = dialogView.findViewById(R.id.et_comm_location);
        TextInputEditText etDesc = dialogView.findViewById(R.id.et_comm_desc);
        ProgressBar dialogProgress = dialogView.findViewById(R.id.progress_dialog_comm);

        String[] types = {"City", "Industry", "Batch", "Department", "General"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types);
        spinnerType.setAdapter(spinnerAdapter);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Create Chapter", null)
                .setNegativeButton("Cancel", (d, which) -> d.dismiss())
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String name = etName.getText() != null ? etName.getText().toString().trim() : "";
                String desc = etDesc.getText() != null ? etDesc.getText().toString().trim() : "";
                String location = etLocation.getText() != null ? etLocation.getText().toString().trim() : "";
                String selectedTypeItem = spinnerType.getSelectedItem().toString().toLowerCase();

                if (name.isEmpty()) {
                    etName.setError("Chapter name is required");
                    return;
                }
                if (desc.isEmpty()) {
                    etDesc.setError("Description is required");
                    return;
                }

                dialogProgress.setVisibility(View.VISIBLE);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

                CommunityCreateRequest request = new CommunityCreateRequest(name, desc, selectedTypeItem, location, null);

                communityRepository.createCommunity(request).enqueue(new Callback<Community>() {
                    @Override
                    public void onResponse(@NonNull Call<Community> call, @NonNull Response<Community> response) {
                        dialogProgress.setVisibility(View.GONE);
                        if (response.isSuccessful()) {
                            dialog.dismiss();
                            Toast.makeText(CommunitiesActivity.this, "Chapter created successfully!", Toast.LENGTH_SHORT).show();
                            loadCommunities();
                        } else {
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                            Toast.makeText(CommunitiesActivity.this, "Creation failed: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Community> call, @NonNull Throwable t) {
                        dialogProgress.setVisibility(View.GONE);
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        Toast.makeText(CommunitiesActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });

        dialog.show();
    }
}
