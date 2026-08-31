package com.alumniconnect.app.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.alumniconnect.app.R;
import com.alumniconnect.app.models.Opportunity;
import com.alumniconnect.app.models.OpportunityCreateRequest;
import com.alumniconnect.app.repositories.OpportunityRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import org.json.JSONObject;
import java.util.Calendar;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateOpportunityActivity extends AppCompatActivity {

    private OpportunityRepository opportunityRepository;

    private TextInputEditText etTitle, etCompany, etLocation, etUrl, etDeadline, etDesc;
    private AutoCompleteTextView actvType;
    private MaterialButton btnSubmit;
    private ProgressBar progressCo;
    private TextView tvError, tvSuccess;

    private final String[] oppTypes = {
            "Internship",
            "Full-Time Job",
            "Referral",
            "Hackathon",
            "Scholarship",
            "Workshop"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_opportunity);

        opportunityRepository = new OpportunityRepository(this);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Post Opportunity");
        }

        etTitle = findViewById(R.id.et_co_title);
        etCompany = findViewById(R.id.et_co_company);
        actvType = findViewById(R.id.actv_co_type);
        etLocation = findViewById(R.id.et_co_location);
        etUrl = findViewById(R.id.et_co_url);
        etDeadline = findViewById(R.id.et_co_deadline);
        etDesc = findViewById(R.id.et_co_desc);

        btnSubmit = findViewById(R.id.btn_create_opp_submit);
        progressCo = findViewById(R.id.progress_co);
        tvError = findViewById(R.id.tv_co_error);
        tvSuccess = findViewById(R.id.tv_co_success);

        // AutoComplete adapter
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, oppTypes);
        actvType.setAdapter(typeAdapter);
        actvType.setText(oppTypes[1], false); // default to Full-Time Job

        // Deadline Picker
        etDeadline.setOnClickListener(v -> showDatePicker());

        btnSubmit.setOnClickListener(v -> submitOpportunity());
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
            String formatted = String.format("%d-%02d-%02d", year1, (monthOfYear + 1), dayOfMonth);
            etDeadline.setText(formatted);
        }, year, month, day);
        dpd.show();
    }

    private void submitOpportunity() {
        String title = etTitle.getText().toString().trim();
        String company = etCompany.getText().toString().trim();
        String type = actvType.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String url = etUrl.getText().toString().trim();
        String deadline = etDeadline.getText().toString().trim();
        String desc = etDesc.getText().toString().trim();

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(company) || TextUtils.isEmpty(desc)) {
            showError("Please fill out all required fields (*).");
            return;
        }

        OpportunityCreateRequest req = new OpportunityCreateRequest();
        req.setTitle(title);
        req.setCompany(company);
        req.setOpportunityType(type);
        req.setLocation(location.isEmpty() ? null : location);
        req.setApplicationUrl(url.isEmpty() ? null : url);
        req.setDeadline(deadline.isEmpty() ? null : deadline);
        req.setDescription(desc);

        setLoading(true);
        hideMessages();

        opportunityRepository.createOpportunity(req).enqueue(new Callback<Opportunity>() {
            @Override
            public void onResponse(Call<Opportunity> call, Response<Opportunity> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    showSuccess("Opportunity shared successfully!");
                    // Clear fields
                    etTitle.setText("");
                    etCompany.setText("");
                    etLocation.setText("");
                    etUrl.setText("");
                    etDeadline.setText("");
                    etDesc.setText("");
                } else if (response.code() == 422) {
                    showError("Validation error. Verify required format / fields.");
                } else if (response.code() == 403) {
                    showError("You do not have permission to post opportunities.");
                } else {
                    showError("Error: " + parseErrorDetail(response));
                }
            }

            @Override
            public void onFailure(Call<Opportunity> call, Throwable t) {
                setLoading(false);
                showError("Network error: " + t.getMessage());
            }
        });
    }

    private void setLoading(boolean loading) {
        progressCo.setVisibility(loading ? View.VISIBLE : View.GONE);
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
