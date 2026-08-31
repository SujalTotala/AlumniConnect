package com.alumniconnect.app.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.alumniconnect.app.R;
import com.alumniconnect.app.activities.CreateEventActivity;
import com.alumniconnect.app.activities.EventDetailsActivity;
import com.alumniconnect.app.adapters.EventAdapter;
import com.alumniconnect.app.models.Event;
import com.alumniconnect.app.repositories.EventRepository;
import com.alumniconnect.app.utils.SessionManager;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EventsFragment extends Fragment {

    private static final int DEBOUNCE_DELAY_MS = 400;

    private EventRepository eventRepository;
    private SessionManager sessionManager;
    private EventAdapter adapter;

    private TextInputEditText etSearch;
    private ChipGroup chipGroupEventType;
    private ProgressBar progressEvents;
    private RecyclerView rvEvents;
    private SwipeRefreshLayout swipeRefresh;
    private View layoutError, layoutEmpty;
    private TextView tvErrorMsg, tvResultCount, tvEmptyMsg;
    private View btnRetry;
    private FloatingActionButton fabCreateEvent;

    private String activeSearch = null;
    private String activeEventType = null;

    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable debounceRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_events, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        eventRepository = new EventRepository(requireContext());
        sessionManager = new SessionManager(requireContext());

        etSearch = view.findViewById(R.id.et_event_search);
        chipGroupEventType = view.findViewById(R.id.chip_group_event_type);
        progressEvents = view.findViewById(R.id.progress_events);
        rvEvents = view.findViewById(R.id.rv_events);
        swipeRefresh = view.findViewById(R.id.swipe_refresh_events);
        layoutError = view.findViewById(R.id.layout_events_error);
        layoutEmpty = view.findViewById(R.id.layout_events_empty);
        tvErrorMsg = view.findViewById(R.id.tv_events_error_msg);
        tvResultCount = view.findViewById(R.id.tv_event_count);
        tvEmptyMsg = view.findViewById(R.id.tv_events_empty_msg);
        btnRetry = view.findViewById(R.id.btn_events_retry);
        fabCreateEvent = view.findViewById(R.id.fab_create_event);

        // RecyclerView
        adapter = new EventAdapter(event -> openEventDetails(event));
        rvEvents.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvEvents.setAdapter(adapter);

        // SwipeRefresh
        swipeRefresh.setColorSchemeResources(R.color.primary);
        swipeRefresh.setOnRefreshListener(() -> loadEvents(true));

        // Search with debounce
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (debounceRunnable != null) debounceHandler.removeCallbacks(debounceRunnable);
                debounceRunnable = () -> {
                    activeSearch = s.toString().trim().isEmpty() ? null : s.toString().trim();
                    loadEvents(false);
                };
                debounceHandler.postDelayed(debounceRunnable, DEBOUNCE_DELAY_MS);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Filter chips group
        chipGroupEventType.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                activeEventType = null;
            } else {
                int id = checkedIds.get(0);
                if (id == R.id.chip_all_events) {
                    activeEventType = null;
                } else if (id == R.id.chip_alumni_meet) {
                    activeEventType = "Alumni Meet";
                } else if (id == R.id.chip_webinar) {
                    activeEventType = "Webinar";
                } else if (id == R.id.chip_workshop) {
                    activeEventType = "Workshop";
                } else if (id == R.id.chip_networking) {
                    activeEventType = "Networking";
                } else if (id == R.id.chip_career) {
                    activeEventType = "Career Guidance";
                } else if (id == R.id.chip_guest_lecture) {
                    activeEventType = "Guest Lecture";
                } else if (id == R.id.chip_reunion) {
                    activeEventType = "Reunion";
                }
            }
            loadEvents(false);
        });

        // Retry button
        btnRetry.setOnClickListener(v -> loadEvents(false));

        // FAB visibility for Event creation
        setupFAB();

        // Load
        loadEvents(false);
    }

    private void setupFAB() {
        String role = sessionManager.getUserRole().toLowerCase();
        if ("admin".equals(role) || "alumni".equals(role)) {
            fabCreateEvent.setVisibility(View.VISIBLE);
            fabCreateEvent.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), CreateEventActivity.class);
                startActivity(intent);
            });
        } else {
            fabCreateEvent.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Load latest list when fragment resumes
        loadEvents(false);
    }

    private void loadEvents(boolean fromSwipe) {
        if (!fromSwipe) {
            progressEvents.setVisibility(View.VISIBLE);
            layoutError.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.GONE);
            rvEvents.setVisibility(View.GONE);
            tvResultCount.setText("");
        }

        eventRepository.getEvents(activeEventType, activeSearch).enqueue(new Callback<List<Event>>() {
            @Override
            public void onResponse(Call<List<Event>> call, Response<List<Event>> response) {
                progressEvents.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null) {
                    List<Event> list = response.body();
                    layoutError.setVisibility(View.GONE);

                    if (list.isEmpty()) {
                        rvEvents.setVisibility(View.GONE);
                        layoutEmpty.setVisibility(View.VISIBLE);
                        tvEmptyMsg.setText(hasActiveFilters()
                                ? "No events match your search/filters."
                                : "No upcoming events scheduled.");
                        tvResultCount.setText("");
                    } else {
                        adapter.setEventList(list);
                        rvEvents.setVisibility(View.VISIBLE);
                        layoutEmpty.setVisibility(View.GONE);
                        tvResultCount.setText(list.size() + " events found");
                    }
                } else if (response.code() == 401) {
                    showError("Session expired. Please login again.");
                } else {
                    showError("Server error (HTTP " + response.code() + "). Pull to retry.");
                }
            }

            @Override
            public void onFailure(Call<List<Event>> call, Throwable t) {
                progressEvents.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                showError("Network unavailable. Check connection and retry.");
            }
        });
    }

    private void showError(String message) {
        layoutError.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);
        rvEvents.setVisibility(View.GONE);
        tvErrorMsg.setText(message);
        tvResultCount.setText("");
    }

    private boolean hasActiveFilters() {
        return (activeSearch != null && !activeSearch.isEmpty())
                || activeEventType != null;
    }

    private void openEventDetails(Event event) {
        Intent intent = new Intent(requireContext(), EventDetailsActivity.class);
        intent.putExtra("event_id", event.getId());
        intent.putExtra("event_title", event.getTitle());
        startActivity(intent);
    }
}
