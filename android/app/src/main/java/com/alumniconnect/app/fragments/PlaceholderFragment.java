package com.alumniconnect.app.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.alumniconnect.app.R;

public class PlaceholderFragment extends Fragment {
    private static final String ARG_TITLE = "title";
    private static final String ARG_ICON = "icon";
    private static final String ARG_MSG = "message";

    public static PlaceholderFragment newInstance(String icon, String title, String message) {
        PlaceholderFragment f = new PlaceholderFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ICON, icon);
        args.putString(ARG_TITLE, title);
        args.putString(ARG_MSG, message);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_placeholder, container, false);
        Bundle args = getArguments();
        if (args != null) {
            ((TextView) view.findViewById(R.id.tv_placeholder_icon)).setText(args.getString(ARG_ICON, "🔧"));
            ((TextView) view.findViewById(R.id.tv_placeholder_title)).setText(args.getString(ARG_TITLE, "Coming Soon"));
            ((TextView) view.findViewById(R.id.tv_placeholder_message)).setText(args.getString(ARG_MSG, ""));
        }
        return view;
    }
}
