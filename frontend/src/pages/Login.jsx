import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import API from "../services/api";

function Login() {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    email: "",
    password: "",
  });
  const [errorMsg, setErrorMsg] = useState("");
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
    setErrorMsg("");
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setErrorMsg("");

    try {
      const response = await API.post("/auth/login", formData);

      if (response.data.access_token) {
        localStorage.setItem("token", response.data.access_token);
      }
      if (response.data.user) {
        localStorage.setItem("user", JSON.stringify(response.data.user));
      }

      navigate("/dashboard");
    } catch (error) {
      const msg =
        error.response?.data?.detail ||
        (typeof error.response?.data === "string" ? error.response.data : null) ||
        "Login failed. Please check your credentials.";
      setErrorMsg(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="h-screen flex items-center justify-center bg-gray-100 px-4">
      <form
        onSubmit={handleSubmit}
        className="bg-white p-8 rounded-2xl shadow-xl w-full max-w-md border border-gray-100"
      >
        <div className="text-center mb-6">
          <h1 className="text-3xl font-extrabold text-blue-700">AlumniConnect</h1>
          <p className="text-sm text-gray-500 mt-1">Sign in to your account</p>
        </div>

        {errorMsg && (
          <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-4 text-sm">
            {errorMsg}
          </div>
        )}

        <div className="mb-4">
          <label className="block text-sm font-medium text-gray-700 mb-1">Email Address</label>
          <input
            type="email"
            name="email"
            placeholder="you@institution.edu"
            value={formData.email}
            onChange={handleChange}
            required
            className="w-full p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:outline-none transition"
          />
        </div>

        <div className="mb-6">
          <label className="block text-sm font-medium text-gray-700 mb-1">Password</label>
          <input
            type="password"
            name="password"
            placeholder="••••••••"
            value={formData.password}
            onChange={handleChange}
            required
            className="w-full p-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:outline-none transition"
          />
        </div>

        <button
          type="submit"
          disabled={loading}
          className="w-full bg-blue-700 text-white p-3.5 rounded-lg font-semibold hover:bg-blue-800 transition shadow-md disabled:bg-blue-400"
        >
          {loading ? "Signing in..." : "Login"}
        </button>

        <p className="text-center mt-5 text-gray-600 text-sm">
          Don't have an account?{" "}
          <Link to="/signup" className="text-blue-700 font-semibold hover:underline">
            Create Account
          </Link>
        </p>
      </form>
    </div>
  );
}

export default Login;