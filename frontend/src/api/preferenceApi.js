import API from "../services/api";

export const preferenceApi = {
  getPreferences: () => API.get("/notification-preferences/"),
  updatePreferences: (data) => API.put("/notification-preferences/", data),
};
