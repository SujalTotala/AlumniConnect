import API from "../services/api";

export const announcementApi = {
  getAnnouncements: (active_only = true) =>
    API.get("/announcements/", { params: { active_only } }),
  createAnnouncement: (data) => API.post("/announcements/", data),
  updateAnnouncement: (id, data) => API.put(`/announcements/${id}`, data),
  deleteAnnouncement: (id) => API.delete(`/announcements/${id}`),
};
