package com.alumniconnect.app.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.alumniconnect.app.R;
import com.alumniconnect.app.models.EventRegistration;
import com.alumniconnect.app.repositories.EventRepository;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EventRegistrationsActivity extends AppCompatActivity {

    private EventRepository eventRepository;
    private int eventId;
    private RecyclerView rvRegistrations;
    private ProgressBar progressRegs;
    private TextView tvEmpty, tvTitle;
    private RegAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_registrations);

        eventRepository = new EventRepository(this);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Event Attendees");
        }

        rvRegistrations = findViewById(R.id.rv_registrations);
        progressRegs = findViewById(R.id.progress_regs);
        tvEmpty = findViewById(R.id.tv_regs_empty);
        tvTitle = findViewById(R.id.tv_reg_title);

        eventId = getIntent().getIntExtra("event_id", -1);
        String eventTitle = getIntent().getStringExtra("event_title");
        if (eventTitle != null) {
            tvTitle.setText("Attendees for: " + eventTitle);
        }

        if (eventId == -1) {
            Toast.makeText(this, "Invalid Event", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        adapter = new RegAdapter();
        rvRegistrations.setLayoutManager(new LinearLayoutManager(this));
        rvRegistrations.setAdapter(adapter);

        loadRegistrations();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadRegistrations() {
        progressRegs.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        rvRegistrations.setVisibility(View.GONE);

        eventRepository.getEventRegistrations(eventId).enqueue(new Callback<List<EventRegistration>>() {
            @Override
            public void onResponse(Call<List<EventRegistration>> call, Response<List<EventRegistration>> response) {
                progressRegs.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    List<EventRegistration> list = response.body();
                    if (list.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                    } else {
                        adapter.setList(list);
                        rvRegistrations.setVisibility(View.VISIBLE);
                    }
                } else {
                    Toast.makeText(EventRegistrationsActivity.this, "Failed to load attendees (HTTP " + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<EventRegistration>> call, Throwable t) {
                progressRegs.setVisibility(View.GONE);
                Toast.makeText(EventRegistrationsActivity.this, "Network error loading attendees.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static class RegAdapter extends RecyclerView.Adapter<RegAdapter.RegViewHolder> {
        private List<EventRegistration> list = new ArrayList<>();

        void setList(List<EventRegistration> list) {
            this.list = list != null ? list : new ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public RegViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_registration, parent, false);
            return new RegViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull RegViewHolder holder, int position) {
            EventRegistration reg = list.get(position);
            holder.tvName.setText(reg.getUserName());
            holder.tvEmail.setText(reg.getUserEmail());
            holder.tvRole.setText(reg.getUserRole().toUpperCase());

            String name = reg.getUserName();
            if (name != null && !name.trim().isEmpty()) {
                String initials = String.valueOf(name.charAt(0)).toUpperCase();
                holder.tvInitial.setText(initials);
            } else {
                holder.tvInitial.setText("?");
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class RegViewHolder extends RecyclerView.ViewHolder {
            TextView tvInitial, tvName, tvEmail, tvRole;

            RegViewHolder(View itemView) {
                super(itemView);
                tvInitial = itemView.findViewById(R.id.tv_reg_initial);
                tvName = itemView.findViewById(R.id.tv_reg_name);
                tvEmail = itemView.findViewById(R.id.tv_reg_email);
                tvRole = itemView.findViewById(R.id.tv_reg_role);
            }
        }
    }
}
