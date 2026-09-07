import { useEffect, useState } from "react";
import DashboardLayout from "../layouts/DashboardLayout";
import { profileApi } from "../api/profileApi";
import { preferenceApi } from "../api/preferenceApi";
import { achievementApi } from "../api/achievementApi";

const Profile = () => {
  const [profileData, setProfileData] = useState(null);
  const [completion, setCompletion] = useState(null);
  const [preferences, setPreferences] = useState({
    events: true,
    mentorship: true,
    opportunities: true,
    announcements: true,
  });
  const [showPrefModal, setShowPrefModal] = useState(false);
  const [savingPrefs, setSavingPrefs] = useState(false);

  // Achievements States
  const [achievements, setAchievements] = useState([]);
  const [showAddAchModal, setShowAddAchModal] = useState(false);
  const [submittingAch, setSubmittingAch] = useState(false);
  const [achForm, setAchForm] = useState({
    title: "",
    description: "",
    category: "HONOR",
    issuer: "",
    issue_date: "",
    evidence_url: "",
  });

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [errorMsg, setErrorMsg] = useState("");
  const [successMsg, setSuccessMsg] = useState("");

  const [formData, setFormData] = useState({
    name: "",
    // Alumni fields
    graduation_year: "",
    department: "",
    company: "",
    job_role: "",
    location: "",
    skills: "",
    bio: "",
    linkedin_url: "",
    github_url: "",
    mentorship_available: false,
    // Student fields
    branch: "",
    year: "",
    interests: "",
    profile_image_url: "",
  });

  const fetchProfile = async () => {
    setLoading(true);
    try {
      const [res, compRes, prefRes] = await Promise.all([
        profileApi.getMyProfile(),
        profileApi.getCompletionSuggestions(),
        preferenceApi.getPreferences(),
      ]);

      setProfileData(res.data);
      setCompletion(compRes.data || null);
      if (prefRes.data) {
        setPreferences({
          events: prefRes.data.events ?? true,
          mentorship: prefRes.data.mentorship ?? true,
          opportunities: prefRes.data.opportunities ?? true,
          announcements: prefRes.data.announcements ?? true,
        });
      }

      const p = res.data.profile || {};
      setFormData({
        name: res.data.name || "",
        graduation_year: p.graduation_year || "",
        department: p.department || "",
        company: p.company || "",
        job_role: p.job_role || "",
        location: p.location || "",
        skills: p.skills || "",
        bio: p.bio || "",
        linkedin_url: p.linkedin_url || "",
        github_url: p.github_url || "",
        mentorship_available: p.mentorship_available || false,
        branch: p.branch || "",
        year: p.year || "",
        interests: p.interests || "",
        profile_image_url: p.profile_image_url || "",
      });

      // Load achievements
      try {
        const achRes = await achievementApi.getMyAchievements();
        setAchievements(achRes.data || []);
      } catch (e) {
        // Unverified or error
      }
    } catch (err) {
      console.error("Failed to load profile:", err);
      setErrorMsg("Failed to load your profile. Please try refreshing.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProfile();
  }, []);

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData({
      ...formData,
      [name]: type === "checkbox" ? checked : value,
    });
    setErrorMsg("");
    setSuccessMsg("");
  };

  const handleSave = async (e) => {
    e.preventDefault();
    setSaving(true);
    setErrorMsg("");
    setSuccessMsg("");

    try {
      const res = await profileApi.updateMyProfile(formData);
      setProfileData(res.data);
      const localUserStr = localStorage.getItem("user");
      if (localUserStr) {
        const u = JSON.parse(localUserStr);
        u.name = res.data.name;
        localStorage.setItem("user", JSON.stringify(u));
      }
      setSuccessMsg("Profile updated successfully!");

      // Refresh completion percentage
      const compRes = await profileApi.getCompletionSuggestions();
      setCompletion(compRes.data || null);
    } catch (err) {
      console.error("Profile update failed:", err);
      const detail = err.response?.data?.detail || "Failed to update profile.";
      setErrorMsg(detail);
    } finally {
      setSaving(false);
    }
  };

  const handleSubmitAchievement = async (e) => {
    e.preventDefault();
    if (!achForm.title.trim() || !achForm.description.trim()) {
      setErrorMsg("Title and description are required.");
      return;
    }
    setSubmittingAch(true);
    setErrorMsg("");
    try {
      await achievementApi.submitAchievement(achForm);
      setSuccessMsg("Achievement submitted! It will appear on your profile once approved by admins.");
      setShowAddAchModal(false);
      setAchForm({
        title: "",
        description: "",
        category: "HONOR",
        issuer: "",
        issue_date: "",
        evidence_url: "",
      });
      const achRes = await achievementApi.getMyAchievements();
      setAchievements(achRes.data || []);
    } catch (err) {
      setErrorMsg(err.response?.data?.detail || "Failed to submit achievement.");
    } finally {
      setSubmittingAch(false);
    }
  };

  const handleDeleteAchievement = async (achId) => {
    if (!window.confirm("Delete this achievement record?")) return;
    try {
      await achievementApi.deleteAchievement(achId);
      setAchievements((prev) => prev.filter((a) => a.id !== achId));
      setSuccessMsg("Achievement removed.");
    } catch (err) {
      setErrorMsg(err.response?.data?.detail || "Failed to delete achievement.");
    }
  };

  const handleSavePreferences = async () => {
    setSavingPrefs(true);
    try {
      await preferenceApi.updatePreferences(preferences);
      setShowPrefModal(false);
      setSuccessMsg("Notification preferences updated.");
      setTimeout(() => setSuccessMsg(""), 3000);
    } catch (err) {
      console.error("Failed to save preferences:", err);
      setErrorMsg("Failed to save notification preferences.");
    } finally {
      setSavingPrefs(false);
    }
  };

  const role = (profileData?.role || "").toLowerCase();
  const isAlumni = role === "alumni";
  const isVerified = isAlumni && profileData?.profile?.is_verified;

  return (
    <DashboardLayout>
      <div className="max-w-4xl mx-auto space-y-6">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h1 className="text-3xl font-extrabold text-slate-900">My Profile</h1>
            <p className="text-sm text-slate-500 mt-1">
              Manage your personal credentials, contact links, and network presence
            </p>
          </div>

          <button
            onClick={() => setShowPrefModal(true)}
            className="p-2.5 px-4 rounded-xl border border-slate-300 text-xs font-semibold text-slate-700 hover:bg-slate-100 transition flex items-center gap-2 self-start sm:self-auto shadow-sm"
          >
            <span>🔔</span> Notification Preferences
          </button>
        </div>

        {/* Completion Suggestions Widget */}
        {completion && (
          <div className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm space-y-3">
            <div className="flex justify-between items-center">
              <div className="flex items-center gap-2">
                <span className="text-lg">📈</span>
                <span className="text-sm font-bold text-slate-800">Profile Strength</span>
              </div>
              <span className="text-xs font-extrabold text-blue-600 bg-blue-50 px-2.5 py-1 rounded-md">
                {completion.completion_percentage}% Complete
              </span>
            </div>

            {/* Progress Bar */}
            <div className="w-full h-2.5 bg-slate-100 rounded-full overflow-hidden">
              <div
                className="h-full bg-gradient-to-r from-blue-500 to-indigo-600 rounded-full transition-all duration-500"
                style={{ width: `${completion.completion_percentage}%` }}
              ></div>
            </div>

            {completion.missing_fields && completion.missing_fields.length > 0 && (
              <div className="text-xs text-slate-500 pt-1">
                <span>To reach 100%, consider adding: </span>
                <span className="font-semibold text-slate-700">
                  {completion.missing_fields.join(", ")}
                </span>
              </div>
            )}
          </div>
        )}

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

        {loading ? (
          <div className="text-center py-12 text-slate-500">
            <div className="inline-block animate-spin rounded-full h-8 w-8 border-4 border-blue-600 border-t-transparent mb-2"></div>
            <p className="text-sm">Loading your profile...</p>
          </div>
        ) : (
          <form onSubmit={handleSave} className="bg-white p-8 rounded-3xl border border-slate-200 shadow-sm space-y-6">
            {/* Header Badge */}
            <div className="flex items-center justify-between pb-6 border-b border-slate-100">
              <div className="flex items-center gap-4">
                <div className="w-16 h-16 rounded-2xl bg-blue-600 text-white flex items-center justify-center font-bold text-2xl shadow-md">
                  {formData.name ? formData.name.charAt(0).toUpperCase() : "U"}
                </div>
                <div>
                  <h2 className="text-xl font-bold text-slate-900">{profileData?.name}</h2>
                  <p className="text-xs text-slate-500">{profileData?.email}</p>
                  <span className="inline-block mt-1 bg-blue-50 text-blue-700 text-xs font-semibold px-2.5 py-0.5 rounded-full border border-blue-200 capitalize">
                    {profileData?.role}
                  </span>
                </div>
              </div>

              {/* Verification Badge for Alumni */}
              {isAlumni && (
                <div>
                  {isVerified ? (
                    <span className="bg-blue-50 text-blue-700 border border-blue-200 font-bold text-xs px-3 py-1.5 rounded-xl flex items-center gap-1.5 shadow-sm">
                      <span>✓</span> Verified Alumni Member
                    </span>
                  ) : (
                    <span className="bg-slate-50 text-slate-600 border border-slate-200 font-medium text-xs px-3 py-1.5 rounded-xl flex items-center gap-1.5">
                      <span>⏳</span> Verification Pending
                    </span>
                  )}
                </div>
              )}
            </div>

            {/* Core Fields */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1">Full Name</label>
                <input
                  type="text"
                  name="name"
                  value={formData.name}
                  onChange={handleChange}
                  required
                  className="w-full border border-slate-300 p-3 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1">Email Address (Read-only)</label>
                <input
                  type="email"
                  value={profileData?.email || ""}
                  disabled
                  className="w-full border border-slate-200 bg-slate-50 text-slate-500 p-3 rounded-xl text-sm cursor-not-allowed"
                />
              </div>

              {/* Alumni Specific Fields */}
              {isAlumni && (
                <>
                  <div>
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Company / Employer</label>
                    <input
                      type="text"
                      name="company"
                      placeholder="e.g. Google, Microsoft"
                      value={formData.company}
                      onChange={handleChange}
                      className="w-full border border-slate-300 p-3 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Job Role / Title</label>
                    <input
                      type="text"
                      name="job_role"
                      placeholder="e.g. Senior Software Engineer"
                      value={formData.job_role}
                      onChange={handleChange}
                      className="w-full border border-slate-300 p-3 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Graduation Year</label>
                    <input
                      type="number"
                      name="graduation_year"
                      placeholder="e.g. 2022"
                      value={formData.graduation_year}
                      onChange={handleChange}
                      className="w-full border border-slate-300 p-3 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Department / Branch</label>
                    <input
                      type="text"
                      name="department"
                      placeholder="e.g. Computer Science"
                      value={formData.department}
                      onChange={handleChange}
                      className="w-full border border-slate-300 p-3 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Location</label>
                    <input
                      type="text"
                      name="location"
                      placeholder="e.g. San Francisco, CA"
                      value={formData.location}
                      onChange={handleChange}
                      className="w-full border border-slate-300 p-3 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-600 mb-1">LinkedIn Profile URL</label>
                    <input
                      type="url"
                      name="linkedin_url"
                      placeholder="https://linkedin.com/in/username"
                      value={formData.linkedin_url}
                      onChange={handleChange}
                      className="w-full border border-slate-300 p-3 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-600 mb-1">GitHub Profile URL</label>
                    <input
                      type="url"
                      name="github_url"
                      placeholder="https://github.com/username"
                      value={formData.github_url}
                      onChange={handleChange}
                      className="w-full border border-slate-300 p-3 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
                    />
                  </div>

                  <div className="md:col-span-2">
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Technical Skills</label>
                    <input
                      type="text"
                      name="skills"
                      placeholder="e.g. Python, Machine Learning, Cloud Architecture"
                      value={formData.skills}
                      onChange={handleChange}
                      className="w-full border border-slate-300 p-3 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
                    />
                  </div>

                  <div className="md:col-span-2">
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Short Bio</label>
                    <textarea
                      name="bio"
                      rows="3"
                      placeholder="Brief background about your journey and expertise..."
                      value={formData.bio}
                      onChange={handleChange}
                      className="w-full border border-slate-300 p-3 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
                    ></textarea>
                  </div>

                  <div className="md:col-span-2 p-4 bg-emerald-50/60 rounded-2xl border border-emerald-200 flex items-center gap-3">
                    <input
                      type="checkbox"
                      id="mentorship_available"
                      name="mentorship_available"
                      checked={formData.mentorship_available}
                      onChange={handleChange}
                      className="w-5 h-5 text-emerald-600 rounded focus:ring-emerald-500"
                    />
                    <label htmlFor="mentorship_available" className="text-sm font-semibold text-emerald-900 cursor-pointer">
                      Available for Mentorship (Display badge in directory and receive student mentorship requests)
                    </label>
                  </div>
                </>
              )}

              {/* Student Specific Fields */}
              {role === "student" && (
                <>
                  <div>
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Branch / Degree Program</label>
                    <input
                      type="text"
                      name="branch"
                      placeholder="e.g. Computer Science Engineering"
                      value={formData.branch}
                      onChange={handleChange}
                      className="w-full border border-slate-300 p-3 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Academic Year</label>
                    <input
                      type="text"
                      name="year"
                      placeholder="e.g. 3rd Year / Senior"
                      value={formData.year}
                      onChange={handleChange}
                      className="w-full border border-slate-300 p-3 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Skills</label>
                    <input
                      type="text"
                      name="skills"
                      placeholder="e.g. React, Python, Data Analysis"
                      value={formData.skills}
                      onChange={handleChange}
                      className="w-full border border-slate-300 p-3 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Career Interests</label>
                    <input
                      type="text"
                      name="interests"
                      placeholder="e.g. Cloud Computing, AI Research, FinTech"
                      value={formData.interests}
                      onChange={handleChange}
                      className="w-full border border-slate-300 p-3 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
                    />
                  </div>

                  <div className="md:col-span-2">
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Student Bio</label>
                    <textarea
                      name="bio"
                      rows="3"
                      placeholder="Share your background, projects, or goals..."
                      value={formData.bio}
                      onChange={handleChange}
                      className="w-full border border-slate-300 p-3 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
                    ></textarea>
                  </div>
                </>
              )}
            </div>

            <div className="pt-4 flex justify-end">
              <button
                type="submit"
                disabled={saving}
                className="bg-blue-700 hover:bg-blue-800 text-white px-8 py-3 rounded-xl font-semibold shadow-md transition disabled:bg-blue-400"
              >
                {saving ? "Saving Changes..." : "Save Profile"}
              </button>
            </div>
          </form>
        )}

        {/* Notification Preferences Modal */}
        {showPrefModal && (
          <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4 backdrop-blur-sm">
            <div className="bg-white rounded-3xl p-8 max-w-md w-full shadow-2xl relative">
              <button
                onClick={() => setShowPrefModal(false)}
                className="absolute right-5 top-5 text-slate-400 hover:text-slate-700 text-xl font-bold"
              >
                ✕
              </button>

              <h3 className="text-xl font-bold text-slate-900 mb-1">Notification Preferences</h3>
              <p className="text-xs text-slate-500 mb-6">
                Choose which categories of platform updates and alerts you wish to receive.
              </p>

              <div className="space-y-4">
                {[
                  { key: "events", label: "Event Notifications", desc: "Invites, reminders, and RSVP updates" },
                  { key: "mentorship", label: "Mentorship Requests", desc: "Incoming mentorship connections and status changes" },
                  { key: "opportunities", label: "Career Opportunities", desc: "New job postings and referral alerts" },
                  { key: "announcements", label: "Campus Announcements", desc: "Administrative alerts and high-priority notices" },
                ].map((item) => (
                  <div key={item.key} className="flex items-center justify-between p-3 rounded-xl bg-slate-50 border border-slate-100">
                    <div>
                      <p className="text-xs font-bold text-slate-800">{item.label}</p>
                      <p className="text-[11px] text-slate-500">{item.desc}</p>
                    </div>
                    <input
                      type="checkbox"
                      checked={preferences[item.key]}
                      onChange={(e) =>
                        setPreferences((prev) => ({ ...prev, [item.key]: e.target.checked }))
                      }
                      className="w-5 h-5 text-blue-600 rounded focus:ring-blue-500"
                    />
                  </div>
                ))}
              </div>

              <div className="mt-6 flex justify-end gap-3 pt-4 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setShowPrefModal(false)}
                  className="px-4 py-2 rounded-xl text-slate-600 text-xs font-semibold"
                >
                  Cancel
                </button>
                <button
                  type="button"
                  onClick={handleSavePreferences}
                  disabled={savingPrefs}
                  className="px-6 py-2 rounded-xl bg-blue-600 hover:bg-blue-700 text-white text-xs font-semibold shadow-md disabled:bg-blue-400"
                >
                  {savingPrefs ? "Saving..." : "Save Preferences"}
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Honors, Awards & Achievements Section (Alumni & Admin) */}
        {role !== "student" && (
          <div className="bg-white p-6 sm:p-8 rounded-3xl border border-slate-200 shadow-sm space-y-5">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-100 pb-4">
              <div>
                <h3 className="text-xl font-bold text-slate-900 flex items-center gap-2">
                  <span>🏆</span> Honors, Awards & Recognitions
                </h3>
                <p className="text-xs text-slate-500 mt-0.5">
                  Showcase professional awards, certifications, patents, and domain milestones.
                </p>
              </div>

              {isVerified && (
                <button
                  type="button"
                  onClick={() => setShowAddAchModal(true)}
                  className="px-4 py-2 bg-amber-600 hover:bg-amber-700 text-white font-bold text-xs rounded-xl shadow-xs self-start sm:self-auto"
                >
                  + Add Recognition
                </button>
              )}
            </div>

            {!isVerified && (
              <div className="p-3.5 bg-amber-50 border border-amber-200 rounded-xl text-xs text-amber-800">
                <span>⚠️ Note: </span>
                Alumni profile verification by administrator is required to add and publish honors.
              </div>
            )}

            {achievements.length === 0 ? (
              <div className="py-8 text-center text-slate-400 text-xs">
                No achievements or honors listed yet.
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {achievements.map((ach) => (
                  <div
                    key={ach.id}
                    className="p-4 bg-slate-50 border border-slate-200 rounded-2xl flex flex-col justify-between gap-3"
                  >
                    <div>
                      <div className="flex items-center justify-between gap-2 mb-1.5">
                        <span className="text-[10px] font-bold px-2.5 py-0.5 rounded-full bg-amber-100 text-amber-800 uppercase tracking-wider">
                          {ach.category}
                        </span>
                        <span
                          className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                            ach.status === "APPROVED"
                              ? "bg-emerald-100 text-emerald-800"
                              : ach.status === "PENDING"
                              ? "bg-amber-100 text-amber-800"
                              : "bg-rose-100 text-rose-800"
                          }`}
                        >
                          {ach.status}
                        </span>
                      </div>
                      <h4 className="font-bold text-slate-900 text-sm">{ach.title}</h4>
                      {ach.issuer && (
                        <p className="text-xs text-slate-500 mt-0.5">
                          Issued by {ach.issuer} {ach.issue_date && `• ${ach.issue_date}`}
                        </p>
                      )}
                      <p className="text-xs text-slate-600 mt-2 line-clamp-3 leading-relaxed">
                        {ach.description}
                      </p>
                    </div>

                    <div className="pt-2 border-t border-slate-200/60 flex items-center justify-between">
                      {ach.evidence_url ? (
                        <a
                          href={ach.evidence_url}
                          target="_blank"
                          rel="noreferrer"
                          className="text-[11px] text-blue-600 hover:underline font-semibold"
                        >
                          View Credential ↗
                        </a>
                      ) : (
                        <span></span>
                      )}
                      <button
                        type="button"
                        onClick={() => handleDeleteAchievement(ach.id)}
                        className="text-xs text-rose-600 hover:text-rose-800 font-semibold"
                      >
                        Remove
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* Add Achievement Modal */}
        {showAddAchModal && (
          <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4 backdrop-blur-sm">
            <div className="bg-white rounded-3xl p-6 sm:p-8 max-w-lg w-full shadow-2xl relative">
              <button
                onClick={() => setShowAddAchModal(false)}
                className="absolute right-5 top-5 text-slate-400 hover:text-slate-700 text-xl font-bold"
              >
                ✕
              </button>

              <h3 className="text-xl font-bold text-slate-900 mb-1">Add Honor or Achievement</h3>
              <p className="text-xs text-slate-500 mb-5">
                Submitted honors undergo quick admin moderation before appearing publicly.
              </p>

              <form onSubmit={handleSubmitAchievement} className="space-y-4">
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">Title *</label>
                  <input
                    type="text"
                    value={achForm.title}
                    onChange={(e) => setAchForm({ ...achForm, title: e.target.value })}
                    placeholder="e.g. AWS Certified Solutions Architect - Professional"
                    className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-xs focus:outline-amber-500"
                    required
                  />
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs font-bold text-slate-700 mb-1">Category</label>
                    <select
                      value={achForm.category}
                      onChange={(e) => setAchForm({ ...achForm, category: e.target.value })}
                      className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-xs focus:outline-amber-500"
                    >
                      <option value="HONOR">Honor</option>
                      <option value="AWARD">Award</option>
                      <option value="CERTIFICATION">Certification</option>
                      <option value="PATENT">Patent</option>
                      <option value="PUBLICATION">Publication</option>
                    </select>
                  </div>
                  <div>
                    <label className="block text-xs font-bold text-slate-700 mb-1">Issue Date</label>
                    <input
                      type="date"
                      value={achForm.issue_date}
                      onChange={(e) => setAchForm({ ...achForm, issue_date: e.target.value })}
                      className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-xs focus:outline-amber-500"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    Issuing Organization
                  </label>
                  <input
                    type="text"
                    value={achForm.issuer}
                    onChange={(e) => setAchForm({ ...achForm, issuer: e.target.value })}
                    placeholder="e.g. Amazon Web Services, IEEE, Forbes"
                    className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-xs focus:outline-amber-500"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">Description *</label>
                  <textarea
                    rows={3}
                    value={achForm.description}
                    onChange={(e) => setAchForm({ ...achForm, description: e.target.value })}
                    placeholder="Summary of what this achievement entails..."
                    className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-xs focus:outline-amber-500"
                    required
                  ></textarea>
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    Evidence / Verification Link
                  </label>
                  <input
                    type="url"
                    value={achForm.evidence_url}
                    onChange={(e) => setAchForm({ ...achForm, evidence_url: e.target.value })}
                    placeholder="https://credly.com/badges/..."
                    className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-xs focus:outline-amber-500"
                  />
                </div>

                <div className="pt-2 flex justify-end gap-2">
                  <button
                    type="button"
                    onClick={() => setShowAddAchModal(false)}
                    className="px-4 py-2 text-slate-600 font-semibold text-xs hover:bg-slate-100 rounded-xl"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={submittingAch}
                    className="px-5 py-2 bg-amber-600 hover:bg-amber-700 text-white font-bold text-xs rounded-xl shadow-xs disabled:opacity-50"
                  >
                    {submittingAch ? "Submitting..." : "Submit Recognition"}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </div>
    </DashboardLayout>
  );
};

export default Profile;
