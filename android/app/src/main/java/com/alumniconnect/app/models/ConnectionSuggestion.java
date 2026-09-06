package com.alumniconnect.app.models;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class ConnectionSuggestion {
    @SerializedName("user_id")
    private int userId;

    @SerializedName("name")
    private String name;

    @SerializedName("email")
    private String email;

    @SerializedName("role")
    private String role;

    @SerializedName("headline")
    private String headline;

    @SerializedName("company")
    private String company;

    @SerializedName("job_role")
    private String jobRole;

    @SerializedName("department")
    private String department;

    @SerializedName("graduation_year")
    private String graduationYear;

    @SerializedName("location")
    private String location;

    @SerializedName("skills")
    private String skills;

    @SerializedName("avatar_url")
    private String avatarUrl;

    @SerializedName("is_verified")
    private boolean isVerified;

    @SerializedName("suggestion_score")
    private int suggestionScore;

    @SerializedName("suggestion_reasons")
    private List<String> suggestionReasons;

    public ConnectionSuggestion() {}

    public int getUserId() { return userId; }
    public String getName() { return name != null ? name : ""; }
    public String getEmail() { return email != null ? email : ""; }
    public String getRole() { return role != null ? role : "student"; }
    public String getHeadline() { return headline != null ? headline : ""; }
    public String getCompany() { return company != null ? company : ""; }
    public String getJobRole() { return jobRole != null ? jobRole : ""; }
    public String getDepartment() { return department != null ? department : ""; }
    public String getGraduationYear() { return graduationYear != null ? graduationYear : ""; }
    public String getLocation() { return location != null ? location : ""; }
    public String getSkills() { return skills; }
    public String getAvatarUrl() { return avatarUrl; }
    public boolean isVerified() { return isVerified; }
    public int getSuggestionScore() { return suggestionScore; }
    public int getScore() { return suggestionScore; }
    public int getId() { return userId; }
    public String getDisplayName() { return getName(); }
    public ConnectionSuggestion getUser() { return this; }
    public List<String> getSuggestionReasons() {
        return suggestionReasons != null ? suggestionReasons : new ArrayList<>();
    }
    public List<String> getReasons() { return getSuggestionReasons(); }
}
