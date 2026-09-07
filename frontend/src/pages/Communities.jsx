import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import DashboardLayout from "../layouts/DashboardLayout";
import { communityApi } from "../api/communityApi";

const COMMUNITY_TYPES = [
  { label: "All Types", value: "" },
  { label: "City Chapters", value: "CITY" },
  { label: "Batch Years", value: "BATCH" },
  { label: "Departments", value: "DEPARTMENT" },
  { label: "Industries", value: "INDUSTRY" },
  { label: "General", value: "GENERAL" },
];

export default function Communities() {
  const [communities, setCommunities] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedType, setSelectedType] = useState("");
  const [searchQuery, setSearchQuery] = useState("");
  const [activeTab, setActiveTab] = useState("all"); // "all" or "my"
  const [errorMsg, setErrorMsg] = useState("");
  const [successMsg, setSuccessMsg] = useState("");

  // Create Modal State
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [creating, setCreating] = useState(false);
  const [createForm, setCreateForm] = useState({
    name: "",
    description: "",
    community_type: "CITY",
  });

  let currentUser = null;
  try {
    const userStr = localStorage.getItem("user");
    if (userStr) currentUser = JSON.parse(userStr);
  } catch (e) {
    console.error(e);
  }

  const isAlumniOrAdmin =
    currentUser?.role?.toLowerCase() === "alumni" ||
    currentUser?.role?.toLowerCase() === "admin";

  const fetchCommunities = async () => {
    setLoading(true);
    setErrorMsg("");
    try {
      if (activeTab === "my") {
        const res = await communityApi.getMyCommunities();
        setCommunities(res.data || []);
      } else {
        const params = {};
        if (selectedType) params.community_type = selectedType;
        if (searchQuery.trim()) params.search = searchQuery.trim();
        const res = await communityApi.getCommunities(params);
        setCommunities(res.data || []);
      }
    } catch (err) {
      console.error("Failed to load communities:", err);
      setErrorMsg("Unable to load alumni chapters and communities.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCommunities();
  }, [activeTab, selectedType]);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    fetchCommunities();
  };

  const handleJoin = async (communityId) => {
    try {
      await communityApi.joinCommunity(communityId);
      setSuccessMsg("Successfully joined community!");
      fetchCommunities();
    } catch (err) {
      setErrorMsg(err.response?.data?.detail || "Could not join community.");
    }
  };

  const handleLeave = async (communityId) => {
    if (!window.confirm("Leave this community chapter?")) return;
    try {
      await communityApi.leaveCommunity(communityId);
      setSuccessMsg("Left community chapter.");
      fetchCommunities();
    } catch (err) {
      setErrorMsg(err.response?.data?.detail || "Could not leave community.");
    }
  };

  const handleCreateCommunity = async (e) => {
    e.preventDefault();
    if (!createForm.name.trim() || !createForm.description.trim()) {
      setErrorMsg("Name and description are required.");
      return;
    }
    setCreating(true);
    setErrorMsg("");
    try {
      await communityApi.createCommunity(createForm);
      setSuccessMsg("Community chapter created successfully!");
      setIsCreateOpen(false);
      setCreateForm({ name: "", description: "", community_type: "CITY" });
      fetchCommunities();
    } catch (err) {
      setErrorMsg(err.response?.data?.detail || "Failed to create community.");
    } finally {
      setCreating(false);
    }
  };

  const getTypeBadgeStyle = (type) => {
    switch (type?.toUpperCase()) {
      case "CITY":
        return "bg-cyan-50 text-cyan-700 border-cyan-200";
      case "BATCH":
        return "bg-amber-50 text-amber-700 border-amber-200";
      case "DEPARTMENT":
        return "bg-indigo-50 text-indigo-700 border-indigo-200";
      case "INDUSTRY":
        return "bg-emerald-50 text-emerald-700 border-emerald-200";
      default:
        return "bg-slate-50 text-slate-700 border-slate-200";
    }
  };

  return (
    <DashboardLayout>
      <div className="space-y-6 max-w-7xl mx-auto pb-12">
        {/* Banner Header */}
        <div className="bg-gradient-to-r from-blue-700 via-indigo-700 to-violet-800 rounded-2xl p-6 sm:p-8 text-white shadow-xl flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div className="max-w-2xl">
            <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-white/20 backdrop-blur-md mb-3">
              🏛️ Regional & Affinity Groups
            </span>
            <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight">
              Communities & Alumni Chapters
            </h1>
            <p className="mt-2 text-blue-100 text-sm sm:text-base leading-relaxed">
              Connect with fellow alumni in your city, graduation batch, academic department,
              or industry sector for discussions, local meetups, and opportunities.
            </p>
          </div>
          {isAlumniOrAdmin && (
            <button
              onClick={() => setIsCreateOpen(true)}
              className="px-5 py-2.5 bg-white text-indigo-700 hover:bg-indigo-50 font-bold rounded-xl shadow-md transition-all whitespace-nowrap self-start md:self-auto text-sm"
            >
              + Create Chapter
            </button>
          )}
        </div>

        {/* Feedback Alerts */}
        {errorMsg && (
          <div className="p-4 bg-rose-50 border border-rose-200 text-rose-700 rounded-xl text-sm flex items-center justify-between">
            <span>{errorMsg}</span>
            <button onClick={() => setErrorMsg("")} className="font-bold text-rose-800">✕</button>
          </div>
        )}
        {successMsg && (
          <div className="p-4 bg-emerald-50 border border-emerald-200 text-emerald-700 rounded-xl text-sm flex items-center justify-between">
            <span>{successMsg}</span>
            <button onClick={() => setSuccessMsg("")} className="font-bold text-emerald-800">✕</button>
          </div>
        )}

        {/* Filters and Search Bar */}
        <div className="bg-white rounded-2xl border border-slate-200 p-4 sm:p-5 shadow-xs space-y-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            {/* Tab switch */}
            <div className="flex gap-2">
              <button
                onClick={() => setActiveTab("all")}
                className={`px-4 py-2 text-sm font-semibold rounded-lg transition ${
                  activeTab === "all"
                    ? "bg-indigo-600 text-white shadow-xs"
                    : "text-slate-600 hover:bg-slate-100"
                }`}
              >
                All Chapters
              </button>
              <button
                onClick={() => setActiveTab("my")}
                className={`px-4 py-2 text-sm font-semibold rounded-lg transition ${
                  activeTab === "my"
                    ? "bg-indigo-600 text-white shadow-xs"
                    : "text-slate-600 hover:bg-slate-100"
                }`}
              >
                My Communities
              </button>
            </div>

            {/* Search Input */}
            {activeTab === "all" && (
              <form onSubmit={handleSearchSubmit} className="flex gap-2 max-w-sm w-full">
                <input
                  type="text"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder="Search chapters by name..."
                  className="w-full px-3.5 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-indigo-500"
                />
                <button
                  type="submit"
                  className="px-4 py-2 bg-slate-900 text-white font-semibold text-sm rounded-xl hover:bg-slate-800"
                >
                  Search
                </button>
              </form>
            )}
          </div>

          {/* Type Filter Chips */}
          {activeTab === "all" && (
            <div className="flex flex-wrap gap-2 pt-2 border-t border-slate-100">
              {COMMUNITY_TYPES.map((type) => (
                <button
                  key={type.value}
                  onClick={() => setSelectedType(type.value)}
                  className={`px-3 py-1.5 text-xs font-semibold rounded-lg border transition ${
                    selectedType === type.value
                      ? "bg-indigo-50 text-indigo-700 border-indigo-300 font-bold"
                      : "bg-white text-slate-600 border-slate-200 hover:bg-slate-50"
                  }`}
                >
                  {type.label}
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Communities Grid */}
        {loading ? (
          <div className="py-20 text-center">
            <div className="w-10 h-10 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin mx-auto mb-3"></div>
            <p className="text-sm text-slate-500">Loading chapters...</p>
          </div>
        ) : communities.length === 0 ? (
          <div className="text-center py-16 bg-white rounded-2xl border border-slate-200 p-8 shadow-xs">
            <span className="text-4xl">🏛️</span>
            <h3 className="mt-3 text-lg font-bold text-slate-800">No Communities Found</h3>
            <p className="mt-1 text-sm text-slate-500 max-w-md mx-auto">
              {activeTab === "my"
                ? "You haven't joined any alumni communities yet. Browse chapters to connect!"
                : "No matching community chapters found for your current filter."}
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {communities.map((comm) => (
              <div
                key={comm.id}
                className="bg-white rounded-2xl border border-slate-200 shadow-xs hover:shadow-md transition flex flex-col justify-between overflow-hidden"
              >
                <div className="p-6">
                  <div className="flex items-center justify-between mb-3">
                    <span
                      className={`text-xs font-bold uppercase tracking-wider px-2.5 py-1 rounded-full border ${getTypeBadgeStyle(
                        comm.community_type
                      )}`}
                    >
                      {comm.community_type}
                    </span>
                    <span className="text-xs text-slate-500 flex items-center gap-1">
                      👥 {comm.members_count} {comm.members_count === 1 ? "member" : "members"}
                    </span>
                  </div>

                  <h3 className="text-lg font-bold text-slate-900 line-clamp-1">{comm.name}</h3>
                  <p className="mt-2 text-sm text-slate-600 line-clamp-3 leading-relaxed">
                    {comm.description}
                  </p>

                  {comm.creator_name && (
                    <p className="mt-4 text-xs text-slate-400">
                      Organized by: <span className="font-semibold text-slate-600">{comm.creator_name}</span>
                    </p>
                  )}
                </div>

                <div className="px-6 py-4 bg-slate-50 border-t border-slate-100 flex items-center justify-between">
                  <Link
                    to={`/communities/${comm.id}`}
                    className="text-sm font-semibold text-indigo-600 hover:text-indigo-800 flex items-center gap-1"
                  >
                    Open Chapter <span>→</span>
                  </Link>

                  {comm.is_member ? (
                    <div className="flex items-center gap-2">
                      <span className="text-xs font-bold text-emerald-700 bg-emerald-100 px-2.5 py-1 rounded-full">
                        Joined ✓
                      </span>
                      <button
                        onClick={() => handleLeave(comm.id)}
                        className="text-xs text-rose-600 hover:text-rose-800 font-semibold underline"
                      >
                        Leave
                      </button>
                    </div>
                  ) : (
                    <button
                      onClick={() => handleJoin(comm.id)}
                      className="px-3.5 py-1.5 bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-bold rounded-xl shadow-xs transition"
                    >
                      Join Chapter
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Create Community Modal */}
        {isCreateOpen && (
          <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4 overflow-y-auto">
            <div className="bg-white rounded-3xl max-w-lg w-full p-6 sm:p-8 shadow-2xl border border-slate-100 my-8">
              <div className="flex justify-between items-center mb-5">
                <div>
                  <h3 className="text-xl font-bold text-slate-900">Create Community Chapter</h3>
                  <p className="text-xs text-slate-500 mt-0.5">
                    Start a regional hub, graduation batch, or industry circle.
                  </p>
                </div>
                <button
                  onClick={() => setIsCreateOpen(false)}
                  className="text-slate-400 hover:text-slate-700 text-xl font-bold"
                >
                  ✕
                </button>
              </div>

              <form onSubmit={handleCreateCommunity} className="space-y-4">
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    Chapter Name *
                  </label>
                  <input
                    type="text"
                    value={createForm.name}
                    onChange={(e) => setCreateForm({ ...createForm, name: e.target.value })}
                    placeholder="e.g. San Francisco Bay Area Alumni"
                    className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-sm focus:outline-indigo-500"
                    required
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    Chapter Classification *
                  </label>
                  <select
                    value={createForm.community_type}
                    onChange={(e) => setCreateForm({ ...createForm, community_type: e.target.value })}
                    className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-sm focus:outline-indigo-500"
                  >
                    <option value="CITY">City Chapter</option>
                    <option value="BATCH">Graduation Batch Year</option>
                    <option value="DEPARTMENT">Academic Department</option>
                    <option value="INDUSTRY">Industry & Domain Group</option>
                    <option value="GENERAL">General Interest Community</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    Description & Purpose *
                  </label>
                  <textarea
                    rows={4}
                    value={createForm.description}
                    onChange={(e) => setCreateForm({ ...createForm, description: e.target.value })}
                    placeholder="Describe who this chapter is for, planned activities, and guidelines..."
                    className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-sm focus:outline-indigo-500"
                    required
                  ></textarea>
                </div>

                <div className="pt-2 flex justify-end gap-3">
                  <button
                    type="button"
                    onClick={() => setIsCreateOpen(false)}
                    className="px-4 py-2.5 text-slate-700 font-semibold text-sm hover:bg-slate-100 rounded-xl"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={creating}
                    className="px-5 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-sm rounded-xl shadow-xs disabled:opacity-50"
                  >
                    {creating ? "Creating..." : "Create Chapter"}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </div>
    </DashboardLayout>
  );
}
