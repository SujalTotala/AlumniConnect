import { useEffect, useState } from "react";
import DashboardLayout from "../layouts/DashboardLayout";
import { adminApi } from "../api/adminApi";
import { announcementApi } from "../api/announcementApi";

const AdminDashboard = () => {
  const [stats, setStats] = useState(null);
  const [users, setUsers] = useState([]);
  const [announcements, setAnnouncements] = useState([]);
  const [loadingStats, setLoadingStats] = useState(true);
  const [loadingUsers, setLoadingUsers] = useState(true);
  const [userSearch, setUserSearch] = useState("");
  const [roleFilter, setRoleFilter] = useState("");
  const [errorMsg, setErrorMsg] = useState("");
  const [successMsg, setSuccessMsg] = useState("");

  // Announcement Modal Form
  const [showAnnModal, setShowAnnModal] = useState(false);
  const [annForm, setAnnForm] = useState({
    title: "",
    content: "",
    priority: "normal",
    expires_at: "",
  });
  const [creatingAnn, setCreatingAnn] = useState(false);
  const [exportingCsv, setExportingCsv] = useState(false);

  const fetchStats = async () => {
    setLoadingStats(true);
    try {
      const res = await adminApi.getStatistics();
      setStats(res.data);
    } catch (err) {
      console.error("Failed to load admin statistics:", err);
      setErrorMsg("Failed to load admin statistics.");
    } finally {
      setLoadingStats(false);
    }
  };

  const fetchUsers = async () => {
    setLoadingUsers(true);
    try {
      const params = {};
      if (userSearch) params.search = userSearch;
      if (roleFilter) params.role = roleFilter;
      const res = await adminApi.getUsers(params);
      setUsers(res.data || []);
    } catch (err) {
      console.error("Failed to load user list:", err);
      setErrorMsg("Failed to load user list.");
    } finally {
      setLoadingUsers(false);
    }
  };

  const fetchAnnouncements = async () => {
    try {
      const res = await announcementApi.getAnnouncements(false);
      setAnnouncements(res.data || []);
    } catch (err) {
      console.error("Failed to load announcements:", err);
    }
  };

  useEffect(() => {
    fetchStats();
    fetchAnnouncements();
  }, []);

  useEffect(() => {
    fetchUsers();
  }, [roleFilter]);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    fetchUsers();
  };

  const handleToggleStatus = async (user) => {
    setErrorMsg("");
    setSuccessMsg("");
    try {
      await adminApi.updateUserStatus(user.id, !user.is_active);
      setSuccessMsg(`User '${user.name}' status updated.`);
      fetchUsers();
      fetchStats();
    } catch (err) {
      const detail = err.response?.data?.detail || "Failed to update user status.";
      setErrorMsg(detail);
    }
  };

  const handleDeleteUser = async (user) => {
    if (!window.confirm(`Are you sure you want to delete user '${user.name}' (${user.email})?`)) return;
    setErrorMsg("");
    setSuccessMsg("");
    try {
      await adminApi.deleteUser(user.id);
      setSuccessMsg(`User '${user.name}' successfully removed.`);
      fetchUsers();
      fetchStats();
    } catch (err) {
      const detail = err.response?.data?.detail || "Failed to delete user.";
      setErrorMsg(detail);
    }
  };

  const handleToggleVerifyAlumni = async (alumniProfileId, currentVerifiedStatus) => {
    setErrorMsg("");
    setSuccessMsg("");
    try {
      await adminApi.verifyAlumni(alumniProfileId, !currentVerifiedStatus);
      setSuccessMsg("Alumni verification status updated.");
      fetchUsers();
      fetchStats();
    } catch (err) {
      const detail = err.response?.data?.detail || "Failed to update alumni verification status.";
      setErrorMsg(detail);
    }
  };

  const handleExportCsv = async () => {
    setExportingCsv(true);
    setErrorMsg("");
    try {
      const res = await adminApi.exportAlumniCsv();
      const blob = new Blob([res.data], { type: "text/csv;charset=utf-8;" });
      const link = document.createElement("a");
      link.href = window.URL.createObjectURL(blob);
      link.setAttribute("download", `alumni_directory_${Date.now()}.csv`);
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      setSuccessMsg("Alumni directory CSV exported successfully.");
      setTimeout(() => setSuccessMsg(""), 3000);
    } catch (err) {
      console.error("Failed to export alumni CSV:", err);
      setErrorMsg("Failed to export alumni CSV.");
    } finally {
      setExportingCsv(false);
    }
  };

  const handleCreateAnnouncement = async (e) => {
    e.preventDefault();
    setCreatingAnn(true);
    setErrorMsg("");
    try {
      const payload = {
        title: annForm.title,
        content: annForm.content,
        priority: annForm.priority,
        expires_at: annForm.expires_at ? new Date(annForm.expires_at).toISOString() : null,
      };
      await announcementApi.createAnnouncement(payload);
      setSuccessMsg("Announcement published successfully!");
      setShowAnnModal(false);
      setAnnForm({ title: "", content: "", priority: "normal", expires_at: "" });
      fetchAnnouncements();
    } catch (err) {
      const detail = err.response?.data?.detail || "Failed to publish announcement.";
      setErrorMsg(detail);
    } finally {
      setCreatingAnn(false);
    }
  };

  const handleDeleteAnnouncement = async (id) => {
    if (!window.confirm("Remove this announcement?")) return;
    try {
      await announcementApi.deleteAnnouncement(id);
      setSuccessMsg("Announcement removed.");
      fetchAnnouncements();
    } catch (err) {
      console.error("Failed to delete announcement:", err);
      setErrorMsg("Failed to delete announcement.");
    }
  };

  return (
    <DashboardLayout>
      <div className="max-w-7xl mx-auto space-y-8">
        {/* Header */}
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
          <div>
            <h1 className="text-3xl font-extrabold text-slate-900">Admin Control Center</h1>
            <p className="text-sm text-slate-500 mt-1">
              System overview, verified member controls, announcements, and export utilities
            </p>
          </div>

          <div className="flex flex-wrap gap-2.5">
            <button
              onClick={() => setShowAnnModal(true)}
              className="bg-purple-700 hover:bg-purple-800 text-white px-4 py-2 rounded-xl text-xs font-semibold shadow-sm transition flex items-center gap-1.5"
            >
              <span>📢</span> + Post Announcement
            </button>
            <button
              onClick={handleExportCsv}
              disabled={exportingCsv}
              className="bg-emerald-600 hover:bg-emerald-700 text-white px-4 py-2 rounded-xl text-xs font-semibold shadow-sm transition flex items-center gap-1.5 disabled:bg-emerald-400"
            >
              <span>📥</span> {exportingCsv ? "Exporting..." : "Export Alumni CSV"}
            </button>
          </div>
        </div>

        {/* Alerts */}
        {errorMsg && (
          <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-xl text-sm flex justify-between">
            <span>{errorMsg}</span>
            <button onClick={() => setErrorMsg("")} className="font-bold">✕</button>
          </div>
        )}
        {successMsg && (
          <div className="bg-emerald-50 border border-emerald-200 text-emerald-700 px-4 py-3 rounded-xl text-sm flex justify-between">
            <span>{successMsg}</span>
            <button onClick={() => setSuccessMsg("")} className="font-bold">✕</button>
          </div>
        )}

        {/* Live KPI Grid */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
          <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm">
            <span className="text-[11px] font-bold text-slate-500 uppercase">Total Users</span>
            <p className="text-3xl font-black text-slate-900 mt-1">{loadingStats ? "..." : stats?.total_users}</p>
            <span className="text-[10px] text-slate-400">All registered accounts</span>
          </div>

          <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm">
            <span className="text-[11px] font-bold text-slate-500 uppercase">Verified Alumni</span>
            <p className="text-3xl font-black text-blue-700 mt-1">{loadingStats ? "..." : stats?.verified_alumni ?? 0}</p>
            <span className="text-[10px] text-blue-500">Of {stats?.total_alumni ?? 0} total alumni</span>
          </div>

          <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm">
            <span className="text-[11px] font-bold text-slate-500 uppercase">Total Students</span>
            <p className="text-3xl font-black text-indigo-700 mt-1">{loadingStats ? "..." : stats?.total_students}</p>
            <span className="text-[10px] text-indigo-500">Undergraduate / Masters</span>
          </div>

          <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm">
            <span className="text-[11px] font-bold text-slate-500 uppercase">Active Mentors</span>
            <p className="text-3xl font-black text-purple-700 mt-1">{loadingStats ? "..." : stats?.active_mentors}</p>
            <span className="text-[10px] text-purple-500">Open for guidance</span>
          </div>

          <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm">
            <span className="text-[11px] font-bold text-slate-500 uppercase">Events Scheduled</span>
            <p className="text-3xl font-black text-emerald-700 mt-1">{loadingStats ? "..." : stats?.total_events}</p>
            <span className="text-[10px] text-emerald-500">Meets & webinars</span>
          </div>

          <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm">
            <span className="text-[11px] font-bold text-slate-500 uppercase">Event RSVPs</span>
            <p className="text-3xl font-black text-emerald-800 mt-1">{loadingStats ? "..." : stats?.total_event_registrations}</p>
            <span className="text-[10px] text-slate-400">Total registrations</span>
          </div>

          <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm">
            <span className="text-[11px] font-bold text-slate-500 uppercase">Opportunities</span>
            <p className="text-3xl font-black text-amber-700 mt-1">{loadingStats ? "..." : stats?.total_opportunities}</p>
            <span className="text-[10px] text-amber-500">Jobs & internships</span>
          </div>

          <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm">
            <span className="text-[11px] font-bold text-slate-500 uppercase">Pending Requests</span>
            <p className="text-3xl font-black text-rose-700 mt-1">{loadingStats ? "..." : stats?.pending_mentorship_requests}</p>
            <span className="text-[10px] text-rose-500">Mentorship backlog</span>
          </div>
        </div>

        {/* Analytics Breakdown: Department, Graduation Year, Companies */}
        {stats && (
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {/* By Department */}
            <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm space-y-3">
              <h3 className="font-bold text-slate-900 text-sm flex items-center gap-1.5">
                <span>🎓</span> Alumni by Department
              </h3>
              {Object.keys(stats.alumni_by_department || {}).length === 0 ? (
                <p className="text-xs text-slate-400 py-3">No department data yet.</p>
              ) : (
                <div className="space-y-2">
                  {Object.entries(stats.alumni_by_department).slice(0, 5).map(([dept, count]) => (
                    <div key={dept} className="flex justify-between items-center text-xs">
                      <span className="text-slate-700 truncate">{dept}</span>
                      <span className="font-bold text-blue-600 bg-blue-50 px-2 py-0.5 rounded">
                        {count}
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* By Graduation Year */}
            <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm space-y-3">
              <h3 className="font-bold text-slate-900 text-sm flex items-center gap-1.5">
                <span>📅</span> Alumni by Class Year
              </h3>
              {Object.keys(stats.alumni_by_graduation_year || {}).length === 0 ? (
                <p className="text-xs text-slate-400 py-3">No graduation year data yet.</p>
              ) : (
                <div className="space-y-2">
                  {Object.entries(stats.alumni_by_graduation_year).slice(0, 5).map(([yr, count]) => (
                    <div key={yr} className="flex justify-between items-center text-xs">
                      <span className="text-slate-700">Class of {yr}</span>
                      <span className="font-bold text-indigo-600 bg-indigo-50 px-2 py-0.5 rounded">
                        {count}
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* By Top Companies */}
            <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm space-y-3">
              <h3 className="font-bold text-slate-900 text-sm flex items-center gap-1.5">
                <span>🏢</span> Top Alumni Employers
              </h3>
              {Object.keys(stats.alumni_by_company || {}).length === 0 ? (
                <p className="text-xs text-slate-400 py-3">No company data yet.</p>
              ) : (
                <div className="space-y-2">
                  {Object.entries(stats.alumni_by_company).slice(0, 5).map(([comp, count]) => (
                    <div key={comp} className="flex justify-between items-center text-xs">
                      <span className="text-slate-700 truncate">{comp}</span>
                      <span className="font-bold text-purple-600 bg-purple-50 px-2 py-0.5 rounded">
                        {count}
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}

        {/* Announcements Management Section */}
        <div className="bg-white p-6 rounded-3xl border border-slate-200 shadow-sm space-y-4">
          <div className="flex justify-between items-center">
            <div>
              <h2 className="text-lg font-bold text-slate-900">Broadcast Announcements</h2>
              <p className="text-xs text-slate-500">Notices displayed on user home feeds</p>
            </div>
            <span className="text-xs text-slate-400">{announcements.length} Total</span>
          </div>

          {announcements.length === 0 ? (
            <p className="text-xs text-slate-400 py-4 text-center">No announcements broadcast yet.</p>
          ) : (
            <div className="divide-y divide-slate-100">
              {announcements.map((ann) => (
                <div key={ann.id} className="py-3 flex justify-between items-start gap-4">
                  <div>
                    <div className="flex items-center gap-2">
                      <span
                        className={`text-[10px] font-bold px-2 py-0.5 rounded uppercase ${
                          ann.priority === "urgent"
                            ? "bg-red-100 text-red-700"
                            : ann.priority === "high"
                            ? "bg-amber-100 text-amber-800"
                            : "bg-slate-100 text-slate-700"
                        }`}
                      >
                        {ann.priority}
                      </span>
                      <h4 className="font-bold text-sm text-slate-900">{ann.title}</h4>
                      {!ann.is_active && (
                        <span className="text-[10px] text-slate-400 bg-slate-100 px-1.5 py-0.2 rounded">Inactive</span>
                      )}
                    </div>
                    <p className="text-xs text-slate-600 mt-1 line-clamp-2">{ann.content}</p>
                    <span className="text-[10px] text-slate-400 mt-1 block">
                      Posted on {new Date(ann.created_at).toLocaleDateString()}
                    </span>
                  </div>

                  <button
                    onClick={() => handleDeleteAnnouncement(ann.id)}
                    className="text-red-500 hover:text-red-700 text-xs font-semibold px-2 py-1"
                  >
                    Delete
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* User Management Section */}
        <div className="bg-white p-6 sm:p-8 rounded-3xl border border-slate-200 shadow-sm space-y-6">
          <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
            <div>
              <h2 className="text-xl font-bold text-slate-900">User Account Management</h2>
              <p className="text-xs text-slate-500 mt-0.5">Control user access, roles, and status</p>
            </div>

            <div className="flex flex-col sm:flex-row gap-3 w-full sm:w-auto">
              <form onSubmit={handleSearchSubmit} className="relative">
                <input
                  type="text"
                  placeholder="Search user name or email..."
                  value={userSearch}
                  onChange={(e) => setUserSearch(e.target.value)}
                  className="pl-9 pr-3 py-2 rounded-xl border border-slate-300 text-xs w-full sm:w-60 focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
                <span className="absolute left-3 top-2 text-xs text-slate-400">🔍</span>
              </form>

              <select
                value={roleFilter}
                onChange={(e) => setRoleFilter(e.target.value)}
                className="border border-slate-300 bg-white p-2 rounded-xl text-xs font-medium text-slate-700 focus:outline-none"
              >
                <option value="">All Roles</option>
                <option value="student">Students</option>
                <option value="alumni">Alumni</option>
                <option value="admin">Admins</option>
              </select>
            </div>
          </div>

          {loadingUsers ? (
            <p className="text-center py-8 text-slate-500 text-sm">Loading users...</p>
          ) : users.length === 0 ? (
            <p className="text-center py-8 text-slate-500 text-sm">No users matched search criteria.</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs text-slate-700">
                <thead className="bg-slate-50 border-b border-slate-200 text-slate-500 uppercase text-[10px] font-bold">
                  <tr>
                    <th className="py-3 px-4">User</th>
                    <th className="py-3 px-4">Email</th>
                    <th className="py-3 px-4">Role</th>
                    <th className="py-3 px-4">Verification</th>
                    <th className="py-3 px-4">Status</th>
                    <th className="py-3 px-4 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {users.map((u) => {
                    const isAlum = u.role?.toLowerCase() === "alumni";
                    const isVerified = u.profile?.is_verified;
                    const alumniId = u.profile?.id;

                    return (
                      <tr key={u.id} className="hover:bg-slate-50/80 transition">
                        <td className="py-3 px-4 font-bold text-slate-900">{u.name}</td>
                        <td className="py-3 px-4 text-slate-600">{u.email}</td>
                        <td className="py-3 px-4">
                          <span
                            className={`px-2 py-0.5 rounded-full font-bold uppercase text-[9px] ${
                              u.role === "admin"
                                ? "bg-purple-100 text-purple-800"
                                : u.role === "alumni"
                                ? "bg-emerald-100 text-emerald-800"
                                : "bg-blue-100 text-blue-800"
                            }`}
                          >
                            {u.role}
                          </span>
                        </td>

                        {/* Verification Column */}
                        <td className="py-3 px-4">
                          {isAlum && alumniId ? (
                            <button
                              onClick={() => handleToggleVerifyAlumni(alumniId, isVerified)}
                              className={`px-2.5 py-1 rounded-lg text-[10px] font-semibold border transition ${
                                isVerified
                                  ? "bg-blue-50 text-blue-700 border-blue-200 hover:bg-blue-100"
                                  : "bg-slate-50 text-slate-500 border-slate-200 hover:bg-slate-100"
                              }`}
                              title="Click to toggle alumni verification"
                            >
                              {isVerified ? "✓ Verified" : "Pending"}
                            </button>
                          ) : (
                            <span className="text-slate-300">—</span>
                          )}
                        </td>

                        <td className="py-3 px-4">
                          <span
                            className={`px-2 py-0.5 rounded-full font-semibold text-[10px] ${
                              u.is_active ? "bg-emerald-50 text-emerald-700" : "bg-red-50 text-red-600"
                            }`}
                          >
                            {u.is_active ? "Active" : "Deactivated"}
                          </span>
                        </td>
                        <td className="py-3 px-4 text-right space-x-2">
                          <button
                            onClick={() => handleToggleStatus(u)}
                            className="text-xs text-blue-700 hover:text-blue-900 font-semibold"
                          >
                            {u.is_active ? "Deactivate" : "Activate"}
                          </button>
                          <button
                            onClick={() => handleDeleteUser(u)}
                            className="text-xs text-red-500 hover:text-red-700 font-semibold"
                          >
                            Delete
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* Create Announcement Modal */}
        {showAnnModal && (
          <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4 backdrop-blur-sm">
            <div className="bg-white rounded-3xl p-8 max-w-lg w-full shadow-2xl relative">
              <button
                onClick={() => setShowAnnModal(false)}
                className="absolute right-5 top-5 text-slate-400 hover:text-slate-700 text-xl font-bold"
              >
                ✕
              </button>

              <h3 className="text-xl font-bold text-slate-900 mb-1">Post System Announcement</h3>
              <p className="text-xs text-slate-500 mb-6">Broadcast an important update or alert to all members.</p>

              <form onSubmit={handleCreateAnnouncement} className="space-y-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-600 mb-1">Announcement Title *</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. Campus Reunion Registration Open"
                    value={annForm.title}
                    onChange={(e) => setAnnForm({ ...annForm, title: e.target.value })}
                    className="w-full border border-slate-300 p-3 rounded-xl text-sm focus:ring-2 focus:ring-purple-500 focus:outline-none"
                  />
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Priority Level</label>
                    <select
                      value={annForm.priority}
                      onChange={(e) => setAnnForm({ ...annForm, priority: e.target.value })}
                      className="w-full border border-slate-300 p-3 rounded-xl text-sm bg-white focus:ring-2 focus:ring-purple-500 focus:outline-none"
                    >
                      <option value="normal">Normal</option>
                      <option value="high">High</option>
                      <option value="urgent">Urgent</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Expiry Date (Optional)</label>
                    <input
                      type="date"
                      value={annForm.expires_at}
                      onChange={(e) => setAnnForm({ ...annForm, expires_at: e.target.value })}
                      className="w-full border border-slate-300 p-3 rounded-xl text-sm focus:ring-2 focus:ring-purple-500 focus:outline-none"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-600 mb-1">Announcement Content *</label>
                  <textarea
                    rows="3"
                    required
                    placeholder="Provide detailed instructions or notice..."
                    value={annForm.content}
                    onChange={(e) => setAnnForm({ ...annForm, content: e.target.value })}
                    className="w-full border border-slate-300 p-3 rounded-xl text-sm focus:ring-2 focus:ring-purple-500 focus:outline-none"
                  ></textarea>
                </div>

                <div className="flex justify-end gap-3 pt-4 border-t border-slate-100">
                  <button
                    type="button"
                    onClick={() => setShowAnnModal(false)}
                    className="px-4 py-2 rounded-xl text-slate-600 text-xs font-semibold"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={creatingAnn}
                    className="px-6 py-2 rounded-xl bg-purple-700 hover:bg-purple-800 text-white text-xs font-semibold shadow-md disabled:bg-purple-400"
                  >
                    {creatingAnn ? "Publishing..." : "Publish Announcement"}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </div>
    </DashboardLayout>
  );
};

export default AdminDashboard;
