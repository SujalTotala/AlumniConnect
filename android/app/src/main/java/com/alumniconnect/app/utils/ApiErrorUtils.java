package com.alumniconnect.app.utils;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Response;

public class ApiErrorUtils {

    /**
     * Parses any HTTP Response errorBody into a clean, human-readable user message.
     * Safely handles:
     * - FastAPI 422 validation error arrays: {"detail": [{"loc": [...], "msg": "..."}]}
     * - Standard FastAPI/Flask error dicts: {"detail": "string"}
     * - Legacy/custom error bodies: {"message": "..."} or {"error": "..."}
     * - HTML error bodies (e.g. 502 Bad Gateway from reverse proxy / Render)
     * - Null response / null errorBody
     * - Malformed JSON
     * Never crashes. Never shows raw stack traces or JSON.
     */
    public static String getErrorMessage(Response<?> response) {
        return parseError(response);
    }

    public static String getNetworkErrorMessage(Throwable t) {
        return parseThrowable(t);
    }

    public static String parseError(Response<?> response) {
        if (response == null) {
            return "An unexpected server response was received.";
        }

        int statusCode = response.code();
        String errorString = null;

        try {
            if (response.errorBody() != null) {
                errorString = response.errorBody().string().trim();
            }
        } catch (Exception ignored) {
            // errorBody stream read failure
        }

        if (errorString != null && !errorString.isEmpty()) {
            // Guard against HTML error pages (e.g. 502 from Cloudflare/Render)
            if (errorString.startsWith("<") || errorString.toLowerCase().contains("<html")) {
                return getStatusDefaultMessage(statusCode);
            }

            try {
                // Check if root is JSON Object
                if (errorString.startsWith("{")) {
                    JSONObject json = new JSONObject(errorString);

                    // 1. Check "detail" field
                    if (json.has("detail")) {
                        Object detailObj = json.get("detail");

                        // Case A: detail is a plain String
                        if (detailObj instanceof String) {
                            String msg = ((String) detailObj).trim();
                            if (!msg.isEmpty()) {
                                return sanitizeDetailString(msg);
                            }
                        }

                        // Case B: detail is a JSONArray (standard FastAPI / Pydantic 422)
                        if (detailObj instanceof JSONArray) {
                            JSONArray array = (JSONArray) detailObj;
                            String parsedArrayMsg = parsePydanticValidationArray(array);
                            if (parsedArrayMsg != null && !parsedArrayMsg.isEmpty()) {
                                return parsedArrayMsg;
                            }
                        }

                        // Case C: detail is a nested JSONObject
                        if (detailObj instanceof JSONObject) {
                            JSONObject nested = (JSONObject) detailObj;
                            if (nested.has("msg")) return nested.getString("msg");
                            if (nested.has("message")) return nested.getString("message");
                        }
                    }

                    // 2. Check fallback keys
                    if (json.has("message") && !json.isNull("message")) {
                        String msg = json.getString("message").trim();
                        if (!msg.isEmpty()) return msg;
                    }

                    if (json.has("error") && !json.isNull("error")) {
                        String msg = json.getString("error").trim();
                        if (!msg.isEmpty()) return msg;
                    }
                }
            } catch (Exception ignored) {
                // If JSON parsing fails, fall through to status-code based mapping
            }
        }

        return getStatusDefaultMessage(statusCode);
    }

    /**
     * Parses a Pydantic/FastAPI validation array:
     * [{"loc": ["body", "email"], "msg": "value is not a valid email address", "type": "value_error"}]
     */
    private static String parsePydanticValidationArray(JSONArray array) {
        if (array == null || array.length() == 0) return null;

        List<String> fieldErrors = new ArrayList<>();
        int maxErrors = Math.min(array.length(), 3); // show at most 3 field errors to avoid UI overflow

        for (int i = 0; i < maxErrors; i++) {
            JSONObject errItem = array.optJSONObject(i);
            if (errItem == null) continue;

            String msg = errItem.optString("msg", "Invalid value");
            JSONArray loc = errItem.optJSONArray("loc");
            String fieldName = null;

            if (loc != null && loc.length() > 0) {
                // The last element of loc usually corresponds to the field name
                fieldName = loc.optString(loc.length() - 1, "");
                // Ignore generic wrapper tokens like "body"
                if ("body".equalsIgnoreCase(fieldName) && loc.length() > 1) {
                    fieldName = loc.optString(loc.length() - 2, "");
                }
            }

            if (fieldName != null && !fieldName.isEmpty() && !"body".equalsIgnoreCase(fieldName)) {
                fieldErrors.add(formatFieldName(fieldName) + ": " + msg);
            } else {
                fieldErrors.add(msg);
            }
        }

        if (fieldErrors.isEmpty()) {
            return "Please check the highlighted information.";
        }

        // Join multiple errors with newline
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fieldErrors.size(); i++) {
            if (i > 0) sb.append("\n");
            sb.append(fieldErrors.get(i));
        }

        if (array.length() > maxErrors) {
            sb.append("\n...and ").append(array.length() - maxErrors).append(" more errors");
        }

        return sb.toString();
    }

    /**
     * Translates field identifiers like "graduation_year" or "event_date" to "Graduation Year" or "Event Date".
     */
    private static String formatFieldName(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        String[] parts = raw.split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (!p.isEmpty()) {
                if (i > 0) sb.append(" ");
                sb.append(Character.toUpperCase(p.charAt(0)));
                if (p.length() > 1) sb.append(p.substring(1));
            }
        }
        return sb.toString();
    }

    private static String sanitizeDetailString(String detail) {
        // Strip any residual brackets/quotes if someone stringified a raw structure
        if (detail.startsWith("\"") && detail.endsWith("\"") && detail.length() >= 2) {
            detail = detail.substring(1, detail.length() - 1);
        }
        return detail;
    }

    /**
     * Maps HTTP status codes to user-friendly messages when no specific detail is provided.
     */
    public static String getStatusDefaultMessage(int statusCode) {
        switch (statusCode) {
            case 400:
                return "Invalid request. Please verify your input.";
            case 401:
                return "Your session has expired. Please log in again.";
            case 403:
                return "You do not have permission to perform this action.";
            case 404:
                return "Requested information was not found.";
            case 409:
                return "This action conflicts with an existing record.";
            case 422:
                return "Please check the highlighted information.";
            case 429:
                return "Too many requests. Please try again shortly.";
            case 500:
            case 502:
            case 503:
            case 504:
                return "Server error. Please try again later.";
            default:
                return "Request failed (HTTP " + statusCode + ").";
        }
    }

    /**
     * Parses Throwable from onFailure callbacks into clean user-facing network errors.
     * Never exposes raw stack traces, technical class names, or "null".
     */
    public static String parseThrowable(Throwable t) {
        if (t == null) {
            return "An unknown network error occurred. Please retry.";
        }

        if (t instanceof UnknownHostException) {
            return "No internet connection. Please verify your network and retry.";
        }

        if (t instanceof SocketTimeoutException) {
            return "Request timed out. The server may be waking up, please try again.";
        }

        if (t instanceof ConnectException) {
            return "Unable to connect to server. Please check your connection or retry shortly.";
        }

        if (t instanceof IOException) {
            return "Network error. Please check your internet connection.";
        }

        String msg = t.getMessage();
        if (msg != null && !msg.trim().isEmpty() && !msg.equalsIgnoreCase("null")) {
            return "Network error: " + msg;
        }

        return "Unable to communicate with server. Please check connection and retry.";
    }
}
