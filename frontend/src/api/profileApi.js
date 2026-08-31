import API from "../services/api";

export const profileApi = {
  getMyProfile: () => API.get("/profile/me"),
  updateMyProfile: (data) => API.put("/profile/me", data),
};
