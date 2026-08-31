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
import com.alumniconnect.app.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import org.json.JSONObject;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EventDetailsActivity extends AppCompatActivity {

    private EventRepository eventRepository;
    private SessionManager sessionManager;

    private int eventId;
    private Event currentEvent;

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
        btnCancelRegistration.setOnClickListener(v -> cancelRegistration());
        btnJoinOnline.setOnClickListener(v -> joinOnline());
        btnViewRegistrations.setOnClickListener(v -> viewRegistrations());
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchEventDetails();
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
        eventRepository.getEventById(eventId).enqueue(new Callback<Event>() {
            @Override
            public void onResponse(Call<Event> call, Response<Event> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentEvent = response.body();
                    renderEvent(currentEvent);
                } else {
                    Toast.makeText(EventDetailsActivity.this, "Failed to load event details.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Event> call, Throwable t) {
                Toast.makeText(EventDetailsActivity.this, "Network error loading details.", Toast.LENGTH_SHORT).show();
            }
        });
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
        setRegistrationLoading(true);
        eventRepository.registerForEvent(eventId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                setRegistrationLoading(false);
                if (response.isSuccessful()) {
                    showSuccessMsg("Successfully registered! Checked-in successfully.");
                    fetchEventDetails();
                } else if (response.code() == 400) {
                    String error = parseErrorDetail(response);
                    if (error.contains("already registered")) {
                        showErrorMsg("You are already registered for this event.");
                    } else {
                        showErrorMsg(error);
                    }
                } else {
                    showErrorMsg("Failed to register. HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                setRegistrationLoading(false);
                showErrorMsg("Network error registering for event.");
            }
        });
    }

    private void cancelRegistration() {
        setRegistrationLoading(true);
        eventRepository.cancelEventRegistration(eventId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                setRegistrationLoading(false);
                if (response.isSuccessful()) {
                    showSuccessMsg("Registration cancelled successfully.");
                    fetchEventDetails();
                } else {
                    showErrorMsg("Failed to cancel registration.");
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                setRegistrationLoading(false);
                showErrorMsg("Network error cancelling registration.");
            }
        });
    }

    private void joinOnline() {
        if (currentEvent == null || !currentEvent.isOnline()) return;
        try {
            String url = currentEvent.getMeetingUrl();
            String sanitized = url.startsWith("http") ? url : "https://" + url;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(sanitized));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open meeting link.", Toast.LENGTH_SHORT).show();
        }
    }

    private void viewRegistrations() {
        Intent intent = new Intent(this, EventRegistrationsActivity.class);
        intent.putExtra("event_id", eventId);
        intent.putExtra("event_title", currentEvent != null ? currentEvent.getTitle() : "Event");
        startActivity(intent);
    }

    private void setRegistrationLoading(boolean loading) {
        progressRegistration.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!loading);
        btnCancelRegistration.setEnabled(!loading);
        tvStatusMsg.setVisibility(View.GONE);
    }

    private void showSuccessMsg(String msg) {
        tvStatusMsg.setText(msg);
        tvStatusMsg.setTextColor(getResources().getColor(R.color.success, null));
        tvStatusMsg.setBackgroundColor(0x1F10B981); // green tinted bg
        tvStatusMsg.setVisibility(View.VISIBLE);
    }

    private void showErrorMsg(String msg) {
        tvStatusMsg.setText(msg);
        tvStatusMsg.setTextColor(getResources().getColor(R.color.error, null));
        tvStatusMsg.setBackgroundColor(0x1FEF4444); // red tinted bg
        tvStatusMsg.setVisibility(View.VISIBLE);
    }

    private String parseErrorDetail(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String json = response.errorBody().string();
                JSONObject obj = new JSONObject(json);
                if (obj.has("detail")) return obj.getString("detail");
            }
        } catch (Exception ignored) {}
        return "Registration rejected by server.";
    }
}
