import axios from "axios";

// Require explicit API base URL in production builds to avoid accidental localhost targeting.
const envBase = import.meta.env.VITE_API_BASE_URL;
const isProd = import.meta.env.MODE === "production";
if (isProd && !envBase) {
  throw new Error("VITE_API_BASE_URL must be set for production builds — aborting startup to avoid accidental localhost targeting.");
}
const API = axios.create({
  baseURL: envBase || "http://127.0.0.1:8000",
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
