import API from "../services/api";

export const mentorshipApi = {
  getMentors: (params) => API.get("/mentorship/mentors", { params }),
  sendRequest: (data) => API.post("/mentorship/requests", data),
  getSentRequests: () => API.get("/mentorship/requests/sent"),
  getReceivedRequests: () => API.get("/mentorship/requests/received"),
  acceptRequest: (id, data) => API.put(`/mentorship/requests/${id}/accept`, data),
  rejectRequest: (id, data) => API.put(`/mentorship/requests/${id}/reject`, data),
  completeRequest: (id) => API.put(`/mentorship/requests/${id}/complete`),
};
