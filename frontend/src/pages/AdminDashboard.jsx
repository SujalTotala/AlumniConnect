import { useEffect, useState } from "react";
import DashboardLayout from "../layouts/DashboardLayout";
import { adminApi } from "../api/adminApi";

const AdminDashboard = () => {
  const [stats, setStats] = useState(null);
  const [users, setUsers] = useState([]);
  const [loadingStats, setLoadingStats] = useState(true);
  const [loadingUsers, setLoadingUsers] = useState(true);
  const [userSearch, setUserSearch] = useState("");
  const [roleFilter, setRoleFilter] = useState("");
  const [errorMsg, setErrorMsg] = useState("");
  const [successMsg, setSuccessMsg] = useState("");

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
      setUsers(res.data);
    } catch (err) {
      console.error("Failed to load user list:", err);
      setErrorMsg("Failed to load user list.");
    } finally {
      setLoadingUsers(false);
    }
  };

  useEffect(() => {
    fetchStats();
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

  return (
    <DashboardLayout>
      <div className="max-w-7xl mx-auto space-y-8">
        {/* Header */}
        <div>
          <h1 className="text-3xl font-extrabold text-slate-900">Admin Control Center</h1>
          <p className="text-sm text-slate-500 mt-1">
            System overview, engagement analytics, and user account management
          </p>
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
            <span className="text-[11px] font-bold text-slate-500 uppercase">Total Alumni</span>
            <p className="text-3xl font-black text-blue-700 mt-1">{loadingStats ? "..." : stats?.total_alumni}</p>
            <span className="text-[10px] text-blue-500">Graduates directory</span>
          </div>

          <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm">
            <span className="text-[11px] font-bold text-slate-500 uppercase">Total Students</span>
            <p className="text-3xl font-black text-indigo-700 mt-1">{loadingStats ? "..." : stats?.total_students}</p>
            <span className="text-[10px] text-indigo-500">Undergraduate/Masters</span>
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
                    <th className="py-3 px-4">Status</th>
                    <th className="py-3 px-4 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {users.map((u) => (
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
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </DashboardLayout>
  );
};

export default AdminDashboard;
