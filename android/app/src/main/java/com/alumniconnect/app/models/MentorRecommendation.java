package com.alumniconnect.app.models;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class MentorRecommendation {
    @SerializedName("id")
    private int id;

    @SerializedName("user_id")
    private Integer userId;

    @SerializedName("name")
    private String name;

    @SerializedName("email")
    private String email;

    @SerializedName("department")
    private String department;

    @SerializedName("graduation_year")
    private String graduationYear;

    @SerializedName("company")
    private String company;

    @SerializedName("job_role")
    private String jobRole;

    @SerializedName("location")
    private String location;

    @SerializedName("skills")
    private String skills;

    @SerializedName("bio")
    private String bio;

    @SerializedName("is_verified")
    private Boolean isVerified;

    @SerializedName("mentorship_available")
    private Boolean mentorshipAvailable;

    @SerializedName("match_score")
    private int matchScore;

    @SerializedName("match_reasons")
    private List<String> matchReasons;

    public MentorRecommendation() {}

    public int getId() { return id; }
    public Integer getUserId() { return userId; }
    public String getName() { return name != null ? name : ""; }
    public String getEmail() { return email != null ? email : ""; }
    public String getDepartment() { return department; }
    public String getGraduationYear() { return graduationYear; }
    public String getCompany() { return company; }
    public String getJobRole() { return jobRole; }
    public String getLocation() { return location; }
    public String getSkills() { return skills; }
    public String getBio() { return bio; }
    public boolean isVerified() { return isVerified != null && isVerified; }
    public boolean isMentorshipAvailable() { return mentorshipAvailable != null && mentorshipAvailable; }
    public int getMatchScore() { return matchScore; }
    public List<String> getMatchReasons() { return matchReasons != null ? matchReasons : new ArrayList<>(); }
}
