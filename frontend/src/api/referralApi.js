import API from "../services/api";

export const referralApi = {
  createReferral: (data) => API.post("/referrals/", data),
  getSentReferrals: () => API.get("/referrals/sent"),
  getReceivedReferrals: () => API.get("/referrals/received"),
  getEligibleAlumni: (opportunityId) => API.get(`/referrals/eligible-alumni/${opportunityId}`),
  acceptReferral: (id) => API.put(`/referrals/${id}/accept`),
  declineReferral: (id) => API.put(`/referrals/${id}/decline`),
  completeReferral: (id) => API.put(`/referrals/${id}/complete`),
  cancelReferral: (id) => API.delete(`/referrals/${id}`),
};
