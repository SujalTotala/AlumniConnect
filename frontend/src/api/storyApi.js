import API from "../services/api";

export const storyApi = {
  getApprovedStories: () => API.get("/success-stories/"),
  getMyStories: () => API.get("/success-stories/mine"),
  getStoryById: (id) => API.get(`/success-stories/${id}`),
  submitStory: (data) => API.post("/success-stories/", data),
  updateStory: (id, data) => API.put(`/success-stories/${id}`, data),
  deleteStory: (id) => API.delete(`/success-stories/${id}`),
  getPendingStories: () => API.get("/success-stories/admin/pending"),
  approveStory: (id) => API.put(`/success-stories/admin/${id}/approve`),
  rejectStory: (id, reason) =>
    API.put(`/success-stories/admin/${id}/reject`, null, { params: { reason } }),
};
