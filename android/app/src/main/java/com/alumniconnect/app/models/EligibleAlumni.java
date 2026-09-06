package com.alumniconnect.app.models;

import com.google.gson.annotations.SerializedName;

public class EligibleAlumni {
    @SerializedName("user_id")
    private int userId;

    @SerializedName("name")
    private String name;

    @SerializedName("company")
    private String company;

    @SerializedName("job_role")
    private String jobRole;

    @SerializedName("department")
    private String department;

    @SerializedName("graduation_year")
    private String graduationYear;

    @SerializedName("is_company_match")
    private boolean isCompanyMatch;

    @SerializedName("avatar_url")
    private String avatarUrl;

    public EligibleAlumni() {}

    public int getUserId() { return userId; }
    public String getName() { return name != null ? name : ""; }
    public String getCompany() { return company != null ? company : ""; }
    public String getJobRole() { return jobRole != null ? jobRole : ""; }
    public String getDepartment() { return department != null ? department : ""; }
    public String getGraduationYear() { return graduationYear != null ? graduationYear : ""; }
    public boolean isCompanyMatch() { return isCompanyMatch; }
    public String getAvatarUrl() { return avatarUrl; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getName());
        if (!getCompany().isEmpty()) {
            sb.append(" (").append(getCompany()).append(")");
        }
        if (isCompanyMatch) {
            sb.append(" ⭐ Company Match");
        }
        return sb.toString();
    }
}
