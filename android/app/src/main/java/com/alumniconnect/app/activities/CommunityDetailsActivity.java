package com.alumniconnect.app.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.alumniconnect.app.R;
import com.alumniconnect.app.adapters.CommunityPostsAdapter;
import com.alumniconnect.app.models.CommunityPost;
import com.alumniconnect.app.repositories.CommunityRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CommunityDetailsActivity extends AppCompatActivity {

    private int communityId;
    private String communityName;
    private String communityDesc;
    private String communityType;
    private String communityLocation;
    private int memberCount;
    private boolean isMember;

    private CommunityRepository communityRepository;
    private CommunityPostsAdapter adapter;

    private TextView tvType;
    private TextView tvMembers;
    private TextView tvName;
    private TextView tvDesc;
    private TextView tvLocation;
    private MaterialButton btnMembership;
    private View cardComposer;
    private TextInputEditText etPostContent;
    private MaterialButton btnSendPost;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvPosts;
    private ProgressBar progressBar;
    private View layoutEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_details);

        communityRepository = new CommunityRepository(this);

        communityId = getIntent().getIntExtra("community_id", -1);
        communityName = getIntent().getStringExtra("community_name");
        communityDesc = getIntent().getStringExtra("community_desc");
        communityType = getIntent().getStringExtra("community_type");
        communityLocation = getIntent().getStringExtra("community_location");
        memberCount = getIntent().getIntExtra("community_member_count", 0);
        isMember = getIntent().getBooleanExtra("community_is_member", false);

        if (communityId == -1) {
            Toast.makeText(this, "Invalid Community ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        bindHeaderData();
        loadPosts();
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_community_details);
        toolbar.setTitle(communityName != null ? communityName : "Chapter");
        toolbar.setNavigationOnClickListener(v -> finish());

        tvType = findViewById(R.id.tv_cd_type);
        tvMembers = findViewById(R.id.tv_cd_members);
        tvName = findViewById(R.id.tv_cd_name);
        tvDesc = findViewById(R.id.tv_cd_desc);
        tvLocation = findViewById(R.id.tv_cd_location);
        btnMembership = findViewById(R.id.btn_cd_membership);
        cardComposer = findViewById(R.id.card_post_composer);
        etPostContent = findViewById(R.id.et_new_post_content);
        btnSendPost = findViewById(R.id.btn_send_post);
        swipeRefresh = findViewById(R.id.swipe_community_posts);
        rvPosts = findViewById(R.id.rv_community_posts);
        progressBar = findViewById(R.id.progress_community_posts);
        layoutEmpty = findViewById(R.id.layout_empty_posts);

        adapter = new CommunityPostsAdapter();
        rvPosts.setLayoutManager(new LinearLayoutManager(this));
        rvPosts.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::loadPosts);

        btnMembership.setOnClickListener(v -> toggleMembership());

        btnSendPost.setOnClickListener(v -> {
            String content = etPostContent.getText() != null ? etPostContent.getText().toString().trim() : "";
            if (content.isEmpty()) return;
            submitPost(content);
        });
    }

    private void bindHeaderData() {
        if (communityType != null) {
            tvType.setText(communityType.toUpperCase().replace("_", " "));
        }
        tvMembers.setText("👥 " + memberCount + " " + (memberCount == 1 ? "member" : "members"));
        if (communityName != null) tvName.setText(communityName);
        if (communityDesc != null) tvDesc.setText(communityDesc);

        if (communityLocation != null && !communityLocation.trim().isEmpty()) {
            tvLocation.setText("📍 " + communityLocation);
            tvLocation.setVisibility(View.VISIBLE);
        } else {
            tvLocation.setVisibility(View.GONE);
        }

        updateMembershipUI();
    }

    private void updateMembershipUI() {
        if (isMember) {
            btnMembership.setText("Leave Chapter");
            btnMembership.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    getResources().getColor(R.color.error)));
            cardComposer.setVisibility(View.VISIBLE);
        } else {
            btnMembership.setText("Join Chapter");
            btnMembership.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    getResources().getColor(R.color.primary)));
            cardComposer.setVisibility(View.GONE);
        }
    }

    private void toggleMembership() {
        btnMembership.setEnabled(false);
        if (isMember) {
            communityRepository.leaveCommunity(communityId).enqueue(new Callback<java.util.Map<String, Object>>() {
                @Override
                public void onResponse(@NonNull Call<java.util.Map<String, Object>> call, @NonNull Response<java.util.Map<String, Object>> response) {
                    btnMembership.setEnabled(true);
                    if (response.isSuccessful()) {
                        isMember = false;
                        memberCount = Math.max(0, memberCount - 1);
                        tvMembers.setText("👥 " + memberCount + " members");
                        updateMembershipUI();
                        Toast.makeText(CommunityDetailsActivity.this, "Left chapter", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(CommunityDetailsActivity.this, "Action failed", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<java.util.Map<String, Object>> call, @NonNull Throwable t) {
                    btnMembership.setEnabled(true);
                    Toast.makeText(CommunityDetailsActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            communityRepository.joinCommunity(communityId).enqueue(new Callback<java.util.Map<String, Object>>() {
                @Override
                public void onResponse(@NonNull Call<java.util.Map<String, Object>> call, @NonNull Response<java.util.Map<String, Object>> response) {
                    btnMembership.setEnabled(true);
                    if (response.isSuccessful()) {
                        isMember = true;
                        memberCount++;
                        tvMembers.setText("👥 " + memberCount + " members");
                        updateMembershipUI();
                        Toast.makeText(CommunityDetailsActivity.this, "Joined chapter!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(CommunityDetailsActivity.this, "Action failed", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<java.util.Map<String, Object>> call, @NonNull Throwable t) {
                    btnMembership.setEnabled(true);
                    Toast.makeText(CommunityDetailsActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void loadPosts() {
        progressBar.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);

        communityRepository.getCommunityPosts(communityId).enqueue(new Callback<List<CommunityPost>>() {
            @Override
            public void onResponse(@NonNull Call<List<CommunityPost>> call, @NonNull Response<List<CommunityPost>> response) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<CommunityPost> list = response.body();
                    adapter.setList(list);
                    if (list.isEmpty()) {
                        layoutEmpty.setVisibility(View.VISIBLE);
                    }
                } else {
                    layoutEmpty.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<CommunityPost>> call, @NonNull Throwable t) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                layoutEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    private void submitPost(String content) {
        btnSendPost.setEnabled(false);
        communityRepository.createCommunityPost(communityId, content).enqueue(new Callback<CommunityPost>() {
            @Override
            public void onResponse(@NonNull Call<CommunityPost> call, @NonNull Response<CommunityPost> response) {
                btnSendPost.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    etPostContent.setText("");
                    layoutEmpty.setVisibility(View.GONE);
                    adapter.addPost(response.body());
                    rvPosts.smoothScrollToPosition(0);
                    Toast.makeText(CommunityDetailsActivity.this, "Post shared!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(CommunityDetailsActivity.this, "Failed to share post (membership required)", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<CommunityPost> call, @NonNull Throwable t) {
                btnSendPost.setEnabled(true);
                Toast.makeText(CommunityDetailsActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
