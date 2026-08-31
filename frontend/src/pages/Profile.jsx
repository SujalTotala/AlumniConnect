import { useEffect, useState } from "react";
import DashboardLayout from "../layouts/DashboardLayout";
import { profileApi } from "../api/profileApi";

const Profile = () => {
  const [profileData, setProfileData] = useState(null);
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
      const res = await profileApi.getMyProfile();
      setProfileData(res.data);
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
      // Update local storage user name if changed
      const localUserStr = localStorage.getItem("user");
      if (localUserStr) {
        const u = JSON.parse(localUserStr);
        u.name = res.data.name;
        localStorage.setItem("user", JSON.stringify(u));
      }
      setSuccessMsg("Profile updated successfully!");
    } catch (err) {
      console.error("Profile update failed:", err);
      const detail = err.response?.data?.detail || "Failed to update profile.";
      setErrorMsg(detail);
    } finally {
      setSaving(false);
    }
  };

  const role = (profileData?.role || "").toLowerCase();

  return (
    <DashboardLayout>
      <div className="max-w-4xl mx-auto">
        <div className="mb-8">
          <h1 className="text-3xl font-extrabold text-slate-900">My Profile</h1>
          <p className="text-sm text-slate-500 mt-1">
            Manage your personal credentials, contact links, and network presence
          </p>
        </div>

        {errorMsg && (
          <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-xl mb-6 text-sm flex justify-between">
            <span>{errorMsg}</span>
            <button onClick={() => setErrorMsg("")} className="font-bold">✕</button>
          </div>
        )}
        {successMsg && (
          <div className="bg-emerald-50 border border-emerald-200 text-emerald-700 px-4 py-3 rounded-xl mb-6 text-sm flex justify-between">
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
            <div className="flex items-center gap-4 pb-6 border-b border-slate-100">
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
              {role === "alumni" && (
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
      </div>
    </DashboardLayout>
  );
};

export default Profile;
