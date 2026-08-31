import { Navigate } from "react-router-dom";

function ProtectedRoute({ children, allowedRoles }) {
  const token = localStorage.getItem("token");
  const userStr = localStorage.getItem("user");

  if (!token || !userStr) {
    return <Navigate to="/" replace />;
  }

  let user;
  try {
    user = JSON.parse(userStr);
  } catch {
    return <Navigate to="/" replace />;
  }

  if (!user) {
    return <Navigate to="/" replace />;
  }

  if (allowedRoles && allowedRoles.length > 0) {
    const role = (user.role || "").toLowerCase();
    const normalizedAllowed = allowedRoles.map((r) => r.toLowerCase());
    if (!normalizedAllowed.includes(role)) {
      return <Navigate to="/dashboard" replace />;
    }
  }

  return children;
}

export default ProtectedRoute;