package com.alumniconnect.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.alumniconnect.app.R;
import com.alumniconnect.app.models.LoginRequest;
import com.alumniconnect.app.models.LoginResponse;
import com.alumniconnect.app.network.ApiClient;
import com.alumniconnect.app.network.ApiService;
import com.alumniconnect.app.utils.ApiErrorUtils;
import com.alumniconnect.app.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private ProgressBar progressBar;
    private TextView tvError, tvGotoRegister;
    private SessionManager sessionManager;
    private boolean isLoggingIn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sessionManager = new SessionManager(this);

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        progressBar = findViewById(R.id.progress_bar);
        tvError = findViewById(R.id.tv_error);
        tvGotoRegister = findViewById(R.id.tv_goto_register);

        // Handle incoming session expired or logout message
        handleIncomingIntent(getIntent());

        btnLogin.setOnClickListener(v -> performLogin());

        tvGotoRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent == null) return;

        if (intent.getBooleanExtra("session_expired", false)) {
            showError("Your session has expired. Please log in again.");
            intent.removeExtra("session_expired");
        } else if (intent.hasExtra("error_message")) {
            String msg = intent.getStringExtra("error_message");
            if (msg != null && !msg.isEmpty()) {
                showError(msg);
            }
            intent.removeExtra("error_message");
        }
    }

    private void performLogin() {
        if (isLoggingIn) return; // Prevent duplicate rapid submission

        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email)) {
            showError("Please enter your email address.");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Please enter a valid email address.");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            showError("Please enter your password.");
            return;
        }

        setLoading(true);
        hideError();

        ApiService apiService = ApiClient.getApiService(this);
        apiService.login(new LoginRequest(email, password)).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();
                    if (loginResponse.getAccessToken() != null && loginResponse.getUser() != null) {
                        sessionManager.saveSession(loginResponse.getAccessToken(), loginResponse.getUser());
                        navigateToMain();
                    } else {
                        showError("Login succeeded but no access token was returned.");
                    }
                } else {
                    String errorMsg = ApiErrorUtils.parseError(response);
                    showError(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                setLoading(false);
                showError(ApiErrorUtils.parseThrowable(t));
            }
        });
    }

    private void setLoading(boolean isLoading) {
        isLoggingIn = isLoading;
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!isLoading);
        etEmail.setEnabled(!isLoading);
        etPassword.setEnabled(!isLoading);
    }

    private void showError(String msg) {
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        tvError.setVisibility(View.GONE);
    }

    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
