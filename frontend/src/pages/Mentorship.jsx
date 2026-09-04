import { useEffect, useState } from "react";
import DashboardLayout from "../layouts/DashboardLayout";
import { mentorshipApi } from "../api/mentorshipApi";
import { alumniApi } from "../api/alumniApi";

const Mentorship = () => {
  const [activeTab, setActiveTab] = useState("mentors"); // 'mentors', 'sent', 'received'
  const [mentors, setMentors] = useState([]);
  const [recommendations, setRecommendations] = useState([]);
  const [sentRequests, setSentRequests] = useState([]);
  const [receivedRequests, setReceivedRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedMentor, setSelectedMentor] = useState(null);
  const [requestMessage, setRequestMessage] = useState("");
  const [sendingRequest, setSendingRequest] = useState(false);
  const [actionNote, setActionNote] = useState("");
  const [actionModal, setActionModal] = useState(null); // { id, action: 'accept' | 'reject' }
  const [errorMsg, setErrorMsg] = useState("");
  const [successMsg, setSuccessMsg] = useState("");

  let currentUser = null;
  try {
    const userStr = localStorage.getItem("user");
    if (userStr) currentUser = JSON.parse(userStr);
  } catch (e) {
    console.error(e);
  }

  const role = currentUser?.role?.toLowerCase();

  const fetchMentors = async () => {
    setLoading(true);
    try {
      const res = await mentorshipApi.getMentors();
      setMentors(res.data || []);

      // If user is logged in, fetch tailored recommendations
      try {
        const recRes = await alumniApi.getRecommendedMentors();
        if (Array.isArray(recRes.data)) {
          setRecommendations(recRes.data.filter((r) => r.match_score > 0));
        }
      } catch (recErr) {
        console.log("No personalized mentor recommendations available yet", recErr);
      }
    } catch (err) {
      console.error("Failed to load mentors", err);
    } finally {
      setLoading(false);
    }
  };

  const fetchSent = async () => {
    setLoading(true);
    try {
      const res = await mentorshipApi.getSentRequests();
      setSentRequests(res.data || []);
    } catch (err) {
      console.error("Failed to load sent requests", err);
    } finally {
      setLoading(false);
    }
  };

  const fetchReceived = async () => {
    setLoading(true);
    try {
      const res = await mentorshipApi.getReceivedRequests();
      setReceivedRequests(res.data || []);
    } catch (err) {
      console.error("Failed to load received requests", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    setErrorMsg("");
    setSuccessMsg("");
    if (activeTab === "mentors") fetchMentors();
    else if (activeTab === "sent") fetchSent();
    else if (activeTab === "received") fetchReceived();
  }, [activeTab]);

  const handleSendRequest = async (e) => {
    e.preventDefault();
    if (!selectedMentor) return;
    setSendingRequest(true);
    setErrorMsg("");
    setSuccessMsg("");

    try {
      await mentorshipApi.sendRequest({
        mentor_id: selectedMentor.user_id || selectedMentor.id,
        message: requestMessage,
      });
      setSuccessMsg(`Mentorship request successfully sent to ${selectedMentor.name}!`);
      setSelectedMentor(null);
      setRequestMessage("");
    } catch (err) {
      const detail = err.response?.data?.detail || "Failed to send mentorship request.";
      setErrorMsg(detail);
    } finally {
      setSendingRequest(false);
    }
  };

  const handleAccept = async () => {
    if (!actionModal) return;
    setErrorMsg("");
    setSuccessMsg("");
    try {
      await mentorshipApi.acceptRequest(actionModal.id, { response_note: actionNote });
      setSuccessMsg("Mentorship request accepted!");
      setActionModal(null);
      setActionNote("");
      fetchReceived();
    } catch (err) {
      const detail = err.response?.data?.detail || "Failed to accept request.";
      setErrorMsg(detail);
    }
  };

  const handleReject = async () => {
    if (!actionModal) return;
    setErrorMsg("");
    setSuccessMsg("");
    try {
      await mentorshipApi.rejectRequest(actionModal.id, { response_note: actionNote });
      setSuccessMsg("Mentorship request rejected.");
      setActionModal(null);
      setActionNote("");
      fetchReceived();
    } catch (err) {
      const detail = err.response?.data?.detail || "Failed to reject request.";
      setErrorMsg(detail);
    }
  };

  const handleComplete = async (id) => {
    if (!window.confirm("Mark this mentorship interaction as completed?")) return;
    try {
      await mentorshipApi.completeRequest(id);
      setSuccessMsg("Mentorship marked as completed.");
      if (activeTab === "received") fetchReceived();
      else fetchSent();
    } catch (err) {
      const detail = err.response?.data?.detail || "Failed to complete request.";
      setErrorMsg(detail);
    }
  };

  const getStatusBadge = (status) => {
    switch (status?.toUpperCase()) {
      case "ACCEPTED":
        return <span className="bg-emerald-100 text-emerald-800 border border-emerald-200 text-xs font-bold px-2.5 py-1 rounded-full">ACCEPTED</span>;
      case "REJECTED":
        return <span className="bg-red-100 text-red-800 border border-red-200 text-xs font-bold px-2.5 py-1 rounded-full">REJECTED</span>;
      case "COMPLETED":
        return <span className="bg-purple-100 text-purple-800 border border-purple-200 text-xs font-bold px-2.5 py-1 rounded-full">COMPLETED</span>;
      default:
        return <span className="bg-amber-100 text-amber-800 border border-amber-200 text-xs font-bold px-2.5 py-1 rounded-full">PENDING</span>;
    }
  };

  return (
    <DashboardLayout>
      <div className="max-w-7xl mx-auto space-y-6">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h1 className="text-3xl font-extrabold text-slate-900">Mentorship Program</h1>
            <p className="text-sm text-slate-500 mt-1">
              Connect with industry experts for career mentorship, guidance, and interview prep
            </p>
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

        {/* Tab Navigation */}
        <div className="flex border-b border-slate-200 gap-4">
          <button
            onClick={() => setActiveTab("mentors")}
            className={`pb-3 text-sm font-semibold transition border-b-2 ${
              activeTab === "mentors"
                ? "border-purple-600 text-purple-700 font-bold"
                : "border-transparent text-slate-500 hover:text-slate-800"
            }`}
          >
            🎓 Available Mentors
          </button>

          <button
            onClick={() => setActiveTab("sent")}
            className={`pb-3 text-sm font-semibold transition border-b-2 ${
              activeTab === "sent"
                ? "border-purple-600 text-purple-700 font-bold"
                : "border-transparent text-slate-500 hover:text-slate-800"
            }`}
          >
            📤 Sent Requests
          </button>

          {(role === "alumni" || role === "admin") && (
            <button
              onClick={() => setActiveTab("received")}
              className={`pb-3 text-sm font-semibold transition border-b-2 ${
                activeTab === "received"
                  ? "border-purple-600 text-purple-700 font-bold"
                  : "border-transparent text-slate-500 hover:text-slate-800"
              }`}
            >
              📥 Received Requests
            </button>
          )}
        </div>

        {/* Tab 1: Available Mentors */}
        {activeTab === "mentors" && (
          <div className="space-y-8">
            {/* Top Recommended Mentors Section */}
            {recommendations.length > 0 && (
              <div className="bg-gradient-to-r from-purple-900 to-indigo-900 rounded-3xl p-6 text-white shadow-xl space-y-4">
                <div className="flex items-center justify-between">
                  <div>
                    <h2 className="text-xl font-bold flex items-center gap-2">
                      <span>✨</span> Recommended Mentors For You
                    </h2>
                    <p className="text-xs text-purple-200 mt-0.5">
                      Matched based on your department, career interests, and overlapping technical skills
                    </p>
                  </div>
                  <span className="text-xs bg-purple-500/30 border border-purple-400/30 px-3 py-1 rounded-full text-purple-200 font-semibold">
                    Smart Match
                  </span>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                  {recommendations.map((rec) => (
                    <div
                      key={rec.id}
                      className="bg-white/10 backdrop-blur-md border border-white/15 rounded-2xl p-5 hover:bg-white/15 transition flex flex-col justify-between"
                    >
                      <div>
                        <div className="flex items-start justify-between gap-2 mb-3">
                          <div>
                            <div className="flex items-center gap-1.5">
                              <h3 className="font-bold text-white text-base leading-tight">{rec.name}</h3>
                              {rec.is_verified && (
                                <span className="text-blue-300 text-xs font-bold" title="Verified Alumni">
                                  ✓
                                </span>
                              )}
                            </div>
                            <p className="text-xs text-purple-200 font-medium">{rec.job_role || "Mentor"}</p>
                          </div>
                          <span className="bg-emerald-500 text-slate-950 font-extrabold text-xs px-2.5 py-1 rounded-full shadow-md">
                            {rec.match_score}% Match
                          </span>
                        </div>

                        <div className="text-xs text-purple-100 space-y-1 mb-3">
                          {rec.company && <p>🏢 {rec.company}</p>}
                          {rec.department && <p>🎓 {rec.department}</p>}
                        </div>

                        {/* Match Reasons Tags */}
                        {rec.match_reasons && rec.match_reasons.length > 0 && (
                          <div className="space-y-1 my-2">
                            <span className="text-[10px] font-semibold text-purple-300 uppercase tracking-wider">
                              Why you match:
                            </span>
                            <div className="flex flex-wrap gap-1">
                              {rec.match_reasons.map((reason, i) => (
                                <span
                                  key={i}
                                  className="text-[10px] bg-white/20 text-white px-2 py-0.5 rounded-md"
                                >
                                  {reason}
                                </span>
                              ))}
                            </div>
                          </div>
                        )}
                      </div>

                      <button
                        onClick={() => setSelectedMentor(rec)}
                        className="mt-4 w-full bg-white text-purple-900 hover:bg-purple-50 font-bold py-2 rounded-xl text-xs transition shadow"
                      >
                        Request Mentorship →
                      </button>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* General Mentors Listing */}
            <div>
              <h3 className="text-lg font-bold text-slate-800 mb-4">All Available Mentors</h3>
              {loading ? (
                <div className="text-center py-16 text-slate-500">
                  <div className="inline-block animate-spin rounded-full h-8 w-8 border-4 border-purple-600 border-t-transparent mb-2"></div>
                  <p className="text-sm">Finding mentors...</p>
                </div>
              ) : mentors.length === 0 ? (
                <div className="bg-white p-12 rounded-2xl border border-slate-200 text-center shadow-sm">
                  <div className="text-4xl mb-3">🎓</div>
                  <h3 className="text-lg font-bold text-slate-800">No Mentors Available Yet</h3>
                  <p className="text-xs text-slate-500 mt-1">
                    Alumni can enable mentorship availability in their profile settings.
                  </p>
                </div>
              ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                  {mentors.map((mentor) => (
                    <div
                      key={mentor.id}
                      className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200 hover:shadow-md transition flex flex-col justify-between"
                    >
                      <div>
                        <div className="flex items-center gap-3 mb-4">
                          <div className="w-12 h-12 rounded-xl bg-purple-100 text-purple-700 flex items-center justify-center font-bold text-lg">
                            {mentor.name ? mentor.name.charAt(0).toUpperCase() : "M"}
                          </div>
                          <div>
                            <div className="flex items-center gap-1.5">
                              <h3 className="text-base font-bold text-slate-900">{mentor.name}</h3>
                              {mentor.is_verified && (
                                <span className="text-blue-600 text-xs font-bold" title="Verified Alumni">
                                  ✓
                                </span>
                              )}
                            </div>
                            <p className="text-xs text-purple-700 font-semibold">{mentor.job_role || "Alumni Mentor"}</p>
                          </div>
                        </div>

                        <div className="space-y-1 text-xs text-slate-600 mb-4 bg-slate-50 p-3 rounded-xl border border-slate-100">
                          {mentor.company && <p>🏢 <strong>{mentor.company}</strong></p>}
                          {mentor.department && <p>🎓 {mentor.department}</p>}
                          {mentor.location && <p>📍 {mentor.location}</p>}
                        </div>

                        {mentor.skills && (
                          <div className="flex flex-wrap gap-1 mb-4">
                            {mentor.skills.split(",").slice(0, 3).map((s, idx) => (
                              <span key={idx} className="bg-purple-50 text-purple-800 text-[10px] px-2 py-0.5 rounded-md font-medium">
                                {s.trim()}
                              </span>
                            ))}
                          </div>
                        )}
                      </div>

                      <div className="pt-4 border-t border-slate-100">
                        <button
                          onClick={() => setSelectedMentor(mentor)}
                          className="w-full bg-purple-700 hover:bg-purple-800 text-white py-2.5 rounded-xl text-xs font-semibold transition shadow-sm"
                        >
                          Request Mentorship
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}

        {/* Tab 2: Sent Requests */}
        {activeTab === "sent" && (
          <div className="space-y-4">
            {loading ? (
              <div className="text-center py-16 text-slate-500">
                <div className="inline-block animate-spin rounded-full h-8 w-8 border-4 border-purple-600 border-t-transparent mb-2"></div>
                <p className="text-sm">Loading your sent requests...</p>
              </div>
            ) : sentRequests.length === 0 ? (
              <div className="bg-white p-12 rounded-2xl border border-slate-200 text-center shadow-sm">
                <div className="text-4xl mb-3">📤</div>
                <h3 className="text-lg font-bold text-slate-800">No Mentorship Requests Sent</h3>
                <p className="text-xs text-slate-500 mt-1">Browse available mentors to request career guidance.</p>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {sentRequests.map((req) => (
                  <div key={req.id} className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm space-y-3">
                    <div className="flex justify-between items-start">
                      <div>
                        <h4 className="font-bold text-slate-900 text-base">Mentor: {req.mentor_name}</h4>
                        <p className="text-xs text-slate-500">{req.mentor_email}</p>
                      </div>
                      {getStatusBadge(req.status)}
                    </div>

                    <div className="bg-slate-50 p-3 rounded-xl border border-slate-100 text-xs text-slate-700">
                      <p className="font-semibold text-slate-500 text-[10px] uppercase mb-1">Your Message:</p>
                      <p>{req.message}</p>
                    </div>

                    {req.response_note && (
                      <div className="bg-emerald-50 p-3 rounded-xl border border-emerald-200 text-xs text-emerald-900">
                        <p className="font-semibold text-emerald-700 text-[10px] uppercase mb-1">Mentor's Note:</p>
                        <p>{req.response_note}</p>
                      </div>
                    )}

                    {req.status === "ACCEPTED" && (
                      <div className="pt-2 flex justify-end">
                        <button
                          onClick={() => handleComplete(req.id)}
                          className="text-xs bg-slate-100 hover:bg-slate-200 text-slate-700 px-3 py-1.5 rounded-lg font-semibold"
                        >
                          Mark Completed
                        </button>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* Tab 3: Received Requests */}
        {activeTab === "received" && (
          <div className="space-y-4">
            {loading ? (
              <div className="text-center py-16 text-slate-500">
                <div className="inline-block animate-spin rounded-full h-8 w-8 border-4 border-purple-600 border-t-transparent mb-2"></div>
                <p className="text-sm">Loading incoming requests...</p>
              </div>
            ) : receivedRequests.length === 0 ? (
              <div className="bg-white p-12 rounded-2xl border border-slate-200 text-center shadow-sm">
                <div className="text-4xl mb-3">📥</div>
                <h3 className="text-lg font-bold text-slate-800">No Incoming Requests</h3>
                <p className="text-xs text-slate-500 mt-1">When students reach out for mentorship, requests will appear here.</p>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {receivedRequests.map((req) => (
                  <div key={req.id} className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm space-y-3">
                    <div className="flex justify-between items-start">
                      <div>
                        <h4 className="font-bold text-slate-900 text-base">Student: {req.student_name}</h4>
                        <p className="text-xs text-slate-500">{req.student_email}</p>
                      </div>
                      {getStatusBadge(req.status)}
                    </div>

                    <div className="bg-slate-50 p-3 rounded-xl border border-slate-100 text-xs text-slate-700">
                      <p className="font-semibold text-slate-500 text-[10px] uppercase mb-1">Student's Note:</p>
                      <p>{req.message}</p>
                    </div>

                    {req.status === "PENDING" && (
                      <div className="flex gap-2 pt-2">
                        <button
                          onClick={() => setActionModal({ id: req.id, action: "accept" })}
                          className="flex-1 bg-emerald-600 hover:bg-emerald-700 text-white py-2 rounded-xl text-xs font-semibold shadow-sm"
                        >
                          Accept Request
                        </button>
                        <button
                          onClick={() => setActionModal({ id: req.id, action: "reject" })}
                          className="flex-1 bg-slate-100 hover:bg-red-50 text-slate-700 hover:text-red-600 py-2 rounded-xl text-xs font-semibold"
                        >
                          Decline
                        </button>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* Send Mentorship Request Modal */}
        {selectedMentor && (
          <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4 backdrop-blur-sm">
            <div className="bg-white rounded-3xl p-8 max-w-lg w-full shadow-2xl relative">
              <button
                onClick={() => setSelectedMentor(null)}
                className="absolute right-5 top-5 text-slate-400 hover:text-slate-700 text-xl font-bold"
              >
                ✕
              </button>

              <h3 className="text-xl font-bold text-slate-900 mb-1">Request Mentorship</h3>
              <p className="text-xs text-slate-500 mb-6">Connecting with: <strong>{selectedMentor.name}</strong></p>

              <form onSubmit={handleSendRequest} className="space-y-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-600 mb-1">Introduce yourself & your goals *</label>
                  <textarea
                    rows="4"
                    required
                    value={requestMessage}
                    onChange={(e) => setRequestMessage(e.target.value)}
                    placeholder="Hi, I am interested in cloud engineering and would love guidance on preparing for technical interviews..."
                    className="w-full border border-slate-300 p-3 rounded-xl text-sm focus:ring-2 focus:ring-purple-500 focus:outline-none"
                  ></textarea>
                </div>

                <div className="flex justify-end gap-3 pt-2">
                  <button
                    type="button"
                    onClick={() => setSelectedMentor(null)}
                    className="px-5 py-2.5 rounded-xl text-slate-600 hover:bg-slate-100 text-sm font-semibold"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={sendingRequest}
                    className="px-6 py-2.5 rounded-xl bg-purple-700 hover:bg-purple-800 text-white text-sm font-semibold shadow-md disabled:bg-purple-400"
                  >
                    {sendingRequest ? "Sending..." : "Send Request"}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

        {/* Action Note Modal (Accept / Reject) */}
        {actionModal && (
          <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4 backdrop-blur-sm">
            <div className="bg-white rounded-3xl p-8 max-w-md w-full shadow-2xl relative">
              <button
                onClick={() => setActionModal(null)}
                className="absolute right-5 top-5 text-slate-400 hover:text-slate-700 text-xl font-bold"
              >
                ✕
              </button>

              <h3 className="text-xl font-bold text-slate-900 mb-1 capitalize">
                {actionModal.action} Mentorship Request
              </h3>
              <p className="text-xs text-slate-500 mb-4">Add an optional message or scheduling link for the student.</p>

              <textarea
                rows="3"
                value={actionNote}
                onChange={(e) => setActionNote(e.target.value)}
                placeholder="e.g. Let's connect over Zoom this Friday at 4 PM..."
                className="w-full border border-slate-300 p-3 rounded-xl text-sm focus:ring-2 focus:ring-purple-500 focus:outline-none mb-4"
              ></textarea>

              <div className="flex justify-end gap-3">
                <button
                  type="button"
                  onClick={() => setActionModal(null)}
                  className="px-4 py-2 rounded-xl text-slate-600 text-xs font-semibold"
                >
                  Cancel
                </button>
                <button
                  type="button"
                  onClick={actionModal.action === "accept" ? handleAccept : handleReject}
                  className={`px-5 py-2 rounded-xl text-white text-xs font-semibold shadow-md ${
                    actionModal.action === "accept" ? "bg-emerald-600 hover:bg-emerald-700" : "bg-red-600 hover:bg-red-700"
                  }`}
                >
                  Confirm {actionModal.action}
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </DashboardLayout>
  );
};

export default Mentorship;