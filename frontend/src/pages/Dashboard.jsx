import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import DashboardLayout from "../layouts/DashboardLayout";
import { alumniApi } from "../api/alumniApi";
import { eventApi } from "../api/eventApi";
import { opportunityApi } from "../api/opportunityApi";
import { adminApi } from "../api/adminApi";
import { announcementApi } from "../api/announcementApi";
import { profileApi } from "../api/profileApi";
import { activityApi } from "../api/activityApi";

const Dashboard = () => {
  const [stats, setStats] = useState({
    totalAlumni: 0,
    activeMentors: 0,
    totalEvents: 0,
    totalOpportunities: 0,
    totalUsers: 0,
  });
  const [recentEvents, setRecentEvents] = useState([]);
  const [announcements, setAnnouncements] = useState([]);
  const [profileCompletion, setProfileCompletion] = useState(null);
  const [recentActivities, setRecentActivities] = useState([]);
  const [loading, setLoading] = useState(true);

  let user = null;
  try {
    const userStr = localStorage.getItem("user");
    if (userStr) user = JSON.parse(userStr);
  } catch (e) {
    console.error(e);
  }

  const isAdmin = user?.role?.toLowerCase() === "admin";

  useEffect(() => {
    const loadDashboardData = async () => {
      setLoading(true);
      try {
        // Core stats loading
        if (isAdmin) {
          const adminStatsRes = await adminApi.getStatistics();
          const d = adminStatsRes.data;
          setStats({
            totalAlumni: d.total_alumni,
            activeMentors: d.active_mentors,
            totalEvents: d.total_events,
            totalOpportunities: d.total_opportunities,
            totalUsers: d.total_users,
            totalRegistrations: d.total_event_registrations,
            pendingMentorship: d.pending_mentorship_requests,
          });
        } else {
          const [alumniRes, eventsRes, oppsRes] = await Promise.all([
            alumniApi.getAlumni(),
            eventApi.getEvents(),
            opportunityApi.getOpportunities(),
          ]);

          const alumniList = Array.isArray(alumniRes.data) ? alumniRes.data : [];
          const eventsList = Array.isArray(eventsRes.data) ? eventsRes.data : [];
          const oppsList = Array.isArray(oppsRes.data) ? oppsRes.data : [];

          setStats({
            totalAlumni: alumniList.length,
            activeMentors: alumniList.filter((a) => a.mentorship_available).length,
            totalEvents: eventsList.length,
            totalOpportunities: oppsList.length,
            totalUsers: alumniList.length + 1,
          });

          setRecentEvents(eventsList.slice(0, 2));
        }

        // Additional engagement data: announcements, profile completion, activity feed
        try {
          const [annRes, compRes, actRes] = await Promise.all([
            announcementApi.getAnnouncements(true),
            profileApi.getCompletionSuggestions(),
            activityApi.getActivityFeed(5),
          ]);
          setAnnouncements(annRes.data || []);
          setProfileCompletion(compRes.data || null);
          setRecentActivities(actRes.data || []);
        } catch (subErr) {
          console.log("Secondary dashboard widgets load note:", subErr);
        }
      } catch (err) {
        console.error("Dashboard data load error:", err);
      } finally {
        setLoading(false);
      }
    };

    loadDashboardData();
  }, [isAdmin]);

  return (
    <DashboardLayout>
      <div className="max-w-7xl mx-auto space-y-8">
        {/* Priority Announcement Alert Banner */}
        {announcements.length > 0 && (
          <div className="bg-gradient-to-r from-amber-500 to-orange-600 rounded-2xl p-4 text-white shadow-md flex items-start justify-between gap-4">
            <div className="flex items-start gap-3">
              <span className="text-2xl">📢</span>
              <div>
                <div className="flex items-center gap-2">
                  <span className="bg-white/20 text-white font-bold text-[10px] px-2 py-0.5 rounded uppercase">
                    {announcements[0].priority} Priority
                  </span>
                  <h3 className="font-bold text-sm sm:text-base">{announcements[0].title}</h3>
                </div>
                <p className="text-xs text-amber-100 mt-1 leading-relaxed">{announcements[0].content}</p>
              </div>
            </div>
            {announcements.length > 1 && (
              <span className="text-xs bg-white/20 px-2 py-1 rounded text-white font-semibold whitespace-nowrap">
                +{announcements.length - 1} more
              </span>
            )}
          </div>
        )}

        {/* Welcome Header */}
        <div className="bg-gradient-to-r from-blue-700 via-indigo-700 to-slate-900 rounded-3xl p-8 text-white shadow-xl flex flex-col md:flex-row justify-between items-start md:items-center gap-6">
          <div>
            <div className="flex items-center gap-2 mb-2">
              <span className="bg-blue-500/30 text-blue-200 text-xs font-semibold px-3 py-1 rounded-full border border-blue-400/30 uppercase tracking-wider">
                {user?.role || "Member"} Portal
              </span>
            </div>
            <h1 className="text-3xl sm:text-4xl font-extrabold tracking-tight">
              Welcome, {user?.name || "Member"}! 👋
            </h1>
            <p className="text-blue-100/90 text-sm sm:text-base mt-2 max-w-xl">
              Connect with fellow alumni, attend upcoming networking events, access 1-on-1 mentorship, and discover career opportunities.
            </p>
          </div>

          <div className="flex flex-wrap gap-3">
            <Link
              to="/profile"
              className="bg-white/10 hover:bg-white/20 text-white border border-white/20 px-4 py-2.5 rounded-xl text-sm font-semibold transition backdrop-blur-sm"
            >
              👤 Edit Profile
            </Link>
            <Link
              to="/alumni"
              className="bg-blue-600 hover:bg-blue-500 text-white px-4 py-2.5 rounded-xl text-sm font-semibold transition shadow-lg"
            >
              👥 Browse Directory
            </Link>
          </div>
        </div>

        {/* Profile Completion Suggestions Banner */}
        {profileCompletion && profileCompletion.completion_percentage < 100 && (
          <div className="bg-white border border-blue-200 rounded-2xl p-5 shadow-sm flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
            <div className="flex items-start gap-3.5">
              <div className="w-10 h-10 rounded-xl bg-blue-100 text-blue-600 flex items-center justify-center font-bold text-lg">
                💡
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <h3 className="font-bold text-slate-900 text-sm">Complete Your Profile</h3>
                  <span className="bg-blue-100 text-blue-800 text-[11px] font-extrabold px-2 py-0.5 rounded-md">
                    {profileCompletion.completion_percentage}%
                  </span>
                </div>
                <p className="text-xs text-slate-500 mt-1">
                  Add details to receive better mentor recommendations:{" "}
                  <span className="font-medium text-slate-700">
                    {profileCompletion.missing_fields?.join(", ")}
                  </span>
                </p>
              </div>
            </div>

            <Link
              to="/profile"
              className="bg-blue-50 hover:bg-blue-100 text-blue-700 font-semibold px-4 py-2 rounded-xl text-xs transition border border-blue-200 whitespace-nowrap"
            >
              Update Profile →
            </Link>
          </div>
        )}

        {/* Live Metrics Grid */}
        <div>
          <div className="flex justify-between items-center mb-4">
            <h2 className="text-lg font-bold text-slate-800">Platform Overview</h2>
            <span className="text-xs font-medium text-emerald-600 flex items-center gap-1.5">
              <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
              Live Synced with Shared Backend
            </span>
          </div>

          <div className="grid grid-cols-2 md:grid-cols-4 gap-5">
            <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200 hover:shadow-md transition">
              <div className="flex items-center justify-between mb-3">
                <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">Alumni</span>
                <span className="p-2 bg-blue-50 text-blue-600 rounded-xl text-lg">👥</span>
              </div>
              <p className="text-3xl font-black text-slate-800">{loading ? "..." : stats.totalAlumni}</p>
              <p className="text-[11px] text-slate-500 mt-1">Verified Alumni</p>
            </div>

            <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200 hover:shadow-md transition">
              <div className="flex items-center justify-between mb-3">
                <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">Active Mentors</span>
                <span className="p-2 bg-purple-50 text-purple-600 rounded-xl text-lg">🎓</span>
              </div>
              <p className="text-3xl font-black text-slate-800">{loading ? "..." : stats.activeMentors}</p>
              <p className="text-[11px] text-purple-600 mt-1 font-medium">Available for Guidance</p>
            </div>

            <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200 hover:shadow-md transition">
              <div className="flex items-center justify-between mb-3">
                <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">Events</span>
                <span className="p-2 bg-emerald-50 text-emerald-600 rounded-xl text-lg">📅</span>
              </div>
              <p className="text-3xl font-black text-slate-800">{loading ? "..." : stats.totalEvents}</p>
              <p className="text-[11px] text-slate-500 mt-1">Upcoming Meets & Webinars</p>
            </div>

            <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200 hover:shadow-md transition">
              <div className="flex items-center justify-between mb-3">
                <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">Opportunities</span>
                <span className="p-2 bg-amber-50 text-amber-600 rounded-xl text-lg">💼</span>
              </div>
              <p className="text-3xl font-black text-slate-800">{loading ? "..." : stats.totalOpportunities}</p>
              <p className="text-[11px] text-amber-600 mt-1 font-medium">Jobs & Internships</p>
            </div>
          </div>
        </div>

        {/* Feature Hub Grid */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {/* Alumni Directory Tile */}
          <Link
            to="/alumni"
            className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm hover:shadow-lg hover:border-blue-300 transition group flex flex-col justify-between"
          >
            <div>
              <div className="w-12 h-12 rounded-2xl bg-blue-100 text-blue-700 flex items-center justify-center text-2xl mb-4 group-hover:scale-110 transition transform">
                👥
              </div>
              <h3 className="text-lg font-bold text-slate-900 group-hover:text-blue-700 transition">
                Alumni Directory
              </h3>
              <p className="text-xs text-slate-600 mt-1.5 leading-relaxed">
                Filter alumni by industry, graduation year, skills, and department. Connect with graduates worldwide.
              </p>
            </div>
            <div className="mt-6 flex items-center text-xs font-semibold text-blue-700 gap-1">
              <span>Open Directory</span>
              <span>→</span>
            </div>
          </Link>

          {/* Mentorship Program Tile */}
          <Link
            to="/mentorship"
            className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm hover:shadow-lg hover:border-purple-300 transition group flex flex-col justify-between"
          >
            <div>
              <div className="w-12 h-12 rounded-2xl bg-purple-100 text-purple-700 flex items-center justify-center text-2xl mb-4 group-hover:scale-110 transition transform">
                🎓
              </div>
              <h3 className="text-lg font-bold text-slate-900 group-hover:text-purple-700 transition">
                Mentorship Hub
              </h3>
              <p className="text-xs text-slate-600 mt-1.5 leading-relaxed">
                Find experienced mentors, get AI-powered match recommendations, and manage 1-on-1 guidance sessions.
              </p>
            </div>
            <div className="mt-6 flex items-center text-xs font-semibold text-purple-700 gap-1">
              <span>Explore Mentors</span>
              <span>→</span>
            </div>
          </Link>

          {/* Career Opportunities Tile */}
          <Link
            to="/opportunities"
            className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm hover:shadow-lg hover:border-amber-300 transition group flex flex-col justify-between"
          >
            <div>
              <div className="w-12 h-12 rounded-2xl bg-amber-100 text-amber-700 flex items-center justify-center text-2xl mb-4 group-hover:scale-110 transition transform">
                💼
              </div>
              <h3 className="text-lg font-bold text-slate-900 group-hover:text-amber-700 transition">
                Career Opportunities
              </h3>
              <p className="text-xs text-slate-600 mt-1.5 leading-relaxed">
                Discover job openings, summer internships, referral programs, and scholarships shared by alumni.
              </p>
            </div>
            <div className="mt-6 flex items-center text-xs font-semibold text-amber-700 gap-1">
              <span>View Openings</span>
              <span>→</span>
            </div>
          </Link>
        </div>

        {/* Quick Highlights: Events & Activity Feed */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2 bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
            <div className="flex justify-between items-center mb-4">
              <h3 className="font-bold text-slate-900 text-base">Upcoming Events Preview</h3>
              <Link to="/events" className="text-xs font-semibold text-blue-700 hover:underline">
                View All Events →
              </Link>
            </div>
            {recentEvents.length === 0 ? (
              <p className="text-xs text-slate-500 py-4">No events scheduled right now.</p>
            ) : (
              <div className="space-y-3">
                {recentEvents.map((ev) => (
                  <div
                    key={ev.id}
                    className="p-4 rounded-xl bg-slate-50 border border-slate-100 flex justify-between items-center gap-4"
                  >
                    <div>
                      <h4 className="font-bold text-slate-800 text-sm">{ev.title}</h4>
                      <p className="text-xs text-slate-500 mt-0.5">
                        📅 {ev.event_date} • 📍 {ev.location}
                      </p>
                    </div>
                    <Link
                      to="/events"
                      className="bg-blue-50 text-blue-700 hover:bg-blue-700 hover:text-white px-3 py-1.5 rounded-lg text-xs font-semibold transition"
                    >
                      {ev.is_registered ? "Registered ✓" : "Register"}
                    </Link>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Activity Feed Highlights */}
          <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm flex flex-col justify-between">
            <div>
              <div className="flex items-center justify-between mb-3">
                <div className="flex items-center gap-2">
                  <span className="text-lg">⚡</span>
                  <h3 className="font-bold text-sm text-slate-900">Network Activity</h3>
                </div>
                <span className="text-[10px] text-slate-400 font-semibold uppercase">Recent Highlights</span>
              </div>

              {recentActivities.length === 0 ? (
                <p className="text-xs text-slate-400 py-4">No recent activity logged.</p>
              ) : (
                <div className="divide-y divide-slate-100 space-y-2.5">
                  {recentActivities.map((act, i) => (
                    <div key={i} className="pt-2 flex items-start gap-2.5">
                      <span className="text-base">{act.icon || "📌"}</span>
                      <div className="flex-1 min-w-0">
                        <p className="text-xs font-bold text-slate-800 truncate">{act.title}</p>
                        <p className="text-[11px] text-slate-500 line-clamp-1">{act.description}</p>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

            <div className="mt-6 pt-4 border-t border-slate-100 text-[11px] text-slate-400 flex items-center justify-between">
              <span>Status: <strong className="text-emerald-600">Active</strong></span>
              <Link to="/saved" className="text-blue-600 font-medium hover:underline">
                Saved Items 🔖
              </Link>
            </div>
          </div>
        </div>
      </div>
    </DashboardLayout>
  );
};

export default Dashboard;