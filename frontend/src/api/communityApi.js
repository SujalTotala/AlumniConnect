import API from "../services/api";

export const communityApi = {
  getCommunities: (params) => API.get("/communities/", { params }),
  getMyCommunities: () => API.get("/communities/my"),
  getCommunityById: (id) => API.get(`/communities/${id}`),
  createCommunity: (data) => API.post("/communities/", data),
  joinCommunity: (id) => API.post(`/communities/${id}/join`),
  leaveCommunity: (id) => API.delete(`/communities/${id}/leave`),
  getMembers: (id) => API.get(`/communities/${id}/members`),
  getPosts: (id) => API.get(`/communities/${id}/posts`),
  createPost: (id, data) => API.post(`/communities/${id}/posts`, data),
};
