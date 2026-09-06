package com.alumniconnect.app.models;

import com.google.gson.annotations.SerializedName;

public class ConnectionUserSummary {
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

    @SerializedName("avatar_url")
    private String avatarUrl;

    @SerializedName("is_verified")
    private boolean isVerified;

    public ConnectionUserSummary() {}

    public int getUserId() { return userId; }
    public int getId() { return userId; }
    public String getName() { return name != null ? name : ""; }
    public String getDisplayName() { return getName(); }
    public String getEmail() { return email != null ? email : ""; }
    public String getRole() { return role != null ? role : "student"; }
    public String getHeadline() { return headline != null ? headline : ""; }
    public String getCompany() { return company != null ? company : ""; }
    public String getJobRole() { return jobRole != null ? jobRole : ""; }
    public String getDepartment() { return department != null ? department : ""; }
    public String getGraduationYear() { return graduationYear != null ? graduationYear : ""; }
    public String getLocation() { return location != null ? location : ""; }
    public String getAvatarUrl() { return avatarUrl; }
    public boolean isVerified() { return isVerified; }
}
