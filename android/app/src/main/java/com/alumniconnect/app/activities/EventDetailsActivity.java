package com.alumniconnect.app.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.alumniconnect.app.R;
import com.alumniconnect.app.models.Event;
import com.alumniconnect.app.repositories.EventRepository;
import com.alumniconnect.app.utils.ApiErrorUtils;
import com.alumniconnect.app.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EventDetailsActivity extends AppCompatActivity {

    private EventRepository eventRepository;
    private SessionManager sessionManager;

    private int eventId;
    private Event currentEvent;
    private boolean isRegistrationInProgress = false;

    // State Views
    private View layoutContent;
    private ProgressBar progressDetail;
    private View layoutError;
    private TextView tvErrorMsg;
    private MaterialButton btnRetry;

    // Header Views
    private TextView tvEdTypeEmoji, tvEdTitle, tvEdType, tvEdRegisteredBadge;
    // Detail Views
    private TextView tvEdDate, tvEdTime, tvEdLocation, tvEdRegCount, tvEdDescription;
    private LinearLayout rowEdTime, rowEdLocation, rowEdDescription;
    // Buttons
    private MaterialButton btnRegister, btnCancelRegistration, btnJoinOnline, btnViewRegistrations;
    private ProgressBar progressRegistration;
    private TextView tvStatusMsg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        eventRepository = new EventRepository(this);
        sessionManager = new SessionManager(this);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Event Details");
        }

        // State views
        layoutContent = findViewById(R.id.layout_event_content);
        progressDetail = findViewById(R.id.progress_event_detail);
        layoutError = findViewById(R.id.layout_event_detail_error);
        tvErrorMsg = findViewById(R.id.tv_event_detail_error_msg);
        btnRetry = findViewById(R.id.btn_event_detail_retry);

        // Bind
        tvEdTypeEmoji = findViewById(R.id.tv_ed_type_emoji);
        tvEdTitle = findViewById(R.id.tv_ed_title);
        tvEdType = findViewById(R.id.tv_ed_type);
        tvEdRegisteredBadge = findViewById(R.id.tv_ed_registered_badge);

        tvEdDate = findViewById(R.id.tv_ed_date);
        tvEdTime = findViewById(R.id.tv_ed_time);
        tvEdLocation = findViewById(R.id.tv_ed_location);
        tvEdRegCount = findViewById(R.id.tv_ed_reg_count);
        tvEdDescription = findViewById(R.id.tv_ed_description);

        rowEdTime = findViewById(R.id.row_ed_time);
        rowEdLocation = findViewById(R.id.row_ed_location);
        rowEdDescription = findViewById(R.id.row_ed_description);

        btnRegister = findViewById(R.id.btn_register_event);
        btnCancelRegistration = findViewById(R.id.btn_cancel_registration);
        btnJoinOnline = findViewById(R.id.btn_join_online);
        btnViewRegistrations = findViewById(R.id.btn_view_registrations);
        progressRegistration = findViewById(R.id.progress_registration);
        tvStatusMsg = findViewById(R.id.tv_registration_status_msg);

        eventId = getIntent().getIntExtra("event_id", -1);
        String title = getIntent().getStringExtra("event_title");

        if (title != null) {
            tvEdTitle.setText(title);
        }

        if (eventId == -1) {
            Toast.makeText(this, "Invalid Event", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Listeners
        btnRegister.setOnClickListener(v -> registerForEvent());
        btnCancelRegistration.setOnClickListener(v -> confirmCancelRegistration());
        btnJoinOnline.setOnClickListener(v -> joinOnline());
        btnViewRegistrations.setOnClickListener(v -> viewRegistrations());
        btnRetry.setOnClickListener(v -> fetchEventDetails());

        fetchEventDetails();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh when coming back from sub-activities
        if (currentEvent != null) {
            fetchEventDetails();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void fetchEventDetails() {
        showLoadingState();

        eventRepository.getEventById(eventId).enqueue(new Callback<Event>() {
            @Override
            public void onResponse(Call<Event> call, Response<Event> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentEvent = response.body();
                    showSuccessState();
                    renderEvent(currentEvent);
                } else {
                    showErrorState(ApiErrorUtils.parseError(response));
                }
            }

            @Override
            public void onFailure(Call<Event> call, Throwable t) {
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

    private void renderEvent(Event event) {
        tvEdTypeEmoji.setText(event.getTypeEmoji());
        tvEdTitle.setText(event.getTitle());
        tvEdType.setText(event.getEventType());

        if (event.isRegistered()) {
            tvEdRegisteredBadge.setVisibility(View.VISIBLE);
        } else {
            tvEdRegisteredBadge.setVisibility(View.GONE);
        }

        tvEdDate.setText(event.getFormattedDate());

        if (Event.hasValue(event.getFormattedTime())) {
            tvEdTime.setText(event.getFormattedTime());
            rowEdTime.setVisibility(View.VISIBLE);
        } else {
            rowEdTime.setVisibility(View.GONE);
        }

        if (Event.hasValue(event.getLocation())) {
            tvEdLocation.setText(event.getLocation());
            rowEdLocation.setVisibility(View.VISIBLE);
        } else {
            rowEdLocation.setVisibility(View.GONE);
        }

        tvEdRegCount.setText(event.getRegistrationsCount() + " attendees registered");

        if (Event.hasValue(event.getDescription())) {
            tvEdDescription.setText(event.getDescription());
            rowEdDescription.setVisibility(View.VISIBLE);
        } else {
            rowEdDescription.setVisibility(View.GONE);
        }

        // Action Buttons state
        progressRegistration.setVisibility(View.GONE);
        tvStatusMsg.setVisibility(View.GONE);

        if (event.isRegistered()) {
            btnRegister.setVisibility(View.GONE);
            btnCancelRegistration.setVisibility(View.VISIBLE);

            if (event.isOnline()) {
                btnJoinOnline.setVisibility(View.VISIBLE);
            } else {
                btnJoinOnline.setVisibility(View.GONE);
            }
        } else {
            btnRegister.setVisibility(View.VISIBLE);
            btnCancelRegistration.setVisibility(View.GONE);
            btnJoinOnline.setVisibility(View.GONE);
        }

        // View Registrations (Admin or Creator)
        String userRole = sessionManager.getUserRole().toLowerCase();
        boolean isCreator = event.getCreatedBy() != null && event.getCreatedBy() == sessionManager.getUserId();
        if ("admin".equals(userRole) || isCreator) {
            btnViewRegistrations.setVisibility(View.VISIBLE);
        } else {
            btnViewRegistrations.setVisibility(View.GONE);
        }
    }

    private void registerForEvent() {
        if (isRegistrationInProgress) return;
        setRegistrationLoading(true);

        eventRepository.registerForEvent(eventId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                setRegistrationLoading(false);
                if (response.isSuccessful()) {
                    showSuccessMsg("Successfully registered for event!");
                    fetchEventDetailsSilently();
                } else {
                    String error = ApiErrorUtils.parseError(response);
                    showErrorMsg(error);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                setRegistrationLoading(false);
                showErrorMsg(ApiErrorUtils.parseThrowable(t));
            }
        });
    }

    private void confirmCancelRegistration() {
        if (isRegistrationInProgress) return;
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_cancel_rsvp_title)
                .setMessage(R.string.dialog_cancel_rsvp_message)
                .setPositiveButton(R.string.dialog_cancel_rsvp_positive, (dialog, which) -> cancelRegistration())
                .setNegativeButton(R.string.dialog_keep_rsvp_negative, null)
                .show();
    }

    private void cancelRegistration() {
        if (isRegistrationInProgress) return;
        setRegistrationLoading(true);

        eventRepository.cancelEventRegistration(eventId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                setRegistrationLoading(false);
                if (response.isSuccessful()) {
                    showSuccessMsg("Registration cancelled successfully.");
                    fetchEventDetailsSilently();
                } else {
                    showErrorMsg(ApiErrorUtils.parseError(response));
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                setRegistrationLoading(false);
                showErrorMsg(ApiErrorUtils.parseThrowable(t));
            }
        });
    }

    private void fetchEventDetailsSilently() {
        eventRepository.getEventById(eventId).enqueue(new Callback<Event>() {
            @Override
            public void onResponse(Call<Event> call, Response<Event> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentEvent = response.body();
                    renderEvent(currentEvent);
                }
            }

            @Override
            public void onFailure(Call<Event> call, Throwable t) {
                // Keep existing UI if silent refresh fails
            }
        });
    }

    private void joinOnline() {
        if (currentEvent == null || !currentEvent.isOnline()) return;
        com.alumniconnect.app.utils.UrlUtils.openUrlSafely(this, currentEvent.getMeetingUrl());
    }

    private void viewRegistrations() {
        Intent intent = new Intent(this, EventRegistrationsActivity.class);
        intent.putExtra("event_id", eventId);
        intent.putExtra("event_title", currentEvent != null ? currentEvent.getTitle() : "Event");
        startActivity(intent);
    }

    private void setRegistrationLoading(boolean loading) {
        isRegistrationInProgress = loading;
        progressRegistration.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!loading);
        btnCancelRegistration.setEnabled(!loading);
        tvStatusMsg.setVisibility(View.GONE);
    }

    private void showSuccessMsg(String msg) {
        tvStatusMsg.setText(msg);
        tvStatusMsg.setTextColor(getResources().getColor(R.color.success, null));
        tvStatusMsg.setBackgroundColor(0x1F10B981);
        tvStatusMsg.setVisibility(View.VISIBLE);
    }

    private void showErrorMsg(String msg) {
        tvStatusMsg.setText(msg);
        tvStatusMsg.setTextColor(getResources().getColor(R.color.error, null));
        tvStatusMsg.setBackgroundColor(0x1FEF4444);
        tvStatusMsg.setVisibility(View.VISIBLE);
    }
}
