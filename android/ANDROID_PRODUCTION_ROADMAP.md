# ALUMNICONNECT — PRODUCTION-GRADE ANDROID ROADMAP

**Project:** AlumniConnect  
**Package:** `com.alumniconnect.app`  
**Target:** Production-Grade Hardening & Polished APK Release  
**Current Phase:** Production Hardening Pass 2 Completed (UX + State + Lifecycle + Maintainability)  

---

## ROADMAP OVERVIEW

```
+-----------------------------------------------------------------------------+
|  PHASE A: CRITICAL BUGS & DATA INTEGRITY                          [DONE]    |
|  * Universal FastAPI 422 JSON parser                              [DONE]    |
|  * Splash offline / timeout ejection prevention                   [DONE]    |
|  * Global 401 session ejection & redirect                         [DONE]    |
+-----------------------------------------------------------------------------+
                                       |
                                       v
+-----------------------------------------------------------------------------+
|  PHASE B: NETWORK RELIABILITY & DUPLICATE PREVENTION              [DONE]    |
|  * Double-tap action guards (RSVP, Mentorship, Events, Opps)      [DONE]    |
|  * Extended timeouts (60s) for Render cold starts                 [DONE]    |
|  * Handler lifecycle cleanup in Fragments (onDestroyView)         [DONE]    |
|  * Safe error message fallbacks (no "Network error: null")        [DONE]    |
+-----------------------------------------------------------------------------+
                                       |
                                       v
+-----------------------------------------------------------------------------+
|  PHASE C: PROFESSIONAL UX & STATE MANAGEMENT                      [DONE]    |
|  * Initial loading indicators on Detail Activities                [DONE]    |
|  * Bottom navigation tab instance retention (no state loss)       [DONE]    |
|  * Form input validators (Email syntax, URLs)                     [DONE]    |
|  * Pull-to-refresh on all list screens & dashboard               [DONE]    |
|  * Standard Loading / Empty / Error / Retry states                [DONE]    |
+-----------------------------------------------------------------------------+
                                       |
                                       v
+-----------------------------------------------------------------------------+
|  PHASE D: FEATURE COMPLETION & POLISH                             [DONE]    |
|  * Dashboard polish (Home pull-to-refresh & profile sync)         [DONE]    |
|  * Profile strength completion calculation                        [DONE]    |
|  * Clickable external links (LinkedIn, GitHub, Meeting, Apply)    [DONE]    |
|  * About AlumniConnect & Platform Status Dialog                   [DONE]    |
|  * Notification polling lifecycle safety (onPause/onStop/destroy) [DONE]    |
+-----------------------------------------------------------------------------+
                                       |
                                       v
+-----------------------------------------------------------------------------+
|  PHASE E: SECURITY & PERFORMANCE HARDENING                        [PARTIAL] |
|  * NetworkSecurityConfig: restrict cleartext to emulator debug    [DONE]    |
|  * EncryptedSharedPreferences for JWT token storage               [DONE]    |
|  * ProGuard / R8 minification & keep rules in release build       [DEFERRED]|
|  * String resources cleanup & accessibility                       [DONE]    |
+-----------------------------------------------------------------------------+
                                       |
                                       v
+-----------------------------------------------------------------------------+
|  PHASE F: FINAL APK QA & SIGNING VERIFICATION                     [PENDING] |
|  * Release APK assembly & lint verification                       [DONE]    |
|  * Real-device test matrix (orientation, network drop, cold boot) [MANUAL]  |
|  * Final production APK handoff                                   [PENDING] |
+-----------------------------------------------------------------------------+
```

---

## IMPLEMENTATION STATUS (PASS 1 & PASS 2 AUDIT ITEMS)

