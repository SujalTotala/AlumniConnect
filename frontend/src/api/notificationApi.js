import API from "../services/api";

export const notificationApi = {
  getNotifications: () => API.get("/notifications/"),
  getUnreadCount: () => API.get("/notifications/unread-count"),
  markRead: (id) => API.put(`/notifications/${id}/read`),
  markAllRead: () => API.put("/notifications/read-all"),
};
