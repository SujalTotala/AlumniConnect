import API from "../services/api";

export const adminAlumniApi = {
  // 1. Bulk Import & Template
  downloadTemplate: () => API.get("/admin/alumni/template", { responseType: "blob" }),
  previewImport: (file) => {
    const formData = new FormData();
    formData.append("file", file);
    return API.post("/admin/alumni/import/preview", formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });
  },
  commitImport: (importSessionId, mode) =>
    API.post("/admin/alumni/import/commit", {
      import_session_id: importSessionId,
      mode,
    }),
  getImportHistory: () => API.get("/admin/alumni/import/history"),

  // 2. Data Quality Dashboard
  getDataQuality: () => API.get("/admin/alumni/data-quality"),

  // 3. Dynamic Segments
  getSegments: () => API.get("/admin/alumni/segments"),
  createSegment: (data) => API.post("/admin/alumni/segments", data),
  previewSegmentCriteria: (filters) => API.post("/admin/alumni/segments/preview", filters),
  getSegmentDetails: (id) => API.get(`/admin/alumni/segments/${id}`),
  updateSegment: (id, data) => API.put(`/admin/alumni/segments/${id}`, data),
  deleteSegment: (id) => API.delete(`/admin/alumni/segments/${id}`),
  getSegmentMembers: (id, params) => API.get(`/admin/alumni/segments/${id}/members`, { params }),
  exportSegmentCsv: (id) => API.get(`/admin/alumni/segments/${id}/export`, { responseType: "blob" }),

  // 4. Admin Audit Logs
  getAuditLogs: (params) => API.get("/admin/audit-logs", { params }),
};
