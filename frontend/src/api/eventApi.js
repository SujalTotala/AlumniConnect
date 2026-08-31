import API from "../services/api";

export const eventApi = {
  getEvents: (params) => API.get("/events/", { params }),
  getEventById: (id) => API.get(`/events/${id}`),
  createEvent: (data) => API.post("/events/", data),
  updateEvent: (id, data) => API.put(`/events/${id}`, data),
  deleteEvent: (id) => API.delete(`/events/${id}`),
  registerEvent: (id) => API.post(`/events/${id}/register`),
  cancelRegistration: (id) => API.delete(`/events/${id}/register`),
  getRegistrations: (id) => API.get(`/events/${id}/registrations`),
};