| Component / Feature | Scope | Status | Notes |
|:---|:---|:---|:---|
| **Bottom Navigation State Retention** | Navigation / UX | **DONE** / **MANUAL TEST REQUIRED** | `MainActivity` uses `show/hide` transaction pattern by tag; preserves scroll position, search query, selected filters, and data lists. |
| **Standard List Screen State Management** | Core Fragments | **DONE** / **MANUAL TEST REQUIRED** | Standardized across Alumni, Events, Mentors, Sent, Received, Opportunities, Notifications with LOADING, SUCCESS, EMPTY, ERROR + Retry. |
| **Pull to Refresh** | Core Fragments | **DONE** / **MANUAL TEST REQUIRED** | Implemented on Dashboard/Home, Alumni, Events, Mentors, Sent, Received, Opportunities, Notifications with safe cancel on success/error. |
| **Search UX** | Search / Filters | **DONE** / **MANUAL TEST REQUIRED** | 400ms debounce, whitespace trimming, restores complete list when cleared, state preserved on tab switch. |
| **Filter State Retention** | Filters | **DONE** / **MANUAL TEST REQUIRED** | Department, Graduation Year, Mentorship, Event Type, Opportunity Type filters preserved when navigating tabs. |
| **Dashboard Polish** | Home | **DONE** / **MANUAL TEST REQUIRED** | Pull-to-refresh available for students/alumni; syncs name/role changes from backend profile. |
| **Profile UX & Strength** | Profile | **DONE** / **MANUAL TEST REQUIRED** | Profile strength completion %; clickable LinkedIn/GitHub links via `UrlUtils`; null-safe text rendering. |
| **Event UX & RSVP Confirmation** | Events | **DONE** / **MANUAL TEST REQUIRED** | Material confirmation dialog for Cancel RSVP; safe online meeting launch; consistent formatting. |
| **Mentorship UX & Confirmation** | Mentorship | **DONE** / **MANUAL TEST REQUIRED** | Consistent status badges (Pending, Accepted, Rejected, Completed); Material confirmation dialogs on Decline/Complete. |
| **Opportunity UX** | Careers | **DONE** / **MANUAL TEST REQUIRED** | Safe Apply link opening via `UrlUtils`; Material confirmation dialog on delete. |
| **Notification Polling Lifecycle** | Notifications | **DONE** / **MANUAL TEST REQUIRED** | Polling pauses on `onPause()`, stops on `onStop()`/`onDestroy()`, resumes on `onResume()`. |
| **Confirmation Dialog Consistency** | UX / Dialogs | **DONE** / **MANUAL TEST REQUIRED** | `MaterialAlertDialogBuilder` used consistently for Logout, Cancel RSVP, Decline Mentorship, Complete Mentorship, Delete Opportunity. |
| **Form & Keyboard UX** | Forms / Input | **DONE** / **MANUAL TEST REQUIRED** | `imeOptions="actionNext"` and `actionDone"`; keyboard dismissed on submit via `KeyboardUtils`. |
| **Date & Time Input** | Forms / Input | **DONE** / **MANUAL TEST REQUIRED** | Native `DatePickerDialog` & `TimePickerDialog` popups format to backend standard (YYYY-MM-DD / HH:MM:00). |
| **Safe External Links** | Utilities | **DONE** / **MANUAL TEST REQUIRED** | Centralized `UrlUtils.openUrlSafely()` validates URLs and prevents crashes when no browser is installed. |
| **Accessibility & Strings** | Resources | **DONE** | Common empty states, confirmation dialogs, error messages, and content descriptions moved to `strings.xml`. |
| **Lifecycle Safety** | Framework | **DONE** | Added `isAdded()` and `getContext() != null` guards across all fragment async callbacks; `onDestroyView()` removes handler runnables. |
| **About / Settings Screen** | Dialog / UX | **DONE** / **MANUAL TEST REQUIRED** | Added About AlumniConnect dialog with Version 1.0, tagline, production connectivity status, and logout shortcut. |
| **ApiErrorUtils Centralization** | Core Network | **DONE** | Crash-proof parser handling FastAPI 422 validation arrays, status codes, and network exceptions. |
| **Encrypted Session Storage** | Security | **DONE** / **MANUAL TEST REQUIRED** | Upgraded `SessionManager` to Keystore `EncryptedSharedPreferences` with automatic plaintext migration. |
| **Network Security Config** | Security | **DONE** | Strict HTTPS on production; cleartext HTTP allowed only for `10.0.2.2`/localhost emulator debug. |
| **60-Second OkHttp Timeouts** | Network | **DONE** | 60s connect, read, and write timeouts for Render free-tier cold starts. |
| **Duplicate Write Protection** | Forms / API | **DONE** / **MANUAL TEST REQUIRED** | Debounce locks and progress indicators across all 14 write operations. |
| **Build Verification** | Build | **DONE** | Both `gradlew.bat assembleDebug` and `gradlew.bat assembleRelease` pass with 0 errors. |
