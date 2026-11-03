package com.alumniconnect.app.repositories;

import android.content.Context;
import com.alumniconnect.app.models.Bookmark;
import com.alumniconnect.app.models.BookmarkCreateRequest;
import com.alumniconnect.app.network.ApiClient;
import com.alumniconnect.app.network.ApiService;
import java.util.List;
import java.util.Map;
import retrofit2.Call;

public class BookmarkRepository {
    private final ApiService apiService;

    public BookmarkRepository(Context context) {
        this.apiService = ApiClient.getApiService(context);
    }

    public Call<List<Bookmark>> getBookmarks(String itemType) {
        return apiService.getBookmarks(itemType);
    }

    public Call<Bookmark> createBookmark(String itemType, int itemId) {
        return apiService.createBookmark(new BookmarkCreateRequest(itemType, itemId));
    }

    public Call<Map<String, Object>> checkBookmark(String itemType, int itemId) {
        return apiService.checkBookmark(itemType, itemId);
    }

    public Call<Map<String, Object>> deleteBookmark(int id) {
        return apiService.deleteBookmark(id);
    }

    public Call<Map<String, Object>> deleteBookmarkByItem(String itemType, int itemId) {
        return apiService.deleteBookmarkByItem(itemType, itemId);
    }
}
