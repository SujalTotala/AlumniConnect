package com.alumniconnect.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
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
import com.alumniconnect.app.adapters.SavedItemAdapter;
import com.alumniconnect.app.models.Bookmark;
import com.alumniconnect.app.repositories.BookmarkRepository;
import com.alumniconnect.app.utils.ApiErrorUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SavedItemsActivity extends AppCompatActivity implements SavedItemAdapter.OnSavedItemClickListener {

    private BookmarkRepository bookmarkRepository;
    private SavedItemAdapter adapter;

    private ProgressBar progressBar;
    private View layoutError;
    private TextView tvError;
    private MaterialButton btnRetry;
    private View layoutEmpty;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView rvSaved;
    private ChipGroup chipGroup;

    private final List<Bookmark> allBookmarks = new ArrayList<>();
    private String currentFilter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_items);

        bookmarkRepository = new BookmarkRepository(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_saved_items);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Saved Items");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        progressBar = findViewById(R.id.progress_saved);
        layoutError = findViewById(R.id.layout_saved_error);
        tvError = findViewById(R.id.tv_saved_error);
        btnRetry = findViewById(R.id.btn_saved_retry);
        layoutEmpty = findViewById(R.id.layout_saved_empty);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_saved);
        rvSaved = findViewById(R.id.rv_saved);
        chipGroup = findViewById(R.id.chip_group_saved);

        rvSaved.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SavedItemAdapter(this, this);
        rvSaved.setAdapter(adapter);

        btnRetry.setOnClickListener(v -> fetchBookmarks(true));
        swipeRefreshLayout.setOnRefreshListener(() -> fetchBookmarks(false));

        chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chip_saved_alumni) {
                currentFilter = "alumni";
            } else if (checkedId == R.id.chip_saved_opps) {
                currentFilter = "opportunity";
            } else if (checkedId == R.id.chip_saved_events) {
                currentFilter = "event";
            } else {
                currentFilter = "all";
            }
            applyFilter();
        });

        fetchBookmarks(true);
    }

    private void fetchBookmarks(boolean showLoading) {
        if (showLoading) {
            progressBar.setVisibility(View.VISIBLE);
            layoutError.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.GONE);
            rvSaved.setVisibility(View.GONE);
        }

        bookmarkRepository.getBookmarks(null).enqueue(new Callback<List<Bookmark>>() {
            @Override
            public void onResponse(@NonNull Call<List<Bookmark>> call, @NonNull Response<List<Bookmark>> response) {
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    allBookmarks.clear();
                    allBookmarks.addAll(response.body());
                    applyFilter();
                } else {
                    showError(ApiErrorUtils.getErrorMessage(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Bookmark>> call, @NonNull Throwable t) {
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                showError(ApiErrorUtils.getNetworkErrorMessage(t));
            }
        });
    }

    private void applyFilter() {
        layoutError.setVisibility(View.GONE);
        List<Bookmark> filtered = new ArrayList<>();

        for (Bookmark b : allBookmarks) {
            if ("all".equalsIgnoreCase(currentFilter) || currentFilter.equalsIgnoreCase(b.getItemType())) {
                filtered.add(b);
            }
        }

        if (filtered.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvSaved.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvSaved.setVisibility(View.VISIBLE);
            adapter.setItems(filtered);
        }
    }

    private void showError(String msg) {
        layoutError.setVisibility(View.VISIBLE);
        tvError.setText(msg);
        layoutEmpty.setVisibility(View.GONE);
        rvSaved.setVisibility(View.GONE);
    }

    @Override
    public void onItemClick(Bookmark bookmark) {
        String type = bookmark.getItemType().toLowerCase();
        int itemId = bookmark.getItemId();

        if ("alumni".equals(type)) {
            Intent intent = new Intent(this, AlumniDetailsActivity.class);
            intent.putExtra("alumni_id", itemId);
            startActivity(intent);
        } else if ("opportunity".equals(type)) {
            Intent intent = new Intent(this, OpportunityDetailsActivity.class);
            intent.putExtra("opportunity_id", itemId);
            startActivity(intent);
        } else if ("event".equals(type)) {
            Intent intent = new Intent(this, EventDetailsActivity.class);
            intent.putExtra("event_id", itemId);
            startActivity(intent);
        }
    }

    @Override
    public void onUnsaveClick(Bookmark bookmark) {
        bookmarkRepository.deleteBookmark(bookmark.getId()).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(SavedItemsActivity.this, "Item removed from bookmarks", Toast.LENGTH_SHORT).show();
                    allBookmarks.remove(bookmark);
                    applyFilter();
                } else {
                    Toast.makeText(SavedItemsActivity.this, ApiErrorUtils.getErrorMessage(response), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                Toast.makeText(SavedItemsActivity.this, ApiErrorUtils.getNetworkErrorMessage(t), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
