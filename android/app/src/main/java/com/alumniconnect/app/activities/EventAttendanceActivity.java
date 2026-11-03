package com.alumniconnect.app.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.alumniconnect.app.R;
import com.alumniconnect.app.adapters.EventAttendanceAdapter;
import com.alumniconnect.app.models.AttendanceStats;
import com.alumniconnect.app.models.EventAttendance;
import com.alumniconnect.app.models.QRCodeTokenResponse;
import com.alumniconnect.app.repositories.AttendanceRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EventAttendanceActivity extends AppCompatActivity {

    private int eventId;
    private String eventTitle;
    private AttendanceRepository attendanceRepository;
    private EventAttendanceAdapter adapter;

    private TextView tvEventTitle;
    private ImageView ivQrCode;
    private ProgressBar progressQr;
    private TextView tvQrToken;
    private MaterialButton btnCopyToken;
    private TextView tvStatRegistered;
    private TextView tvStatAttended;
    private TextView tvStatRate;
    private TextInputEditText etManualUserId;
    private MaterialButton btnManualCheckin;
    private RecyclerView rvAttendees;
    private TextView tvEmptyAttendance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_attendance);

        attendanceRepository = new AttendanceRepository(this);

        eventId = getIntent().getIntExtra("event_id", -1);
        eventTitle = getIntent().getStringExtra("event_title");

        if (eventId == -1) {
            Toast.makeText(this, "Invalid Event ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        loadAllData();
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_attendance);
        toolbar.setNavigationOnClickListener(v -> finish());

        tvEventTitle = findViewById(R.id.tv_att_event_title);
        if (eventTitle != null) {
            tvEventTitle.setText(eventTitle);
        }

        ivQrCode = findViewById(R.id.iv_qr_code);
        progressQr = findViewById(R.id.progress_qr);
        tvQrToken = findViewById(R.id.tv_qr_token);
        btnCopyToken = findViewById(R.id.btn_copy_token);
        tvStatRegistered = findViewById(R.id.tv_stat_registered);
        tvStatAttended = findViewById(R.id.tv_stat_attended);
        tvStatRate = findViewById(R.id.tv_stat_rate);
        etManualUserId = findViewById(R.id.et_manual_user_id);
        btnManualCheckin = findViewById(R.id.btn_manual_checkin);
        rvAttendees = findViewById(R.id.rv_attendance_roster);
        tvEmptyAttendance = findViewById(R.id.tv_empty_attendance);

        adapter = new EventAttendanceAdapter();
        rvAttendees.setLayoutManager(new LinearLayoutManager(this));
        rvAttendees.setAdapter(adapter);

        btnCopyToken.setOnClickListener(v -> {
            String token = tvQrToken.getText().toString();
            if (!token.isEmpty() && !token.contains("Loading")) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Event QR Token", token);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Token copied to clipboard", Toast.LENGTH_SHORT).show();
            }
        });

        btnManualCheckin.setOnClickListener(v -> {
            String input = etManualUserId.getText() != null ? etManualUserId.getText().toString().trim() : "";
            if (input.isEmpty()) {
                etManualUserId.setError("Enter user ID or email");
                return;
            }
            performManualCheckIn(input);
        });
    }

    private void loadAllData() {
        loadQRCode();
        loadStats();
        loadAttendees();
    }

    private void loadQRCode() {
        progressQr.setVisibility(View.VISIBLE);
        attendanceRepository.getQRCodeToken(eventId).enqueue(new Callback<QRCodeTokenResponse>() {
            @Override
            public void onResponse(@NonNull Call<QRCodeTokenResponse> call, @NonNull Response<QRCodeTokenResponse> response) {
                progressQr.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    String token = response.body().getToken();
                    tvQrToken.setText(token);
                    generateQrBitmap(token);
                } else {
                    tvQrToken.setText("Failed to load token");
                }
            }

            @Override
            public void onFailure(@NonNull Call<QRCodeTokenResponse> call, @NonNull Throwable t) {
                progressQr.setVisibility(View.GONE);
                tvQrToken.setText("Network error");
            }
        });
    }

    private void generateQrBitmap(String text) {
        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(text, BarcodeFormat.QR_CODE, 440, 440);
            ivQrCode.setImageBitmap(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error generating QR bitmap", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadStats() {
        attendanceRepository.getAttendanceStats(eventId).enqueue(new Callback<AttendanceStats>() {
            @Override
            public void onResponse(@NonNull Call<AttendanceStats> call, @NonNull Response<AttendanceStats> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AttendanceStats s = response.body();
                    tvStatRegistered.setText(String.valueOf(s.getTotalRegistered()));
                    tvStatAttended.setText(String.valueOf(s.getTotalAttended()));
                    tvStatRate.setText(String.format("%.1f%%", s.getAttendancePercentage()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<AttendanceStats> call, @NonNull Throwable t) {}
        });
    }

    private void loadAttendees() {
        attendanceRepository.getEventAttendees(eventId).enqueue(new Callback<List<EventAttendance>>() {
            @Override
            public void onResponse(@NonNull Call<List<EventAttendance>> call, @NonNull Response<List<EventAttendance>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<EventAttendance> list = response.body();
                    adapter.setList(list);
                    if (list.isEmpty()) {
                        tvEmptyAttendance.setVisibility(View.VISIBLE);
                    } else {
                        tvEmptyAttendance.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<EventAttendance>> call, @NonNull Throwable t) {}
        });
    }

    private void performManualCheckIn(String input) {
        try {
            int userId = Integer.parseInt(input);
            attendanceRepository.manualCheckIn(eventId, userId).enqueue(new Callback<EventAttendance>() {
                @Override
                public void onResponse(@NonNull Call<EventAttendance> call, @NonNull Response<EventAttendance> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Toast.makeText(EventAttendanceActivity.this, "Attendee checked in successfully!", Toast.LENGTH_SHORT).show();
                        etManualUserId.setText("");
                        loadStats();
                        loadAttendees();
                    } else {
                        Toast.makeText(EventAttendanceActivity.this, "Manual check-in failed (already checked in or not registered)", Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<EventAttendance> call, @NonNull Throwable t) {
                    Toast.makeText(EventAttendanceActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid numeric User ID", Toast.LENGTH_SHORT).show();
        }
    }
}
