import API from "../services/api";

export const alumniApi = {
  getAlumni: (params) => API.get("/alumni/", { params }),
  getAlumniById: (id) => API.get(`/alumni/${id}`),
  createAlumni: (data) => API.post("/alumni/", data),
  updateAlumni: (id, data) => API.put(`/alumni/${id}`, data),
  deleteAlumni: (id) => API.delete(`/alumni/${id}`),
  getRecommendedMentors: () => API.get("/alumni/recommendations/mentors"),
};
