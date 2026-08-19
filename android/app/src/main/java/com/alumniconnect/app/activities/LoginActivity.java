package com.alumniconnect.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.alumniconnect.app.R;
import com.alumniconnect.app.models.LoginRequest;
import com.alumniconnect.app.models.LoginResponse;
import com.alumniconnect.app.network.ApiClient;
import com.alumniconnect.app.network.ApiService;
import com.alumniconnect.app.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private ProgressBar progressBar;
    private TextView tvError, tvGotoRegister;
    private SessionManager sessionManager;

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

        btnLogin.setOnClickListener(v -> performLogin());

        tvGotoRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private void performLogin() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email)) {
            showError("Please enter your email address");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            showError("Please enter your password");
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
                        showError("Login response missing access token");
                    }
                } else {
                    String errorMsg = parseErrorMessage(response);
                    showError(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                setLoading(false);
                showError("Unable to connect to server: " + t.getMessage());
            }
        });
    }

    private String parseErrorMessage(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String errorJson = response.errorBody().string();
                JSONObject jsonObject = new JSONObject(errorJson);
                if (jsonObject.has("detail")) {
                    return jsonObject.getString("detail");
                }
            }
        } catch (Exception e) {
            // Fallback
        }
        return "Invalid email or password";
    }

    private void setLoading(boolean isLoading) {
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
