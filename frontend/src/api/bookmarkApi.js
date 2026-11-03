import API from "../services/api";

export const bookmarkApi = {
  getBookmarks: (item_type) =>
    API.get("/bookmarks/", { params: item_type ? { item_type } : {} }),
  createBookmark: (item_type, item_id) =>
    API.post("/bookmarks/", { item_type, item_id }),
  checkBookmark: (item_type, item_id) =>
    API.get(`/bookmarks/check/${item_type}/${item_id}`),
  deleteBookmark: (id) => API.delete(`/bookmarks/${id}`),
  deleteBookmarkByItem: (item_type, item_id) =>
    API.delete(`/bookmarks/${item_type}/${item_id}`),
};
