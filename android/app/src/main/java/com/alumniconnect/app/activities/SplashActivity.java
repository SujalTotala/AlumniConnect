package com.alumniconnect.app.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import com.alumniconnect.app.R;
import com.alumniconnect.app.models.User;
import com.alumniconnect.app.network.ApiClient;
import com.alumniconnect.app.network.ApiService;
import com.alumniconnect.app.utils.SessionManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        sessionManager = new SessionManager(this);

        new Handler(Looper.getMainLooper()).postDelayed(this::checkAuthenticationSession, 1200);
    }

    private void checkAuthenticationSession() {
        if (!sessionManager.isLoggedIn()) {
            navigateToLogin();
            return;
        }

        // Token exists; verify with GET /auth/me
        ApiService apiService = ApiClient.getApiService(this);
        apiService.getMe().enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    sessionManager.saveSession(sessionManager.getToken(), user);
                    navigateToMain();
                } else {
                    // Token invalid or expired
                    sessionManager.clearSession();
                    navigateToLogin();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                // If offline or network error, but token was previously saved, allow proceeding to main or login
                navigateToLogin();
            }
        });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
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
