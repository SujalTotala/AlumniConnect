import API from "../services/api";

export const opportunityApi = {
  getOpportunities: (params) => API.get("/opportunities/", { params }),
  getOpportunityById: (id) => API.get(`/opportunities/${id}`),
  createOpportunity: (data) => API.post("/opportunities/", data),
  updateOpportunity: (id, data) => API.put(`/opportunities/${id}`, data),
  deleteOpportunity: (id) => API.delete(`/opportunities/${id}`),
};
