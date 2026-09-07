import { useEffect, useState } from "react";
import DashboardLayout from "../layouts/DashboardLayout";
import { adminApi } from "../api/adminApi";
import { announcementApi } from "../api/announcementApi";
import { storyApi } from "../api/storyApi";
import { achievementApi } from "../api/achievementApi";
import { adminAlumniApi } from "../api/adminAlumniApi";

const AdminDashboard = () => {
  const [stats, setStats] = useState(null);
  const [users, setUsers] = useState([]);
  const [announcements, setAnnouncements] = useState([]);
  const [pendingStories, setPendingStories] = useState([]);
  const [pendingAchievements, setPendingAchievements] = useState([]);
  const [savedSegments, setSavedSegments] = useState([]);
  const [managementTab, setManagementTab] = useState("users"); // "users", "stories", "achievements", "announcements"
  const [loadingStats, setLoadingStats] = useState(true);
  const [loadingUsers, setLoadingUsers] = useState(true);
  const [loadingModeration, setLoadingModeration] = useState(false);
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
    audience_type: "ALL_USERS",
    segment_id: "",
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
        priority: (annForm.priority || "NORMAL").toUpperCase(),
        audience_type: annForm.audience_type || "ALL_USERS",
        segment_id:
          annForm.audience_type === "SEGMENT" && annForm.segment_id
            ? parseInt(annForm.segment_id, 10)
            : null,
        expires_at: annForm.expires_at ? new Date(annForm.expires_at).toISOString() : null,
      };
      await announcementApi.createAnnouncement(payload);
      setSuccessMsg("Announcement published successfully!");
      setShowAnnModal(false);
      setAnnForm({
        title: "",
        content: "",
        priority: "normal",
        audience_type: "ALL_USERS",
        segment_id: "",
        expires_at: "",
      });
      fetchAnnouncements();
    } catch (err) {
      const detail = err.response?.data?.detail || "Failed to publish announcement.";
      setErrorMsg(detail);
    } finally {
      setCreatingAnn(false);
    }
  };

  const fetchModerationQueues = async () => {
    setLoadingModeration(true);
    try {
      const [storiesRes, achievementsRes] = await Promise.all([
        storyApi.getPendingStories(),
        achievementApi.getPendingAchievements(),
      ]);
      setPendingStories(storiesRes.data || []);
      setPendingAchievements(achievementsRes.data || []);
    } catch (err) {
      console.error("Failed to load moderation queues:", err);
    } finally {
      setLoadingModeration(false);
    }
  };

  useEffect(() => {
    fetchModerationQueues();
  }, []);

  const handleApproveStory = async (storyId) => {
    try {
      await storyApi.approveStory(storyId);
      setSuccessMsg("Success story approved and published!");
      fetchModerationQueues();
      fetchStats();
    } catch (err) {
      setErrorMsg(err.response?.data?.detail || "Failed to approve story.");
    }
  };

  const handleRejectStory = async (storyId) => {
    const reason = window.prompt("Enter rejection reason (optional):");
    if (reason === null) return;
    try {
      await storyApi.rejectStory(storyId, reason);
      setSuccessMsg("Success story rejected.");
      fetchModerationQueues();
      fetchStats();
    } catch (err) {
      setErrorMsg(err.response?.data?.detail || "Failed to reject story.");
    }
  };

  const handleApproveAchievement = async (achId) => {
    try {
      await achievementApi.approveAchievement(achId);
      setSuccessMsg("Achievement verified and approved!");
      fetchModerationQueues();
      fetchStats();
    } catch (err) {
      setErrorMsg(err.response?.data?.detail || "Failed to approve achievement.");
    }
  };

  const handleRejectAchievement = async (achId) => {
    const reason = window.prompt("Enter rejection reason (optional):");
    if (reason === null) return;
    try {
      await achievementApi.rejectAchievement(achId, reason);
      setSuccessMsg("Achievement rejected.");
      fetchModerationQueues();
      fetchStats();
    } catch (err) {
      setErrorMsg(err.response?.data?.detail || "Failed to reject achievement.");
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
            <span className="text-[11px] font-bold text-slate-500 uppercase">Event Attendance</span>
            <p className="text-3xl font-black text-emerald-700 mt-1">{loadingStats ? "..." : stats?.event_attendance_total ?? 0}</p>
            <span className="text-[10px] text-emerald-600 font-semibold">{stats?.event_attendance_rate ?? 0}% check-in rate ({stats?.events_registered ?? 0} RSVPs)</span>
          </div>

          <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm">
            <span className="text-[11px] font-bold text-slate-500 uppercase">Success Stories</span>
            <p className="text-3xl font-black text-rose-700 mt-1">{loadingStats ? "..." : stats?.success_stories_total ?? 0}</p>
            <span className="text-[10px] text-rose-600 font-semibold">{stats?.success_stories_pending ?? 0} pending review</span>
          </div>

          <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm">
            <span className="text-[11px] font-bold text-slate-500 uppercase">Active Chapters</span>
            <p className="text-3xl font-black text-indigo-800 mt-1">{loadingStats ? "..." : stats?.communities_total ?? 0}</p>
            <span className="text-[10px] text-indigo-600 font-semibold">{stats?.community_memberships ?? 0} memberships</span>
          </div>

          <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm">
            <span className="text-[11px] font-bold text-slate-500 uppercase">Alumni Honors</span>
            <p className="text-3xl font-black text-amber-700 mt-1">{loadingStats ? "..." : stats?.achievements_total ?? 0}</p>
            <span className="text-[10px] text-amber-600 font-semibold">{stats?.achievements_pending ?? 0} pending verification</span>
          </div>

          <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm">
            <span className="text-[11px] font-bold text-slate-500 uppercase">Opportunities</span>
            <p className="text-3xl font-black text-amber-600 mt-1">{loadingStats ? "..." : stats?.total_opportunities}</p>
            <span className="text-[10px] text-amber-500">Jobs & internships</span>
          </div>

          <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm">
            <span className="text-[11px] font-bold text-slate-500 uppercase">Total Connections</span>
            <p className="text-3xl font-black text-cyan-700 mt-1">{loadingStats ? "..." : stats?.total_connections ?? 0}</p>
            <span className="text-[10px] text-cyan-600">{stats?.accepted_connections ?? 0} active ({stats?.pending_connection_requests ?? 0} pending)</span>
          </div>

          <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm">
            <span className="text-[11px] font-bold text-slate-500 uppercase">Referral Requests</span>
            <p className="text-3xl font-black text-teal-700 mt-1">{loadingStats ? "..." : stats?.total_referral_requests ?? 0}</p>
            <span className="text-[10px] text-teal-600">{stats?.accepted_referral_requests ?? 0} accepted by alumni</span>
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

        {/* Multi-Tab Management & Moderation Center */}
        <div className="bg-white p-6 sm:p-8 rounded-3xl border border-slate-200 shadow-sm space-y-6">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-100 pb-4">
            <div>
              <h2 className="text-xl font-bold text-slate-900">Administration & Moderation Hub</h2>
              <p className="text-xs text-slate-500 mt-0.5">
                Review submissions, manage directory verification, and control broadcasts
              </p>
            </div>

            {/* Management Tabs */}
            <div className="flex flex-wrap gap-2">
              <button
                onClick={() => setManagementTab("users")}
                className={`px-3.5 py-1.5 rounded-xl text-xs font-bold transition ${
                  managementTab === "users"
                    ? "bg-slate-900 text-white shadow-xs"
                    : "bg-slate-100 text-slate-600 hover:bg-slate-200"
                }`}
              >
                👥 Users ({users.length})
              </button>
              <button
                onClick={() => setManagementTab("stories")}
                className={`px-3.5 py-1.5 rounded-xl text-xs font-bold transition flex items-center gap-1.5 ${
                  managementTab === "stories"
                    ? "bg-rose-600 text-white shadow-xs"
                    : "bg-rose-50 text-rose-700 hover:bg-rose-100 border border-rose-200"
                }`}
              >
                <span>🌟</span> Pending Stories
                {pendingStories.length > 0 && (
                  <span className="w-5 h-5 rounded-full bg-white text-rose-700 flex items-center justify-center text-[10px] font-extrabold">
                    {pendingStories.length}
                  </span>
                )}
              </button>
              <button
                onClick={() => setManagementTab("achievements")}
                className={`px-3.5 py-1.5 rounded-xl text-xs font-bold transition flex items-center gap-1.5 ${
                  managementTab === "achievements"
                    ? "bg-amber-600 text-white shadow-xs"
                    : "bg-amber-50 text-amber-800 hover:bg-amber-100 border border-amber-200"
                }`}
              >
                <span>🏆</span> Pending Honors
                {pendingAchievements.length > 0 && (
                  <span className="w-5 h-5 rounded-full bg-white text-amber-800 flex items-center justify-center text-[10px] font-extrabold">
                    {pendingAchievements.length}
                  </span>
                )}
              </button>
              <button
                onClick={() => setManagementTab("announcements")}
                className={`px-3.5 py-1.5 rounded-xl text-xs font-bold transition ${
                  managementTab === "announcements"
                    ? "bg-purple-700 text-white shadow-xs"
                    : "bg-purple-50 text-purple-700 hover:bg-purple-100 border border-purple-200"
                }`}
              >
                📢 Announcements ({announcements.length})
              </button>
            </div>
          </div>

          {/* Tab 1: Users Account Management */}
          {managementTab === "users" && (
            <div className="space-y-4">
              <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3">
                <form onSubmit={handleSearchSubmit} className="relative w-full sm:w-72">
                  <input
                    type="text"
                    placeholder="Search user name or email..."
                    value={userSearch}
                    onChange={(e) => setUserSearch(e.target.value)}
                    className="pl-9 pr-3 py-2 rounded-xl border border-slate-300 text-xs w-full focus:outline-none focus:ring-2 focus:ring-blue-500"
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
          )}

          {/* Tab 2: Success Stories Moderation Queue */}
          {managementTab === "stories" && (
            <div className="space-y-4">
              <div className="flex justify-between items-center">
                <h3 className="font-bold text-slate-800 text-sm">
                  Stories Awaiting Review ({pendingStories.length})
                </h3>
                <button
                  onClick={fetchModerationQueues}
                  className="text-xs text-slate-500 hover:text-slate-800"
                >
                  ↻ Refresh Queue
                </button>
              </div>

              {pendingStories.length === 0 ? (
                <div className="text-center py-12 bg-slate-50 rounded-2xl border border-dashed border-slate-200">
                  <span className="text-3xl">✨</span>
                  <p className="text-sm font-semibold text-slate-700 mt-2">Moderation Queue Clear</p>
                  <p className="text-xs text-slate-400">All submitted success stories have been reviewed.</p>
                </div>
              ) : (
                <div className="space-y-4">
                  {pendingStories.map((story) => (
                    <div
                      key={story.id}
                      className="bg-slate-50 border border-slate-200 rounded-2xl p-5 space-y-3"
                    >
                      <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-3">
                        <div>
                          <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-amber-100 text-amber-800 uppercase">
                            Pending Review
                          </span>
                          <h4 className="font-bold text-slate-900 text-base mt-1">{story.title}</h4>
                          <p className="text-xs text-slate-500 mt-0.5">
                            By {story.author_name} ({story.author_email}) • {story.role} at {story.company}
                          </p>
                        </div>
                        <div className="flex items-center gap-2 self-end sm:self-start">
                          <button
                            onClick={() => handleApproveStory(story.id)}
                            className="px-3.5 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold rounded-xl shadow-xs"
                          >
                            ✓ Approve & Publish
                          </button>
                          <button
                            onClick={() => handleRejectStory(story.id)}
                            className="px-3.5 py-1.5 bg-rose-600 hover:bg-rose-700 text-white text-xs font-bold rounded-xl shadow-xs"
                          >
                            ✕ Reject
                          </button>
                        </div>
                      </div>

                      <div className="bg-white p-3.5 rounded-xl border border-slate-200 text-xs text-slate-700 space-y-2">
                        <p className="font-semibold text-slate-800">Summary: "{story.summary}"</p>
                        <p className="text-slate-600 whitespace-pre-wrap">{story.content}</p>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* Tab 3: Achievements Moderation Queue */}
          {managementTab === "achievements" && (
            <div className="space-y-4">
              <div className="flex justify-between items-center">
                <h3 className="font-bold text-slate-800 text-sm">
                  Alumni Honors Awaiting Verification ({pendingAchievements.length})
                </h3>
                <button
                  onClick={fetchModerationQueues}
                  className="text-xs text-slate-500 hover:text-slate-800"
                >
                  ↻ Refresh Queue
                </button>
              </div>

              {pendingAchievements.length === 0 ? (
                <div className="text-center py-12 bg-slate-50 rounded-2xl border border-dashed border-slate-200">
                  <span className="text-3xl">🏅</span>
                  <p className="text-sm font-semibold text-slate-700 mt-2">No Pending Honors</p>
                  <p className="text-xs text-slate-400">All submitted alumni achievements have been verified.</p>
                </div>
              ) : (
                <div className="space-y-4">
                  {pendingAchievements.map((ach) => (
                    <div
                      key={ach.id}
                      className="bg-slate-50 border border-slate-200 rounded-2xl p-5 space-y-3"
                    >
                      <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-3">
                        <div>
                          <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-amber-100 text-amber-800 uppercase">
                            {ach.category}
                          </span>
                          <h4 className="font-bold text-slate-900 text-base mt-1">{ach.title}</h4>
                          <p className="text-xs text-slate-500 mt-0.5">
                            By {ach.user_name} • Issued by {ach.issuer || "Self/Organization"}
                          </p>
                        </div>
                        <div className="flex items-center gap-2 self-end sm:self-start">
                          <button
                            onClick={() => handleApproveAchievement(ach.id)}
                            className="px-3.5 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold rounded-xl shadow-xs"
                          >
                            ✓ Verify & Approve
                          </button>
                          <button
                            onClick={() => handleRejectAchievement(ach.id)}
                            className="px-3.5 py-1.5 bg-rose-600 hover:bg-rose-700 text-white text-xs font-bold rounded-xl shadow-xs"
                          >
                            ✕ Reject
                          </button>
                        </div>
                      </div>

                      <div className="bg-white p-3.5 rounded-xl border border-slate-200 text-xs text-slate-700 space-y-2">
                        <p>{ach.description}</p>
                        {ach.evidence_url && (
                          <p className="pt-1">
                            <span className="font-semibold">Evidence Link: </span>
                            <a
                              href={ach.evidence_url}
                              target="_blank"
                              rel="noreferrer"
                              className="text-blue-600 underline font-mono"
                            >
                              {ach.evidence_url}
                            </a>
                          </p>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* Tab 4: Announcements Broadcasts */}
          {managementTab === "announcements" && (
            <div className="space-y-4">
              <div className="flex justify-between items-center">
                <h3 className="font-bold text-slate-800 text-sm">Active Broadcasts ({announcements.length})</h3>
                <button
                  onClick={() => {
                    adminAlumniApi.getSegments().then((r) => setSavedSegments(r.data || [])).catch(() => {});
                    setShowAnnModal(true);
                  }}
                  className="px-3 py-1.5 bg-purple-700 hover:bg-purple-800 text-white font-bold text-xs rounded-xl"
                >
                  + New Announcement
                </button>
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
                          <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-blue-100 text-blue-800 uppercase">
                            {ann.audience_type || "ALL_USERS"}
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
              <p className="text-xs text-slate-500 mb-6">Broadcast an important update or alert with targeted audience delivery.</p>

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

                {/* Audience Targeting */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Audience Target</label>
                    <select
                      value={annForm.audience_type}
                      onChange={(e) => setAnnForm({ ...annForm, audience_type: e.target.value })}
                      className="w-full border border-slate-300 p-3 rounded-xl text-sm bg-white focus:ring-2 focus:ring-purple-500 focus:outline-none"
                    >
                      <option value="ALL_USERS">All Users (Everyone)</option>
                      <option value="STUDENTS">Students Only</option>
                      <option value="ALUMNI">Alumni Only</option>
                      <option value="SEGMENT">Target Specific Segment</option>
                    </select>
                  </div>

                  {annForm.audience_type === "SEGMENT" ? (
                    <div>
                      <label className="block text-xs font-semibold text-slate-600 mb-1">Select Alumni Segment *</label>
                      <select
                        required
                        value={annForm.segment_id}
                        onChange={(e) => setAnnForm({ ...annForm, segment_id: e.target.value })}
                        className="w-full border border-slate-300 p-3 rounded-xl text-sm bg-white focus:ring-2 focus:ring-purple-500 focus:outline-none"
                      >
                        <option value="">-- Choose Segment --</option>
                        {savedSegments.map((s) => (
                          <option key={s.id} value={s.id}>
                            {s.name} (~{s.estimated_count} members)
                          </option>
                        ))}
                      </select>
                    </div>
                  ) : (
                    <div>
                      <label className="block text-xs font-semibold text-slate-600 mb-1">Target Description</label>
                      <p className="text-xs text-slate-500 p-3 bg-slate-50 rounded-xl border border-slate-200">
                        {annForm.audience_type === "ALL_USERS" && "Delivers in-app alerts to all members"}
                        {annForm.audience_type === "STUDENTS" && "Visible only to verified student profiles"}
                        {annForm.audience_type === "ALUMNI" && "Visible only to registered alumni profiles"}
                      </p>
                    </div>
                  )}
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
