package com.alumniconnect.app.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.alumniconnect.app.R;
import com.alumniconnect.app.models.User;
import com.alumniconnect.app.network.ApiClient;
import com.alumniconnect.app.network.ApiService;
import com.alumniconnect.app.utils.ApiErrorUtils;
import com.alumniconnect.app.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {
    private SessionManager sessionManager;
    private ProgressBar splashProgress;
    private View layoutRetry;
    private TextView tvRetryMsg;
    private MaterialButton btnRetry;
    private MaterialButton btnOffline;
    private TextView tvLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        sessionManager = new SessionManager(this);

        splashProgress = findViewById(R.id.splash_progress);
        layoutRetry = findViewById(R.id.layout_splash_retry);
        tvRetryMsg = findViewById(R.id.tv_splash_retry_msg);
        btnRetry = findViewById(R.id.btn_splash_retry);
        btnOffline = findViewById(R.id.btn_splash_offline);
        tvLogout = findViewById(R.id.tv_splash_logout);

        btnRetry.setOnClickListener(v -> checkAuthenticationSession());

        btnOffline.setOnClickListener(v -> {
            // Proceed to main with cached session credentials
            navigateToMain();
        });

        tvLogout.setOnClickListener(v -> {
            sessionManager.clearSession();
            navigateToLogin(null, false);
        });

        new Handler(Looper.getMainLooper()).postDelayed(this::checkAuthenticationSession, 1000);
    }

    private void checkAuthenticationSession() {
        if (!sessionManager.isLoggedIn()) {
            navigateToLogin(null, false);
            return;
        }

        // Show loading spinner, hide retry UI
        showLoading(true);

        // Token exists; verify with GET /auth/me
        ApiService apiService = ApiClient.getApiService(this);
        apiService.getMe().enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    sessionManager.saveSession(sessionManager.getToken(), user);
                    navigateToMain();
                } else if (response.code() == 401) {
                    // Confirmed invalid or expired token
                    sessionManager.clearSession();
                    navigateToLogin("Your session has expired. Please log in again.", true);
                } else if (response.code() == 403) {
                    // Inactive or deactivated user
                    String errorMsg = ApiErrorUtils.parseError(response);
                    sessionManager.clearSession();
                    navigateToLogin(errorMsg, false);
                } else if (response.code() >= 500) {
                    // Temporary server-side issue or cold start; DO NOT clear session
                    showLoading(false);
                    showRetryState("Server is warming up or temporarily busy (HTTP " + response.code() + ").");
                } else {
                    // Other unexpected code: show retry
                    showLoading(false);
                    showRetryState(ApiErrorUtils.parseError(response));
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                // Transient network failure / timeout / cold-start: DO NOT clear session
                showLoading(false);
                String friendlyMsg = ApiErrorUtils.parseThrowable(t);
                showRetryState(friendlyMsg);
            }
        });
    }

    private void showLoading(boolean isLoading) {
        if (splashProgress != null) {
            splashProgress.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (layoutRetry != null && isLoading) {
            layoutRetry.setVisibility(View.GONE);
        }
    }

    private void showRetryState(String message) {
        if (layoutRetry != null) {
            layoutRetry.setVisibility(View.VISIBLE);
        }
        if (tvRetryMsg != null) {
            tvRetryMsg.setText(message);
        }
    }

    private void navigateToLogin(String message, boolean sessionExpired) {
        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
        if (sessionExpired) {
            intent.putExtra("session_expired", true);
        }
        if (message != null && !message.isEmpty()) {
            intent.putExtra("error_message", message);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToMain() {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
