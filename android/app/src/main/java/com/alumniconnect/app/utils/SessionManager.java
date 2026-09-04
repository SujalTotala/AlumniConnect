package com.alumniconnect.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import com.alumniconnect.app.models.User;

public class SessionManager {
    private static final String ENCRYPTED_PREF_NAME = "AlumniConnectSecurePrefs";

    private final SharedPreferences pref;

    public SessionManager(Context context) {
        Context appContext = context.getApplicationContext();
        SharedPreferences securePref = null;

        try {
            MasterKey masterKey = new MasterKey.Builder(appContext)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            securePref = EncryptedSharedPreferences.create(
                    appContext,
                    ENCRYPTED_PREF_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception ignored) {
            // In case of Android Keystore incompatibility or vendor-specific Keystore failure,
            // securePref remains null and we fall back gracefully below.
        }

        if (securePref != null) {
            pref = securePref;
            // Migrate legacy plaintext session if present
            migrateLegacyPrefs(appContext, securePref);
        } else {
            // Safe fallback to private preferences if hardware Keystore unavailable
            pref = appContext.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
        }
    }

    private void migrateLegacyPrefs(Context appContext, SharedPreferences securePref) {
        try {
            SharedPreferences legacyPref = appContext.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
            if (legacyPref.contains(Constants.KEY_TOKEN) && !securePref.contains(Constants.KEY_TOKEN)) {
                String token = legacyPref.getString(Constants.KEY_TOKEN, null);
                int userId = legacyPref.getInt(Constants.KEY_USER_ID, -1);
                String name = legacyPref.getString(Constants.KEY_USER_NAME, "");
                String email = legacyPref.getString(Constants.KEY_USER_EMAIL, "");
                String role = legacyPref.getString(Constants.KEY_USER_ROLE, "student");
                boolean isLoggedIn = legacyPref.getBoolean(Constants.KEY_IS_LOGGED_IN, false);

                securePref.edit()
                        .putString(Constants.KEY_TOKEN, token)
                        .putInt(Constants.KEY_USER_ID, userId)
                        .putString(Constants.KEY_USER_NAME, name)
                        .putString(Constants.KEY_USER_EMAIL, email)
                        .putString(Constants.KEY_USER_ROLE, role)
                        .putBoolean(Constants.KEY_IS_LOGGED_IN, isLoggedIn)
                        .apply();

                // Clear sensitive token from plaintext legacy file
                legacyPref.edit().clear().apply();
            }
        } catch (Exception ignored) {
            // Migration failure should not block session creation
        }
    }

    public synchronized void saveSession(String token, User user) {
        SharedPreferences.Editor editor = pref.edit();
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

    public synchronized void clearSession() {
        pref.edit().clear().apply();
    }
}
