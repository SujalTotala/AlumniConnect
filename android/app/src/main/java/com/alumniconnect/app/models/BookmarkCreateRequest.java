package com.alumniconnect.app.models;

import com.google.gson.annotations.SerializedName;

public class BookmarkCreateRequest {
    @SerializedName("item_type")
    private String itemType;

    @SerializedName("item_id")
    private int itemId;

    public BookmarkCreateRequest(String itemType, int itemId) {
        this.itemType = itemType;
        this.itemId = itemId;
    }

    public String getItemType() { return itemType; }
    public int getItemId() { return itemId; }
}
