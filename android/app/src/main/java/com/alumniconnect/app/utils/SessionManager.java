package com.alumniconnect.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.alumniconnect.app.models.User;

public class SessionManager {
    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        pref = context.getApplicationContext().getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void saveSession(String token, User user) {
        editor.putString(Constants.KEY_TOKEN, token);
        if (user != null) {
            editor.putInt(Constants.KEY_USER_ID, user.getId());
            editor.putString(Constants.KEY_USER_NAME, user.getName());
            editor.putString(Constants.KEY_USER_EMAIL, user.getEmail());
            editor.putString(Constants.KEY_USER_ROLE, user.getRole());
        }
        editor.putBoolean(Constants.KEY_IS_LOGGED_IN, true);
        editor.apply();
    }

    public String getToken() {
        return pref.getString(Constants.KEY_TOKEN, null);
    }

    public int getUserId() {
        return pref.getInt(Constants.KEY_USER_ID, -1);
    }

    public String getUserName() {
        return pref.getString(Constants.KEY_USER_NAME, "");
    }

    public String getUserEmail() {
        return pref.getString(Constants.KEY_USER_EMAIL, "");
    }

    public String getUserRole() {
        return pref.getString(Constants.KEY_USER_ROLE, "student");
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(Constants.KEY_IS_LOGGED_IN, false) && getToken() != null;
    }

    public void clearSession() {
        editor.clear();
        editor.apply();
    }
}
