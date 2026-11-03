import { Routes, Route } from "react-router-dom";

import Login from "./pages/Login";
import Signup from "./pages/Signup";
import Dashboard from "./pages/Dashboard";
import Profile from "./pages/Profile";
import Alumni from "./pages/Alumni";
import Events from "./pages/Events";
import Mentorship from "./pages/Mentorship";
import Opportunities from "./pages/Opportunities";
import Notifications from "./pages/Notifications";
import SavedItems from "./pages/SavedItems";
import AdminDashboard from "./pages/AdminDashboard";
import MyNetwork from "./pages/MyNetwork";
import SuccessStories from "./pages/SuccessStories";
import Communities from "./pages/Communities";
import CommunityDetails from "./pages/CommunityDetails";
import AdminAlumniData from "./pages/AdminAlumniData";

import ProtectedRoute from "./components/ProtectedRoute";

function App() {
  return (
    <Routes>
      {/* Public Authentication Routes */}
      <Route path="/" element={<Login />} />
      <Route path="/signup" element={<Signup />} />

      {/* Authenticated Member Routes */}
      <Route
        path="/dashboard"
        element={
          <ProtectedRoute>
            <Dashboard />
          </ProtectedRoute>
        }
      />

      <Route
        path="/profile"
        element={
          <ProtectedRoute>
            <Profile />
          </ProtectedRoute>
        }
      />

      <Route
        path="/alumni"
        element={
          <ProtectedRoute>
            <Alumni />
          </ProtectedRoute>
        }
      />

      <Route
        path="/events"
        element={
          <ProtectedRoute>
            <Events />
          </ProtectedRoute>
        }
      />

      <Route
        path="/mentorship"
        element={
          <ProtectedRoute>
            <Mentorship />
          </ProtectedRoute>
        }
      />

      <Route
        path="/opportunities"
        element={
          <ProtectedRoute>
            <Opportunities />
          </ProtectedRoute>
        }
      />

      <Route
        path="/notifications"
        element={
          <ProtectedRoute>
            <Notifications />
          </ProtectedRoute>
        }
      />

      <Route
        path="/saved"
        element={
          <ProtectedRoute>
            <SavedItems />
          </ProtectedRoute>
        }
      />

      <Route
        path="/network"
        element={
          <ProtectedRoute>
            <MyNetwork />
          </ProtectedRoute>
        }
      />

      <Route
        path="/my-network"
        element={
          <ProtectedRoute>
            <MyNetwork />
          </ProtectedRoute>
        }
      />

      <Route
        path="/stories"
        element={
          <ProtectedRoute>
            <SuccessStories />
          </ProtectedRoute>
        }
      />

      <Route
        path="/communities"
        element={
          <ProtectedRoute>
            <Communities />
          </ProtectedRoute>
        }
      />

      <Route
        path="/communities/:id"
        element={
          <ProtectedRoute>
            <CommunityDetails />
          </ProtectedRoute>
        }
      />

      {/* Protected Admin Portal */}
      <Route
        path="/admin"
        element={
          <ProtectedRoute allowedRoles={["admin"]}>
            <AdminDashboard />
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/alumni-data"
        element={
          <ProtectedRoute allowedRoles={["admin"]}>
            <AdminAlumniData />
          </ProtectedRoute>
        }
      />
      <Route
        path="/admin/alumni"
        element={
          <ProtectedRoute allowedRoles={["admin"]}>
            <AdminAlumniData />
          </ProtectedRoute>
        }
      />
    </Routes>
  );
}

export default App;