import API from "../services/api";

export const activityApi = {
  getActivityFeed: (limit = 20) => API.get("/activity-feed/", { params: { limit } }),
};
