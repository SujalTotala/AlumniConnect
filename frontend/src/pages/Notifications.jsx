import { useEffect, useState } from "react";
import DashboardLayout from "../layouts/DashboardLayout";
import { notificationApi } from "../api/notificationApi";

const Notifications = () => {
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState("all"); // 'all' | 'unread'
  const [errorMsg, setErrorMsg] = useState("");
  const [successMsg, setSuccessMsg] = useState("");

  const fetchNotifications = async () => {
    setLoading(true);
    try {
      const res = await notificationApi.getNotifications();
      setNotifications(res.data);
    } catch (err) {
      console.error("Failed to load notifications:", err);
      setErrorMsg("Failed to load notifications.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchNotifications();
  }, []);

  const handleMarkRead = async (id) => {
    try {
      await notificationApi.markRead(id);
      setNotifications((prev) =>
        prev.map((n) => (n.id === id ? { ...n, is_read: true } : n))
      );
    } catch (err) {
      console.error("Failed to mark as read:", err);
    }
  };

  const handleMarkAllRead = async () => {
    try {
      await notificationApi.markAllRead();
      setNotifications((prev) => prev.map((n) => ({ ...n, is_read: true })));
      setSuccessMsg("All notifications marked as read.");
    } catch (err) {
      console.error("Failed to mark all as read:", err);
    }
  };

  const filteredNotifs = notifications.filter((n) => {
    if (filter === "unread") return !n.is_read;
    return true;
  });

  const getIcon = (type) => {
    switch (type?.toUpperCase()) {
      case "MENTORSHIP":
        return "🎓";
      case "EVENT":
        return "📅";
      case "OPPORTUNITY":
        return "💼";
      default:
        return "🔔";
    }
  };

  return (
    <DashboardLayout>
      <div className="max-w-4xl mx-auto space-y-6">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h1 className="text-3xl font-extrabold text-slate-900">Notifications</h1>
            <p className="text-sm text-slate-500 mt-1">
              Stay updated on mentorship requests, event confirmations, and new announcements
            </p>
          </div>

          <button
            onClick={handleMarkAllRead}
            className="bg-slate-100 hover:bg-slate-200 text-slate-700 px-4 py-2 rounded-xl text-xs font-semibold transition self-start sm:self-auto"
          >
            Mark All as Read
          </button>
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

        {/* Filter Bar */}
        <div className="flex gap-2 border-b border-slate-200 pb-2">
          <button
            onClick={() => setFilter("all")}
            className={`px-4 py-1.5 rounded-xl text-xs font-semibold transition ${
              filter === "all"
                ? "bg-blue-600 text-white shadow-sm"
                : "bg-slate-100 text-slate-600 hover:bg-slate-200"
            }`}
          >
            All ({notifications.length})
          </button>
          <button
            onClick={() => setFilter("unread")}
            className={`px-4 py-1.5 rounded-xl text-xs font-semibold transition ${
              filter === "unread"
                ? "bg-blue-600 text-white shadow-sm"
                : "bg-slate-100 text-slate-600 hover:bg-slate-200"
            }`}
          >
            Unread ({notifications.filter((n) => !n.is_read).length})
          </button>
        </div>

        {/* Notifications List */}
        {loading ? (
          <div className="text-center py-16 text-slate-500">
            <div className="inline-block animate-spin rounded-full h-8 w-8 border-4 border-blue-600 border-t-transparent mb-2"></div>
            <p className="text-sm">Loading notifications...</p>
          </div>
        ) : filteredNotifs.length === 0 ? (
          <div className="bg-white p-12 rounded-2xl border border-slate-200 text-center shadow-sm">
            <div className="text-4xl mb-3">📭</div>
            <h3 className="text-lg font-bold text-slate-800">No Notifications</h3>
            <p className="text-xs text-slate-500 mt-1">You're all caught up with your updates.</p>
          </div>
        ) : (
          <div className="space-y-3">
            {filteredNotifs.map((n) => (
              <div
                key={n.id}
                className={`p-5 rounded-2xl border transition flex items-start justify-between gap-4 ${
                  n.is_read
                    ? "bg-white border-slate-200 opacity-80"
                    : "bg-blue-50/60 border-blue-200 shadow-sm"
                }`}
              >
                <div className="flex items-start gap-4">
                  <div className="w-10 h-10 rounded-xl bg-white shadow-sm border border-slate-100 flex items-center justify-center text-xl shrink-0">
                    {getIcon(n.notification_type)}
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <h4 className="text-sm font-bold text-slate-900">{n.title}</h4>
                      {!n.is_read && (
                        <span className="w-2 h-2 rounded-full bg-blue-600"></span>
                      )}
                    </div>
                    <p className="text-xs text-slate-600 mt-1 leading-relaxed">{n.message}</p>
                    <span className="text-[10px] text-slate-400 mt-2 block font-medium">
                      {new Date(n.created_at).toLocaleString()}
                    </span>
                  </div>
                </div>

                {!n.is_read && (
                  <button
                    onClick={() => handleMarkRead(n.id)}
                    className="text-xs text-blue-700 hover:text-blue-900 font-semibold shrink-0"
                  >
                    Mark read
                  </button>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </DashboardLayout>
  );
};

export default Notifications;
