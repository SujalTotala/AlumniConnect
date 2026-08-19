package com.alumniconnect.app.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.alumniconnect.app.R;
import com.alumniconnect.app.utils.SessionManager;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MentorshipFragment extends Fragment {

    private SessionManager sessionManager;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mentorship, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());
        tabLayout = view.findViewById(R.id.tab_layout_mentorship);
        viewPager = view.findViewById(R.id.view_pager_mentorship);

        String role = sessionManager.getUserRole().toLowerCase();
        MentorshipPagerAdapter adapter = new MentorshipPagerAdapter(this, role);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if ("student".equals(role)) {
                if (position == 0) tab.setText("Available Mentors");
                else tab.setText("Sent Requests");
            } else if ("alumni".equals(role)) {
                if (position == 0) tab.setText("Available Mentors");
                else tab.setText("Received Requests");
            } else {
                // Admin / fallback
                if (position == 0) tab.setText("Available Mentors");
                else if (position == 1) tab.setText("Sent Requests");
                else tab.setText("Received Requests");
            }
        }).attach();
    }

    private static class MentorshipPagerAdapter extends FragmentStateAdapter {
        private final String role;

        MentorshipPagerAdapter(@NonNull Fragment fragment, String role) {
            super(fragment);
            this.role = role;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if ("student".equals(role)) {
                if (position == 0) return new AvailableMentorsFragment();
                return new SentRequestsFragment();
            } else if ("alumni".equals(role)) {
                if (position == 0) return new AvailableMentorsFragment();
                return new ReceivedRequestsFragment();
            } else {
                // Admin (can see all 3 tabs)
                if (position == 0) return new AvailableMentorsFragment();
                if (position == 1) return new SentRequestsFragment();
                return new ReceivedRequestsFragment();
            }
        }

        @Override
        public int getItemCount() {
            if ("student".equals(role) || "alumni".equals(role)) {
                return 2;
            }
            return 3; // Admin shows 3
        }
    }
}
