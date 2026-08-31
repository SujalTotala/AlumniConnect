import API from "../services/api";

export const adminApi = {
  getStatistics: () => API.get("/admin/statistics"),
  getUsers: (params) => API.get("/admin/users", { params }),
  updateUserStatus: (id, is_active) => API.put(`/admin/users/${id}/status`, { is_active }),
  deleteUser: (id) => API.delete(`/admin/users/${id}`),
};
