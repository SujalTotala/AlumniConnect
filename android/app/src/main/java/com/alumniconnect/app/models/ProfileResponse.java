package com.alumniconnect.app.models;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class ProfileResponse {
    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("email")
    private String email;

    @SerializedName("role")
    private String role;

    @SerializedName("is_active")
    private boolean isActive;

    // Profile is a dynamic dict from backend: keys depend on role
    @SerializedName("profile")
    private Map<String, Object> profile;

    public ProfileResponse() {}

    public int getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public boolean isActive() { return isActive; }
    public Map<String, Object> getProfile() { return profile; }

    // Helper: safely get a String field from the profile map
    public String getProfileString(String key) {
        if (profile == null || !profile.containsKey(key)) return null;
        Object val = profile.get(key);
        return val != null ? val.toString() : null;
    }

    // Helper: safely get a Boolean field from the profile map
    public boolean getProfileBoolean(String key) {
        if (profile == null || !profile.containsKey(key)) return false;
        Object val = profile.get(key);
        if (val instanceof Boolean) return (Boolean) val;
        return "true".equalsIgnoreCase(String.valueOf(val));
    }
}
