import { useEffect, useState } from "react";
import DashboardLayout from "../layouts/DashboardLayout";
import { connectionApi } from "../api/connectionApi";
import { referralApi } from "../api/referralApi";

const MyNetwork = () => {
  const [activeTab, setActiveTab] = useState("connections"); // 'connections', 'received', 'sent', 'suggestions', 'referrals'
  const [referralSubTab, setReferralSubTab] = useState("sent"); // 'sent', 'received'
  
  const [connections, setConnections] = useState([]);
  const [receivedRequests, setReceivedRequests] = useState([]);
  const [sentRequests, setSentRequests] = useState([]);
  const [suggestions, setSuggestions] = useState([]);
  const [sentReferrals, setSentReferrals] = useState([]);
  const [receivedReferrals, setReceivedReferrals] = useState([]);
  const [summary, setSummary] = useState({
    total_connections: 0,
    pending_requests_received: 0,
    pending_requests_sent: 0,
    total_referrals_sent: 0,
    total_referrals_received: 0,
  });

  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState("");
  const [actionLoadingId, setActionLoadingId] = useState(null);
  const [errorMsg, setErrorMsg] = useState("");
  const [successMsg, setSuccessMsg] = useState("");

  let currentUser = null;
  try {
    const userStr = localStorage.getItem("user");
    if (userStr) currentUser = JSON.parse(userStr);
  } catch (e) {
    console.error(e);
  }

  const isAlumni = currentUser?.role?.toLowerCase() === "alumni";

  const showSuccess = (msg) => {
    setSuccessMsg(msg);
    setErrorMsg("");
    setTimeout(() => setSuccessMsg(""), 4000);
  };

  const showError = (msg) => {
    setErrorMsg(msg);
    setSuccessMsg("");
    setTimeout(() => setErrorMsg(""), 5000);
  };

  const loadSummary = async () => {
    try {
      const res = await connectionApi.getNetworkSummary();
      if (res.data) setSummary(res.data);
    } catch (err) {
      console.error("Error loading network summary:", err);
    }
  };

  const loadData = async () => {
    setLoading(true);
    try {
      await loadSummary();
      if (activeTab === "connections") {
        const res = await connectionApi.getMyNetwork();
        setConnections(res.data || []);
      } else if (activeTab === "received") {
        const res = await connectionApi.getReceivedRequests();
        setReceivedRequests(res.data || []);
      } else if (activeTab === "sent") {
        const res = await connectionApi.getSentRequests();
        setSentRequests(res.data || []);
      } else if (activeTab === "suggestions") {
        const res = await connectionApi.getSuggestions({ limit: 20 });
        setSuggestions(res.data || []);
      } else if (activeTab === "referrals") {
        const [sentRes, recRes] = await Promise.all([
          referralApi.getSentReferrals(),
          referralApi.getReceivedReferrals(),
        ]);
        setSentReferrals(sentRes.data || []);
        setReceivedReferrals(recRes.data || []);
      }
    } catch (err) {
      console.error("Error loading network data:", err);
      showError("Failed to load networking data. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [activeTab]);

  // Connection Handlers
  const handleSendConnection = async (userId) => {
    setActionLoadingId(`conn-send-${userId}`);
    try {
      await connectionApi.sendRequest(userId);
      showSuccess("Connection request sent successfully!");
      setSuggestions((prev) => prev.filter((s) => s.user_id !== userId));
      loadSummary();
    } catch (err) {
      showError(err.response?.data?.detail || "Failed to send connection request.");
    } finally {
      setActionLoadingId(null);
    }
  };

  const handleAcceptConnection = async (connectionId) => {
    setActionLoadingId(`conn-accept-${connectionId}`);
    try {
      await connectionApi.acceptRequest(connectionId);
      showSuccess("Connection accepted! User added to your network.");
      setReceivedRequests((prev) => prev.filter((r) => r.id !== connectionId));
      loadSummary();
    } catch (err) {
      showError(err.response?.data?.detail || "Failed to accept connection request.");
    } finally {
      setActionLoadingId(null);
    }
  };

  const handleRejectConnection = async (connectionId) => {
    setActionLoadingId(`conn-reject-${connectionId}`);
    try {
      await connectionApi.rejectRequest(connectionId);
      showSuccess("Connection request declined.");
      setReceivedRequests((prev) => prev.filter((r) => r.id !== connectionId));
      loadSummary();
    } catch (err) {
      showError(err.response?.data?.detail || "Failed to decline connection request.");
    } finally {
      setActionLoadingId(null);
    }
  };

  const handleRemoveConnection = async (connectionId) => {
    if (!window.confirm("Are you sure you want to remove this connection from your network?")) return;
    setActionLoadingId(`conn-remove-${connectionId}`);
    try {
      await connectionApi.removeConnection(connectionId);
      showSuccess("Connection removed from your network.");
      setConnections((prev) => prev.filter((c) => c.id !== connectionId));
      setSentRequests((prev) => prev.filter((s) => s.id !== connectionId));
      loadSummary();
    } catch (err) {
      showError(err.response?.data?.detail || "Failed to remove connection.");
    } finally {
      setActionLoadingId(null);
    }
  };

  // Referral Handlers
  const handleAcceptReferral = async (referralId) => {
    setActionLoadingId(`ref-accept-${referralId}`);
    try {
      await referralApi.acceptReferral(referralId);
      showSuccess("Referral request accepted! The requester has been notified.");
      const recRes = await referralApi.getReceivedReferrals();
      setReceivedReferrals(recRes.data || []);
      loadSummary();
    } catch (err) {
      showError(err.response?.data?.detail || "Failed to accept referral.");
    } finally {
      setActionLoadingId(null);
    }
  };

  const handleDeclineReferral = async (referralId) => {
    setActionLoadingId(`ref-decline-${referralId}`);
    try {
      await referralApi.declineReferral(referralId);
      showSuccess("Referral request declined.");
      const recRes = await referralApi.getReceivedReferrals();
      setReceivedReferrals(recRes.data || []);
      loadSummary();
    } catch (err) {
      showError(err.response?.data?.detail || "Failed to decline referral.");
    } finally {
      setActionLoadingId(null);
    }
  };

  const handleCompleteReferral = async (referralId) => {
    setActionLoadingId(`ref-comp-${referralId}`);
    try {
      await referralApi.completeReferral(referralId);
      showSuccess("Referral marked as completed! Thank you for supporting fellow community members.");
      const recRes = await referralApi.getReceivedReferrals();
      setReceivedReferrals(recRes.data || []);
      loadSummary();
    } catch (err) {
      showError(err.response?.data?.detail || "Failed to mark referral complete.");
    } finally {
      setActionLoadingId(null);
    }
  };

  const handleCancelReferral = async (referralId) => {
    if (!window.confirm("Are you sure you want to cancel this referral request?")) return;
    setActionLoadingId(`ref-cancel-${referralId}`);
    try {
      await referralApi.cancelReferral(referralId);
      showSuccess("Referral request cancelled.");
      setSentReferrals((prev) => prev.filter((r) => r.id !== referralId));
      loadSummary();
    } catch (err) {
      showError(err.response?.data?.detail || "Failed to cancel referral.");
    } finally {
      setActionLoadingId(null);
    }
  };

  // Helper to extract the other party from a connection object
  const getPartnerUser = (conn) => {
    if (!conn) return null;
    if (conn.sender_id === currentUser?.id) return conn.receiver;
    return conn.sender;
  };

  const filteredConnections = connections.filter((c) => {
    const p = getPartnerUser(c);
    if (!p) return false;
    const q = searchQuery.toLowerCase();
    return (
      p.name?.toLowerCase().includes(q) ||
      p.company?.toLowerCase().includes(q) ||
      p.department?.toLowerCase().includes(q) ||
      p.job_role?.toLowerCase().includes(q) ||
      p.role?.toLowerCase().includes(q)
    );
  });

  return (
    <DashboardLayout>
      <div className="max-w-7xl mx-auto space-y-6 pb-12">
        {/* Banner / Header */}
        <div className="bg-gradient-to-r from-blue-700 via-indigo-800 to-slate-900 rounded-3xl p-8 text-white shadow-xl flex flex-col md:flex-row justify-between items-start md:items-center gap-6">
          <div>
            <div className="flex items-center gap-2 mb-2">
              <span className="bg-blue-500/30 text-blue-200 text-xs font-semibold px-3 py-1 rounded-full border border-blue-400/30 uppercase tracking-wider">
                Networking & Connections Hub
              </span>
            </div>
            <h1 className="text-3xl sm:text-4xl font-extrabold tracking-tight">
              My Network & Referrals 🌐
            </h1>
            <p className="text-blue-100/90 text-sm sm:text-base mt-2 max-w-2xl">
              Connect with fellow students and verified alumni, exchange internal job referrals, and build lasting professional relationships.
            </p>
          </div>

          {/* Quick Summary Pills */}
          <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
            <div className="bg-white/10 backdrop-blur-md rounded-2xl p-3.5 border border-white/20 text-center">
              <span className="text-2xl font-black">{summary.total_connections}</span>
              <p className="text-[11px] text-blue-200 uppercase tracking-wider font-semibold mt-0.5">Connections</p>
            </div>
            <div className="bg-white/10 backdrop-blur-md rounded-2xl p-3.5 border border-white/20 text-center">
              <span className="text-2xl font-black">{summary.pending_requests_received}</span>
              <p className="text-[11px] text-blue-200 uppercase tracking-wider font-semibold mt-0.5">Pending In</p>
            </div>
            <div className="bg-white/10 backdrop-blur-md rounded-2xl p-3.5 border border-white/20 text-center col-span-2 sm:col-span-1">
              <span className="text-2xl font-black">
                {isAlumni ? summary.total_referrals_received : summary.total_referrals_sent}
              </span>
              <p className="text-[11px] text-blue-200 uppercase tracking-wider font-semibold mt-0.5">Referrals</p>
            </div>
          </div>
        </div>

        {/* Notifications / Alerts */}
        {successMsg && (
          <div className="bg-emerald-50 border border-emerald-200 text-emerald-800 px-5 py-3.5 rounded-2xl shadow-sm flex items-center justify-between text-sm font-medium">
            <div className="flex items-center gap-2">
              <span>✅</span>
              <span>{successMsg}</span>
            </div>
            <button onClick={() => setSuccessMsg("")} className="text-emerald-600 hover:text-emerald-900 font-bold">✕</button>
          </div>
        )}
        {errorMsg && (
          <div className="bg-red-50 border border-red-200 text-red-800 px-5 py-3.5 rounded-2xl shadow-sm flex items-center justify-between text-sm font-medium">
            <div className="flex items-center gap-2">
              <span>⚠️</span>
              <span>{errorMsg}</span>
            </div>
            <button onClick={() => setErrorMsg("")} className="text-red-600 hover:text-red-900 font-bold">✕</button>
          </div>
        )}

        {/* Main Tab Bar */}
        <div className="bg-white rounded-2xl border border-slate-200 p-2 shadow-sm flex flex-wrap gap-2">
          <button
            onClick={() => setActiveTab("connections")}
            className={`px-4 py-2.5 rounded-xl text-sm font-bold transition flex items-center gap-2 ${
              activeTab === "connections"
                ? "bg-blue-600 text-white shadow-md"
                : "text-slate-600 hover:bg-slate-100"
            }`}
          >
            <span>👥 Connections</span>
            <span className={`text-xs px-2 py-0.5 rounded-full ${
              activeTab === "connections" ? "bg-white/20 text-white" : "bg-slate-100 text-slate-700"
            }`}>
              {summary.total_connections}
            </span>
          </button>

          <button
            onClick={() => setActiveTab("received")}
            className={`px-4 py-2.5 rounded-xl text-sm font-bold transition flex items-center gap-2 ${
              activeTab === "received"
                ? "bg-blue-600 text-white shadow-md"
                : "text-slate-600 hover:bg-slate-100"
            }`}
          >
            <span>📥 Received Requests</span>
            {summary.pending_requests_received > 0 && (
              <span className="text-xs px-2 py-0.5 rounded-full bg-amber-500 text-white font-black animate-pulse">
                {summary.pending_requests_received}
              </span>
            )}
          </button>

          <button
            onClick={() => setActiveTab("sent")}
            className={`px-4 py-2.5 rounded-xl text-sm font-bold transition flex items-center gap-2 ${
              activeTab === "sent"
                ? "bg-blue-600 text-white shadow-md"
                : "text-slate-600 hover:bg-slate-100"
            }`}
          >
            <span>📤 Sent Requests</span>
            <span className={`text-xs px-2 py-0.5 rounded-full ${
              activeTab === "sent" ? "bg-white/20 text-white" : "bg-slate-100 text-slate-700"
            }`}>
              {summary.pending_requests_sent}
            </span>
          </button>

          <button
            onClick={() => setActiveTab("suggestions")}
            className={`px-4 py-2.5 rounded-xl text-sm font-bold transition flex items-center gap-2 ${
              activeTab === "suggestions"
                ? "bg-indigo-600 text-white shadow-md"
                : "text-slate-600 hover:bg-slate-100"
            }`}
          >
            <span>✨ Suggested Connections</span>
          </button>

          <button
            onClick={() => setActiveTab("referrals")}
            className={`px-4 py-2.5 rounded-xl text-sm font-bold transition flex items-center gap-2 ${
              activeTab === "referrals"
                ? "bg-emerald-600 text-white shadow-md"
                : "text-slate-600 hover:bg-slate-100"
            }`}
          >
            <span>💼 Referral Requests</span>
          </button>
        </div>

        {/* Tab Content Loading */}
        {loading ? (
          <div className="py-16 text-center text-slate-500 flex flex-col items-center gap-3">
            <div className="w-10 h-10 border-4 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
            <p className="font-semibold text-sm">Loading your network...</p>
          </div>
        ) : (
          <div>
            {/* 1. CONNECTIONS TAB */}
            {activeTab === "connections" && (
              <div className="space-y-4">
                <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-white p-4 rounded-2xl border border-slate-200">
                  <div className="relative w-full sm:w-96">
                    <span className="absolute left-3.5 top-2.5 text-slate-400">🔍</span>
                    <input
                      type="text"
                      placeholder="Search connections by name, company, or role..."
                      value={searchQuery}
                      onChange={(e) => setSearchQuery(e.target.value)}
                      className="w-full pl-10 pr-4 py-2 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                    />
                  </div>
                  <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">
                    Showing {filteredConnections.length} of {connections.length} Connections
                  </span>
                </div>

                {filteredConnections.length === 0 ? (
                  <div className="bg-white rounded-3xl p-12 text-center border border-slate-200 space-y-4">
                    <div className="text-5xl">👥</div>
                    <h3 className="text-xl font-bold text-slate-800">
                      {searchQuery ? "No matching connections found" : "Your network is just getting started"}
                    </h3>
                    <p className="text-sm text-slate-500 max-w-md mx-auto">
                      {searchQuery
                        ? "Try refining your search terms."
                        : "Connect with classmates, alumni mentors, and peers to expand your career opportunities and request job referrals."}
                    </p>
                    {!searchQuery && (
                      <button
                        onClick={() => setActiveTab("suggestions")}
                        className="bg-blue-600 hover:bg-blue-500 text-white px-5 py-2.5 rounded-xl text-sm font-bold shadow-md transition"
                      >
                        Explore Suggested Connections →
                      </button>
                    )}
                  </div>
                ) : (
                  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
                    {filteredConnections.map((conn) => {
                      const partner = getPartnerUser(conn);
                      if (!partner) return null;
                      return (
                        <div
                          key={conn.id}
                          className="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm hover:shadow-md transition flex flex-col justify-between"
                        >
                          <div>
                            <div className="flex items-start justify-between gap-3 mb-3">
                              <div className="flex items-center gap-3">
                                <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-blue-600 to-indigo-600 text-white font-extrabold flex items-center justify-center text-lg shadow-sm">
                                  {partner.name.charAt(0)}
                                </div>
                                <div>
                                  <div className="flex items-center gap-1.5">
                                    <h4 className="font-bold text-slate-900 text-base">{partner.name}</h4>
                                    {partner.is_verified && (
                                      <span className="text-blue-600 text-sm" title="Verified Alumni">✓</span>
                                    )}
                                  </div>
                                  <span className={`text-[10px] uppercase font-extrabold px-2 py-0.5 rounded-md ${
                                    partner.role === "alumni" ? "bg-purple-100 text-purple-700" : "bg-blue-100 text-blue-700"
                                  }`}>
                                    {partner.role}
                                  </span>
                                </div>
                              </div>
                            </div>

                            <p className="text-xs text-slate-600 line-clamp-2 min-h-[32px] mt-2">
                              {partner.headline || partner.company || partner.department || "Active Member"}
                            </p>

                            <div className="mt-4 pt-3 border-t border-slate-100 flex flex-wrap gap-2 text-[11px] text-slate-500">
                              {partner.department && (
                                <span className="bg-slate-50 px-2 py-1 rounded-lg border border-slate-100">
                                  🏛️ {partner.department}
                                </span>
                              )}
                              {partner.graduation_year && (
                                <span className="bg-slate-50 px-2 py-1 rounded-lg border border-slate-100">
                                  🎓 Class of {partner.graduation_year}
                                </span>
                              )}
                              {partner.location && (
                                <span className="bg-slate-50 px-2 py-1 rounded-lg border border-slate-100">
                                  📍 {partner.location}
                                </span>
                              )}
                            </div>
                          </div>

                          <div className="mt-5 pt-3 border-t border-slate-100 flex items-center justify-between">
                            <span className="text-[11px] text-emerald-600 font-bold flex items-center gap-1">
                              <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span> Connected
                            </span>
                            <button
                              disabled={actionLoadingId === `conn-remove-${conn.id}`}
                              onClick={() => handleRemoveConnection(conn.id)}
                              className="text-xs text-red-600 hover:text-red-700 font-semibold px-2.5 py-1 rounded-lg hover:bg-red-50 transition"
                            >
                              {actionLoadingId === `conn-remove-${conn.id}` ? "Removing..." : "Remove"}
                            </button>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>
            )}

            {/* 2. RECEIVED REQUESTS TAB */}
            {activeTab === "received" && (
              <div className="space-y-4">
                {receivedRequests.length === 0 ? (
                  <div className="bg-white rounded-3xl p-12 text-center border border-slate-200">
                    <div className="text-4xl mb-3">📬</div>
                    <h3 className="text-lg font-bold text-slate-800">No Pending Received Requests</h3>
                    <p className="text-xs text-slate-500 mt-1">When fellow alumni or students send you connection invites, they'll appear here.</p>
                  </div>
                ) : (
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {receivedRequests.map((req) => (
                      <div
                        key={req.id}
                        className="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4"
                      >
                        <div className="flex items-center gap-3">
                          <div className="w-12 h-12 rounded-2xl bg-indigo-100 text-indigo-700 font-bold flex items-center justify-center text-lg">
                            {req.sender?.name?.charAt(0) || "U"}
                          </div>
                          <div>
                            <div className="flex items-center gap-2">
                              <h4 className="font-bold text-slate-900 text-base">{req.sender?.name}</h4>
                              <span className="text-[10px] uppercase font-bold bg-blue-100 text-blue-700 px-2 py-0.5 rounded">
                                {req.sender?.role}
                              </span>
                            </div>
                            <p className="text-xs text-slate-500 mt-0.5">
                              {req.sender?.headline || req.sender?.department || req.sender?.email}
                            </p>
                            <span className="text-[10px] text-slate-400 mt-1 block">
                              Received {new Date(req.created_at).toLocaleDateString()}
                            </span>
                          </div>
                        </div>

                        <div className="flex items-center gap-2 w-full sm:w-auto">
                          <button
                            disabled={actionLoadingId === `conn-accept-${req.id}`}
                            onClick={() => handleAcceptConnection(req.id)}
                            className="flex-1 sm:flex-none bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold px-4 py-2 rounded-xl transition shadow-sm"
                          >
                            {actionLoadingId === `conn-accept-${req.id}` ? "Accepting..." : "Accept ✓"}
                          </button>
                          <button
                            disabled={actionLoadingId === `conn-reject-${req.id}`}
                            onClick={() => handleRejectConnection(req.id)}
                            className="flex-1 sm:flex-none bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold px-3 py-2 rounded-xl transition"
                          >
                            {actionLoadingId === `conn-reject-${req.id}` ? "..." : "Decline"}
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}

            {/* 3. SENT REQUESTS TAB */}
            {activeTab === "sent" && (
              <div className="space-y-4">
                {sentRequests.length === 0 ? (
                  <div className="bg-white rounded-3xl p-12 text-center border border-slate-200">
                    <div className="text-4xl mb-3">✉️</div>
                    <h3 className="text-lg font-bold text-slate-800">No Pending Sent Requests</h3>
                    <p className="text-xs text-slate-500 mt-1">Check out suggestions to discover new mentors and peers.</p>
                  </div>
                ) : (
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {sentRequests.map((req) => (
                      <div
                        key={req.id}
                        className="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4"
                      >
                        <div className="flex items-center gap-3">
                          <div className="w-12 h-12 rounded-2xl bg-slate-100 text-slate-700 font-bold flex items-center justify-center text-lg">
                            {req.receiver?.name?.charAt(0) || "U"}
                          </div>
                          <div>
                            <div className="flex items-center gap-2">
                              <h4 className="font-bold text-slate-900 text-base">{req.receiver?.name}</h4>
                              <span className="text-[10px] uppercase font-bold bg-slate-100 text-slate-700 px-2 py-0.5 rounded">
                                {req.receiver?.role}
                              </span>
                            </div>
                            <p className="text-xs text-slate-500 mt-0.5">
                              {req.receiver?.headline || req.receiver?.department || req.receiver?.email}
                            </p>
                            <span className="text-[10px] text-amber-600 font-semibold mt-1 inline-block">
                              ⏳ Awaiting response
                            </span>
                          </div>
                        </div>

                        <button
                          disabled={actionLoadingId === `conn-remove-${req.id}`}
                          onClick={() => handleRemoveConnection(req.id)}
                          className="w-full sm:w-auto text-xs text-slate-600 hover:text-red-600 font-bold px-3 py-2 rounded-xl border border-slate-200 hover:border-red-200 hover:bg-red-50 transition"
                        >
                          {actionLoadingId === `conn-remove-${req.id}` ? "Cancelling..." : "Cancel Request"}
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}

            {/* 4. SUGGESTIONS TAB */}
            {activeTab === "suggestions" && (
              <div className="space-y-4">
                <div className="bg-gradient-to-r from-indigo-50 to-blue-50 border border-indigo-100 rounded-2xl p-4 flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <span className="text-2xl">🎯</span>
                    <div>
                      <h4 className="text-sm font-bold text-indigo-950">Deterministic Affinity Matching</h4>
                      <p className="text-xs text-indigo-700">Ranked by shared department, graduation batch, overlapping skills, and company background.</p>
                    </div>
                  </div>
                  <span className="text-xs font-black bg-indigo-200 text-indigo-800 px-3 py-1 rounded-full uppercase">
                    Rule-Based
                  </span>
                </div>

                {suggestions.length === 0 ? (
                  <div className="bg-white rounded-3xl p-12 text-center border border-slate-200">
                    <div className="text-4xl mb-3">🎉</div>
                    <h3 className="text-lg font-bold text-slate-800">You're fully caught up!</h3>
                    <p className="text-xs text-slate-500 mt-1">No new suggestions at this moment. You are already connected or have invites pending with suggested members.</p>
                  </div>
                ) : (
                  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
                    {suggestions.map((sug) => (
                      <div
                        key={sug.user_id}
                        className="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm hover:shadow-md transition flex flex-col justify-between"
                      >
                        <div>
                          <div className="flex items-start justify-between gap-3 mb-3">
                            <div className="flex items-center gap-3">
                              <div className="w-12 h-12 rounded-2xl bg-indigo-600 text-white font-extrabold flex items-center justify-center text-lg shadow-sm">
                                {sug.name.charAt(0)}
                              </div>
                              <div>
                                <div className="flex items-center gap-1.5">
                                  <h4 className="font-bold text-slate-900 text-base">{sug.name}</h4>
                                  {sug.is_verified && (
                                    <span className="text-blue-600 text-sm" title="Verified Alumni">✓</span>
                                  )}
                                </div>
                                <span className={`text-[10px] uppercase font-extrabold px-2 py-0.5 rounded-md ${
                                  sug.role === "alumni" ? "bg-purple-100 text-purple-700" : "bg-blue-100 text-blue-700"
                                }`}>
                                  {sug.role}
                                </span>
                              </div>
                            </div>

                            <span className="bg-indigo-50 border border-indigo-200 text-indigo-800 font-extrabold text-xs px-2.5 py-1 rounded-xl">
                              {sug.suggestion_score}% Match
                            </span>
                          </div>

                          <p className="text-xs text-slate-600 line-clamp-2 min-h-[32px] mt-2">
                            {sug.headline || sug.company || sug.department || "Community Member"}
                          </p>

                          {/* Reasons */}
                          <div className="mt-3 space-y-1">
                            {sug.suggestion_reasons.slice(0, 3).map((r, i) => (
                              <span
                                key={i}
                                className="inline-block text-[10px] font-semibold bg-blue-50 text-blue-700 border border-blue-100 px-2 py-0.5 rounded-md mr-1.5 mb-1"
                              >
                                • {r}
                              </span>
                            ))}
                          </div>
                        </div>

                        <div className="mt-5 pt-3 border-t border-slate-100">
                          <button
                            disabled={actionLoadingId === `conn-send-${sug.user_id}`}
                            onClick={() => handleSendConnection(sug.user_id)}
                            className="w-full bg-blue-600 hover:bg-blue-500 text-white text-xs font-bold py-2.5 rounded-xl transition shadow-sm flex items-center justify-center gap-1.5"
                          >
                            {actionLoadingId === `conn-send-${sug.user_id}` ? (
                              "Sending Request..."
                            ) : (
                              <>
                                <span>+ Connect</span>
                              </>
                            )}
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}

            {/* 5. REFERRALS TAB */}
            {activeTab === "referrals" && (
              <div className="space-y-4">
                {/* Referral Sub-tab Toggle */}
                <div className="flex gap-2 border-b border-slate-200 pb-2">
                  <button
                    onClick={() => setReferralSubTab("sent")}
                    className={`px-4 py-2 rounded-xl text-xs font-extrabold transition ${
                      referralSubTab === "sent"
                        ? "bg-slate-900 text-white"
                        : "bg-slate-100 text-slate-600 hover:bg-slate-200"
                    }`}
                  >
                    Sent Referrals ({sentReferrals.length})
                  </button>
                  {isAlumni && (
                    <button
                      onClick={() => setReferralSubTab("received")}
                      className={`px-4 py-2 rounded-xl text-xs font-extrabold transition ${
                        referralSubTab === "received"
                          ? "bg-slate-900 text-white"
                          : "bg-slate-100 text-slate-600 hover:bg-slate-200"
                      }`}
                    >
                      Received Referrals ({receivedReferrals.length})
                    </button>
                  )}
                </div>

                {/* Sent Referrals Sub-view */}
                {referralSubTab === "sent" && (
                  <div className="space-y-3">
                    {sentReferrals.length === 0 ? (
                      <div className="bg-white rounded-3xl p-12 text-center border border-slate-200">
                        <div className="text-4xl mb-3">💼</div>
                        <h3 className="text-lg font-bold text-slate-800">No Sent Referral Requests</h3>
                        <p className="text-xs text-slate-500 mt-1 max-w-md mx-auto">
                          Browse job & internship opportunities and click "Request Referral" to ask connected alumni for an internal endorsement.
                        </p>
                      </div>
                    ) : (
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        {sentReferrals.map((ref) => (
                          <div
                            key={ref.id}
                            className="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm flex flex-col justify-between space-y-3"
                          >
                            <div>
                              <div className="flex items-start justify-between gap-3">
                                <div>
                                  <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                                    {ref.opportunity?.opportunity_type || "Opportunity"}
                                  </span>
                                  <h4 className="font-bold text-slate-900 text-base">{ref.opportunity?.title}</h4>
                                  <p className="text-xs text-blue-600 font-semibold">{ref.opportunity?.company}</p>
                                </div>
                                <span className={`text-[11px] font-black uppercase px-2.5 py-1 rounded-lg ${
                                  ref.status === "ACCEPTED"
                                    ? "bg-blue-100 text-blue-800"
                                    : ref.status === "COMPLETED"
                                    ? "bg-emerald-100 text-emerald-800"
                                    : ref.status === "DECLINED"
                                    ? "bg-red-100 text-red-800"
                                    : "bg-amber-100 text-amber-800"
                                }`}>
                                  {ref.status}
                                </span>
                              </div>

                              <div className="mt-3 p-3 bg-slate-50 rounded-xl text-xs text-slate-600 border border-slate-100">
                                <span className="font-bold text-slate-800">Requested from:</span> {ref.alumni?.name} ({ref.alumni?.company || "Alumnus"})
                                {ref.message && (
                                  <p className="mt-1 italic text-slate-500">"{ref.message}"</p>
                                )}
                              </div>
                            </div>

                            <div className="pt-2 flex justify-between items-center text-[11px] text-slate-400">
                              <span>Sent {new Date(ref.created_at).toLocaleDateString()}</span>
                              {ref.status === "PENDING" && (
                                <button
                                  disabled={actionLoadingId === `ref-cancel-${ref.id}`}
                                  onClick={() => handleCancelReferral(ref.id)}
                                  className="text-red-600 hover:text-red-700 font-bold"
                                >
                                  {actionLoadingId === `ref-cancel-${ref.id}` ? "Cancelling..." : "Cancel Request"}
                                </button>
                              )}
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                )}

                {/* Received Referrals Sub-view (Alumni only) */}
                {referralSubTab === "received" && isAlumni && (
                  <div className="space-y-3">
                    {receivedReferrals.length === 0 ? (
                      <div className="bg-white rounded-3xl p-12 text-center border border-slate-200">
                        <div className="text-4xl mb-3">📬</div>
                        <h3 className="text-lg font-bold text-slate-800">No Received Referral Requests</h3>
                        <p className="text-xs text-slate-500 mt-1">When students in your network apply for roles at your company, their referral requests will appear here.</p>
                      </div>
                    ) : (
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        {receivedReferrals.map((ref) => (
                          <div
                            key={ref.id}
                            className="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm flex flex-col justify-between space-y-3"
                          >
                            <div>
                              <div className="flex items-start justify-between gap-3">
                                <div>
                                  <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                                    {ref.opportunity?.opportunity_type}
                                  </span>
                                  <h4 className="font-bold text-slate-900 text-base">{ref.opportunity?.title}</h4>
                                  <p className="text-xs text-blue-600 font-semibold">{ref.opportunity?.company}</p>
                                </div>
                                <span className={`text-[11px] font-black uppercase px-2.5 py-1 rounded-lg ${
                                  ref.status === "ACCEPTED"
                                    ? "bg-blue-100 text-blue-800"
                                    : ref.status === "COMPLETED"
                                    ? "bg-emerald-100 text-emerald-800"
                                    : ref.status === "DECLINED"
                                    ? "bg-red-100 text-red-800"
                                    : "bg-amber-100 text-amber-800"
                                }`}>
                                  {ref.status}
                                </span>
                              </div>

                              <div className="mt-3 p-3 bg-slate-50 rounded-xl text-xs text-slate-600 border border-slate-100">
                                <span className="font-bold text-slate-800">Candidate:</span> {ref.requester?.name} ({ref.requester?.department || "Student"})
                                {ref.message && (
                                  <p className="mt-1.5 italic text-slate-700 bg-white p-2 rounded-lg border border-slate-100">
                                    "{ref.message}"
                                  </p>
                                )}
                              </div>
                            </div>

                            {/* Action Buttons for Alumni */}
                            <div className="pt-2 border-t border-slate-100 flex flex-wrap items-center justify-between gap-2">
                              <span className="text-[11px] text-slate-400">
                                {new Date(ref.created_at).toLocaleDateString()}
                              </span>
                              <div className="flex items-center gap-2">
                                {ref.status === "PENDING" && (
                                  <>
                                    <button
                                      disabled={actionLoadingId === `ref-accept-${ref.id}`}
                                      onClick={() => handleAcceptReferral(ref.id)}
                                      className="bg-blue-600 hover:bg-blue-500 text-white text-xs font-bold px-3 py-1.5 rounded-xl shadow-sm transition"
                                    >
                                      {actionLoadingId === `ref-accept-${ref.id}` ? "Accepting..." : "Accept Request"}
                                    </button>
                                    <button
                                      disabled={actionLoadingId === `ref-decline-${ref.id}`}
                                      onClick={() => handleDeclineReferral(ref.id)}
                                      className="bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold px-3 py-1.5 rounded-xl transition"
                                    >
                                      {actionLoadingId === `ref-decline-${ref.id}` ? "..." : "Decline"}
                                    </button>
                                  </>
                                )}
                                {ref.status === "ACCEPTED" && (
                                  <button
                                    disabled={actionLoadingId === `ref-comp-${ref.id}`}
                                    onClick={() => handleCompleteReferral(ref.id)}
                                    className="bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold px-3 py-1.5 rounded-xl shadow-sm transition"
                                  >
                                    {actionLoadingId === `ref-comp-${ref.id}` ? "Updating..." : "Mark Completed ✓"}
                                  </button>
                                )}
                              </div>
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                )}
              </div>
            )}
          </div>
        )}
      </div>
    </DashboardLayout>
  );
};

export default MyNetwork;
