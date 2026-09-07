import API from "../services/api";

export const achievementApi = {
  getApprovedAchievements: (params) => API.get("/achievements/", { params }),
  getMyAchievements: () => API.get("/achievements/mine"),
  getAlumniAchievements: (alumniId) => API.get(`/achievements/alumni/${alumniId}`),
  getAchievementById: (id) => API.get(`/achievements/${id}`),
  submitAchievement: (data) => API.post("/achievements/", data),
  deleteAchievement: (id) => API.delete(`/achievements/${id}`),
  getPendingAchievements: () => API.get("/achievements/admin/pending"),
  approveAchievement: (id) => API.put(`/achievements/admin/${id}/approve`),
  rejectAchievement: (id, reason) =>
    API.put(`/achievements/admin/${id}/reject`, null, { params: { reason } }),
};
