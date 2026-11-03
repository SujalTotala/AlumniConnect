import { useEffect, useState, useRef } from "react";
import DashboardLayout from "../layouts/DashboardLayout";
import { adminAlumniApi } from "../api/adminAlumniApi";
import { adminApi } from "../api/adminApi";

const AdminAlumniData = () => {
  const [activeTab, setActiveTab] = useState("quality"); // "quality", "import", "segments", "audit"
  const [errorMsg, setErrorMsg] = useState("");
  const [successMsg, setSuccessMsg] = useState("");

  // --- Quality & Cohort State ---
  const [loadingQuality, setLoadingQuality] = useState(false);
  const [qualityData, setQualityData] = useState(null);
  const [cohortStats, setCohortStats] = useState(null);

  // --- Import State ---
  const fileInputRef = useRef(null);
  const [uploadingFile, setUploadingFile] = useState(false);
  const [importPreview, setImportPreview] = useState(null);
  const [commitMode, setCommitMode] = useState("SKIP_EXISTING"); // "SKIP_EXISTING", "UPDATE_EXISTING", "CREATE_NEW_ONLY"
  const [committing, setCommitting] = useState(false);
  const [importHistory, setImportHistory] = useState([]);
  const [loadingHistory, setLoadingHistory] = useState(false);

  // --- Segments State ---
  const [segments, setSegments] = useState([]);
  const [loadingSegments, setLoadingSegments] = useState(false);
  const [showSegmentModal, setShowSegmentModal] = useState(false);
  const [segmentForm, setSegmentForm] = useState({
    name: "",
    description: "",
    departments: "",
    graduation_year_min: "",
    graduation_year_max: "",
    companies: "",
    skills: "",
    locations: "",
    mentorship_available: null,
    is_verified: null,
  });
  const [criteriaPreview, setCriteriaPreview] = useState(null);
  const [previewingCriteria, setPreviewingCriteria] = useState(false);
  const [savingSegment, setSavingSegment] = useState(false);

  // Member preview modal
  const [selectedSegment, setSelectedSegment] = useState(null);
  const [segmentMembers, setSegmentMembers] = useState([]);
  const [loadingMembers, setLoadingMembers] = useState(false);
  const [memberPage, setMemberPage] = useState(0);
  const [totalMembers, setTotalMembers] = useState(0);

  // --- Audit Log State ---
  const [auditLogs, setAuditLogs] = useState([]);
  const [loadingLogs, setLoadingLogs] = useState(false);
  const [actionFilter, setActionFilter] = useState("");
  const [targetFilter, setTargetFilter] = useState("");
  const [selectedLogDetails, setSelectedLogDetails] = useState(null);

  // -------------------------------------------------------------
  // Data Fetching
  // -------------------------------------------------------------
  const fetchQualityData = async () => {
    setLoadingQuality(true);
    try {
      const [qRes, statsRes] = await Promise.all([
        adminAlumniApi.getDataQuality(),
        adminApi.getStatistics(),
      ]);
      setQualityData(qRes.data);
      setCohortStats(statsRes.data?.cohort_analytics || {});
    } catch (err) {
      console.error(err);
      setErrorMsg("Failed to load data quality metrics.");
    } finally {
      setLoadingQuality(false);
    }
  };

  const fetchImportHistory = async () => {
    setLoadingHistory(true);
    try {
      const res = await adminAlumniApi.getImportHistory();
      setImportHistory(res.data || []);
    } catch (err) {
      console.error(err);
    } finally {
      setLoadingHistory(false);
    }
  };

  const fetchSegments = async () => {
    setLoadingSegments(true);
    try {
      const res = await adminAlumniApi.getSegments();
      setSegments(res.data || []);
    } catch (err) {
      console.error(err);
      setErrorMsg("Failed to load alumni segments.");
    } finally {
      setLoadingSegments(false);
    }
  };

  const fetchAuditLogs = async () => {
    setLoadingLogs(true);
    try {
      const params = {};
      if (actionFilter) params.action = actionFilter;
      if (targetFilter) params.target_type = targetFilter;
      const res = await adminAlumniApi.getAuditLogs(params);
      setAuditLogs(res.data || []);
    } catch (err) {
      console.error(err);
      setErrorMsg("Failed to load audit logs.");
    } finally {
      setLoadingLogs(false);
    }
  };

  useEffect(() => {
    if (activeTab === "quality") {
      fetchQualityData();
    } else if (activeTab === "import") {
      fetchImportHistory();
    } else if (activeTab === "segments") {
      fetchSegments();
    } else if (activeTab === "audit") {
      fetchAuditLogs();
    }
  }, [activeTab, actionFilter, targetFilter]);

  // -------------------------------------------------------------
  // Bulk Import Handlers
  // -------------------------------------------------------------
  const handleDownloadTemplate = async () => {
    try {
      const res = await adminAlumniApi.downloadTemplate();
      const blob = new Blob([res.data], { type: "text/csv" });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = "alumni_import_template.csv";
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error(err);
      setErrorMsg("Failed to download CSV template.");
    }
  };

  const handleFileUpload = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setUploadingFile(true);
    setErrorMsg("");
    setSuccessMsg("");
    try {
      const res = await adminAlumniApi.previewImport(file);
      setImportPreview(res.data);
      setSuccessMsg(`CSV parsed successfully: ${res.data.total_rows} records validated.`);
    } catch (err) {
      console.error(err);
      setErrorMsg(err.response?.data?.detail || "Failed to parse CSV file.");
    } finally {
      setUploadingFile(false);
      if (fileInputRef.current) fileInputRef.current.value = "";
    }
  };

  const handleCommitImport = async () => {
    if (!importPreview?.import_session_id) return;
    setCommitting(true);
    setErrorMsg("");
    setSuccessMsg("");
    try {
      const res = await adminAlumniApi.commitImport(importPreview.import_session_id, commitMode);
      setSuccessMsg(
        `Import committed successfully! Created: ${res.data.created_count}, Updated: ${res.data.updated_count}, Skipped: ${res.data.skipped_count}`
      );
      setImportPreview(null);
      fetchImportHistory();
    } catch (err) {
      console.error(err);
      setErrorMsg(err.response?.data?.detail || "Import commit failed.");
    } finally {
      setCommitting(false);
    }
  };

  // -------------------------------------------------------------
  // Segment Handlers
  // -------------------------------------------------------------
  const buildCriteriaPayload = () => {
    const filters = {};
    if (segmentForm.departments) {
      filters.departments = segmentForm.departments.split(",").map((d) => d.trim()).filter(Boolean);
    }
    if (segmentForm.graduation_year_min) {
      filters.graduation_year_min = segmentForm.graduation_year_min.trim();
    }
    if (segmentForm.graduation_year_max) {
      filters.graduation_year_max = segmentForm.graduation_year_max.trim();
    }
    if (segmentForm.companies) {
      filters.companies = segmentForm.companies.split(",").map((c) => c.trim()).filter(Boolean);
    }
    if (segmentForm.skills) {
      filters.skills = segmentForm.skills.split(",").map((s) => s.trim()).filter(Boolean);
    }
    if (segmentForm.locations) {
      filters.locations = segmentForm.locations.split(",").map((l) => l.trim()).filter(Boolean);
    }
    if (segmentForm.mentorship_available !== null && segmentForm.mentorship_available !== "") {
      filters.mentorship_available = segmentForm.mentorship_available === "true";
    }
    if (segmentForm.is_verified !== null && segmentForm.is_verified !== "") {
      filters.is_verified = segmentForm.is_verified === "true";
    }
    return filters;
  };

  const handlePreviewCriteria = async () => {
    setPreviewingCriteria(true);
    try {
      const filters = buildCriteriaPayload();
      const res = await adminAlumniApi.previewSegmentCriteria(filters);
      setCriteriaPreview(res.data);
    } catch (err) {
      console.error(err);
      setErrorMsg("Failed to preview segment matching.");
    } finally {
      setPreviewingCriteria(false);
    }
  };

  const handleSaveSegment = async (e) => {
    e.preventDefault();
    if (!segmentForm.name.trim()) return;

    setSavingSegment(true);
    setErrorMsg("");
    setSuccessMsg("");
    try {
      const filters = buildCriteriaPayload();
      await adminAlumniApi.createSegment({
        name: segmentForm.name.trim(),
        description: segmentForm.description.trim() || undefined,
        filters,
      });
      setSuccessMsg(`Segment '${segmentForm.name}' saved successfully!`);
      setShowSegmentModal(false);
      setCriteriaPreview(null);
      setSegmentForm({
        name: "",
        description: "",
        departments: "",
        graduation_year_min: "",
        graduation_year_max: "",
        companies: "",
        skills: "",
        locations: "",
        mentorship_available: null,
        is_verified: null,
      });
      fetchSegments();
    } catch (err) {
      console.error(err);
      setErrorMsg(err.response?.data?.detail || "Failed to create segment.");
    } finally {
      setSavingSegment(false);
    }
  };

  const handleDeleteSegment = async (id, name) => {
    if (!window.confirm(`Are you sure you want to delete segment '${name}'?`)) return;
    try {
      await adminAlumniApi.deleteSegment(id);
      setSuccessMsg(`Segment '${name}' deleted.`);
      fetchSegments();
    } catch (err) {
      console.error(err);
      setErrorMsg("Failed to delete segment.");
    }
  };

  const handleExportSegment = async (id, name) => {
    try {
      const res = await adminAlumniApi.exportSegmentCsv(id);
      const blob = new Blob([res.data], { type: "text/csv" });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `segment_${name.toLowerCase().replace(/\s+/g, "_")}.csv`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error(err);
      setErrorMsg("Failed to export segment CSV.");
    }
  };

  const handleViewSegmentMembers = async (segment, page = 0) => {
    setSelectedSegment(segment);
    setMemberPage(page);
    setLoadingMembers(true);
    try {
      const res = await adminAlumniApi.getSegmentMembers(segment.id, {
        limit: 20,
        offset: page * 20,
      });
      setSegmentMembers(res.data.members || []);
      setTotalMembers(res.data.total_count || 0);
    } catch (err) {
      console.error(err);
      setErrorMsg("Failed to load segment members.");
    } finally {
      setLoadingMembers(false);
    }
  };

  return (
    <DashboardLayout>
      <div className="space-y-6 pb-12">
        {/* Top Header */}
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-white p-6 rounded-3xl border border-slate-200 shadow-xs">
          <div>
            <div className="flex items-center gap-2">
              <span className="text-2xl">🗂️</span>
              <h1 className="text-2xl font-bold text-slate-900">Alumni Data Management</h1>
            </div>
            <p className="text-xs text-slate-500 mt-1">
              Bulk imports, dynamic institutional segments, data quality health checks, and administrative audit trails.
            </p>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={handleDownloadTemplate}
              className="flex items-center gap-2 px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-semibold rounded-xl transition"
            >
              <span>📥</span> Download CSV Template
            </button>
            <button
              onClick={() => {
                if (activeTab === "quality") fetchQualityData();
                if (activeTab === "import") fetchImportHistory();
                if (activeTab === "segments") fetchSegments();
                if (activeTab === "audit") fetchAuditLogs();
              }}
              className="px-3.5 py-2 bg-blue-50 text-blue-600 hover:bg-blue-100 text-xs font-semibold rounded-xl transition"
              title="Refresh Current Tab"
            >
              ↻ Refresh
            </button>
          </div>
        </div>

        {/* Global Feedback Banners */}
        {errorMsg && (
          <div className="p-4 bg-rose-50 border border-rose-200 rounded-2xl text-xs font-medium text-rose-700 flex justify-between items-center animate-fade-in">
            <span>⚠️ {errorMsg}</span>
            <button onClick={() => setErrorMsg("")} className="font-bold text-rose-500 hover:text-rose-700">✕</button>
          </div>
        )}
        {successMsg && (
          <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-2xl text-xs font-medium text-emerald-700 flex justify-between items-center animate-fade-in">
            <span>✓ {successMsg}</span>
            <button onClick={() => setSuccessMsg("")} className="font-bold text-emerald-500 hover:text-emerald-700">✕</button>
          </div>
        )}

        {/* Tab Navigation */}
        <div className="flex border-b border-slate-200 gap-2 overflow-x-auto pb-1">
          <button
            onClick={() => setActiveTab("quality")}
            className={`flex items-center gap-2 px-5 py-3 text-xs font-bold rounded-t-2xl transition border-b-2 ${
              activeTab === "quality"
                ? "border-blue-600 text-blue-600 bg-blue-50/50"
                : "border-transparent text-slate-500 hover:text-slate-800 hover:bg-slate-50"
            }`}
          >
            <span>📊</span> Data Quality & Cohorts
          </button>
          <button
            onClick={() => setActiveTab("import")}
            className={`flex items-center gap-2 px-5 py-3 text-xs font-bold rounded-t-2xl transition border-b-2 ${
              activeTab === "import"
                ? "border-blue-600 text-blue-600 bg-blue-50/50"
                : "border-transparent text-slate-500 hover:text-slate-800 hover:bg-slate-50"
            }`}
          >
            <span>📤</span> Bulk CSV Import
          </button>
          <button
            onClick={() => setActiveTab("segments")}
            className={`flex items-center gap-2 px-5 py-3 text-xs font-bold rounded-t-2xl transition border-b-2 ${
              activeTab === "segments"
                ? "border-blue-600 text-blue-600 bg-blue-50/50"
                : "border-transparent text-slate-500 hover:text-slate-800 hover:bg-slate-50"
            }`}
          >
            <span>🎯</span> Saved Segments
          </button>
          <button
            onClick={() => setActiveTab("audit")}
            className={`flex items-center gap-2 px-5 py-3 text-xs font-bold rounded-t-2xl transition border-b-2 ${
              activeTab === "audit"
                ? "border-blue-600 text-blue-600 bg-blue-50/50"
                : "border-transparent text-slate-500 hover:text-slate-800 hover:bg-slate-50"
            }`}
          >
            <span>🛡️</span> Admin Audit Log
          </button>
        </div>

        {/* ========================================================= */}
        {/* TAB 1: DATA QUALITY & COHORTS                             */}
        {/* ========================================================= */}
        {activeTab === "quality" && (
          <div className="space-y-6">
            {loadingQuality ? (
              <div className="text-center py-16 bg-white rounded-3xl border border-slate-200">
                <span className="text-3xl animate-spin inline-block">⏳</span>
                <p className="text-xs text-slate-500 mt-2 font-medium">Computing institutional data quality metrics...</p>
              </div>
            ) : qualityData ? (
              <>
                {/* Metric Summary Cards */}
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                  <div className="bg-white p-5 rounded-3xl border border-slate-200 shadow-xs space-y-1">
                    <p className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">Total Alumni Profiles</p>
                    <p className="text-3xl font-extrabold text-slate-900">{qualityData.total_alumni}</p>
                    <p className="text-xs text-slate-500">Institutional record count</p>
                  </div>

                  <div className="bg-white p-5 rounded-3xl border border-slate-200 shadow-xs space-y-1">
                    <p className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">Verification Status</p>
                    <div className="flex items-baseline gap-2">
                      <p className="text-3xl font-extrabold text-emerald-600">{qualityData.verified_count}</p>
                      <span className="text-xs text-slate-400">/ {qualityData.total_alumni} verified</span>
                    </div>
                    <p className="text-xs text-slate-500">{qualityData.unverified_count} pending verification</p>
                  </div>

                  <div className="bg-white p-5 rounded-3xl border border-slate-200 shadow-xs space-y-1">
                    <p className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">Average Profile Completion</p>
                    <p className="text-3xl font-extrabold text-blue-600">{qualityData.average_completion_percentage}%</p>
                    <div className="w-full bg-slate-100 rounded-full h-2 mt-1">
                      <div
                        className="bg-blue-600 h-2 rounded-full transition-all duration-500"
                        style={{ width: `${qualityData.average_completion_percentage}%` }}
                      ></div>
                    </div>
                  </div>

                  <div className="bg-white p-5 rounded-3xl border border-slate-200 shadow-xs space-y-1">
                    <p className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">Potential Duplicate Candidates</p>
                    <p className="text-3xl font-extrabold text-amber-600">{qualityData.duplicate_candidates_count}</p>
                    <p className="text-xs text-slate-500">Matching name & grad year</p>
                  </div>
                </div>

                {/* Profile Completion Distribution & Missing Fields */}
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                  {/* Completion Distribution */}
                  <div className="bg-white p-6 rounded-3xl border border-slate-200 shadow-xs space-y-4">
                    <h3 className="text-sm font-bold text-slate-900">Profile Completion Distribution</h3>
                    <div className="space-y-3">
                      {Object.entries(qualityData.completion_distribution || {}).map(([range, count]) => {
                        const pct = qualityData.total_alumni ? Math.round((count / qualityData.total_alumni) * 100) : 0;
                        return (
                          <div key={range} className="space-y-1">
                            <div className="flex justify-between text-xs font-semibold text-slate-700">
                              <span>Tier: {range}</span>
                              <span>{count} alumni ({pct}%)</span>
                            </div>
                            <div className="w-full bg-slate-100 rounded-full h-2.5">
                              <div
                                className="bg-gradient-to-r from-blue-500 to-indigo-600 h-2.5 rounded-full transition-all duration-500"
                                style={{ width: `${pct}%` }}
                              ></div>
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  </div>

                  {/* Missing Fields Breakdown */}
                  <div className="bg-white p-6 rounded-3xl border border-slate-200 shadow-xs space-y-4">
                    <h3 className="text-sm font-bold text-slate-900">Data Completeness by Field</h3>
                    <div className="space-y-2.5">
                      {Object.entries(qualityData.missing_fields_count || {}).map(([field, missingCount]) => {
                        const filledCount = qualityData.total_alumni - missingCount;
                        const filledPct = qualityData.total_alumni
                          ? Math.round((filledCount / qualityData.total_alumni) * 100)
                          : 0;
                        return (
                          <div key={field} className="flex items-center justify-between text-xs">
                            <span className="font-semibold text-slate-700 capitalize w-36 truncate">
                              {field.replace(/_/g, " ")}
                            </span>
                            <div className="flex-1 mx-3 bg-slate-100 rounded-full h-2">
                              <div
                                className={`h-2 rounded-full ${
                                  filledPct > 80
                                    ? "bg-emerald-500"
                                    : filledPct > 50
                                    ? "bg-blue-500"
                                    : "bg-amber-500"
                                }`}
                                style={{ width: `${filledPct}%` }}
                              ></div>
                            </div>
                            <span className="text-slate-400 font-mono text-[11px] w-20 text-right">
                              {filledPct}% filled
                            </span>
                          </div>
                        );
                      })}
                    </div>
                  </div>
                </div>

                {/* Cohort Analytics Breakdown */}
                {cohortStats && Object.keys(cohortStats).length > 0 && (
                  <div className="bg-white p-6 rounded-3xl border border-slate-200 shadow-xs space-y-4">
                    <h3 className="text-sm font-bold text-slate-900">Cohort Analytics by Graduation Year</h3>
                    <div className="overflow-x-auto">
                      <table className="w-full text-left text-xs border-collapse">
                        <thead>
                          <tr className="border-b border-slate-100 text-slate-400 font-bold uppercase text-[10px]">
                            <th className="py-2.5 px-3">Cohort Year</th>
                            <th className="py-2.5 px-3">Total Alumni</th>
                            <th className="py-2.5 px-3">Verified Alumni</th>
                            <th className="py-2.5 px-3">Active Mentors</th>
                            <th className="py-2.5 px-3">Verification Rate</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100 text-slate-700 font-medium">
                          {Object.entries(cohortStats).map(([year, c]) => {
                            const vRate = c.total_alumni ? Math.round((c.verified_alumni / c.total_alumni) * 100) : 0;
                            return (
                              <tr key={year} className="hover:bg-slate-50/50">
                                <td className="py-2.5 px-3 font-bold text-slate-900">{year}</td>
                                <td className="py-2.5 px-3">{c.total_alumni}</td>
                                <td className="py-2.5 px-3 text-emerald-600 font-semibold">{c.verified_alumni}</td>
                                <td className="py-2.5 px-3 text-blue-600 font-semibold">{c.active_mentors}</td>
                                <td className="py-2.5 px-3">
                                  <span
                                    className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                                      vRate >= 75
                                        ? "bg-emerald-100 text-emerald-800"
                                        : vRate >= 40
                                        ? "bg-blue-100 text-blue-800"
                                        : "bg-slate-100 text-slate-700"
                                    }`}
                                  >
                                    {vRate}%
                                  </span>
                                </td>
                              </tr>
                            );
                          })}
                        </tbody>
                      </table>
                    </div>
                  </div>
                )}
              </>
            ) : null}
          </div>
        )}

        {/* ========================================================= */}
        {/* TAB 2: BULK CSV IMPORT                                    */}
        {/* ========================================================= */}
        {activeTab === "import" && (
          <div className="space-y-6">
            {/* Upload & Instructions Card */}
            <div className="bg-white p-6 rounded-3xl border border-slate-200 shadow-xs space-y-4">
              <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
                <div>
                  <h2 className="text-base font-bold text-slate-900">Upload Alumni Directory CSV</h2>
                  <p className="text-xs text-slate-500 mt-0.5">
                    Upload an institutional alumni roster. You will be able to review duplicate detection and resolution modes before committing.
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  <input
                    type="file"
                    ref={fileInputRef}
                    accept=".csv"
                    onChange={handleFileUpload}
                    className="hidden"
                    id="csv-upload-input"
                  />
                  <label
                    htmlFor="csv-upload-input"
                    className={`px-5 py-2.5 bg-blue-600 hover:bg-blue-700 text-white text-xs font-bold rounded-xl cursor-pointer shadow-md transition ${
                      uploadingFile ? "opacity-50 pointer-events-none" : ""
                    }`}
                  >
                    {uploadingFile ? "Parsing File..." : "📤 Select CSV File"}
                  </label>
                </div>
              </div>

              <div className="bg-slate-50 p-4 rounded-2xl border border-slate-200 text-xs text-slate-600 space-y-1">
                <p className="font-bold text-slate-800">CSV Guidelines:</p>
                <ul className="list-disc list-inside space-y-0.5 text-slate-500 text-[11px]">
                  <li>Headers required: <code className="bg-white px-1 py-0.5 rounded border border-slate-200 text-blue-600 font-mono">name</code>, <code className="bg-white px-1 py-0.5 rounded border border-slate-200 text-blue-600 font-mono">email</code></li>
                  <li>Optional: <code className="bg-white px-1 py-0.5 rounded border border-slate-200 font-mono">graduation_year</code>, <code className="bg-white px-1 py-0.5 rounded border border-slate-200 font-mono">department</code>, <code className="bg-white px-1 py-0.5 rounded border border-slate-200 font-mono">company</code>, <code className="bg-white px-1 py-0.5 rounded border border-slate-200 font-mono">job_role</code>, <code className="bg-white px-1 py-0.5 rounded border border-slate-200 font-mono">location</code>, <code className="bg-white px-1 py-0.5 rounded border border-slate-200 font-mono">skills</code>, <code className="bg-white px-1 py-0.5 rounded border border-slate-200 font-mono">bio</code>, <code className="bg-white px-1 py-0.5 rounded border border-slate-200 font-mono">linkedin_url</code></li>
                  <li>Safe update mode will never overwrite existing credentials, roles, or verification status.</li>
                </ul>
              </div>
            </div>

            {/* Live Preview Modal / Card */}
            {importPreview && (
              <div className="bg-white p-6 rounded-3xl border-2 border-blue-500 shadow-xl space-y-6">
                <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 border-b border-slate-100 pb-4">
                  <div>
                    <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-blue-100 text-blue-800 uppercase">
                      Session #{importPreview.import_session_id.slice(0, 8)}
                    </span>
                    <h3 className="text-lg font-bold text-slate-900 mt-1">
                      Import Preview: {importPreview.file_name}
                    </h3>
                  </div>

                  <div className="flex flex-wrap gap-2">
                    <span className="px-3 py-1 bg-slate-100 text-slate-700 text-xs font-semibold rounded-lg">
                      Total: {importPreview.total_rows}
                    </span>
                    <span className="px-3 py-1 bg-emerald-100 text-emerald-800 text-xs font-semibold rounded-lg">
                      Valid New: {importPreview.valid_rows}
                    </span>
                    <span className="px-3 py-1 bg-amber-100 text-amber-800 text-xs font-semibold rounded-lg">
                      Exact Dups: {importPreview.exact_duplicates}
                    </span>
                    <span className="px-3 py-1 bg-orange-100 text-orange-800 text-xs font-semibold rounded-lg">
                      Possible Dups: {importPreview.possible_duplicates}
                    </span>
                    {importPreview.invalid_rows > 0 && (
                      <span className="px-3 py-1 bg-rose-100 text-rose-800 text-xs font-semibold rounded-lg">
                        Invalid: {importPreview.invalid_rows}
                      </span>
                    )}
                  </div>
                </div>

                {/* Resolution Mode Selection */}
                <div className="space-y-2">
                  <label className="text-xs font-bold text-slate-800 block">Select Resolution Strategy:</label>
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                    <div
                      onClick={() => setCommitMode("SKIP_EXISTING")}
                      className={`p-3.5 rounded-2xl border cursor-pointer transition ${
                        commitMode === "SKIP_EXISTING"
                          ? "border-blue-600 bg-blue-50/50 shadow-xs"
                          : "border-slate-200 hover:bg-slate-50"
                      }`}
                    >
                      <p className="font-bold text-xs text-slate-900">1. Skip Existing (Recommended)</p>
                      <p className="text-[11px] text-slate-500 mt-0.5">
                        Ignore duplicate email profiles and only insert valid, new alumni records.
                      </p>
                    </div>

                    <div
                      onClick={() => setCommitMode("UPDATE_EXISTING")}
                      className={`p-3.5 rounded-2xl border cursor-pointer transition ${
                        commitMode === "UPDATE_EXISTING"
                          ? "border-blue-600 bg-blue-50/50 shadow-xs"
                          : "border-slate-200 hover:bg-slate-50"
                      }`}
                    >
                      <p className="font-bold text-xs text-slate-900">2. Safe Update Existing</p>
                      <p className="text-[11px] text-slate-500 mt-0.5">
                        Update existing profiles with non-empty CSV values without modifying verification or passwords.
                      </p>
                    </div>

                    <div
                      onClick={() => setCommitMode("CREATE_NEW_ONLY")}
                      className={`p-3.5 rounded-2xl border cursor-pointer transition ${
                        commitMode === "CREATE_NEW_ONLY"
                          ? "border-blue-600 bg-blue-50/50 shadow-xs"
                          : "border-slate-200 hover:bg-slate-50"
                      }`}
                    >
                      <p className="font-bold text-xs text-slate-900">3. Create New Only</p>
                      <p className="text-[11px] text-slate-500 mt-0.5">
                        Fails if duplicate records exist. Guarantees zero duplicate side effects.
                      </p>
                    </div>
                  </div>
                </div>

                {/* Preview Table */}
                <div className="max-h-72 overflow-y-auto rounded-2xl border border-slate-200">
                  <table className="w-full text-left text-xs border-collapse">
                    <thead className="bg-slate-50 text-slate-500 font-bold uppercase text-[10px] sticky top-0">
                      <tr>
                        <th className="py-2.5 px-3">Row</th>
                        <th className="py-2.5 px-3">Name</th>
                        <th className="py-2.5 px-3">Email</th>
                        <th className="py-2.5 px-3">Grad Year</th>
                        <th className="py-2.5 px-3">Department</th>
                        <th className="py-2.5 px-3">Status</th>
                        <th className="py-2.5 px-3">Notes / Reason</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 text-slate-700">
                      {importPreview.preview_rows.map((row) => (
                        <tr key={row.row_number} className="hover:bg-slate-50/50">
                          <td className="py-2 px-3 font-mono text-slate-400">#{row.row_number}</td>
                          <td className="py-2 px-3 font-semibold text-slate-900">{row.name}</td>
                          <td className="py-2 px-3">{row.email}</td>
                          <td className="py-2 px-3">{row.graduation_year || "—"}</td>
                          <td className="py-2 px-3">{row.department || "—"}</td>
                          <td className="py-2 px-3">
                            <span
                              className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                                row.status === "VALID"
                                  ? "bg-emerald-100 text-emerald-800"
                                  : row.status === "EXACT_DUPLICATE"
                                  ? "bg-amber-100 text-amber-800"
                                  : row.status === "POSSIBLE_DUPLICATE"
                                  ? "bg-orange-100 text-orange-800"
                                  : "bg-rose-100 text-rose-800"
                              }`}
                            >
                              {row.status.replace(/_/g, " ")}
                            </span>
                          </td>
                          <td className="py-2 px-3 text-slate-500 text-[11px]">
                            {row.errors?.length > 0
                              ? row.errors.join(", ")
                              : row.duplicate_reason || "Ready for commit"}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>

                {/* Commit Action Buttons */}
                <div className="flex justify-end gap-3 pt-2">
                  <button
                    onClick={() => setImportPreview(null)}
                    className="px-4 py-2 rounded-xl text-slate-600 hover:text-slate-900 text-xs font-semibold"
                  >
                    Cancel Preview
                  </button>
                  <button
                    onClick={handleCommitImport}
                    disabled={committing || (commitMode === "CREATE_NEW_ONLY" && (importPreview.exact_duplicates > 0 || importPreview.possible_duplicates > 0))}
                    className="px-6 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold shadow-md disabled:bg-slate-300 disabled:cursor-not-allowed transition"
                  >
                    {committing ? "Committing Transactions..." : `✓ Commit Import (${commitMode})`}
                  </button>
                </div>
              </div>
            )}

            {/* Import Job History */}
            <div className="bg-white p-6 rounded-3xl border border-slate-200 shadow-xs space-y-4">
              <h3 className="text-sm font-bold text-slate-900">Import Session History</h3>
              {loadingHistory ? (
                <p className="text-xs text-slate-400 py-4 text-center">Loading import jobs...</p>
              ) : importHistory.length === 0 ? (
                <p className="text-xs text-slate-400 py-4 text-center">No previous bulk imports recorded.</p>
              ) : (
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs border-collapse">
                    <thead className="border-b border-slate-100 text-slate-400 font-bold uppercase text-[10px]">
                      <tr>
                        <th className="py-2.5 px-3">Session ID</th>
                        <th className="py-2.5 px-3">Filename</th>
                        <th className="py-2.5 px-3">Total</th>
                        <th className="py-2.5 px-3">Created</th>
                        <th className="py-2.5 px-3">Updated</th>
                        <th className="py-2.5 px-3">Skipped</th>
                        <th className="py-2.5 px-3">Mode</th>
                        <th className="py-2.5 px-3">Status</th>
                        <th className="py-2.5 px-3">Date</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 text-slate-700">
                      {importHistory.map((job) => (
                        <tr key={job.id} className="hover:bg-slate-50/50">
                          <td className="py-2.5 px-3 font-mono text-slate-500">#{job.import_session_id.slice(0, 8)}</td>
                          <td className="py-2.5 px-3 font-semibold text-slate-900">{job.file_name}</td>
                          <td className="py-2.5 px-3">{job.total_rows}</td>
                          <td className="py-2.5 px-3 text-emerald-600 font-semibold">{job.created_count}</td>
                          <td className="py-2.5 px-3 text-blue-600 font-semibold">{job.updated_count}</td>
                          <td className="py-2.5 px-3 text-slate-400">{job.skipped_count}</td>
                          <td className="py-2.5 px-3 text-slate-500 text-[10px] uppercase">{job.mode || "—"}</td>
                          <td className="py-2.5 px-3">
                            <span
                              className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                                job.status === "COMMITTED"
                                  ? "bg-emerald-100 text-emerald-800"
                                  : job.status === "PREVIEWED"
                                  ? "bg-blue-100 text-blue-800"
                                  : "bg-rose-100 text-rose-800"
                              }`}
                            >
                              {job.status}
                            </span>
                          </td>
                          <td className="py-2.5 px-3 text-slate-400 text-[11px]">
                            {new Date(job.created_at).toLocaleString()}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>
        )}

        {/* ========================================================= */}
        {/* TAB 3: SAVED SEGMENTS                                     */}
        {/* ========================================================= */}
        {activeTab === "segments" && (
          <div className="space-y-6">
            <div className="flex justify-between items-center bg-white p-6 rounded-3xl border border-slate-200 shadow-xs">
              <div>
                <h2 className="text-base font-bold text-slate-900">Dynamic Alumni Segments</h2>
                <p className="text-xs text-slate-500 mt-0.5">
                  Save reusable criteria queries for targeted announcements, reports, and member campaigns.
                </p>
              </div>
              <button
                onClick={() => setShowSegmentModal(true)}
                className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white text-xs font-bold rounded-xl shadow-xs transition"
              >
                + Create Saved Segment
              </button>
            </div>

            {loadingSegments ? (
              <p className="text-xs text-slate-400 py-8 text-center">Loading segments...</p>
            ) : segments.length === 0 ? (
              <div className="text-center py-16 bg-white rounded-3xl border border-dashed border-slate-200">
                <span className="text-3xl">🎯</span>
                <p className="text-sm font-semibold text-slate-700 mt-2">No Segments Saved</p>
                <p className="text-xs text-slate-400">Create dynamic queries based on department, year, company, or skills.</p>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {segments.map((seg) => (
                  <div key={seg.id} className="bg-white p-5 rounded-3xl border border-slate-200 shadow-xs flex flex-col justify-between space-y-4">
                    <div className="space-y-2">
                      <div className="flex justify-between items-start gap-2">
                        <h3 className="font-bold text-slate-900 text-base">{seg.name}</h3>
                        <span className="px-2.5 py-1 bg-blue-50 text-blue-700 text-xs font-extrabold rounded-xl shrink-0">
                          {seg.estimated_count} Alumni
                        </span>
                      </div>
                      {seg.description && <p className="text-xs text-slate-500">{seg.description}</p>}

                      {/* Filter summary badges */}
                      <div className="flex flex-wrap gap-1.5 pt-1">
                        {seg.filters?.departments && (
                          <span className="text-[10px] bg-slate-100 text-slate-600 px-2 py-0.5 rounded-md font-mono">
                            Dept: {seg.filters.departments.join(", ")}
                          </span>
                        )}
                        {seg.filters?.graduation_year_min && (
                          <span className="text-[10px] bg-slate-100 text-slate-600 px-2 py-0.5 rounded-md font-mono">
                            Year &gt;= {seg.filters.graduation_year_min}
                          </span>
                        )}
                        {seg.filters?.graduation_year_max && (
                          <span className="text-[10px] bg-slate-100 text-slate-600 px-2 py-0.5 rounded-md font-mono">
                            Year &lt;= {seg.filters.graduation_year_max}
                          </span>
                        )}
                        {seg.filters?.companies && (
                          <span className="text-[10px] bg-slate-100 text-slate-600 px-2 py-0.5 rounded-md font-mono">
                            Co: {seg.filters.companies.join(", ")}
                          </span>
                        )}
                        {seg.filters?.mentorship_available && (
                          <span className="text-[10px] bg-purple-50 text-purple-700 px-2 py-0.5 rounded-md font-bold">
                            Mentors Only
                          </span>
                        )}
                        {seg.filters?.is_verified && (
                          <span className="text-[10px] bg-emerald-50 text-emerald-700 px-2 py-0.5 rounded-md font-bold">
                            Verified Only
                          </span>
                        )}
                      </div>
                    </div>

                    <div className="flex items-center justify-between pt-3 border-t border-slate-100 text-xs font-semibold">
                      <div className="flex items-center gap-2">
                        <button
                          onClick={() => handleViewSegmentMembers(seg, 0)}
                          className="text-blue-600 hover:text-blue-800"
                        >
                          View Members
                        </button>
                        <span className="text-slate-300">•</span>
                        <button
                          onClick={() => handleExportSegment(seg.id, seg.name)}
                          className="text-slate-600 hover:text-slate-900"
                        >
                          Export CSV
                        </button>
                      </div>
                      <button
                        onClick={() => handleDeleteSegment(seg.id, seg.name)}
                        className="text-rose-500 hover:text-rose-700 text-xs"
                      >
                        Delete
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}

            {/* Create Segment Modal */}
            {showSegmentModal && (
              <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4 backdrop-blur-sm">
                <div className="bg-white rounded-3xl p-6 sm:p-8 max-w-xl w-full shadow-2xl relative max-h-[90vh] overflow-y-auto">
                  <button
                    onClick={() => {
                      setShowSegmentModal(false);
                      setCriteriaPreview(null);
                    }}
                    className="absolute right-5 top-5 text-slate-400 hover:text-slate-700 text-xl font-bold"
                  >
                    ✕
                  </button>

                  <h3 className="text-lg font-bold text-slate-900">Create Dynamic Alumni Segment</h3>
                  <p className="text-xs text-slate-500 mb-5">
                    Define criteria filters. Members dynamically match in real time based on active database records.
                  </p>

                  <form onSubmit={handleSaveSegment} className="space-y-4 text-xs">
                    <div>
                      <label className="block font-semibold text-slate-700 mb-1">Segment Name *</label>
                      <input
                        type="text"
                        required
                        placeholder="e.g. Bay Area CS Alumni (2020-2024)"
                        value={segmentForm.name}
                        onChange={(e) => setSegmentForm({ ...segmentForm, name: e.target.value })}
                        className="w-full border border-slate-300 p-2.5 rounded-xl focus:ring-2 focus:ring-blue-500 focus:outline-none"
                      />
                    </div>

                    <div>
                      <label className="block font-semibold text-slate-700 mb-1">Description (Optional)</label>
                      <input
                        type="text"
                        placeholder="Brief summary of audience intent"
                        value={segmentForm.description}
                        onChange={(e) => setSegmentForm({ ...segmentForm, description: e.target.value })}
                        className="w-full border border-slate-300 p-2.5 rounded-xl focus:ring-2 focus:ring-blue-500 focus:outline-none"
                      />
                    </div>

                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                      <div>
                        <label className="block font-semibold text-slate-700 mb-1">Departments (Comma-separated)</label>
                        <input
                          type="text"
                          placeholder="e.g. Computer Science, IT"
                          value={segmentForm.departments}
                          onChange={(e) => setSegmentForm({ ...segmentForm, departments: e.target.value })}
                          className="w-full border border-slate-300 p-2.5 rounded-xl focus:ring-2 focus:ring-blue-500 focus:outline-none"
                        />
                      </div>
                      <div>
                        <label className="block font-semibold text-slate-700 mb-1">Graduation Year Range</label>
                        <div className="flex gap-2">
                          <input
                            type="number"
                            placeholder="Min (2018)"
                            value={segmentForm.graduation_year_min}
                            onChange={(e) => setSegmentForm({ ...segmentForm, graduation_year_min: e.target.value })}
                            className="w-1/2 border border-slate-300 p-2.5 rounded-xl focus:ring-2 focus:ring-blue-500 focus:outline-none"
                          />
                          <input
                            type="number"
                            placeholder="Max (2024)"
                            value={segmentForm.graduation_year_max}
                            onChange={(e) => setSegmentForm({ ...segmentForm, graduation_year_max: e.target.value })}
                            className="w-1/2 border border-slate-300 p-2.5 rounded-xl focus:ring-2 focus:ring-blue-500 focus:outline-none"
                          />
                        </div>
                      </div>
                    </div>

                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                      <div>
                        <label className="block font-semibold text-slate-700 mb-1">Companies (Comma-separated)</label>
                        <input
                          type="text"
                          placeholder="e.g. Google, Microsoft, Meta"
                          value={segmentForm.companies}
                          onChange={(e) => setSegmentForm({ ...segmentForm, companies: e.target.value })}
                          className="w-full border border-slate-300 p-2.5 rounded-xl focus:ring-2 focus:ring-blue-500 focus:outline-none"
                        />
                      </div>
                      <div>
                        <label className="block font-semibold text-slate-700 mb-1">Locations (Comma-separated)</label>
                        <input
                          type="text"
                          placeholder="e.g. New York, San Francisco"
                          value={segmentForm.locations}
                          onChange={(e) => setSegmentForm({ ...segmentForm, locations: e.target.value })}
                          className="w-full border border-slate-300 p-2.5 rounded-xl focus:ring-2 focus:ring-blue-500 focus:outline-none"
                        />
                      </div>
                    </div>

                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                      <div>
                        <label className="block font-semibold text-slate-700 mb-1">Skills (Comma-separated)</label>
                        <input
                          type="text"
                          placeholder="e.g. Python, AI, React"
                          value={segmentForm.skills}
                          onChange={(e) => setSegmentForm({ ...segmentForm, skills: e.target.value })}
                          className="w-full border border-slate-300 p-2.5 rounded-xl focus:ring-2 focus:ring-blue-500 focus:outline-none"
                        />
                      </div>
                      <div>
                        <label className="block font-semibold text-slate-700 mb-1">Mentorship Filter</label>
                        <select
                          value={segmentForm.mentorship_available ?? ""}
                          onChange={(e) => setSegmentForm({ ...segmentForm, mentorship_available: e.target.value })}
                          className="w-full border border-slate-300 p-2.5 rounded-xl bg-white focus:ring-2 focus:ring-blue-500 focus:outline-none"
                        >
                          <option value="">Any (Mentors & Non-Mentors)</option>
                          <option value="true">Mentors Available Only</option>
                          <option value="false">Non-Mentors Only</option>
                        </select>
                      </div>
                    </div>

                    {/* Criteria Preview Bar */}
                    <div className="flex items-center justify-between p-3 bg-slate-50 rounded-2xl border border-slate-200">
                      <div>
                        <span className="text-slate-500">Live Preview: </span>
                        {criteriaPreview ? (
                          <span className="font-bold text-blue-600">
                            {criteriaPreview.estimated_count} matching alumni
                          </span>
                        ) : (
                          <span className="text-slate-400">Click preview to test criteria</span>
                        )}
                      </div>
                      <button
                        type="button"
                        onClick={handlePreviewCriteria}
                        disabled={previewingCriteria}
                        className="px-3 py-1.5 bg-slate-200 hover:bg-slate-300 text-slate-800 text-xs font-semibold rounded-xl"
                      >
                        {previewingCriteria ? "Evaluating..." : "🔍 Preview Match Count"}
                      </button>
                    </div>

                    {/* Sample matches */}
                    {criteriaPreview?.sample?.length > 0 && (
                      <div className="max-h-32 overflow-y-auto bg-white rounded-xl border border-slate-100 p-2 divide-y divide-slate-100 text-[11px]">
                        {criteriaPreview.sample.map((s) => (
                          <div key={s.id} className="py-1 flex justify-between">
                            <span className="font-semibold text-slate-800">{s.name} ({s.email})</span>
                            <span className="text-slate-500">{s.department || "—"}, {s.graduation_year || "—"}</span>
                          </div>
                        ))}
                      </div>
                    )}

                    <div className="flex justify-end gap-3 pt-3 border-t border-slate-100">
                      <button
                        type="button"
                        onClick={() => {
                          setShowSegmentModal(false);
                          setCriteriaPreview(null);
                        }}
                        className="px-4 py-2 rounded-xl text-slate-600 text-xs font-semibold"
                      >
                        Cancel
                      </button>
                      <button
                        type="submit"
                        disabled={savingSegment}
                        className="px-5 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-700 text-white text-xs font-bold shadow-md disabled:bg-blue-300"
                      >
                        {savingSegment ? "Saving..." : "Save Dynamic Segment"}
                      </button>
                    </div>
                  </form>
                </div>
              </div>
            )}

            {/* View Segment Members Modal */}
            {selectedSegment && (
              <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4 backdrop-blur-sm">
                <div className="bg-white rounded-3xl p-6 sm:p-8 max-w-4xl w-full shadow-2xl relative max-h-[90vh] flex flex-col">
                  <div className="flex justify-between items-start pb-4 border-b border-slate-100">
                    <div>
                      <h3 className="text-lg font-bold text-slate-900">
                        Segment Members: {selectedSegment.name}
                      </h3>
                      <p className="text-xs text-slate-500">
                        Total {totalMembers} alumni match this dynamic criteria.
                      </p>
                    </div>
                    <button
                      onClick={() => setSelectedSegment(null)}
                      className="text-slate-400 hover:text-slate-700 text-xl font-bold"
                    >
                      ✕
                    </button>
                  </div>

                  {loadingMembers ? (
                    <div className="py-16 text-center text-slate-400 text-xs">Loading members...</div>
                  ) : segmentMembers.length === 0 ? (
                    <div className="py-16 text-center text-slate-400 text-xs">No members found matching criteria.</div>
                  ) : (
                    <div className="flex-1 overflow-y-auto my-4">
                      <table className="w-full text-left text-xs border-collapse">
                        <thead className="bg-slate-50 text-slate-400 font-bold uppercase text-[10px] sticky top-0">
                          <tr>
                            <th className="py-2.5 px-3">Name</th>
                            <th className="py-2.5 px-3">Email</th>
                            <th className="py-2.5 px-3">Grad Year</th>
                            <th className="py-2.5 px-3">Department</th>
                            <th className="py-2.5 px-3">Company</th>
                            <th className="py-2.5 px-3">Role</th>
                            <th className="py-2.5 px-3">Completion</th>
                            <th className="py-2.5 px-3">Verified</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100 text-slate-700">
                          {segmentMembers.map((m) => (
                            <tr key={m.id} className="hover:bg-slate-50/50">
                              <td className="py-2.5 px-3 font-semibold text-slate-900">{m.name}</td>
                              <td className="py-2.5 px-3">{m.email}</td>
                              <td className="py-2.5 px-3">{m.graduation_year || "—"}</td>
                              <td className="py-2.5 px-3">{m.department || "—"}</td>
                              <td className="py-2.5 px-3">{m.company || "—"}</td>
                              <td className="py-2.5 px-3">{m.job_role || "—"}</td>
                              <td className="py-2.5 px-3 font-mono">{m.completion_percentage}%</td>
                              <td className="py-2.5 px-3">
                                <span
                                  className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                                    m.is_verified ? "bg-emerald-100 text-emerald-800" : "bg-slate-100 text-slate-600"
                                  }`}
                                >
                                  {m.is_verified ? "Yes" : "No"}
                                </span>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}

                  {/* Pagination footer */}
                  <div className="flex justify-between items-center pt-3 border-t border-slate-100 text-xs">
                    <span className="text-slate-500">
                      Showing {memberPage * 20 + 1} - {Math.min((memberPage + 1) * 20, totalMembers)} of {totalMembers}
                    </span>
                    <div className="flex gap-2">
                      <button
                        onClick={() => handleViewSegmentMembers(selectedSegment, memberPage - 1)}
                        disabled={memberPage === 0}
                        className="px-3 py-1 bg-slate-100 hover:bg-slate-200 disabled:opacity-40 rounded-lg text-xs font-semibold"
                      >
                        ← Prev
                      </button>
                      <button
                        onClick={() => handleViewSegmentMembers(selectedSegment, memberPage + 1)}
                        disabled={(memberPage + 1) * 20 >= totalMembers}
                        className="px-3 py-1 bg-slate-100 hover:bg-slate-200 disabled:opacity-40 rounded-lg text-xs font-semibold"
                      >
                        Next →
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            )}
          </div>
        )}

        {/* ========================================================= */}
        {/* TAB 4: ADMIN AUDIT LOG                                    */}
        {/* ========================================================= */}
        {activeTab === "audit" && (
          <div className="space-y-6">
            <div className="bg-white p-6 rounded-3xl border border-slate-200 shadow-xs space-y-4">
              <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
                <div>
                  <h2 className="text-base font-bold text-slate-900">Institutional Administrative Audit Log</h2>
                  <p className="text-xs text-slate-500 mt-0.5">
                    Immutable security record of administrative interventions, verifications, segment creation, and broadcasts.
                  </p>
                </div>

                {/* Filters */}
                <div className="flex flex-wrap items-center gap-2">
                  <select
                    value={actionFilter}
                    onChange={(e) => setActionFilter(e.target.value)}
                    className="border border-slate-200 rounded-xl px-3 py-1.5 text-xs bg-white focus:outline-none"
                  >
                    <option value="">All Actions</option>
                    <option value="VERIFY_ALUMNI">VERIFY_ALUMNI</option>
                    <option value="UNVERIFY_ALUMNI">UNVERIFY_ALUMNI</option>
                    <option value="IMPORT_COMMIT">IMPORT_COMMIT</option>
                    <option value="CREATE_SEGMENT">CREATE_SEGMENT</option>
                    <option value="UPDATE_SEGMENT">UPDATE_SEGMENT</option>
                    <option value="DELETE_SEGMENT">DELETE_SEGMENT</option>
                    <option value="CREATE_ANNOUNCEMENT">CREATE_ANNOUNCEMENT</option>
                    <option value="MODERATE_STORY">MODERATE_STORY</option>
                    <option value="MODERATE_ACHIEVEMENT">MODERATE_ACHIEVEMENT</option>
                    <option value="MANUAL_CHECKIN">MANUAL_CHECKIN</option>
                  </select>

                  <select
                    value={targetFilter}
                    onChange={(e) => setTargetFilter(e.target.value)}
                    className="border border-slate-200 rounded-xl px-3 py-1.5 text-xs bg-white focus:outline-none"
                  >
                    <option value="">All Targets</option>
                    <option value="ALUMNI">ALUMNI</option>
                    <option value="IMPORT_JOB">IMPORT_JOB</option>
                    <option value="SEGMENT">SEGMENT</option>
                    <option value="ANNOUNCEMENT">ANNOUNCEMENT</option>
                    <option value="STORY">STORY</option>
                    <option value="ACHIEVEMENT">ACHIEVEMENT</option>
                    <option value="ATTENDANCE">ATTENDANCE</option>
                  </select>
                </div>
              </div>

              {loadingLogs ? (
                <p className="text-xs text-slate-400 py-8 text-center">Loading audit logs...</p>
              ) : auditLogs.length === 0 ? (
                <p className="text-xs text-slate-400 py-8 text-center">No audit logs matching query.</p>
              ) : (
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs border-collapse">
                    <thead className="border-b border-slate-100 text-slate-400 font-bold uppercase text-[10px]">
                      <tr>
                        <th className="py-2.5 px-3">Timestamp</th>
                        <th className="py-2.5 px-3">Administrator</th>
                        <th className="py-2.5 px-3">Action</th>
                        <th className="py-2.5 px-3">Target</th>
                        <th className="py-2.5 px-3">Target ID</th>
                        <th className="py-2.5 px-3">Details</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 text-slate-700">
                      {auditLogs.map((log) => (
                        <tr key={log.id} className="hover:bg-slate-50/50">
                          <td className="py-2.5 px-3 text-slate-400 font-mono text-[11px]">
                            {new Date(log.created_at).toLocaleString()}
                          </td>
                          <td className="py-2.5 px-3 font-semibold text-slate-900">{log.admin_name}</td>
                          <td className="py-2.5 px-3">
                            <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-slate-100 text-slate-800">
                              {log.action}
                            </span>
                          </td>
                          <td className="py-2.5 px-3 text-slate-600 font-mono text-[11px]">{log.target_type}</td>
                          <td className="py-2.5 px-3 text-slate-400 font-mono text-[11px]">#{log.target_id || "—"}</td>
                          <td className="py-2.5 px-3">
                            <button
                              onClick={() => setSelectedLogDetails(log)}
                              className="text-blue-600 hover:text-blue-800 font-semibold text-[11px]"
                            >
                              Inspect Details
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>

            {/* Log Details Modal */}
            {selectedLogDetails && (
              <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4 backdrop-blur-sm">
                <div className="bg-white rounded-3xl p-6 max-w-lg w-full shadow-2xl relative space-y-4">
                  <div className="flex justify-between items-center border-b border-slate-100 pb-3">
                    <h3 className="text-base font-bold text-slate-900">Audit Log Record #{selectedLogDetails.id}</h3>
                    <button
                      onClick={() => setSelectedLogDetails(null)}
                      className="text-slate-400 hover:text-slate-700 font-bold"
                    >
                      ✕
                    </button>
                  </div>
                  <div className="space-y-2 text-xs text-slate-700">
                    <p><span className="font-semibold text-slate-500">Action:</span> {selectedLogDetails.action}</p>
                    <p><span className="font-semibold text-slate-500">Performed By:</span> {selectedLogDetails.admin_name}</p>
                    <p><span className="font-semibold text-slate-500">Target Type:</span> {selectedLogDetails.target_type}</p>
                    <p><span className="font-semibold text-slate-500">Target ID:</span> {selectedLogDetails.target_id}</p>
                    <p><span className="font-semibold text-slate-500">Timestamp:</span> {new Date(selectedLogDetails.created_at).toISOString()}</p>
                    <div>
                      <span className="font-semibold text-slate-500 block mb-1">Payload Details (Redacted):</span>
                      <pre className="p-3 bg-slate-900 text-emerald-400 rounded-xl overflow-x-auto text-[11px] font-mono whitespace-pre-wrap">
                        {(() => {
                          try {
                            return JSON.stringify(JSON.parse(selectedLogDetails.details || "{}"), null, 2);
                          } catch {
                            return selectedLogDetails.details || "None";
                          }
                        })()}
                      </pre>
                    </div>
                  </div>
                  <div className="flex justify-end pt-2">
                    <button
                      onClick={() => setSelectedLogDetails(null)}
                      className="px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-semibold rounded-xl"
                    >
                      Close
                    </button>
                  </div>
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </DashboardLayout>
  );
};

export default AdminAlumniData;
