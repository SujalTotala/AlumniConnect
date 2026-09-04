package com.alumniconnect.app;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;

public class AlumniConnectApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Explicitly enforce Light theme to guarantee high-contrast legibility across all Android versions
        // and prevent unreadable inverted text/cards on devices with system dark mode enabled.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }
}
