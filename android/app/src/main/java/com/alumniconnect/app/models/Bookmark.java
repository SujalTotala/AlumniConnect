package com.alumniconnect.app.models;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class Bookmark {
    @SerializedName("id")
    private int id;

    @SerializedName("user_id")
    private int userId;

    @SerializedName("item_type")
    private String itemType;

    @SerializedName("item_id")
    private int itemId;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("item_details")
    private Map<String, Object> itemDetails;

    public Bookmark() {}

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public String getItemType() { return itemType != null ? itemType : ""; }
    public int getItemId() { return itemId; }
    public String getCreatedAt() { return createdAt != null ? createdAt : ""; }
    public Map<String, Object> getItemDetails() { return itemDetails; }

    public String getItemTitle() {
        if (itemDetails != null) {
            if (itemDetails.containsKey("name")) return String.valueOf(itemDetails.get("name"));
            if (itemDetails.containsKey("title")) return String.valueOf(itemDetails.get("title"));
        }
        return itemType + " #" + itemId;
    }

    public String getItemSubtitle() {
        if (itemDetails != null) {
            if ("alumni".equalsIgnoreCase(itemType)) {
                String role = itemDetails.containsKey("job_role") && itemDetails.get("job_role") != null ? String.valueOf(itemDetails.get("job_role")) : "";
                String company = itemDetails.containsKey("company") && itemDetails.get("company") != null ? String.valueOf(itemDetails.get("company")) : "";
                if (!role.isEmpty() && !company.isEmpty()) return role + " @ " + company;
                if (!role.isEmpty()) return role;
                if (!company.isEmpty()) return company;
            } else if ("opportunity".equalsIgnoreCase(itemType)) {
                String comp = itemDetails.containsKey("company") && itemDetails.get("company") != null ? String.valueOf(itemDetails.get("company")) : "";
                String type = itemDetails.containsKey("opportunity_type") && itemDetails.get("opportunity_type") != null ? String.valueOf(itemDetails.get("opportunity_type")) : "";
                return comp + (!type.isEmpty() ? " • " + type : "");
            } else if ("event".equalsIgnoreCase(itemType)) {
                String date = itemDetails.containsKey("event_date") && itemDetails.get("event_date") != null ? String.valueOf(itemDetails.get("event_date")) : "";
                String loc = itemDetails.containsKey("location") && itemDetails.get("location") != null ? String.valueOf(itemDetails.get("location")) : "";
                return date + (!loc.isEmpty() ? " • " + loc : "");
            }
        }
        return "";
    }
}
