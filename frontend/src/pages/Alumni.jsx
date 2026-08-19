import { useEffect, useState } from "react";
import DashboardLayout from "../layouts/DashboardLayout";
import { alumniApi } from "../api/alumniApi";

const Alumni = () => {
  const [alumni, setAlumni] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [deptFilter, setDeptFilter] = useState("");
  const [mentorFilter, setMentorFilter] = useState(false);
  const [showAddModal, setShowAddModal] = useState(false);
  const [selectedAlumni, setSelectedAlumni] = useState(null);
  const [errorMsg, setErrorMsg] = useState("");
  const [successMsg, setSuccessMsg] = useState("");

  let currentUser = null;
  try {
    const userStr = localStorage.getItem("user");
    if (userStr) currentUser = JSON.parse(userStr);
  } catch (e) {
    console.error(e);
  }
  const isAdmin = currentUser?.role?.toLowerCase() === "admin";

  const [formData, setFormData] = useState({
    name: "",
    email: "",
    department: "",
    graduation_year: "",
    company: "",
    job_role: "",
    location: "",
    skills: "",
    bio: "",
    linkedin_url: "",
    github_url: "",
    mentorship_available: false,
  });

  const fetchAlumni = async () => {
    setLoading(true);
    try {
      const params = {};
      if (search) params.search = search;
      if (deptFilter) params.department = deptFilter;
      if (mentorFilter) params.mentorship_available = true;

      const response = await alumniApi.getAlumni(params);
      setAlumni(response.data);
    } catch (error) {
      console.error("Failed to fetch alumni", error);
      setErrorMsg("Failed to load alumni directory.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAlumni();
  }, [deptFilter, mentorFilter]);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    fetchAlumni();
  };

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData({
      ...formData,
      [name]: type === "checkbox" ? checked : value,
    });
  };

  const handleAddAlumni = async (e) => {
    e.preventDefault();
    setErrorMsg("");
    setSuccessMsg("");

    try {
      await alumniApi.createAlumni(formData);
      setSuccessMsg("Alumni profile successfully created!");
      setShowAddModal(false);
      setFormData({
        name: "",
        email: "",
        department: "",
        graduation_year: "",
        company: "",
        job_role: "",
        location: "",
        skills: "",
        bio: "",
        linkedin_url: "",
        github_url: "",
        mentorship_available: false,
      });
      fetchAlumni();
    } catch (error) {
      const detail = error.response?.data?.detail || "Failed to add alumni profile.";
      setErrorMsg(detail);
    }
  };

  const handleDeleteAlumni = async (id) => {
    if (!window.confirm("Are you sure you want to delete this alumni record?")) return;

    try {
      await alumniApi.deleteAlumni(id);
      setSuccessMsg("Alumni record deleted successfully.");
      if (selectedAlumni?.id === id) setSelectedAlumni(null);
      fetchAlumni();
    } catch (error) {
      const detail = error.response?.data?.detail || "Failed to delete alumni record.";
      setErrorMsg(detail);
    }
  };

  return (
    <DashboardLayout>
      <div className="max-w-7xl mx-auto space-y-6">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h1 className="text-3xl font-extrabold text-slate-900">Alumni Directory</h1>
            <p className="text-sm text-slate-500 mt-1">
              Connect with graduates across departments, industries, and locations
            </p>
          </div>

          <button
            onClick={() => setShowAddModal(true)}
            className="bg-blue-700 hover:bg-blue-800 text-white px-5 py-2.5 rounded-xl font-semibold shadow-md transition flex items-center gap-2 self-start sm:self-auto"
          >
            <span>+ Add Alumni Record</span>
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

        {/* Search & Filters */}
        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex flex-col md:flex-row gap-4 justify-between items-center">
          <form onSubmit={handleSearchSubmit} className="relative flex-1 w-full">
            <input
              type="text"
              placeholder="Search by name, company, job title, skills, or location..."
              className="w-full pl-11 pr-24 py-2.5 rounded-xl border border-slate-300 bg-slate-50 text-sm focus:bg-white focus:ring-2 focus:ring-blue-500 focus:outline-none"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
            <span className="absolute left-3.5 top-2.5 text-base text-slate-400">🔍</span>
            <button
              type="submit"
              className="absolute right-1.5 top-1.5 bg-blue-600 hover:bg-blue-700 text-white px-3 py-1.5 rounded-lg text-xs font-semibold"
            >
              Search
            </button>
          </form>

          <div className="flex items-center gap-3 w-full md:w-auto">
            <select
              value={deptFilter}
              onChange={(e) => setDeptFilter(e.target.value)}
              className="border border-slate-300 bg-slate-50 p-2.5 rounded-xl text-xs font-medium text-slate-700 focus:outline-none"
            >
              <option value="">All Departments</option>
              <option value="Computer">Computer Science</option>
              <option value="Information">Information Tech</option>
              <option value="Mechanical">Mechanical</option>
              <option value="Electrical">Electrical</option>
              <option value="Business">Business / Management</option>
            </select>

            <label className="flex items-center gap-2 text-xs font-medium text-slate-700 cursor-pointer bg-slate-50 p-2.5 px-3 rounded-xl border border-slate-300">
              <input
                type="checkbox"
                checked={mentorFilter}
                onChange={(e) => setMentorFilter(e.target.checked)}
                className="w-4 h-4 text-blue-600 rounded"
              />
              <span>Mentors Only</span>
            </label>
          </div>
        </div>

        {/* Directory Cards */}
        {loading ? (
          <div className="text-center py-16 text-slate-500">
            <div className="inline-block animate-spin rounded-full h-8 w-8 border-4 border-blue-600 border-t-transparent mb-2"></div>
            <p className="text-sm">Loading Alumni Directory...</p>
          </div>
        ) : alumni.length === 0 ? (
          <div className="bg-white p-12 rounded-2xl border border-slate-200 text-center shadow-sm">
            <div className="text-4xl mb-3">👥</div>
            <h3 className="text-lg font-bold text-slate-800">No Alumni Records Found</h3>
            <p className="text-xs text-slate-500 mt-1">Try resetting your filters or search keywords.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {alumni.map((item) => (
              <div
                key={item.id}
                className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200 hover:shadow-md transition flex flex-col justify-between"
              >
                <div>
                  <div className="flex items-start justify-between gap-2 mb-3">
                    <div className="flex items-center gap-3">
                      <div className="w-12 h-12 rounded-xl bg-blue-100 text-blue-700 flex items-center justify-center font-bold text-lg">
                        {item.name ? item.name.charAt(0).toUpperCase() : "A"}
                      </div>
                      <div>
                        <h2 className="text-lg font-bold text-slate-900 leading-tight">{item.name}</h2>
                        <p className="text-xs text-blue-700 font-semibold">{item.job_role || "Alumni Member"}</p>
                      </div>
                    </div>
                    {item.mentorship_available && (
                      <span className="bg-emerald-50 text-emerald-700 text-[10px] font-bold px-2 py-0.5 rounded-full border border-emerald-200">
                        Mentor
                      </span>
                    )}
                  </div>

                  <div className="space-y-1 text-xs text-slate-600 my-3">
                    {item.company && <p>🏢 <strong className="text-slate-800">{item.company}</strong></p>}
                    {item.department && <p>🎓 {item.department}</p>}
                    {item.graduation_year && <p>📅 Class of {item.graduation_year}</p>}
                    {item.location && <p>📍 {item.location}</p>}
                  </div>

                  {item.skills && (
                    <div className="flex flex-wrap gap-1 mt-2">
                      {item.skills.split(",").slice(0, 3).map((s, idx) => (
                        <span key={idx} className="bg-slate-100 text-slate-700 text-[10px] px-2 py-0.5 rounded-md">
                          {s.trim()}
                        </span>
                      ))}
                    </div>
                  )}
                </div>

                <div className="pt-4 mt-4 border-t border-slate-100 flex justify-between items-center">
                  <button
                    onClick={() => setSelectedAlumni(item)}
                    className="text-xs text-blue-700 hover:text-blue-900 font-semibold"
                  >
                    View Details →
                  </button>
                  {(isAdmin || currentUser?.email === item.email) && (
                    <button
                      onClick={() => handleDeleteAlumni(item.id)}
                      className="text-xs text-red-500 hover:text-red-700 font-medium"
                    >
                      Delete
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Alumni Details Modal */}
        {selectedAlumni && (
          <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4 backdrop-blur-sm">
            <div className="bg-white rounded-3xl p-8 max-w-lg w-full shadow-2xl relative max-h-[90vh] overflow-y-auto">
              <button
                onClick={() => setSelectedAlumni(null)}
                className="absolute right-5 top-5 text-slate-400 hover:text-slate-700 text-xl font-bold"
              >
                ✕
              </button>

              <div className="flex items-center gap-4 mb-6">
                <div className="w-16 h-16 rounded-2xl bg-blue-600 text-white flex items-center justify-center font-bold text-2xl">
                  {selectedAlumni.name.charAt(0)}
                </div>
                <div>
                  <h3 className="text-2xl font-bold text-slate-900">{selectedAlumni.name}</h3>
                  <p className="text-sm text-blue-700 font-semibold">{selectedAlumni.job_role || "Alumni"}</p>
                  <p className="text-xs text-slate-500">{selectedAlumni.email}</p>
                </div>
              </div>

              <div className="space-y-3 text-sm text-slate-700">
                {selectedAlumni.company && (
                  <div>
                    <span className="text-xs font-semibold text-slate-500 uppercase">Company:</span>
                    <p className="font-semibold text-slate-900">{selectedAlumni.company}</p>
                  </div>
                )}
                {selectedAlumni.department && (
                  <div>
                    <span className="text-xs font-semibold text-slate-500 uppercase">Department:</span>
                    <p>{selectedAlumni.department}</p>
                  </div>
                )}
                {selectedAlumni.graduation_year && (
                  <div>
                    <span className="text-xs font-semibold text-slate-500 uppercase">Graduation Year:</span>
                    <p>{selectedAlumni.graduation_year}</p>
                  </div>
                )}
                {selectedAlumni.location && (
                  <div>
                    <span className="text-xs font-semibold text-slate-500 uppercase">Location:</span>
                    <p>{selectedAlumni.location}</p>
                  </div>
                )}
                {selectedAlumni.bio && (
                  <div>
                    <span className="text-xs font-semibold text-slate-500 uppercase">Bio:</span>
                    <p className="text-xs text-slate-600 bg-slate-50 p-3 rounded-xl border border-slate-200 mt-1">
                      {selectedAlumni.bio}
                    </p>
                  </div>
                )}
                {selectedAlumni.skills && (
                  <div>
                    <span className="text-xs font-semibold text-slate-500 uppercase">Skills:</span>
                    <div className="flex flex-wrap gap-1.5 mt-1">
                      {selectedAlumni.skills.split(",").map((s, idx) => (
                        <span key={idx} className="bg-slate-100 text-slate-800 text-xs px-2.5 py-1 rounded-lg">
                          {s.trim()}
                        </span>
                      ))}
                    </div>
                  </div>
                )}

                <div className="flex gap-4 pt-4 border-t border-slate-100">
                  {selectedAlumni.linkedin_url && (
                    <a
                      href={selectedAlumni.linkedin_url}
                      target="_blank"
                      rel="noreferrer"
                      className="text-xs text-blue-700 font-semibold hover:underline"
                    >
                      🔗 LinkedIn Profile
                    </a>
                  )}
                  {selectedAlumni.github_url && (
                    <a
                      href={selectedAlumni.github_url}
                      target="_blank"
                      rel="noreferrer"
                      className="text-xs text-slate-800 font-semibold hover:underline"
                    >
                      🐙 GitHub Profile
                    </a>
                  )}
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Add Alumni Modal */}
        {showAddModal && (
          <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4 backdrop-blur-sm">
            <div className="bg-white rounded-3xl p-8 max-w-2xl w-full shadow-2xl relative max-h-[90vh] overflow-y-auto">
              <button
                onClick={() => setShowAddModal(false)}
                className="absolute right-5 top-5 text-slate-400 hover:text-slate-700 text-xl font-bold"
              >
                ✕
              </button>

              <h2 className="text-2xl font-bold text-slate-900 mb-6 pb-2 border-b border-slate-100">
                Add New Alumni Profile
              </h2>

              <form onSubmit={handleAddAlumni} className="space-y-4">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Full Name *</label>
                    <input
                      type="text"
                      name="name"
                      value={formData.name}
                      onChange={handleChange}
                      required
                      placeholder="e.g. Jane Doe"
                      className="w-full border border-slate-300 p-3 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Email Address *</label>
                    <input
                      type="email"
                      name="email"
                      value={formData.email}
                      onChange={handleChange}
                      required
                      placeholder="e.g. jane@tech.com"
                      className="w-full border border-slate-300 p-3 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Department / Branch</label>
                    <input
                      type="text"
                      name="department"
                      value={formData.department}
                      onChange={handleChange}
                      placeholder="e.g. Computer Science"
                      className="w-full border border-slate-300 p-3 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Graduation Year</label>
                    <input
                      type="number"
                      name="graduation_year"
                      value={formData.graduation_year}
                      onChange={handleChange}
                      placeholder="e.g. 2024"
                      className="w-full border border-slate-300 p-3 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Company / Organization</label>
                    <input
                      type="text"
                      name="company"
                      value={formData.company}
                      onChange={handleChange}
                      placeholder="e.g. Microsoft"
                      className="w-full border border-slate-300 p-3 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Job Role / Title</label>
                    <input
                      type="text"
                      name="job_role"
                      value={formData.job_role}
                      onChange={handleChange}
                      placeholder="e.g. Software Engineer"
                      className="w-full border border-slate-300 p-3 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Location</label>
                    <input
                      type="text"
                      name="location"
                      value={formData.location}
                      onChange={handleChange}
                      placeholder="e.g. Seattle, WA"
                      className="w-full border border-slate-300 p-3 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Skills</label>
                    <input
                      type="text"
                      name="skills"
                      value={formData.skills}
                      onChange={handleChange}
                      placeholder="e.g. Python, AWS, React"
                      className="w-full border border-slate-300 p-3 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
                    />
                  </div>

                  <div className="md:col-span-2">
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Bio</label>
                    <textarea
                      name="bio"
                      rows="2"
                      value={formData.bio}
                      onChange={handleChange}
                      placeholder="Brief summary..."
                      className="w-full border border-slate-300 p-3 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
                    ></textarea>
                  </div>

                  <div className="md:col-span-2 flex items-center gap-2">
                    <input
                      type="checkbox"
                      id="modal_mentor"
                      name="mentorship_available"
                      checked={formData.mentorship_available}
                      onChange={handleChange}
                      className="w-4 h-4 text-blue-600 rounded"
                    />
                    <label htmlFor="modal_mentor" className="text-xs font-medium text-slate-700 cursor-pointer">
                      Available for Student Mentorship
                    </label>
                  </div>
                </div>

                <div className="flex justify-end gap-3 pt-4 border-t border-slate-100">
                  <button
                    type="button"
                    onClick={() => setShowAddModal(false)}
                    className="px-5 py-2.5 rounded-xl text-slate-600 hover:bg-slate-100 text-sm font-semibold"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="px-6 py-2.5 rounded-xl bg-blue-700 hover:bg-blue-800 text-white text-sm font-semibold shadow-md"
                  >
                    Create Profile
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

export default Alumni;