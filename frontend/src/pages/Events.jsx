import { useEffect, useState } from "react";
import DashboardLayout from "../layouts/DashboardLayout";
import { eventApi } from "../api/eventApi";
import { bookmarkApi } from "../api/bookmarkApi";
import { attendanceApi } from "../api/attendanceApi";

const Events = () => {
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [typeFilter, setTypeFilter] = useState("");
  const [search, setSearch] = useState("");
  const [savedEventIds, setSavedEventIds] = useState(new Set());
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [selectedEvent, setSelectedEvent] = useState(null);
  const [attendeesModalEvent, setAttendeesModalEvent] = useState(null);
  const [attendees, setAttendees] = useState([]);
  const [attendeesLoading, setAttendeesLoading] = useState(false);

  // Attendance & QR States
  const [attendanceModalEvent, setAttendanceModalEvent] = useState(null);
  const [qrData, setQrData] = useState(null);
  const [attendanceStats, setAttendanceStats] = useState(null);
  const [attendanceList, setAttendanceList] = useState([]);
  const [attendanceLoading, setAttendanceLoading] = useState(false);
  const [manualUserId, setManualUserId] = useState("");
  const [manualNotes, setManualNotes] = useState("");

  // Attendee Check-In Modal States
  const [showCheckInModal, setShowCheckInModal] = useState(false);
  const [checkInCode, setCheckInCode] = useState("");
  const [checkingIn, setCheckingIn] = useState(false);
  const [checkInSuccess, setCheckInSuccess] = useState(null);

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
  const canCreate = role === "admin" || role === "alumni";

  const [formData, setFormData] = useState({
    title: "",
    description: "",
    event_type: "Alumni Meet",
    event_date: "",
    start_time: "",
    location: "",
    meeting_url: "",
    image_url: "",
  });

  const fetchBookmarks = async () => {
    try {
      const res = await bookmarkApi.getBookmarks("event");
      const ids = new Set((res.data || []).map((b) => b.item_id));
      setSavedEventIds(ids);
    } catch (err) {
      console.error("Failed to load saved event bookmarks:", err);
    }
  };

  const fetchEvents = async () => {
    setLoading(true);
    try {
      const params = {};
      if (typeFilter) params.event_type = typeFilter;
      if (search) params.search = search;

      const res = await eventApi.getEvents(params);
      setEvents(res.data || []);
    } catch (err) {
      console.error("Failed to load events:", err);
      setErrorMsg("Failed to load events.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchEvents();
    fetchBookmarks();
  }, [typeFilter]);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    fetchEvents();
  };

  const handleToggleBookmark = async (e, event) => {
    e.stopPropagation();
    const isSaved = savedEventIds.has(event.id);
    try {
      if (isSaved) {
        await bookmarkApi.deleteBookmarkByItem("event", event.id);
        setSavedEventIds((prev) => {
          const next = new Set(prev);
          next.delete(event.id);
          return next;
        });
      } else {
        await bookmarkApi.createBookmark("event", event.id);
        setSavedEventIds((prev) => new Set(prev).add(event.id));
      }
    } catch (err) {
      console.error("Failed to update bookmark:", err);
      setErrorMsg("Failed to update bookmark status.");
    }
  };

  const handleAddToCalendar = (event) => {
    const pad = (n) => (n < 10 ? "0" + n : n);
    let startDateStr = "";
    if (event.event_date) {
      const d = new Date(event.event_date);
      startDateStr = `${d.getFullYear()}${pad(d.getMonth() + 1)}${pad(d.getDate())}T090000Z`;
    } else {
      const now = new Date();
      startDateStr = `${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}T090000Z`;
    }

    const icsContent = [
      "BEGIN:VCALENDAR",
      "VERSION:2.0",
      "PRODID:-//AlumniConnect//Event Calendar//EN",
      "BEGIN:VEVENT",
      `UID:event-${event.id}@alumniconnect.edu`,
      `DTSTAMP:${startDateStr}`,
      `DTSTART:${startDateStr}`,
      `SUMMARY:${(event.title || "Alumni Event").replace(/\n/g, " ")}`,
      `DESCRIPTION:${(event.description || "").replace(/\n/g, "\\n")}`,
      `LOCATION:${(event.location || "Online").replace(/\n/g, " ")}`,
      "END:VEVENT",
      "END:VCALENDAR",
    ].join("\r\n");

    const blob = new Blob([icsContent], { type: "text/calendar;charset=utf-8" });
    const link = document.createElement("a");
    link.href = window.URL.createObjectURL(blob);
    link.setAttribute("download", `${(event.title || "Event").replace(/[^a-zA-Z0-9]/g, "_")}.ics`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);

    setSuccessMsg(`Calendar file (.ics) downloaded for "${event.title}"`);
    setTimeout(() => setSuccessMsg(""), 3000);
  };

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  const handleCreateEvent = async (e) => {
    e.preventDefault();
    setErrorMsg("");
    setSuccessMsg("");

    try {
      await eventApi.createEvent(formData);
      setSuccessMsg("Event successfully created!");
      setShowCreateModal(false);
      setFormData({
        title: "",
        description: "",
        event_type: "Alumni Meet",
        event_date: "",
        start_time: "",
        location: "",
        meeting_url: "",
        image_url: "",
      });
      fetchEvents();
    } catch (err) {
      const detail = err.response?.data?.detail || "Failed to create event.";
      setErrorMsg(detail);
    }
  };

  const handleRegister = async (eventId) => {
    setErrorMsg("");
    setSuccessMsg("");
    try {
      await eventApi.registerEvent(eventId);
      setSuccessMsg("Registered for event! A confirmation notification has been added.");
      fetchEvents();
    } catch (err) {
      const detail = err.response?.data?.detail || "Registration failed.";
      setErrorMsg(detail);
    }
  };

  const handleCancelRegistration = async (eventId) => {
    if (!window.confirm("Cancel your registration for this event?")) return;
    setErrorMsg("");
    setSuccessMsg("");
    try {
      await eventApi.cancelRegistration(eventId);
      setSuccessMsg("Event registration cancelled.");
      fetchEvents();
    } catch (err) {
      const detail = err.response?.data?.detail || "Failed to cancel registration.";
      setErrorMsg(detail);
    }
  };

  const handleDeleteEvent = async (eventId) => {
    if (!window.confirm("Are you sure you want to delete this event?")) return;
    try {
      await eventApi.deleteEvent(eventId);
      setSuccessMsg("Event deleted successfully.");
      if (selectedEvent?.id === eventId) setSelectedEvent(null);
      fetchEvents();
    } catch (err) {
      const detail = err.response?.data?.detail || "Failed to delete event.";
      setErrorMsg(detail);
    }
  };

  const handleViewAttendees = async (event) => {
    setAttendeesModalEvent(event);
    setAttendeesLoading(true);
    try {
      const res = await eventApi.getRegistrations(event.id);
      setAttendees(res.data || []);
    } catch (err) {
      console.error("Failed to load attendees:", err);
      setAttendees([]);
    } finally {
      setAttendeesLoading(false);
    }
  };

  const handleOpenAttendance = async (event) => {
    setAttendanceModalEvent(event);
    setAttendanceLoading(true);
    setErrorMsg("");
    try {
      const [tokenRes, statsRes, listRes] = await Promise.all([
        attendanceApi.generateQrToken(event.id),
        attendanceApi.getAttendanceStats(event.id),
        attendanceApi.getAttendanceList(event.id),
      ]);
      setQrData(tokenRes.data);
      setAttendanceStats(statsRes.data);
      setAttendanceList(listRes.data || []);
    } catch (err) {
      console.error("Failed to load event attendance:", err);
      setErrorMsg(err.response?.data?.detail || "Failed to load attendance details.");
    } finally {
      setAttendanceLoading(false);
    }
  };

  const handleManualCheckIn = async (e) => {
    e.preventDefault();
    if (!attendanceModalEvent || !manualUserId.trim()) return;
    try {
      await attendanceApi.manualCheckIn(
        attendanceModalEvent.id,
        parseInt(manualUserId),
        manualNotes.trim()
      );
      setSuccessMsg("Attendee checked in manually.");
      setManualUserId("");
      setManualNotes("");
      // Refresh stats & list
      const [statsRes, listRes] = await Promise.all([
        attendanceApi.getAttendanceStats(attendanceModalEvent.id),
        attendanceApi.getAttendanceList(attendanceModalEvent.id),
      ]);
      setAttendanceStats(statsRes.data);
      setAttendanceList(listRes.data || []);
    } catch (err) {
      setErrorMsg(err.response?.data?.detail || "Manual check-in failed.");
    }
  };

  const handleSubmitCheckIn = async (e) => {
    e.preventDefault();
    if (!checkInCode.trim()) return;
    setCheckingIn(true);
    setErrorMsg("");
    try {
      const res = await attendanceApi.checkIn(checkInCode.trim());
      setCheckInSuccess(res.data);
      setCheckInCode("");
      setSuccessMsg("Checked in successfully!");
      fetchEvents();
    } catch (err) {
      setErrorMsg(err.response?.data?.detail || "Check-in failed. Please verify your token.");
    } finally {
      setCheckingIn(false);
    }
  };

  return (
    <DashboardLayout>
      <div className="max-w-7xl mx-auto space-y-6">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h1 className="text-3xl font-extrabold text-slate-900">Alumni Events</h1>
            <p className="text-sm text-slate-500 mt-1">
              Browse reunions, workshops, webinars, and institution networking meetups
            </p>
          </div>

          <div className="flex items-center gap-3 self-start sm:self-auto">
            <button
              onClick={() => {
                setShowCheckInModal(true);
                setCheckInSuccess(null);
              }}
              className="bg-emerald-600 hover:bg-emerald-700 text-white px-4 py-2.5 rounded-xl font-semibold shadow-xs transition flex items-center gap-1.5 text-sm"
            >
              <span>🎟️</span> Check In with Code
            </button>
            {canCreate && (
              <button
                onClick={() => setShowCreateModal(true)}
                className="bg-blue-700 hover:bg-blue-800 text-white px-5 py-2.5 rounded-xl font-semibold shadow-md transition flex items-center gap-2 text-sm"
              >
                <span>+ Create Event</span>
              </button>
            )}
          </div>
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

        {/* Search & Type Filter Bar */}
        <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex flex-col md:flex-row gap-4 justify-between items-center">
          <form onSubmit={handleSearchSubmit} className="relative flex-1 w-full">
            <input
              type="text"
              placeholder="Search event title or description..."
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
              value={typeFilter}
              onChange={(e) => setTypeFilter(e.target.value)}
              className="border border-slate-300 bg-slate-50 p-2.5 rounded-xl text-xs font-medium text-slate-700 focus:outline-none"
            >
              <option value="">All Event Types</option>
              <option value="Alumni Meet">Alumni Meet</option>
              <option value="Webinar">Webinar</option>
              <option value="Workshop">Workshop</option>
              <option value="Networking Session">Networking Session</option>
              <option value="Career Guidance">Career Guidance</option>
              <option value="Reunion">Reunion</option>
            </select>
          </div>
        </div>

        {/* Events Grid */}
        {loading ? (
          <div className="text-center py-16 text-slate-500">
            <div className="inline-block animate-spin rounded-full h-8 w-8 border-4 border-blue-600 border-t-transparent mb-2"></div>
            <p className="text-sm">Loading events...</p>
          </div>
        ) : events.length === 0 ? (
          <div className="bg-white p-12 rounded-2xl border border-slate-200 text-center shadow-sm">
            <div className="text-4xl mb-3">📅</div>
            <h3 className="text-lg font-bold text-slate-800">No Events Found</h3>
            <p className="text-xs text-slate-500 mt-1">Check back later or change your search criteria.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {events.map((event) => {
              const isSaved = savedEventIds.has(event.id);

              return (
                <div
                  key={event.id}
                  className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200 hover:shadow-md transition flex flex-col justify-between"
                >
                  <div>
                    <div className="flex justify-between items-start gap-2 mb-3">
                      <span className="bg-blue-50 text-blue-700 text-xs font-bold px-2.5 py-1 rounded-full border border-blue-200">
                        {event.event_type}
                      </span>
                      <div className="flex items-center gap-1.5">
                        {event.is_registered && (
                          <span className="bg-emerald-50 text-emerald-700 text-xs font-semibold px-2.5 py-0.5 rounded-full border border-emerald-200">
                            Registered ✓
                          </span>
                        )}
                        <button
                          onClick={(e) => handleToggleBookmark(e, event)}
                          className={`p-1.5 rounded-lg border text-sm transition ${
                            isSaved
                              ? "bg-amber-50 text-amber-500 border-amber-200"
                              : "text-slate-400 border-slate-200 hover:text-amber-500 hover:bg-slate-50"
                          }`}
                          title={isSaved ? "Remove from saved" : "Save event"}
                          aria-label={isSaved ? "Saved" : "Save"}
                        >
                          {isSaved ? "★" : "☆"}
                        </button>
                      </div>
                    </div>

                    <h2 className="text-xl font-bold text-slate-900 mb-2 leading-tight">{event.title}</h2>
                    <p className="text-xs text-slate-600 line-clamp-3 mb-4">{event.description}</p>

                    <div className="space-y-1.5 text-xs text-slate-600 bg-slate-50 p-3 rounded-xl border border-slate-100">
                      <div className="flex items-center gap-2">
                        <span>📅</span>
                        <span className="font-semibold text-slate-800">
                          {event.event_date} {event.start_time && `• ${event.start_time}`}
                        </span>
                      </div>
                      <div className="flex items-center gap-2">
                        <span>📍</span>
                        <span>{event.location}</span>
                      </div>
                      {event.meeting_url && (
                        <div className="flex items-center gap-2">
                          <span>🔗</span>
                          <a
                            href={event.meeting_url}
                            target="_blank"
                            rel="noreferrer"
                            className="text-blue-700 font-semibold hover:underline truncate"
                          >
                            Join Virtual Stage
                          </a>
                        </div>
                      )}
                      <div className="flex items-center gap-2 text-slate-400">
                        <span>👥</span>
                        <span>{event.registrations_count} Attendees Registered</span>
                      </div>
                    </div>
                  </div>

                  <div className="pt-4 mt-4 border-t border-slate-100 flex flex-col gap-2">
                    <div className="flex justify-between items-center">
                      {event.is_registered ? (
                        <button
                          onClick={() => handleCancelRegistration(event.id)}
                          className="bg-slate-100 hover:bg-red-50 text-slate-700 hover:text-red-600 px-4 py-2 rounded-xl text-xs font-semibold transition"
                        >
                          Cancel RSVP
                        </button>
                      ) : (
                        <button
                          onClick={() => handleRegister(event.id)}
                          className="bg-blue-700 hover:bg-blue-800 text-white px-5 py-2 rounded-xl text-xs font-semibold transition shadow-sm"
                        >
                          Register for Event
                        </button>
                      )}

                      <button
                        onClick={() => handleAddToCalendar(event)}
                        className="text-xs text-blue-600 hover:text-blue-800 font-semibold flex items-center gap-1"
                        title="Download .ics Calendar Invite"
                      >
                        <span>📆</span>
                        <span>Add to Cal</span>
                      </button>
                    </div>

                    {(role === "admin" || currentUser?.id === event.created_by) && (
                      <div className="flex justify-end gap-3 pt-1 border-t border-slate-50">
                        <button
                          onClick={() => handleOpenAttendance(event)}
                          className="text-xs text-emerald-700 bg-emerald-50 hover:bg-emerald-100 border border-emerald-200 px-2.5 py-1 rounded-lg font-bold flex items-center gap-1"
                        >
                          <span>📱</span> Attendance & QR
                        </button>
                        <button
                          onClick={() => handleViewAttendees(event)}
                          className="text-xs text-indigo-600 hover:underline font-semibold"
                        >
                          Attendees
                        </button>
                        <button
                          onClick={() => handleDeleteEvent(event.id)}
                          className="text-xs text-red-500 hover:underline font-medium"
                        >
                          Delete
                        </button>
                      </div>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}

        {/* Create Event Modal */}
        {showCreateModal && (
          <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4 backdrop-blur-sm">
            <div className="bg-white rounded-3xl p-8 max-w-2xl w-full shadow-2xl relative max-h-[90vh] overflow-y-auto">
              <button
                onClick={() => setShowCreateModal(false)}
                className="absolute right-5 top-5 text-slate-400 hover:text-slate-700 text-xl font-bold"
              >
                ✕
              </button>

              <h2 className="text-2xl font-bold text-slate-900 mb-6 pb-2 border-b border-slate-100">
                Announce New Event
              </h2>

              <form onSubmit={handleCreateEvent} className="space-y-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-600 mb-1">Event Title *</label>
                  <input
                    type="text"
                    name="title"
                    value={formData.title}
                    onChange={handleChange}
                    required
                    placeholder="e.g. Annual Alumni Meet 2026"
                    className="w-full border border-slate-300 p-3 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
                  />
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Event Type</label>
                    <select
                      name="event_type"
                      value={formData.event_type}
                      onChange={handleChange}
                      className="w-full border border-slate-300 p-3 rounded-xl text-sm bg-white focus:ring-2 focus:ring-blue-500 focus:outline-none"
                    >
                      <option value="Alumni Meet">Alumni Meet</option>
                      <option value="Webinar">Webinar</option>
                      <option value="Workshop">Workshop</option>
                      <option value="Networking Session">Networking Session</option>
                      <option value="Career Guidance">Career Guidance</option>
                      <option value="Reunion">Reunion</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Event Date *</label>
                    <input
                      type="date"
                      name="event_date"
                      value={formData.event_date}
                      onChange={handleChange}
                      required
                      className="w-full border border-slate-300 p-3 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Start Time</label>
                    <input
                      type="text"
                      name="start_time"
                      value={formData.start_time}
                      onChange={handleChange}
                      placeholder="e.g. 10:00 AM - 1:00 PM"
                      className="w-full border border-slate-300 p-3 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-600 mb-1">Location / Venue *</label>
                    <input
                      type="text"
                      name="location"
                      value={formData.location}
                      onChange={handleChange}
                      required
                      placeholder="e.g. Auditorium / Virtual Zoom"
                      className="w-full border border-slate-300 p-3 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-600 mb-1">Virtual Meeting URL</label>
                  <input
                    type="url"
                    name="meeting_url"
                    value={formData.meeting_url}
                    onChange={handleChange}
                    placeholder="https://zoom.us/j/... or https://meet.google.com/..."
                    className="w-full border border-slate-300 p-3 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-600 mb-1">Event Description *</label>
                  <textarea
                    name="description"
                    rows="3"
                    value={formData.description}
                    onChange={handleChange}
                    required
                    placeholder="Detailed agenda, key speakers, and goals..."
                    className="w-full border border-slate-300 p-3 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
                  ></textarea>
                </div>

                <div className="flex justify-end gap-3 pt-4 border-t border-slate-100">
                  <button
                    type="button"
                    onClick={() => setShowCreateModal(false)}
                    className="px-5 py-2.5 rounded-xl text-slate-600 hover:bg-slate-100 text-sm font-semibold"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="px-6 py-2.5 rounded-xl bg-blue-700 hover:bg-blue-800 text-white text-sm font-semibold shadow-md"
                  >
                    Publish Event
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

        {/* View Attendees Modal */}
        {attendeesModalEvent && (
          <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4 backdrop-blur-sm">
            <div className="bg-white rounded-3xl p-8 max-w-xl w-full shadow-2xl relative max-h-[90vh] overflow-y-auto">
              <button
                onClick={() => setAttendeesModalEvent(null)}
                className="absolute right-5 top-5 text-slate-400 hover:text-slate-700 text-xl font-bold"
              >
                ✕
              </button>

              <h3 className="text-xl font-bold text-slate-900 mb-1">Event Attendees</h3>
              <p className="text-xs text-slate-500 mb-6">{attendeesModalEvent.title}</p>

              {attendeesLoading ? (
                <p className="text-center py-6 text-slate-500 text-sm">Loading attendees...</p>
              ) : attendees.length === 0 ? (
                <p className="text-center py-6 text-slate-500 text-sm">No registrations yet.</p>
              ) : (
                <div className="divide-y divide-slate-100 space-y-2">
                  {attendees.map((a) => (
                    <div key={a.id} className="pt-2 flex justify-between items-center">
                      <div>
                        <p className="text-sm font-bold text-slate-900">{a.user_name}</p>
                        <p className="text-xs text-slate-500">{a.user_email}</p>
                      </div>
                      <span className="text-[10px] uppercase font-bold bg-slate-100 text-slate-700 px-2 py-0.5 rounded-full">
                        {a.user_role}
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}

        {/* Attendance & QR Check-In Modal (Organizer / Admin) */}
        {attendanceModalEvent && (
          <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4 backdrop-blur-sm">
            <div className="bg-white rounded-3xl p-6 sm:p-8 max-w-2xl w-full shadow-2xl relative max-h-[90vh] overflow-y-auto">
              <button
                onClick={() => setAttendanceModalEvent(null)}
                className="absolute right-5 top-5 text-slate-400 hover:text-slate-700 text-xl font-bold"
              >
                ✕
              </button>

              <div className="flex items-center gap-2 mb-1">
                <span className="text-xs font-bold uppercase tracking-wider text-emerald-700 bg-emerald-50 px-2.5 py-0.5 rounded-full">
                  Live Attendance Portal
                </span>
              </div>
              <h3 className="text-xl sm:text-2xl font-bold text-slate-900">
                {attendanceModalEvent.title}
              </h3>
              <p className="text-xs text-slate-500 mb-6">
                Display this QR code at the event entrance or copy the check-in token for attendees.
              </p>

              {attendanceLoading ? (
                <div className="py-12 text-center">
                  <div className="w-8 h-8 border-4 border-emerald-600 border-t-transparent rounded-full animate-spin mx-auto mb-2"></div>
                  <p className="text-xs text-slate-500">Generating secure attendance token...</p>
                </div>
              ) : (
                <div className="space-y-6">
                  {/* QR and Token Section */}
                  <div className="bg-slate-50 border border-slate-200 rounded-2xl p-5 text-center">
                    {qrData?.qr_token && (
                      <div className="space-y-3">
                        <img
                          src={`https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${encodeURIComponent(
                            qrData.qr_token
                          )}`}
                          alt="Event Check-In QR Code"
                          className="w-44 h-44 mx-auto rounded-xl border border-slate-200 shadow-xs bg-white p-2"
                        />
                        <p className="text-xs text-slate-500">
                          Scan using the AlumniConnect mobile app scanner
                        </p>
                        <div className="flex items-center justify-center gap-2 max-w-md mx-auto">
                          <input
                            type="text"
                            readOnly
                            value={qrData.qr_token}
                            className="text-xs font-mono bg-white border border-slate-300 rounded-lg px-2.5 py-1.5 w-full text-slate-600 truncate select-all"
                          />
                          <button
                            onClick={() => {
                              navigator.clipboard.writeText(qrData.qr_token);
                              setSuccessMsg("Check-in token copied to clipboard!");
                            }}
                            className="px-3 py-1.5 bg-slate-900 hover:bg-slate-800 text-white text-xs font-bold rounded-lg whitespace-nowrap shadow-xs"
                          >
                            Copy Code
                          </button>
                        </div>
                      </div>
                    )}
                  </div>

                  {/* Attendance Stats Cards */}
                  {attendanceStats && (
                    <div className="grid grid-cols-3 gap-3 text-center">
                      <div className="bg-blue-50/60 border border-blue-100 rounded-xl p-3">
                        <p className="text-xs text-blue-600 font-semibold">Registered</p>
                        <p className="text-xl font-extrabold text-blue-900 mt-0.5">
                          {attendanceStats.total_registered}
                        </p>
                      </div>
                      <div className="bg-emerald-50/60 border border-emerald-100 rounded-xl p-3">
                        <p className="text-xs text-emerald-600 font-semibold">Attended</p>
                        <p className="text-xl font-extrabold text-emerald-900 mt-0.5">
                          {attendanceStats.total_attended}
                        </p>
                      </div>
                      <div className="bg-purple-50/60 border border-purple-100 rounded-xl p-3">
                        <p className="text-xs text-purple-600 font-semibold">Attendance Rate</p>
                        <p className="text-xl font-extrabold text-purple-900 mt-0.5">
                          {attendanceStats.attendance_percentage}%
                        </p>
                      </div>
                    </div>
                  )}

                  {/* Manual Check-In Form */}
                  <form onSubmit={handleManualCheckIn} className="bg-slate-50 border border-slate-200 rounded-2xl p-4">
                    <h4 className="text-xs font-bold uppercase tracking-wider text-slate-700 mb-2">
                      Manual Attendance Check-In
                    </h4>
                    <div className="flex flex-col sm:flex-row gap-2">
                      <input
                        type="number"
                        value={manualUserId}
                        onChange={(e) => setManualUserId(e.target.value)}
                        placeholder="Attendee User ID"
                        className="px-3 py-2 bg-white border border-slate-300 rounded-xl text-xs sm:w-44 focus:outline-emerald-500"
                        required
                      />
                      <input
                        type="text"
                        value={manualNotes}
                        onChange={(e) => setManualNotes(e.target.value)}
                        placeholder="Desk notes / Walk-in reason"
                        className="px-3 py-2 bg-white border border-slate-300 rounded-xl text-xs flex-1 focus:outline-emerald-500"
                      />
                      <button
                        type="submit"
                        className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs rounded-xl shadow-xs"
                      >
                        Check In
                      </button>
                    </div>
                  </form>

                  {/* Checked-In Roster */}
                  <div>
                    <h4 className="text-xs font-bold uppercase tracking-wider text-slate-700 mb-3 flex items-center justify-between">
                      <span>Checked-In Roster ({attendanceList.length})</span>
                    </h4>
                    {attendanceList.length === 0 ? (
                      <p className="text-xs text-slate-400 py-3 text-center">
                        No attendees have checked in yet.
                      </p>
                    ) : (
                      <div className="divide-y divide-slate-100 max-h-48 overflow-y-auto border border-slate-100 rounded-xl bg-white">
                        {attendanceList.map((rec) => (
                          <div key={rec.id} className="p-3 flex items-center justify-between text-xs">
                            <div>
                              <p className="font-bold text-slate-800">{rec.user_name}</p>
                              <p className="text-slate-400 text-[11px]">{rec.user_email}</p>
                            </div>
                            <div className="text-right">
                              <span
                                className={`px-2 py-0.5 rounded-md font-bold text-[10px] ${
                                  rec.checkin_method === "QR"
                                    ? "bg-emerald-100 text-emerald-800"
                                    : "bg-blue-100 text-blue-800"
                                }`}
                              >
                                {rec.checkin_method}
                              </span>
                              <p className="text-[10px] text-slate-400 mt-0.5">
                                {new Date(rec.checked_in_at).toLocaleTimeString([], {
                                  hour: "2-digit",
                                  minute: "2-digit",
                                })}
                              </p>
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              )}
            </div>
          </div>
        )}

        {/* Attendee Check-In Modal */}
        {showCheckInModal && (
          <div className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4 backdrop-blur-sm">
            <div className="bg-white rounded-3xl p-6 sm:p-8 max-w-md w-full shadow-2xl relative">
              <button
                onClick={() => {
                  setShowCheckInModal(false);
                  setCheckInSuccess(null);
                }}
                className="absolute right-5 top-5 text-slate-400 hover:text-slate-700 text-xl font-bold"
              >
                ✕
              </button>

              <div className="text-center mb-5">
                <span className="text-3xl">🎟️</span>
                <h3 className="text-xl font-bold text-slate-900 mt-2">Event Check-In</h3>
                <p className="text-xs text-slate-500 mt-1">
                  Enter the event check-in code provided by your event organizer.
                </p>
              </div>

              {checkInSuccess ? (
                <div className="bg-emerald-50 border border-emerald-200 rounded-2xl p-5 text-center space-y-3">
                  <span className="text-4xl">🎉</span>
                  <h4 className="font-bold text-emerald-800 text-base">Check-In Confirmed!</h4>
                  <p className="text-xs text-emerald-700">
                    Welcome to the event. Your attendance has been officially verified and recorded.
                  </p>
                  <button
                    onClick={() => {
                      setShowCheckInModal(false);
                      setCheckInSuccess(null);
                    }}
                    className="mt-2 px-5 py-2 bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs rounded-xl shadow-xs"
                  >
                    Done
                  </button>
                </div>
              ) : (
                <form onSubmit={handleSubmitCheckIn} className="space-y-4">
                  <div>
                    <label className="block text-xs font-bold text-slate-700 mb-1">
                      Check-In Code or Token *
                    </label>
                    <textarea
                      rows={4}
                      value={checkInCode}
                      onChange={(e) => setCheckInCode(e.target.value)}
                      placeholder="Paste the event check-in code here..."
                      className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-xs font-mono focus:outline-emerald-500 resize-none"
                      required
                    ></textarea>
                  </div>

                  <div className="pt-2 flex justify-end gap-2">
                    <button
                      type="button"
                      onClick={() => setShowCheckInModal(false)}
                      className="px-4 py-2 text-slate-600 font-semibold text-xs hover:bg-slate-100 rounded-xl"
                    >
                      Cancel
                    </button>
                    <button
                      type="submit"
                      disabled={checkingIn || !checkInCode.trim()}
                      className="px-5 py-2 bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs rounded-xl shadow-xs disabled:opacity-50"
                    >
                      {checkingIn ? "Verifying..." : "Confirm Attendance"}
                    </button>
                  </div>
                </form>
              )}
            </div>
          </div>
        )}
      </div>
    </DashboardLayout>
  );
};

export default Events;