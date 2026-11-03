import API from "../services/api";

export const connectionApi = {
  sendRequest: (userId) => API.post(`/connections/request/${userId}`),
  getReceivedRequests: () => API.get("/connections/received"),
  getSentRequests: () => API.get("/connections/sent"),
  getMyNetwork: () => API.get("/connections/my-network"),
  acceptRequest: (connectionId) => API.put(`/connections/${connectionId}/accept`),
  rejectRequest: (connectionId) => API.put(`/connections/${connectionId}/reject`),
  removeConnection: (connectionId) => API.delete(`/connections/${connectionId}`),
  getConnectionStatus: (userId) => API.get(`/connections/status/${userId}`),
  getSuggestions: (params) => API.get("/connections/suggestions", { params }),
  getNetworkSummary: () => API.get("/connections/network-summary"),
};
