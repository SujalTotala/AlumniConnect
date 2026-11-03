import { useEffect, useState } from "react";
import DashboardLayout from "../layouts/DashboardLayout";
import { bookmarkApi } from "../api/bookmarkApi";

const SavedItems = () => {
  const [bookmarks, setBookmarks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState("all"); // 'all', 'alumni', 'opportunity', 'event'
  const [errorMsg, setErrorMsg] = useState("");
  const [successMsg, setSuccessMsg] = useState("");

  const fetchBookmarks = async () => {
    setLoading(true);
    setErrorMsg("");
    try {
      const typeParam = activeTab === "all" ? undefined : activeTab;
      const res = await bookmarkApi.getBookmarks(typeParam);
      setBookmarks(res.data || []);
    } catch (err) {
      console.error("Failed to load saved items:", err);
      setErrorMsg("Failed to load saved items. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBookmarks();
  }, [activeTab]);

  const handleRemoveBookmark = async (bookmarkId) => {
    try {
      await bookmarkApi.deleteBookmark(bookmarkId);
      setBookmarks((prev) => prev.filter((b) => b.id !== bookmarkId));
      setSuccessMsg("Item removed from saved list.");
      setTimeout(() => setSuccessMsg(""), 3000);
    } catch (err) {
      console.error("Failed to remove bookmark:", err);
      setErrorMsg("Failed to remove saved item.");
    }
  };

  const filteredBookmarks = bookmarks.filter((b) => {
    if (activeTab === "all") return true;
    return b.item_type === activeTab;
  });

  return (
    <DashboardLayout>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-slate-800 tracking-tight flex items-center gap-2">
              <span>🔖</span> Saved & Bookmarked Items
            </h1>
            <p className="text-sm text-slate-500 mt-1">
              Quick access to your saved alumni profiles, career opportunities, and upcoming events.
            </p>
          </div>
        </div>

        {/* Feedback Alerts */}
        {errorMsg && (
          <div className="p-4 bg-red-50 border border-red-200 text-red-700 text-sm rounded-xl flex items-center justify-between">
            <span>{errorMsg}</span>
            <button onClick={() => setErrorMsg("")} className="font-bold">✕</button>
          </div>
        )}
        {successMsg && (
          <div className="p-4 bg-emerald-50 border border-emerald-200 text-emerald-700 text-sm rounded-xl flex items-center justify-between">
            <span>{successMsg}</span>
            <button onClick={() => setSuccessMsg("")} className="font-bold">✕</button>
          </div>
        )}

        {/* Tab Selection */}
        <div className="flex gap-2 border-b border-slate-200 pb-2">
          {[
            { id: "all", label: "All Items", icon: "📑" },
            { id: "alumni", label: "Alumni", icon: "👥" },
            { id: "opportunity", label: "Opportunities", icon: "💼" },
            { id: "event", label: "Events", icon: "📅" },
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`px-4 py-2 rounded-lg text-sm font-medium transition flex items-center gap-2 ${
                activeTab === tab.id
                  ? "bg-blue-600 text-white shadow-sm"
                  : "bg-slate-100 text-slate-600 hover:bg-slate-200"
              }`}
            >
              <span>{tab.icon}</span>
              <span>{tab.label}</span>
              {activeTab === tab.id && (
                <span className="ml-1 px-1.5 py-0.5 text-xs bg-blue-500 text-white rounded-full">
                  {filteredBookmarks.length}
                </span>
              )}
            </button>
          ))}
        </div>

        {/* Content Body */}
        {loading ? (
          <div className="py-20 flex flex-col items-center justify-center text-slate-400 gap-3">
            <div className="w-8 h-8 border-3 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
            <p className="text-sm">Loading saved items...</p>
          </div>
        ) : filteredBookmarks.length === 0 ? (
          <div className="text-center py-16 bg-white border border-slate-200 rounded-2xl shadow-sm p-8">
            <div className="text-5xl mb-3">🔖</div>
            <h3 className="text-lg font-semibold text-slate-800">No saved items yet</h3>
            <p className="text-sm text-slate-500 mt-1 max-w-md mx-auto">
              Save alumni profiles, job opportunities, or events by clicking the bookmark icon to revisit them anytime.
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
            {filteredBookmarks.map((b) => {
              const details = b.item_details || {};
              return (
                <div
                  key={b.id}
                  className="bg-white border border-slate-200 rounded-2xl p-5 shadow-sm hover:shadow-md transition flex flex-col justify-between"
                >
                  <div className="space-y-3">
                    <div className="flex items-center justify-between">
                      <span
                        className={`text-xs font-semibold px-2.5 py-1 rounded-full uppercase tracking-wider ${
                          b.item_type === "alumni"
                            ? "bg-purple-100 text-purple-700"
                            : b.item_type === "opportunity"
                            ? "bg-emerald-100 text-emerald-700"
                            : "bg-blue-100 text-blue-700"
                        }`}
                      >
                        {b.item_type}
                      </span>
                      <button
                        onClick={() => handleRemoveBookmark(b.id)}
                        className="text-slate-400 hover:text-red-600 transition p-1 text-sm rounded"
                        title="Remove bookmark"
                        aria-label="Remove bookmark"
                      >
                        ✕
                      </button>
                    </div>

                    {/* Alumni Item Content */}
                    {b.item_type === "alumni" && (
                      <div>
                        <div className="flex items-center gap-2">
                          <h3 className="font-bold text-slate-900 text-base">{details.name || "Alumni Profile"}</h3>
                          {details.is_verified && (
                            <span className="text-blue-600 text-xs font-bold bg-blue-50 px-1.5 py-0.5 rounded-md flex items-center gap-0.5" title="Verified Alumni">
                              ✓
                            </span>
                          )}
                        </div>
                        <p className="text-xs text-blue-600 font-medium mt-0.5">
                          {details.job_role || "Professional"} {details.company ? `@ ${details.company}` : ""}
                        </p>
                        <div className="mt-2 text-xs text-slate-500 space-y-1">
                          {details.department && <p>🎓 {details.department} ({details.graduation_year || "Alum"})</p>}
                          {details.location && <p>📍 {details.location}</p>}
                          {details.skills && (
                            <p className="line-clamp-1 text-slate-400">💡 {details.skills}</p>
                          )}
                        </div>
                      </div>
                    )}

                    {/* Opportunity Item Content */}
                    {b.item_type === "opportunity" && (
                      <div>
                        <h3 className="font-bold text-slate-900 text-base">{details.title || "Career Opportunity"}</h3>
                        <p className="text-xs text-slate-600 font-medium mt-0.5">
                          🏢 {details.company || "Company"} {details.location ? `• ${details.location}` : ""}
                        </p>
                        <span className="inline-block mt-2 px-2 py-0.5 text-[11px] font-semibold bg-slate-100 text-slate-700 rounded-md">
                          {details.opportunity_type || "Job"}
                        </span>
                        {details.deadline && (
                          <p className="text-xs text-amber-600 mt-2">
                            ⏰ Deadline: {new Date(details.deadline).toLocaleDateString()}
                          </p>
                        )}
                      </div>
                    )}

                    {/* Event Item Content */}
                    {b.item_type === "event" && (
                      <div>
                        <h3 className="font-bold text-slate-900 text-base">{details.title || "Campus Event"}</h3>
                        <p className="text-xs text-blue-600 font-medium mt-0.5">
                          📅 {details.event_date ? new Date(details.event_date).toLocaleDateString() : "Upcoming"} {details.start_time || ""}
                        </p>
                        <p className="text-xs text-slate-500 mt-1">
                          📍 {details.location || "Online"}
                        </p>
                        {details.description && (
                          <p className="text-xs text-slate-600 mt-2 line-clamp-2">{details.description}</p>
                        )}
                      </div>
                    )}
                  </div>

                  <div className="pt-4 mt-4 border-t border-slate-100 flex items-center justify-between text-xs text-slate-400">
                    <span>Saved on {new Date(b.created_at).toLocaleDateString()}</span>
                    <button
                      onClick={() => handleRemoveBookmark(b.id)}
                      className="text-red-500 hover:text-red-700 font-medium"
                    >
                      Unsave
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </DashboardLayout>
  );
};

export default SavedItems;
