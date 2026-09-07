import { useState, useEffect } from "react";
import { useParams, Link } from "react-router-dom";
import DashboardLayout from "../layouts/DashboardLayout";
import { communityApi } from "../api/communityApi";

export default function CommunityDetails() {
  const { id } = useParams();
  const [community, setCommunity] = useState(null);
  const [posts, setPosts] = useState([]);
  const [members, setMembers] = useState([]);
  const [activeTab, setActiveTab] = useState("posts"); // "posts" or "members"
  const [loading, setLoading] = useState(true);
  const [errorMsg, setErrorMsg] = useState("");
  const [successMsg, setSuccessMsg] = useState("");

  // Post Submission State
  const [postContent, setPostContent] = useState("");
  const [posting, setPosting] = useState(false);

  const fetchCommunityData = async () => {
    setLoading(true);
    setErrorMsg("");
    try {
      const commRes = await communityApi.getCommunityById(id);
      setCommunity(commRes.data);

      const [postsRes, membersRes] = await Promise.all([
        communityApi.getPosts(id),
        communityApi.getMembers(id),
      ]);
      setPosts(postsRes.data || []);
      setMembers(membersRes.data || []);
    } catch (err) {
      console.error("Failed to load community details:", err);
      setErrorMsg("Failed to load community chapter details.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCommunityData();
  }, [id]);

  const handleJoinLeave = async () => {
    try {
      if (community?.is_member) {
        if (!window.confirm("Leave this community chapter?")) return;
        await communityApi.leaveCommunity(id);
        setSuccessMsg("You have left this chapter.");
      } else {
        await communityApi.joinCommunity(id);
        setSuccessMsg("You are now a member of this chapter!");
      }
      fetchCommunityData();
    } catch (err) {
      setErrorMsg(err.response?.data?.detail || "Action failed.");
    }
  };

  const handleCreatePost = async (e) => {
    e.preventDefault();
    if (!postContent.trim()) return;
    setPosting(true);
    setErrorMsg("");
    try {
      await communityApi.createPost(id, { content: postContent.trim() });
      setPostContent("");
      setSuccessMsg("Update shared with community!");
      const updatedPosts = await communityApi.getPosts(id);
      setPosts(updatedPosts.data || []);
    } catch (err) {
      setErrorMsg(err.response?.data?.detail || "Could not post message.");
    } finally {
      setPosting(false);
    }
  };

  return (
    <DashboardLayout>
      <div className="space-y-6 max-w-5xl mx-auto pb-12">
        {/* Navigation Breadcrumb */}
        <div className="flex items-center gap-2 text-sm text-slate-500">
          <Link to="/communities" className="hover:text-indigo-600 transition flex items-center gap-1">
            <span>←</span> All Communities
          </Link>
          <span>/</span>
          <span className="text-slate-800 font-semibold">{community?.name || "Chapter Details"}</span>
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

        {loading ? (
          <div className="py-24 text-center">
            <div className="w-10 h-10 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin mx-auto mb-3"></div>
            <p className="text-sm text-slate-500">Loading community chapter...</p>
          </div>
        ) : community ? (
          <>
            {/* Community Header Card */}
            <div className="bg-white rounded-3xl border border-slate-200 p-6 sm:p-8 shadow-xs relative overflow-hidden">
              <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-6">
                <div>
                  <div className="flex items-center gap-3 mb-2">
                    <span className="px-3 py-1 bg-indigo-50 border border-indigo-200 text-indigo-700 rounded-full text-xs font-bold uppercase tracking-wider">
                      {community.community_type}
                    </span>
                    <span className="text-xs text-slate-500">
                      👥 {community.members_count} {community.members_count === 1 ? "Member" : "Members"}
                    </span>
                  </div>
                  <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900">
                    {community.name}
                  </h1>
                  <p className="mt-2 text-slate-600 text-sm sm:text-base leading-relaxed max-w-2xl">
                    {community.description}
                  </p>
                </div>

                <div className="flex flex-col sm:flex-row items-center gap-3 shrink-0">
                  <button
                    onClick={handleJoinLeave}
                    className={`w-full sm:w-auto px-6 py-2.5 rounded-xl font-bold text-sm shadow-xs transition ${
                      community.is_member
                        ? "bg-slate-100 text-slate-700 hover:bg-rose-50 hover:text-rose-700 border border-slate-200"
                        : "bg-indigo-600 text-white hover:bg-indigo-700"
                    }`}
                  >
                    {community.is_member ? "Leave Chapter" : "Join Chapter"}
                  </button>
                </div>
              </div>
            </div>

            {/* Tab Controls */}
            <div className="border-b border-slate-200 flex gap-4">
              <button
                onClick={() => setActiveTab("posts")}
                className={`pb-3 text-sm font-bold border-b-2 transition ${
                  activeTab === "posts"
                    ? "border-indigo-600 text-indigo-600"
                    : "border-transparent text-slate-500 hover:text-slate-800"
                }`}
              >
                Discussions & Updates ({posts.length})
              </button>
              <button
                onClick={() => setActiveTab("members")}
                className={`pb-3 text-sm font-bold border-b-2 transition ${
                  activeTab === "members"
                    ? "border-indigo-600 text-indigo-600"
                    : "border-transparent text-slate-500 hover:text-slate-800"
                }`}
              >
                Members Directory ({members.length})
              </button>
            </div>

            {/* Tab 1: Discussions Feed */}
            {activeTab === "posts" && (
              <div className="space-y-6">
                {/* Post Creator (for members only) */}
                {community.is_member ? (
                  <form
                    onSubmit={handleCreatePost}
                    className="bg-white rounded-2xl border border-slate-200 p-5 shadow-xs space-y-3"
                  >
                    <label className="block text-xs font-bold uppercase tracking-wider text-slate-600">
                      Share an update or question with chapter members
                    </label>
                    <textarea
                      rows={3}
                      value={postContent}
                      onChange={(e) => setPostContent(e.target.value)}
                      placeholder="Organizing a meetup, sharing a job lead, or asking for advice..."
                      className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:outline-indigo-500 resize-none"
                    ></textarea>
                    <div className="flex justify-end">
                      <button
                        type="submit"
                        disabled={posting || !postContent.trim()}
                        className="px-5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-xs rounded-xl shadow-xs disabled:opacity-50 transition"
                      >
                        {posting ? "Posting..." : "Post Message"}
                      </button>
                    </div>
                  </form>
                ) : (
                  <div className="bg-indigo-50/50 border border-indigo-100 rounded-2xl p-4 text-center text-sm text-indigo-800">
                    Join this chapter to participate in discussions and post updates.
                  </div>
                )}

                {/* Posts Feed */}
                {posts.length === 0 ? (
                  <div className="bg-white rounded-2xl border border-slate-200 p-8 text-center text-slate-500">
                    <span className="text-3xl">💬</span>
                    <p className="mt-2 text-sm">No discussion posts in this chapter yet.</p>
                  </div>
                ) : (
                  <div className="space-y-4">
                    {posts.map((post) => (
                      <div
                        key={post.id}
                        className="bg-white rounded-2xl border border-slate-200 p-5 shadow-xs space-y-3"
                      >
                        <div className="flex items-center justify-between">
                          <div className="flex items-center gap-3">
                            <div className="w-9 h-9 rounded-full bg-gradient-to-br from-indigo-500 to-blue-600 text-white font-bold flex items-center justify-center text-xs">
                              {post.author_name?.charAt(0) || "U"}
                            </div>
                            <div>
                              <p className="text-sm font-bold text-slate-800 flex items-center gap-1.5">
                                {post.author_name}
                                <span className="text-xs px-2 py-0.5 rounded-md bg-slate-100 text-slate-600 font-medium capitalize">
                                  {post.author_role}
                                </span>
                              </p>
                              <p className="text-xs text-slate-400">
                                {post.created_at ? new Date(post.created_at).toLocaleString() : ""}
                              </p>
                            </div>
                          </div>
                        </div>

                        <p className="text-sm text-slate-700 leading-relaxed whitespace-pre-wrap pl-12">
                          {post.content}
                        </p>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}

            {/* Tab 2: Members Directory */}
            {activeTab === "members" && (
              <div className="bg-white rounded-2xl border border-slate-200 shadow-xs overflow-hidden">
                <div className="divide-y divide-slate-100">
                  {members.map((member) => (
                    <div
                      key={member.id}
                      className="p-4 sm:p-5 flex items-center justify-between hover:bg-slate-50 transition"
                    >
                      <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-full bg-indigo-100 text-indigo-700 font-bold flex items-center justify-center text-sm">
                          {member.user_name?.charAt(0) || "M"}
                        </div>
                        <div>
                          <p className="text-sm font-bold text-slate-900">{member.user_name}</p>
                          <p className="text-xs text-slate-500">{member.user_email}</p>
                        </div>
                      </div>

                      <div className="flex items-center gap-3">
                        <span
                          className={`text-xs font-bold px-2.5 py-1 rounded-full ${
                            member.member_role === "MODERATOR"
                              ? "bg-purple-100 text-purple-800"
                              : "bg-slate-100 text-slate-700"
                          }`}
                        >
                          {member.member_role}
                        </span>
                        <span className="text-xs text-slate-400 hidden sm:inline">
                          Joined {new Date(member.joined_at).toLocaleDateString()}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </>
        ) : null}
      </div>
    </DashboardLayout>
  );
}
