package com.alumniconnect.app.models;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class ProfileCompletion {
    @SerializedName("completion_percentage")
    private int completionPercentage;

    @SerializedName("missing_fields")
    private List<String> missingFields;

    @SerializedName("suggestions")
    private List<String> suggestions;

    public ProfileCompletion() {}

    public int getCompletionPercentage() { return completionPercentage; }
    public List<String> getMissingFields() { return missingFields != null ? missingFields : new ArrayList<>(); }
    public List<String> getSuggestions() { return suggestions != null ? suggestions : new ArrayList<>(); }
}
