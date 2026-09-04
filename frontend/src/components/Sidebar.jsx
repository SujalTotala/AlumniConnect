import { NavLink, useNavigate } from "react-router-dom";

const Sidebar = ({ isOpen = false, onClose = () => {} }) => {
  const navigate = useNavigate();

  let user = null;
  try {
    const userStr = localStorage.getItem("user");
    if (userStr) user = JSON.parse(userStr);
  } catch (e) {
    console.error(e);
  }

  const logout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("user");
    onClose();
    navigate("/");
  };

  const navItems = [
    { path: "/dashboard", label: "Dashboard", icon: "📊" },
    { path: "/alumni", label: "Alumni Directory", icon: "👥" },
    { path: "/events", label: "Events", icon: "📅" },
    { path: "/mentorship", label: "Mentorship", icon: "🎓" },
    { path: "/opportunities", label: "Opportunities", icon: "💼" },
    { path: "/notifications", label: "Notifications", icon: "🔔" },
    { path: "/profile", label: "My Profile", icon: "👤" },
  ];

  if (user?.role?.toLowerCase() === "admin") {
    navItems.push({ path: "/admin", label: "Admin Portal", icon: "⚙️" });
  }

  return (
    <aside
      className={`w-64 bg-slate-900 text-white min-h-screen fixed left-0 top-0 flex flex-col justify-between shadow-xl z-30 overflow-y-auto transition-transform duration-300 ease-in-out ${
        isOpen ? "translate-x-0" : "-translate-x-full md:translate-x-0"
      }`}
    >
      <div>
        {/* Brand Header */}
        <div className="p-6 border-b border-slate-800 flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 rounded-xl bg-blue-600 flex items-center justify-center font-bold text-xl shadow-lg">
              AC
            </div>
            <div>
              <h1 className="text-xl font-bold text-white tracking-wide">AlumniConnect</h1>
              <p className="text-xs text-slate-400">Portal & Network</p>
            </div>
          </div>
          {/* Mobile close button */}
          <button
            onClick={onClose}
            className="md:hidden text-slate-400 hover:text-white p-1 rounded-lg transition"
            aria-label="Close menu"
          >
            <span className="text-xl leading-none">✕</span>
          </button>
        </div>

        {/* Navigation Links */}
        <nav className="flex flex-col p-4 gap-1.5 mt-2">
          {navItems.map((item) => (
            <NavLink
              key={item.path}
              to={item.path}
              onClick={onClose}
              className={({ isActive }) =>
                `flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-medium transition ${
                  isActive
                    ? "bg-blue-600 text-white shadow-md font-semibold"
                    : "text-slate-300 hover:bg-slate-800 hover:text-white"
                }`
              }
            >
              <span className="text-lg">{item.icon}</span>
              <span>{item.label}</span>
            </NavLink>
          ))}
        </nav>
      </div>

      {/* User Info & Logout button at bottom */}
      <div className="p-4 border-t border-slate-800 space-y-3">
        <div className="flex items-center gap-3 px-2">
          <div className="w-8 h-8 rounded-lg bg-slate-800 flex items-center justify-center text-xs font-bold text-blue-400">
            {user?.name ? user.name.charAt(0).toUpperCase() : "U"}
          </div>
          <div className="truncate">
            <p className="text-xs font-semibold text-white truncate">{user?.name || "Member"}</p>
            <p className="text-[10px] text-slate-400 capitalize">{user?.role || "user"}</p>
          </div>
        </div>

        <button
          onClick={logout}
          className="w-full flex items-center justify-center gap-2 bg-red-500/10 hover:bg-red-600 text-red-400 hover:text-white p-2.5 rounded-xl font-medium text-xs transition border border-red-500/20"
        >
          <span>🚪</span>
          <span>Logout</span>
        </button>
      </div>
    </aside>
  );
};

export default Sidebar;