import axios from "axios";

// If running in browser on a Vercel deployment (*.vercel.app), use relative path ("")
// so that Vercel rewrites proxy all API requests server-side to Render without CORS issues.
// Otherwise, use VITE_API_BASE_URL or default to Render backend.
let API_BASE = import.meta.env.VITE_API_BASE_URL;

if (!API_BASE) {
  if (typeof window !== "undefined" && window.location && window.location.hostname.includes("vercel.app")) {
    API_BASE = "";
  } else {
    API_BASE = "https://alumniconnect-bwoi.onrender.com";
  }
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
