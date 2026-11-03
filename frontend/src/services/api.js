import axios from "axios";

// Runtime resolution of API Base URL:
// 1. On Vercel deployments (*.vercel.app), use relative path ("") to route through Vercel's reverse proxy rewrites, completely bypassing browser CORS preflight.
// 2. On localhost development, connect to local backend (http://127.0.0.1:8000) or VITE_API_BASE_URL.
// 3. Otherwise, fall back to production Render backend.
let API_BASE = "";
if (typeof window !== "undefined" && window.location && window.location.hostname.includes("vercel.app")) {
  API_BASE = "";
} else if (import.meta.env.VITE_API_BASE_URL) {
  API_BASE = import.meta.env.VITE_API_BASE_URL;
} else if (typeof window !== "undefined" && window.location && (window.location.hostname === "localhost" || window.location.hostname === "127.0.0.1")) {
  API_BASE = "http://127.0.0.1:8000";
} else {
  API_BASE = "https://alumniconnect-bwoi.onrender.com";
}

const API = axios.create({
  baseURL: API_BASE,
  headers: {
    "Content-Type": "application/json",
  },
});

// Request Interceptor: Attach JWT Bearer token if available
API.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response Interceptor: Handle 401 Unauthorized globally
API.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem("token");
      localStorage.removeItem("user");
      if (window.location.pathname !== "/" && window.location.pathname !== "/signup") {
        window.location.href = "/";
      }
    }
    return Promise.reject(error);
  }
);

export default API;
