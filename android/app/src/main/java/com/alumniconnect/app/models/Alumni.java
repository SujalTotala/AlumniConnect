package com.alumniconnect.app.models;

import com.google.gson.annotations.SerializedName;

/**
 * Matches backend AlumniResponse schema exactly.
 * All optional fields map to nullable types.
 */
public class Alumni {
    @SerializedName("id")
    private int id;

    @SerializedName("user_id")
    private Integer userId;          // nullable

    @SerializedName("name")
    private String name;

    @SerializedName("email")
    private String email;

    @SerializedName("graduation_year")
    private String graduationYear;   // nullable

    @SerializedName("department")
    private String department;       // nullable

    @SerializedName("company")
    private String company;          // nullable

    @SerializedName("job_role")
    private String jobRole;          // nullable

    @SerializedName("location")
    private String location;         // nullable

    @SerializedName("skills")
    private String skills;           // nullable

    @SerializedName("bio")
    private String bio;              // nullable

    @SerializedName("linkedin_url")
    private String linkedinUrl;      // nullable

    @SerializedName("github_url")
    private String githubUrl;        // nullable

    @SerializedName("mentorship_available")
    private Boolean mentorshipAvailable;  // nullable, default false

    @SerializedName("is_verified")
    private Boolean isVerified;           // nullable, default false

    public Alumni() {}

    // Getters
    public int getId() { return id; }
    public Integer getUserId() { return userId; }
    public String getName() { return name != null ? name : ""; }
    public String getEmail() { return email != null ? email : ""; }
    public String getGraduationYear() { return graduationYear; }
    public String getDepartment() { return department; }
    public String getCompany() { return company; }
    public String getJobRole() { return jobRole; }
    public String getLocation() { return location; }
    public String getSkills() { return skills; }
    public String getBio() { return bio; }
    public String getLinkedinUrl() { return linkedinUrl; }
    public String getGithubUrl() { return githubUrl; }
    public boolean isMentorshipAvailable() {
        return mentorshipAvailable != null && mentorshipAvailable;
    }

    public boolean isVerified() {
        return isVerified != null && isVerified;
    }

    /** Returns a non-null display name — never "null" or empty */
    public String getDisplayName() {
        String n = getName();
        return n.isEmpty() ? "Unknown Alumni" : n;
    }

    /** Returns initials for avatar (first char of first + last word) */
    public String getInitials() {
        String n = getDisplayName().trim();
        if (n.isEmpty()) return "?";
        String[] parts = n.split("\\s+");
        if (parts.length == 1) return String.valueOf(parts[0].charAt(0)).toUpperCase();
        return (String.valueOf(parts[0].charAt(0)) + String.valueOf(parts[parts.length - 1].charAt(0))).toUpperCase();
    }

    /** Returns headline: Job Role @ Company, or department, or graduation year */
    public String getHeadline() {
        if (hasValue(jobRole) && hasValue(company)) return jobRole + " @ " + company;
        if (hasValue(jobRole)) return jobRole;
        if (hasValue(company)) return company;
        if (hasValue(department)) return department;
        if (hasValue(graduationYear)) return "Class of " + graduationYear;
        return null;
    }

    /** Safe null+empty check */
    public static boolean hasValue(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
