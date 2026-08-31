import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { notificationApi } from "../api/notificationApi";

function Navbar() {
  const navigate = useNavigate();
  const [unreadCount, setUnreadCount] = useState(0);

  let user = null;
  try {
    const userStr = localStorage.getItem("user");
    if (userStr) user = JSON.parse(userStr);
  } catch (e) {
    console.error("Error parsing user from localStorage", e);
  }

  const fetchUnreadCount = async () => {
    try {
      const res = await notificationApi.getUnreadCount();
      setUnreadCount(res.data.unread_count || 0);
    } catch {
      // User might be logged out or network error
    }
  };

  useEffect(() => {
    fetchUnreadCount();
    const interval = setInterval(fetchUnreadCount, 30000); // 30s auto-refresh
    return () => clearInterval(interval);
  }, []);

  const handleLogout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("user");
    navigate("/");
  };

  const getRoleBadgeColor = (role) => {
    switch (role?.toLowerCase()) {
      case "admin":
        return "bg-purple-100 text-purple-800 border-purple-200";
      case "alumni":
        return "bg-emerald-100 text-emerald-800 border-emerald-200";
      default:
        return "bg-blue-100 text-blue-800 border-blue-200";
    }
  };

  return (
    <header className="bg-white shadow-sm border-b border-slate-200 px-8 py-4 flex justify-between items-center sticky top-0 z-10">
      <div className="flex items-center space-x-3">
        <h2 className="text-xl font-bold text-slate-800">
          Welcome, <span className="text-blue-700">{user?.name || "Member"}</span>
        </h2>
        {user?.role && (
          <span
            className={`text-xs font-semibold px-2.5 py-0.5 rounded-full border capitalize ${getRoleBadgeColor(
              user.role
            )}`}
          >
            {user.role}
          </span>
        )}
      </div>

      <div className="flex items-center space-x-4">
        {/* Notifications Icon Button with Live Unread Badge */}
        <Link
          to="/notifications"
          className="relative p-2.5 text-slate-600 hover:text-blue-700 hover:bg-slate-100 rounded-xl transition flex items-center justify-center"
          title="Notifications"
        >
          <span className="text-xl">🔔</span>
          {unreadCount > 0 && (
            <span className="absolute -top-0.5 -right-0.5 bg-red-600 text-white text-[10px] font-bold px-1.5 py-0.5 rounded-full border-2 border-white animate-pulse">
              {unreadCount > 99 ? "99+" : unreadCount}
            </span>
          )}
        </Link>

        {/* Profile Link */}
        <Link
          to="/profile"
          className="flex items-center gap-2 p-1.5 px-3 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 text-sm font-medium transition"
        >
          <span className="w-7 h-7 rounded-lg bg-blue-600 text-white flex items-center justify-center text-xs font-bold">
            {user?.name ? user.name.charAt(0).toUpperCase() : "U"}
          </span>
          <span className="hidden md:inline-block">{user?.name}</span>
        </Link>

        {/* Logout */}
        <button
          onClick={handleLogout}
          className="bg-red-50 text-red-600 hover:bg-red-600 hover:text-white px-3.5 py-2 rounded-xl text-xs font-semibold transition border border-red-200"
        >
          Logout
        </button>
      </div>
    </header>
  );
}

export default Navbar;