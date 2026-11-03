package com.alumniconnect.app.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.alumniconnect.app.R;
import com.alumniconnect.app.models.EventAttendance;
import com.alumniconnect.app.repositories.AttendanceRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QRScannerActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 2001;

    private DecoratedBarcodeView barcodeView;
    private TextInputEditText etManualToken;
    private MaterialButton btnSubmitToken;
    private ProgressBar progressBar;
    private TextView tvStatus;
    private AttendanceRepository attendanceRepository;
    private Integer expectedEventId = null;
    private boolean isCheckingIn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);

        attendanceRepository = new AttendanceRepository(this);

        if (getIntent().hasExtra("event_id")) {
            expectedEventId = getIntent().getIntExtra("event_id", -1);
            if (expectedEventId == -1) expectedEventId = null;
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar_qr_scanner);
        toolbar.setNavigationOnClickListener(v -> finish());

        barcodeView = findViewById(R.id.barcode_scanner_view);
        etManualToken = findViewById(R.id.et_manual_checkin_token);
        btnSubmitToken = findViewById(R.id.btn_submit_manual_token);
        progressBar = findViewById(R.id.progress_scanner);
        tvStatus = findViewById(R.id.tv_scanner_status);

        btnSubmitToken.setOnClickListener(v -> {
            String token = etManualToken.getText() != null ? etManualToken.getText().toString().trim() : "";
            if (token.isEmpty()) {
                etManualToken.setError("Please enter a valid check-in code");
                return;
            }
            performCheckIn(token);
        });

        requestCameraAndStartScanning();
    }

    private void requestCameraAndStartScanning() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            startCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission is required to scan QR codes. You can still enter the code manually.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startCamera() {
        barcodeView.initializeFromIntent(getIntent());
        barcodeView.decodeSingle(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (result != null && result.getText() != null && !result.getText().trim().isEmpty()) {
                    runOnUiThread(() -> performCheckIn(result.getText().trim()));
                }
            }

            @Override
            public void possibleResultPoints(List<ResultPoint> resultPoints) {
            }
        });
        barcodeView.resume();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            barcodeView.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeView.pause();
    }

    private void performCheckIn(String token) {
        if (isCheckingIn) return;
        isCheckingIn = true;
        progressBar.setVisibility(View.VISIBLE);
        tvStatus.setVisibility(View.GONE);
        btnSubmitToken.setEnabled(false);

        Call<EventAttendance> call;
        if (expectedEventId != null) {
            call = attendanceRepository.checkInAttendee(expectedEventId, token);
        } else {
            call = attendanceRepository.checkInAttendee(token);
        }

        call.enqueue(new Callback<EventAttendance>() {
            @Override
            public void onResponse(@NonNull Call<EventAttendance> call, @NonNull Response<EventAttendance> response) {
                isCheckingIn = false;
                progressBar.setVisibility(View.GONE);
                btnSubmitToken.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    EventAttendance att = response.body();
                    showSuccessDialog(att);
                } else {
                    String errorMsg = "Check-in failed. Please verify the code and try again.";
                    if (response.code() == 400 || response.code() == 409) {
                        errorMsg = "You are already checked in for this event.";
                    } else if (response.code() == 404) {
                        errorMsg = "Invalid or expired check-in token.";
                    }
                    tvStatus.setText(errorMsg);
                    tvStatus.setTextColor(getResources().getColor(R.color.error));
                    tvStatus.setVisibility(View.VISIBLE);

                    // Resume scanner for another attempt
                    barcodeView.decodeSingle(new BarcodeCallback() {
                        @Override
                        public void barcodeResult(BarcodeResult result) {
                            if (result != null && result.getText() != null) {
                                runOnUiThread(() -> performCheckIn(result.getText().trim()));
                            }
                        }

                        @Override
                        public void possibleResultPoints(List<ResultPoint> resultPoints) {}
                    });
                }
            }

            @Override
            public void onFailure(@NonNull Call<EventAttendance> call, @NonNull Throwable t) {
                isCheckingIn = false;
                progressBar.setVisibility(View.GONE);
                btnSubmitToken.setEnabled(true);
                tvStatus.setText("Network error: " + t.getMessage());
                tvStatus.setTextColor(getResources().getColor(R.color.error));
                tvStatus.setVisibility(View.VISIBLE);

                barcodeView.decodeSingle(new BarcodeCallback() {
                    @Override
                    public void barcodeResult(BarcodeResult result) {
                        if (result != null && result.getText() != null) {
                            runOnUiThread(() -> performCheckIn(result.getText().trim()));
                        }
                    }

                    @Override
                    public void possibleResultPoints(List<ResultPoint> resultPoints) {}
                });
            }
        });
    }

    private void showSuccessDialog(EventAttendance att) {
        new AlertDialog.Builder(this)
                .setTitle("Check-In Successful! 🎉")
                .setMessage("You have been successfully checked in for Event #" + att.getEventId() + " via " + att.getCheckinMethod() + ".")
                .setPositiveButton("Done", (dialog, which) -> {
                    setResult(RESULT_OK);
                    finish();
                })
                .setCancelable(false)
                .show();
    }
}
