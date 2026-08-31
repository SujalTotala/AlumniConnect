import { useEffect, useState } from "react";
import DashboardLayout from "../layouts/DashboardLayout";
import { opportunityApi } from "../api/opportunityApi";

const Opportunities = () => {
  const [opportunities, setOpportunities] = useState([]);
  const [loading, setLoading] = useState(true);
  const [typeFilter, setTypeFilter] = useState("");
  const [search, setSearch] = useState("");
  const [showPostModal, setShowPostModal] = useState(false);
  const [selectedOpp, setSelectedOpp] = useState(null);
  const [errorMsg, setErrorMsg] = useState("");
  const [successMsg, setSuccessMsg] = useState("");

  let currentUser = null;
  try {
    const userStr = localStorage.getItem("user");
    if (userStr) currentUser = JSON.parse(userStr);
  } catch (e) {
    console.error(e);
  }

  const role = currentUser?.role?.toLowerCase();
  const canPost = role === "admin" || role === "alumni";

  const [formData, setFormData] = useState({
    title: "",
    company: "",
    description: "",
    opportunity_type: "Full-Time Job",
    location: "",
    deadline: "",
    application_url: "",
  });

  const fetchOpportunities = async () => {
    setLoading(true);
    try {
      const params = {};
      if (typeFilter) params.opportunity_type = typeFilter;
      if (search) params.search = search;

      const res = await opportunityApi.getOpportunities(params);
      setOpportunities(res.data);
    } catch (err) {
      console.error("Failed to load opportunities:", err);
      setErrorMsg("Failed to load opportunities.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOpportunities();
  }, [typeFilter]);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    fetchOpportunities();
  };

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  const handlePostOpportunity = async (e) => {
    e.preventDefault();
    setErrorMsg("");
    setSuccessMsg("");

    try {
      await opportunityApi.createOpportunity(formData);
      setSuccessMsg("Opportunity successfully posted!");
      setShowPostModal(false);
      setFormData({
        title: "",
        company: "",
        description: "",
        opportunity_type: "Full-Time Job",
        location: "",
        deadline: "",
        application_url: "",
      });
      fetchOpportunities();
    } catch (err) {
      const detail = err.response?.data?.detail || "Failed to post opportunity.";
      setErrorMsg(detail);
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm("Are you sure you want to remove this opportunity?")) return;
    try {
      await opportunityApi.deleteOpportunity(id);
      setSuccessMsg("Opportunity deleted successfully.");
      if (selectedOpp?.id === id) setSelectedOpp(null);
      fetchOpportunities();
    } catch (err) {
      const detail = err.response?.data?.detail || "Failed to delete opportunity.";
      setErrorMsg(detail);
    }
  };

  return (
    <DashboardLayout>
      <div className="max-w-7xl mx-auto space-y-6">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h1 className="text-3xl font-extrabold text-slate-900">Career & Tech Opportunities</h1>
            <p className="text-sm text-slate-500 mt-1">
              Explore job openings, internships, referrals, and hackathons shared by alumni
            </p>
          </div>

          {canPost && (
            <button
              onClick={() => setShowPostModal(true)}
              className="bg-amber-600 hover:bg-amber-700 text-white px-5 py-2.5 rounded-xl font-semibold shadow-md transition flex items-center gap-2 self-start sm:self-auto"
            >
              <span>+ Post Opportunity</span>
            </button>
          )}
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

        {/* Search & Filter Bar */}
        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex flex-col md:flex-row gap-4 justify-between items-center">
          <form onSubmit={handleSearchSubmit} className="relative flex-1 w-full">
            <input
              type="text"
              placeholder="Search roles, companies, keywords..."
              className="w-full pl-11 pr-24 py-2.5 rounded-xl border border-slate-300 bg-slate-50 text-sm focus:bg-white focus:ring-2 focus:ring-amber-500 focus:outline-none"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
            <span className="absolute left-3.5 top-2.5 text-base text-slate-400">🔍</span>
            <button
              type="submit"
              className="absolute right-1.5 top-1.5 bg-amber-600 hover:bg-amber-700 text-white px-3 py-1.5 rounded-lg text-xs font-semibold"
            >
              Search
            </button>
          </form>

          <div className="flex items-center gap-3 w-full md:w-auto">
            <select
              value={typeFilter}
              onChange={(e) => setTypeFilter(e.target.value)}
              className="border border-slate-300 bg-slate-50 p-2.5 rounded-xl text-xs font-medium text-slate-700 focus:outline-none"
            >
              <option value="">All Opportunity Types</option>
              <option value="Internship">Internships</option>
              <option value="Full-Time Job">Full-Time Jobs</option>
              <option value="Referral">Referrals</option>
              <option value="Hackathon">Hackathons</option>
              <option value="Scholarship">Scholarships</option>
              <option value="Workshop">Workshops</option>
            </select>
          </div>
        </div>

        {/* Opportunities Grid */}
        {loading ? (
          <div className="text-center py-16 text-slate-500">
            <div className="inline-block animate-spin rounded-full h-8 w-8 border-4 border-amber-600 border-t-transparent mb-2"></div>
            <p className="text-sm">Loading opportunities...</p>
          </div>
        ) : opportunities.length === 0 ? (
          <div className="bg-white p-12 rounded-2xl border border-slate-200 text-center shadow-sm">
            <div className="text-4xl mb-3">💼</div>
            <h3 className="text-lg font-bold text-slate-800">No Opportunities Found</h3>
            <p className="text-xs text-slate-500 mt-1">Check back soon or post a new opening for the community.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {opportunities.map((opp) => (
              <div
                key={opp.id}
                className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200 hover:shadow-md transition flex flex-col justify-between"
              >
                <div>
                  <div className="flex justify-between items-start gap-2 mb-3">
                    <span className="bg-amber-50 text-amber-800 text-xs font-bold px-2.5 py-1 rounded-full border border-amber-200">
                      {opp.opportunity_type}
                    </span>
                    {opp.deadline && (
                      <span className="text-[11px] text-red-600 font-semibold bg-red-50 px-2 py-0.5 rounded-md">
                        Due: {opp.deadline}
                      </span>
                    )}
                  </div>

                  <h3 className="text-xl font-bold text-slate-900 mb-1 leading-tight">{opp.title}</h3>
                  <p className="text-sm font-semibold text-amber-700 mb-3">{opp.company}</p>
                  <p className="text-xs text-slate-600 line-clamp-3 mb-4">{opp.description}</p>

                  <div className="space-y-1 text-xs text-slate-500 bg-slate-50 p-3 rounded-xl border border-slate-100 mb-4">
                    {opp.location && <p>📍 {opp.location}</p>}
                    <p>👤 Posted by: <span className="font-semibold text-slate-700">{opp.poster_name}</span></p>
                  </div>
                </div>

                <div className="pt-4 border-t border-slate-100 flex justify-between items-center">
                  <div className="flex gap-2">
                    {opp.application_url ? (
                      <a
                        href={opp.application_url}
                        target="_blank"
                        rel="noreferrer"
                        className="bg-amber-600 hover:bg-amber-700 text-white px-4 py-2 rounded-xl text-xs font-semibold shadow-sm transition"
                      >
                        Apply Now ↗
                      </a>
                    ) : (
                      <button
                        onClick={() => setSelectedOpp(opp)}
                        className="bg-slate-100 hover:bg-slate-200 text-slate-800 px-4 py-2 rounded-xl text-xs font-semibold"
                      >
                        View Details
                      </button>
                    )}
                  </div>

                  {(role === "admin" || currentUser?.id === opp.posted_by) && (
                    <button
                      onClick={() => handleDelete(opp.id)}
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

        {/* Post Opportunity Modal */}
        {showPostModal && (
          <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4 backdrop-blur-sm">
            <div className="bg-white rounded-3xl p-8 max-w-2xl w-full shadow-2xl relative max-h-[90vh] overflow-y-auto">
              <button
                onClick={() => setShowPostModal(false)}
                className="absolute right-5 top-5 text-slate-400 hover:text-slate-700 text-xl font-bold"
              >
                ✕
              </button>

              <h2 className="text-2xl font-bold text-slate-900 mb-6 pb-2 border-b border-slate-100">
                Post Opportunity for Students & Alumni
              </h2>

              <form onSubmit={handlePostOpportunity} className="space-y-4">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Position / Role Title *</label>
                    <input
                      type="text"
                      name="title"
                      value={formData.title}
                      onChange={handleChange}
                      required
                      placeholder="e.g. Cloud Security Intern"
                      className="w-full border border-slate-300 p-3 rounded-xl text-sm focus:ring-2 focus:ring-amber-500 focus:outline-none"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Company / Organization *</label>
                    <input
                      type="text"
                      name="company"
                      value={formData.company}
                      onChange={handleChange}
                      required
                      placeholder="e.g. Acme Technologies"
                      className="w-full border border-slate-300 p-3 rounded-xl text-sm focus:ring-2 focus:ring-amber-500 focus:outline-none"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Opportunity Type</label>
                    <select
                      name="opportunity_type"
                      value={formData.opportunity_type}
                      onChange={handleChange}
                      className="w-full border border-slate-300 p-3 rounded-xl text-sm bg-white focus:ring-2 focus:ring-amber-500 focus:outline-none"
                    >
                      <option value="Internship">Internship</option>
                      <option value="Full-Time Job">Full-Time Job</option>
                      <option value="Referral">Referral</option>
                      <option value="Hackathon">Hackathon</option>
                      <option value="Scholarship">Scholarship</option>
                      <option value="Workshop">Workshop</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Location</label>
                    <input
                      type="text"
                      name="location"
                      value={formData.location}
                      onChange={handleChange}
                      placeholder="e.g. Remote / Boston, MA"
                      className="w-full border border-slate-300 p-3 rounded-xl text-sm focus:ring-2 focus:ring-amber-500 focus:outline-none"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Application Deadline</label>
                    <input
                      type="text"
                      name="deadline"
                      value={formData.deadline}
                      onChange={handleChange}
                      placeholder="e.g. Oct 31, 2026"
                      className="w-full border border-slate-300 p-3 rounded-xl text-sm focus:ring-2 focus:ring-amber-500 focus:outline-none"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Application Link / Portal URL</label>
                    <input
                      type="url"
                      name="application_url"
                      value={formData.application_url}
                      onChange={handleChange}
                      placeholder="https://careers.company.com/apply/..."
                      className="w-full border border-slate-300 p-3 rounded-xl text-sm focus:ring-2 focus:ring-amber-500 focus:outline-none"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-600 mb-1">Description & Requirements *</label>
                  <textarea
                    name="description"
                    rows="3"
                    value={formData.description}
                    onChange={handleChange}
                    required
                    placeholder="Responsibilities, required skills, eligibility..."
                    className="w-full border border-slate-300 p-3 rounded-xl text-sm focus:ring-2 focus:ring-amber-500 focus:outline-none"
                  ></textarea>
                </div>

                <div className="flex justify-end gap-3 pt-4 border-t border-slate-100">
                  <button
                    type="button"
                    onClick={() => setShowPostModal(false)}
                    className="px-5 py-2.5 rounded-xl text-slate-600 hover:bg-slate-100 text-sm font-semibold"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="px-6 py-2.5 rounded-xl bg-amber-600 hover:bg-amber-700 text-white text-sm font-semibold shadow-md"
                  >
                    Publish Opportunity
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

export default Opportunities;
