import API from "../services/api";

export const attendanceApi = {
  generateQrToken: (eventId) => API.post(`/events/${eventId}/attendance/qr-token`),
  checkIn: (qrToken) => API.post("/events/attendance/check-in", { qr_token: qrToken }),
  manualCheckIn: (eventId, userId, notes) =>
    API.post(`/events/${eventId}/attendance/manual`, { user_id: userId, notes }),
  getAttendanceList: (eventId) => API.get(`/events/${eventId}/attendance`),
  getAttendanceStats: (eventId) => API.get(`/events/${eventId}/attendance/stats`),
};
