package com.alumniconnect.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.alumniconnect.app.R;
import com.alumniconnect.app.models.LoginResponse;
import com.alumniconnect.app.models.RegisterRequest;
import com.alumniconnect.app.network.ApiClient;
import com.alumniconnect.app.network.ApiService;
import com.alumniconnect.app.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {
    private TextInputEditText etName, etEmail, etPassword;
    private RadioGroup rgRole;
    private RadioButton rbStudent, rbAlumni;
    private MaterialButton btnRegister;
    private ProgressBar progressBar;
    private TextView tvError, tvGotoLogin;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        sessionManager = new SessionManager(this);

        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        rgRole = findViewById(R.id.rg_role);
        rbStudent = findViewById(R.id.rb_student);
        rbAlumni = findViewById(R.id.rb_alumni);
        btnRegister = findViewById(R.id.btn_register);
        progressBar = findViewById(R.id.progress_bar);
        tvError = findViewById(R.id.tv_error);
        tvGotoLogin = findViewById(R.id.tv_goto_login);

        btnRegister.setOnClickListener(v -> performRegistration());

        tvGotoLogin.setOnClickListener(v -> finish());
    }

    private void performRegistration() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
        String role = rbAlumni.isChecked() ? "alumni" : "student";

        if (TextUtils.isEmpty(name)) {
            showError("Please enter your full name");
            return;
        }

        if (TextUtils.isEmpty(email)) {
            showError("Please enter your email address");
            return;
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            showError("Password must be at least 6 characters");
            return;
        }

        setLoading(true);
        hideError();

        ApiService apiService = ApiClient.getApiService(this);
        apiService.register(new RegisterRequest(name, email, password, role)).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse registerResponse = response.body();
                    if (registerResponse.getAccessToken() != null && registerResponse.getUser() != null) {
                        sessionManager.saveSession(registerResponse.getAccessToken(), registerResponse.getUser());
                        navigateToMain();
                    } else {
                        // Registration succeeded without auto-token; return to login
                        finish();
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
        return "Registration failed. Please try again.";
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!isLoading);
        etName.setEnabled(!isLoading);
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
        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
