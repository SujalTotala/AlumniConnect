package com.alumniconnect.app.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.alumniconnect.app.R;
import com.alumniconnect.app.models.Event;
import com.alumniconnect.app.models.EventCreateRequest;
import com.alumniconnect.app.repositories.EventRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import org.json.JSONObject;
import java.util.Calendar;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateEventActivity extends AppCompatActivity {

    private EventRepository eventRepository;

    private TextInputEditText etTitle, etDesc, etDate, etTime, etLocation, etUrl;
    private AutoCompleteTextView actvType;
    private MaterialButton btnSubmit;
    private ProgressBar progressCe;
    private TextView tvError, tvSuccess;

    private final String[] eventTypes = {
            "Alumni Meet",
            "Webinar",
            "Workshop",
            "Networking",
            "Career Guidance",
            "Reunion",
            "Guest Lecture"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        eventRepository = new EventRepository(this);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Create Event");
        }

        etTitle = findViewById(R.id.et_ce_title);
        etDesc = findViewById(R.id.et_ce_desc);
        actvType = findViewById(R.id.actv_ce_type);
        etDate = findViewById(R.id.et_ce_date);
        etTime = findViewById(R.id.et_ce_time);
        etLocation = findViewById(R.id.et_ce_location);
        etUrl = findViewById(R.id.et_ce_url);

        btnSubmit = findViewById(R.id.btn_create_event_submit);
        progressCe = findViewById(R.id.progress_ce);
        tvError = findViewById(R.id.tv_ce_error);
        tvSuccess = findViewById(R.id.tv_ce_success);

        // Event Type Adapter
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, eventTypes);
        actvType.setAdapter(typeAdapter);
        actvType.setText(eventTypes[0], false);

        // Date Picker
        etDate.setOnClickListener(v -> showDatePicker());

        // Time Picker
        etTime.setOnClickListener(v -> showTimePicker());

        btnSubmit.setOnClickListener(v -> submitEvent());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dpd = new DatePickerDialog(this, (view, year1, monthOfYear, dayOfMonth) -> {
            // format YYYY-MM-DD
            String formatted = String.format("%d-%02d-%02d", year1, (monthOfYear + 1), dayOfMonth);
            etDate.setText(formatted);
        }, year, month, day);
        dpd.show();
    }

    private void showTimePicker() {
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        TimePickerDialog tpd = new TimePickerDialog(this, (view, hourOfDay, minute1) -> {
            // format HH:MM:00
            String formatted = String.format("%02d:%02d:00", hourOfDay, minute1);
            etTime.setText(formatted);
        }, hour, minute, true);
        tpd.show();
    }

    private void submitEvent() {
        String title = etTitle.getText().toString().trim();
        String desc = etDesc.getText().toString().trim();
        String type = actvType.getText().toString().trim();
        String date = etDate.getText().toString().trim();
        String time = etTime.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String url = etUrl.getText().toString().trim();

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(desc) || TextUtils.isEmpty(date) || TextUtils.isEmpty(location)) {
            showError("Please fill out all required fields (*).");
            return;
        }

        EventCreateRequest req = new EventCreateRequest();
        req.setTitle(title);
        req.setDescription(desc);
        req.setEventType(type);
        req.setEventDate(date);
        req.setStartTime(time.isEmpty() ? null : time);
        req.setLocation(location);
        req.setMeetingUrl(url.isEmpty() ? null : url);

        setLoading(true);
        hideMessages();

        eventRepository.createEvent(req).enqueue(new Callback<Event>() {
            @Override
            public void onResponse(Call<Event> call, Response<Event> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    showSuccess("Event created successfully!");
                    // Clear inputs
                    etTitle.setText("");
                    etDesc.setText("");
                    etDate.setText("");
                    etTime.setText("");
                    etLocation.setText("");
                    etUrl.setText("");
                } else if (response.code() == 422) {
                    showError("Validation error. Please verify input fields.");
                } else {
                    String msg = parseErrorDetail(response);
                    showError("Error: " + msg);
                }
            }

            @Override
            public void onFailure(Call<Event> call, Throwable t) {
                setLoading(false);
                showError("Network error: " + t.getMessage());
            }
        });
    }

    private void setLoading(boolean loading) {
        progressCe.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSubmit.setEnabled(!loading);
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

    private String parseErrorDetail(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String json = response.errorBody().string();
                JSONObject obj = new JSONObject(json);
                if (obj.has("detail")) return obj.getString("detail");
            }
        } catch (Exception ignored) {}
        return "Server rejected request (HTTP " + response.code() + ")";
    }
}
