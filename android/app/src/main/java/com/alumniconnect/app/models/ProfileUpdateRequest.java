package com.alumniconnect.app.models;

import com.google.gson.annotations.SerializedName;

/**
 * Matches backend ProfileMeUpdate schema exactly.
 * All fields are optional - only non-null fields get updated.
 */
public class ProfileUpdateRequest {
    @SerializedName("name")
    private String name;

    // Shared fields
    @SerializedName("skills")
    private String skills;

    @SerializedName("bio")
    private String bio;

    // Alumni-specific
    @SerializedName("graduation_year")
    private String graduationYear;

    @SerializedName("department")
    private String department;

    @SerializedName("company")
    private String company;

    @SerializedName("job_role")
    private String jobRole;

    @SerializedName("location")
    private String location;

    @SerializedName("linkedin_url")
    private String linkedinUrl;

    @SerializedName("github_url")
    private String githubUrl;

    @SerializedName("mentorship_available")
    private Boolean mentorshipAvailable;

    // Student-specific
    @SerializedName("branch")
    private String branch;

    @SerializedName("year")
    private String year;

    @SerializedName("interests")
    private String interests;

    @SerializedName("profile_image_url")
    private String profileImageUrl;

    public ProfileUpdateRequest() {}

    // Setters
    public void setName(String name) { this.name = name; }
    public void setSkills(String skills) { this.skills = skills; }
    public void setBio(String bio) { this.bio = bio; }
    public void setGraduationYear(String graduationYear) { this.graduationYear = graduationYear; }
    public void setDepartment(String department) { this.department = department; }
    public void setCompany(String company) { this.company = company; }
    public void setJobRole(String jobRole) { this.jobRole = jobRole; }
    public void setLocation(String location) { this.location = location; }
    public void setLinkedinUrl(String linkedinUrl) { this.linkedinUrl = linkedinUrl; }
    public void setGithubUrl(String githubUrl) { this.githubUrl = githubUrl; }
    public void setMentorshipAvailable(Boolean mentorshipAvailable) { this.mentorshipAvailable = mentorshipAvailable; }
    public void setBranch(String branch) { this.branch = branch; }
    public void setYear(String year) { this.year = year; }
    public void setInterests(String interests) { this.interests = interests; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
}
