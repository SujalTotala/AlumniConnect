import { useState, useEffect } from "react";
import DashboardLayout from "../layouts/DashboardLayout";
import { storyApi } from "../api/storyApi";

export default function SuccessStories() {
  const [stories, setStories] = useState([]);
  const [myStories, setMyStories] = useState([]);
  const [activeTab, setActiveTab] = useState("feed"); // "feed" or "mine"
  const [loading, setLoading] = useState(true);
  const [errorMsg, setErrorMsg] = useState("");
  const [successMsg, setSuccessMsg] = useState("");

  // Modal States
  const [isSubmitOpen, setIsSubmitOpen] = useState(false);
  const [selectedStory, setSelectedStory] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  // Form State
  const [formData, setFormData] = useState({
    title: "",
    summary: "",
    content: "",
    company: "",
    role: "",
    achievement_date: "",
    image_url: "",
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

  const fetchStories = async () => {
    setLoading(true);
    setErrorMsg("");
    try {
      if (activeTab === "feed") {
        const res = await storyApi.getApprovedStories();
        setStories(res.data || []);
      } else {
        const res = await storyApi.getMyStories();
        setMyStories(res.data || []);
      }
    } catch (err) {
      console.error("Failed to load stories:", err);
      setErrorMsg("Unable to load success stories. Please check your network.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchStories();
  }, [activeTab]);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmitStory = async (e) => {
    e.preventDefault();
    if (!formData.title.trim() || !formData.summary.trim() || !formData.content.trim()) {
      setErrorMsg("Title, summary, and story content are required.");
      return;
    }

    setSubmitting(true);
    setErrorMsg("");
    try {
      await storyApi.submitStory(formData);
      setSuccessMsg("Success story submitted! It will appear publicly once approved by admins.");
      setIsSubmitOpen(false);
      setFormData({
        title: "",
        summary: "",
        content: "",
        company: "",
        role: "",
        achievement_date: "",
        image_url: "",
      });
      setActiveTab("mine");
    } catch (err) {
      const msg = err.response?.data?.detail || "Failed to submit success story.";
      setErrorMsg(msg);
    } finally {
      setSubmitting(false);
    }
  };

  const handleDeleteStory = async (storyId) => {
    if (!window.confirm("Are you sure you want to remove this story?")) return;
    try {
      await storyApi.deleteStory(storyId);
      setMyStories((prev) => prev.filter((s) => s.id !== storyId));
      setSuccessMsg("Story removed successfully.");
    } catch (err) {
      setErrorMsg(err.response?.data?.detail || "Failed to remove story.");
    }
  };

  return (
    <DashboardLayout>
      <div className="space-y-6 max-w-7xl mx-auto pb-12">
        {/* Banner Header */}
        <div className="bg-gradient-to-r from-amber-600 via-rose-600 to-indigo-700 rounded-2xl p-6 sm:p-8 text-white shadow-xl relative overflow-hidden">
          <div className="relative z-10 max-w-2xl">
            <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-white/20 backdrop-blur-md mb-3">
              🌟 Alumni Spotlights & Milestones
            </span>
            <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight">
              Alumni Success Stories
            </h1>
            <p className="mt-2 text-amber-100 text-sm sm:text-base leading-relaxed">
              Discover inspiring career trajectories, entrepreneurial breakthroughs, and
              contributions made by our alumni community across industries worldwide.
            </p>
            {isAlumniOrAdmin && (
              <button
                onClick={() => setIsSubmitOpen(true)}
                className="mt-4 px-5 py-2.5 bg-white text-rose-700 hover:bg-rose-50 font-bold rounded-xl shadow-md transition-all transform active:scale-95 flex items-center gap-2 text-sm"
              >
                <span>✍️</span> Share Your Journey
              </button>
            )}
          </div>
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

        {/* Tab Controls */}
        <div className="flex border-b border-slate-200 pb-2 items-center justify-between">
          <div className="flex gap-2">
            <button
              onClick={() => setActiveTab("feed")}
              className={`px-4 py-2 text-sm font-semibold rounded-lg transition ${
                activeTab === "feed"
                  ? "bg-rose-600 text-white shadow-sm"
                  : "text-slate-600 hover:bg-slate-100"
              }`}
            >
              Public Featured Feed
            </button>
            {isAlumniOrAdmin && (
              <button
                onClick={() => setActiveTab("mine")}
                className={`px-4 py-2 text-sm font-semibold rounded-lg transition ${
                  activeTab === "mine"
                    ? "bg-rose-600 text-white shadow-sm"
                    : "text-slate-600 hover:bg-slate-100"
                }`}
              >
                My Submissions
              </button>
            )}
          </div>
          <button
            onClick={fetchStories}
            className="text-xs font-semibold text-slate-500 hover:text-slate-800 px-3 py-1.5 rounded-lg border border-slate-200 bg-white"
          >
            ↻ Refresh
          </button>
        </div>

        {/* Main Content Feed */}
        {loading ? (
          <div className="py-20 text-center">
            <div className="w-10 h-10 border-4 border-rose-600 border-t-transparent rounded-full animate-spin mx-auto mb-3"></div>
            <p className="text-sm text-slate-500">Loading inspiring stories...</p>
          </div>
        ) : activeTab === "feed" ? (
          stories.length === 0 ? (
            <div className="text-center py-16 bg-white rounded-2xl border border-slate-200 p-8 shadow-xs">
              <span className="text-4xl">🌟</span>
              <h3 className="mt-3 text-lg font-bold text-slate-800">No Featured Stories Yet</h3>
              <p className="mt-1 text-sm text-slate-500 max-w-md mx-auto">
                Be the first verified alumni to share your career milestones and achievements with the network.
              </p>
              {isAlumniOrAdmin && (
                <button
                  onClick={() => setIsSubmitOpen(true)}
                  className="mt-4 px-4 py-2 bg-rose-600 hover:bg-rose-700 text-white text-sm font-semibold rounded-xl shadow-xs"
                >
                  Submit a Story
                </button>
              )}
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {stories.map((story) => (
                <div
                  key={story.id}
                  className="bg-white rounded-2xl border border-slate-200 shadow-xs hover:shadow-md transition flex flex-col justify-between overflow-hidden"
                >
                  <div className="p-6">
                    {/* Header Tags */}
                    <div className="flex items-center justify-between text-xs text-slate-500 mb-3">
                      <span className="font-semibold text-rose-600 bg-rose-50 px-2.5 py-1 rounded-full">
                        {story.company || "Alumni Spotlight"}
                      </span>
                      <span>
                        {story.created_at ? new Date(story.created_at).toLocaleDateString() : ""}
                      </span>
                    </div>

                    <h2 className="text-lg font-bold text-slate-900 leading-snug line-clamp-2">
                      {story.title}
                    </h2>

                    <p className="mt-2.5 text-sm text-slate-600 line-clamp-3 leading-relaxed">
                      {story.summary}
                    </p>

                    {/* Author block */}
                    <div className="mt-5 pt-4 border-t border-slate-100 flex items-center gap-3">
                      <div className="w-10 h-10 rounded-full bg-gradient-to-br from-indigo-500 to-purple-600 text-white font-bold flex items-center justify-center text-sm shadow-xs">
                        {story.author_name?.charAt(0) || "A"}
                      </div>
                      <div className="min-w-0">
                        <p className="text-sm font-bold text-slate-800 flex items-center gap-1 truncate">
                          {story.author_name}
                          {story.is_verified && (
                            <span className="text-blue-500 text-xs" title="Verified Alumni">
                              ✓
                            </span>
                          )}
                        </p>
                        <p className="text-xs text-slate-500 truncate">
                          {story.role ? `${story.role}` : "Alumni Member"}
                        </p>
                      </div>
                    </div>
                  </div>

                  <div className="px-6 py-3 bg-slate-50 border-t border-slate-100 flex justify-between items-center">
                    <button
                      onClick={() => setSelectedStory(story)}
                      className="text-sm font-semibold text-rose-600 hover:text-rose-700 flex items-center gap-1"
                    >
                      Read Story <span>→</span>
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )
        ) : (
          /* My Submissions Tab */
          myStories.length === 0 ? (
            <div className="text-center py-16 bg-white rounded-2xl border border-slate-200 p-8 shadow-xs">
              <span className="text-4xl">📝</span>
              <h3 className="mt-3 text-lg font-bold text-slate-800">No Submissions Found</h3>
              <p className="mt-1 text-sm text-slate-500">
                You haven't submitted any success stories yet.
              </p>
              <button
                onClick={() => setIsSubmitOpen(true)}
                className="mt-4 px-4 py-2 bg-rose-600 hover:bg-rose-700 text-white text-sm font-semibold rounded-xl"
              >
                Write Story Now
              </button>
            </div>
          ) : (
            <div className="space-y-4">
              {myStories.map((story) => (
                <div
                  key={story.id}
                  className="bg-white rounded-2xl border border-slate-200 p-6 shadow-xs flex flex-col md:flex-row md:items-center md:justify-between gap-4"
                >
                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      <span
                        className={`text-xs font-bold px-2.5 py-0.5 rounded-full ${
                          story.status === "APPROVED"
                            ? "bg-emerald-100 text-emerald-800"
                            : story.status === "PENDING"
                            ? "bg-amber-100 text-amber-800"
                            : "bg-rose-100 text-rose-800"
                        }`}
                      >
                        {story.status}
                      </span>
                      <h3 className="font-bold text-slate-800 text-base">{story.title}</h3>
                    </div>
                    <p className="text-sm text-slate-500 line-clamp-1">{story.summary}</p>
                    <p className="text-xs text-slate-400">
                      Submitted on {new Date(story.created_at).toLocaleDateString()}
                    </p>
                  </div>
                  <div className="flex items-center gap-3">
                    <button
                      onClick={() => setSelectedStory(story)}
                      className="px-3.5 py-1.5 text-xs font-semibold text-slate-700 border border-slate-300 hover:bg-slate-50 rounded-lg"
                    >
                      Preview
                    </button>
                    <button
                      onClick={() => handleDeleteStory(story.id)}
                      className="px-3.5 py-1.5 text-xs font-semibold text-rose-600 border border-rose-200 hover:bg-rose-50 rounded-lg"
                    >
                      Delete
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )
        )}

        {/* Read Story Modal */}
        {selectedStory && (
          <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4 overflow-y-auto">
            <div className="bg-white rounded-3xl max-w-2xl w-full p-6 sm:p-8 shadow-2xl border border-slate-100 my-8">
              <div className="flex justify-between items-start">
                <div>
                  <span className="text-xs font-bold uppercase tracking-wider text-rose-600 bg-rose-50 px-2.5 py-1 rounded-full">
                    {selectedStory.company || "Alumni Feature"}
                  </span>
                  <h2 className="text-xl sm:text-2xl font-bold text-slate-900 mt-2">
                    {selectedStory.title}
                  </h2>
                </div>
                <button
                  onClick={() => setSelectedStory(null)}
                  className="text-slate-400 hover:text-slate-700 text-2xl font-bold p-1"
                >
                  ✕
                </button>
              </div>

              {/* Author Metadata */}
              <div className="mt-4 pb-4 border-b border-slate-100 flex items-center gap-3">
                <div className="w-11 h-11 rounded-full bg-gradient-to-tr from-indigo-500 to-purple-600 text-white font-bold flex items-center justify-center text-base">
                  {selectedStory.author_name?.charAt(0) || "A"}
                </div>
                <div>
                  <p className="text-sm font-bold text-slate-800">
                    {selectedStory.author_name}
                    {selectedStory.is_verified && (
                      <span className="text-blue-500 ml-1">✓ (Verified Alumni)</span>
                    )}
                  </p>
                  <p className="text-xs text-slate-500">
                    {selectedStory.role} {selectedStory.company && `at ${selectedStory.company}`}
                  </p>
                </div>
              </div>

              {/* Story Content */}
              <div className="mt-6 space-y-4 text-slate-700 text-sm sm:text-base leading-relaxed whitespace-pre-wrap max-h-[50vh] overflow-y-auto pr-2">
                <p className="font-semibold text-slate-800 italic bg-slate-50 p-4 rounded-xl border-l-4 border-rose-500">
                  "{selectedStory.summary}"
                </p>
                <div>{selectedStory.content}</div>
              </div>

              <div className="mt-6 pt-4 border-t border-slate-100 flex justify-end">
                <button
                  onClick={() => setSelectedStory(null)}
                  className="px-5 py-2.5 bg-slate-900 text-white font-semibold rounded-xl text-sm hover:bg-slate-800"
                >
                  Close
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Submit Story Modal */}
        {isSubmitOpen && (
          <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4 overflow-y-auto">
            <div className="bg-white rounded-3xl max-w-2xl w-full p-6 sm:p-8 shadow-2xl border border-slate-100 my-8">
              <div className="flex justify-between items-center mb-5">
                <div>
                  <h3 className="text-xl font-bold text-slate-900">Share Your Success Story</h3>
                  <p className="text-xs text-slate-500 mt-0.5">
                    Verified alumni stories are highlighted in the student & alumni network feed.
                  </p>
                </div>
                <button
                  onClick={() => setIsSubmitOpen(false)}
                  className="text-slate-400 hover:text-slate-700 text-xl font-bold"
                >
                  ✕
                </button>
              </div>

              <form onSubmit={handleSubmitStory} className="space-y-4">
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    Story Title *
                  </label>
                  <input
                    type="text"
                    name="title"
                    value={formData.title}
                    onChange={handleInputChange}
                    placeholder="e.g. Scaling a FinTech Platform from Seed to Series B"
                    className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-sm focus:outline-rose-500"
                    required
                  />
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs font-bold text-slate-700 mb-1">
                      Current Company / Venture
                    </label>
                    <input
                      type="text"
                      name="company"
                      value={formData.company}
                      onChange={handleInputChange}
                      placeholder="e.g. Google, Stripe, or Founder"
                      className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-sm focus:outline-rose-500"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-bold text-slate-700 mb-1">
                      Job Role / Position
                    </label>
                    <input
                      type="text"
                      name="role"
                      value={formData.role}
                      onChange={handleInputChange}
                      placeholder="e.g. Principal Architect, VP Product"
                      className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-sm focus:outline-rose-500"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    One-Sentence Summary *
                  </label>
                  <input
                    type="text"
                    name="summary"
                    value={formData.summary}
                    onChange={handleInputChange}
                    placeholder="Brief highlight that appears on the feed card"
                    className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-sm focus:outline-rose-500"
                    required
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    Full Story / Advice to Juniors *
                  </label>
                  <textarea
                    rows={6}
                    name="content"
                    value={formData.content}
                    onChange={handleInputChange}
                    placeholder="Share how your education, challenges, projects, and career milestones unfolded..."
                    className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-sm focus:outline-rose-500"
                    required
                  ></textarea>
                </div>

                <div className="pt-2 flex justify-end gap-3">
                  <button
                    type="button"
                    onClick={() => setIsSubmitOpen(false)}
                    className="px-4 py-2.5 text-slate-700 font-semibold text-sm hover:bg-slate-100 rounded-xl"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={submitting}
                    className="px-5 py-2.5 bg-rose-600 hover:bg-rose-700 text-white font-bold text-sm rounded-xl shadow-xs disabled:opacity-50"
                  >
                    {submitting ? "Submitting..." : "Submit for Moderation"}
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
