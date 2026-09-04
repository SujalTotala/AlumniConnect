# ALUMNICONNECT — PRODUCTION-GRADE ANDROID APPLICATION AUDIT

**Project:** AlumniConnect  
**Target Package:** `com.alumniconnect.app`  
**Platform:** Native Android (Java 8 / Android SDK 34 / minSdk 24)  
**Backend:** FastAPI (Python / PostgreSQL) — Production: `https://alumniconnect-bwoi.onrender.com/`  
**Audit Date:** 2026-09-04  
**Audit Status:** COMPLETE  

---

## EXECUTIVE SUMMARY

A rigorous, exhaustive architectural and code audit of the AlumniConnect Android native application was conducted. The codebase contains **12 Activities**, **9 Fragments**, **5 RecyclerView Adapters**, **19 Data Models**, **7 Repositories**, and a centralized **Retrofit API Service** communicating with the FastAPI backend.

The application compiles cleanly with zero errors on both Debug and Release build variants:
- `.\gradlew.bat clean`: **PASS** (6s)
- `.\gradlew.bat assembleDebug`: **PASS** (38s) — Generated: `app-debug.apk` (6.56 MB)
- `.\gradlew.bat assembleRelease` & `lintVitalRelease`: **PASS** (51s) — Generated: `app-release-unsigned.apk` (5.30 MB)

The application possesses solid core business features (Authentication, Alumni Directory with search and filters, Events with RSVP and attendee roster, Mentorship lifecycle workflow, Career Opportunities with external application linkout, Notifications with background unread count polling and deep links, and dynamic Profile editing). However, multiple production hardening deficiencies were identified, including FastAPI 422 validation array parsing errors, cold-start offline ejection on splash, lack of navigation upon silent 401 session clearing, unencrypted token storage, unminified release builds, global cleartext permissions, and missing initial loading states on detail activities.

---

## ARCHITECTURAL MAP

```
+-----------------------------------------------------------------------------------+
|                                 APPLICATION LAYER                                 |
+-----------------------------------------------------------------------------------+
|  Activities:                                                                      |
|    * SplashActivity (Launcher)                                                    |
|    * LoginActivity                                                                |
|    * RegisterActivity                                                             |
|    * MainActivity (Hosts BottomNavigationView & Fragment Container)               |
|    * AlumniDetailsActivity                                                        |
|    * EventDetailsActivity                                                         |
|    * EventRegistrationsActivity                                                   |
|    * CreateEventActivity                                                          |
|    * OpportunityDetailsActivity                                                   |
|    * CreateOpportunityActivity                                                    |
|    * NotificationsActivity                                                        |
|    * EditProfileActivity                                                          |
|                                                                                   |
|  Fragments:                                                                       |
|    * HomeFragment (Admin Statistics / Student & Alumni Quick Actions)             |
|    * AlumniFragment (Directory, Filters, Search)                                  |
|    * EventsFragment (Upcoming Events, Type Chips, RSVP)                           |
|    * MentorshipFragment (Hosts TabLayout + ViewPager2)                           |
|        - AvailableMentorsFragment                                                 |
|        - SentRequestsFragment                                                     |
|        - ReceivedRequestsFragment                                                 |
|    * OpportunitiesFragment (Career Postings, Filters, Search)                     |
|    * ProfileFragment (Role-aware Profile Display)                                 |
+-----------------------------------------------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|                                 REPOSITORY LAYER                                  |
+-----------------------------------------------------------------------------------+
|  * AdminRepository        * AlumniRepository         * EventRepository            |
|  * MentorshipRepository   * NotificationRepository   * OpportunityRepository      |
|  * ProfileRepository                                                              |
+-----------------------------------------------------------------------------------+
                                         |
                                         v
+-----------------------------------------------------------------------------------+
|                                   NETWORK LAYER                                   |
+-----------------------------------------------------------------------------------+
|  * ApiClient (Retrofit 2 + OkHttpClient, 30s timeouts, HttpLoggingInterceptor)    |
|  * AuthInterceptor (Injects Bearer token; handles 401 clearSession)               |
|  * ApiService (23 endpoints mapped to FastAPI backend)                            |
|  * SessionManager (SharedPreferences: token, user info, login state)              |
+-----------------------------------------------------------------------------------+
```

---

## A. EXISTING WORKING FEATURES

The following features have been verified and are actively functional in the current codebase:

1. **Authentication & Session System:**
   - User login with email and password via `POST /auth/login`.
   - Registration with role selection (`student` or `alumni`) via `POST /auth/register`.
   - Session persistence in `SessionManager` storing JWT `access_token`, `user_id`, `user_name`, `user_email`, `user_role`, and `is_logged_in`.
   - Splash screen automated session check verifying validity with `GET /auth/me`.
   - Explicit logout functionality that wipes preferences and cleans the activity task stack (`FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK`).

2. **Dashboard & Role Architecture:**
   - Dynamic `HomeFragment`:
     - **Admin Role:** Live statistics cards fetching `GET /admin/statistics` (total users, total alumni, total events, active mentors, total opportunities, pending mentorship requests) with SwipeRefresh.
     - **Student & Alumni Roles:** Quick action navigation cards directing into respective sections.
   - Dynamic toolbar subtitle indicating current user role in uppercase.

3. **Alumni Directory:**
   - Live query fetching via `GET /alumni/`.
   - Search with 400ms debounce across name, email, department, company, job role, skills, and location.
   - Filter chips for **Mentorship Available** (toggle), **Department** (dialog input), and **Graduation Year** (dialog input).
   - "Clear All Filters" chip that dynamically shows/hides.
   - Differentiated empty states for filtered queries vs. empty directory.
   - Smooth navigation into `AlumniDetailsActivity`.

4. **Alumni Profile Details:**
   - Fresh data re-fetch from `GET /alumni/{id}`.
   - Hero header with computed avatar initials circle and mentor badge.
   - Dynamic visibility of metadata rows (only non-empty rows render).
   - External launch for LinkedIn and GitHub profiles via `Intent.ACTION_VIEW`.
   - Mentorship Request modal trigger (exclusively visible when a Student views an available mentor).

5. **Events Management:**
   - Event list with type filter chips (Alumni Meet, Webinar, Workshop, Networking, Career Guidance, Reunion, Guest Lecture).
   - Real-time search with 400ms debounce.
   - Event details screen displaying formatted date (`25 Aug 2026`) and time (`2:30 PM`), location, online meeting status, and attendee count.
   - Direct RSVP (`POST /events/{id}/register`) and RSVP cancellation (`DELETE /events/{id}/register`) updating user status and attendee counter.
   - Online meeting launcher button opening URL via browser intent.
   - Attendee roster screen (`EventRegistrationsActivity`) accessible to Administrators and the Event Creator.
   - Event creation screen (`CreateEventActivity`) for Admin and Alumni roles with interactive DatePicker and TimePicker.

6. **Mentorship Lifecycle System:**
   - Role-customized ViewPager2 tabs:
     - **Student:** "Available Mentors" + "Sent Requests".
     - **Alumni:** "Available Mentors" + "Received Requests".
     - **Admin:** "Available Mentors" + "Sent Requests" + "Received Requests".
   - Available Mentors tab with client-side fallback search across name, skills, company, and department.
   - Mentorship request modal allowing students to write a personalized introduction note.
   - Sent Requests list with color-coded status badges: `PENDING` (amber), `ACCEPTED` (green), `REJECTED` (red), `COMPLETED` (blue), and response note visibility.
   - Received Requests list for mentors with inline action buttons to **Accept** (with optional response note), **Reject** (with optional response note), or **Complete**.
   - Student and Alumni mutual mentorship completion action.

7. **Career Opportunities:**
   - Opportunity list with type filter chips (Internship, Full-Time Job, Referral, Hackathon, Scholarship, Workshop).
   - Search with 400ms debounce across title, company, and description.
   - Opportunity details displaying company, location, deadline with "Applications Closed" warning if passed, poster name, and full description.
   - "Apply Now" button opening external application link in web browser.
   - Delete opportunity feature for Administrators and post creators with confirmation dialog.
   - Post opportunity screen (`CreateOpportunityActivity`) for Admin and Alumni roles with interactive DatePicker for application deadlines.

8. **Notifications & Real-time Badge:**
   - Real-time unread notification count badge in MainActivity toolbar using Google Material `BadgeDrawable`.
   - Background polling every 30 seconds (`POLL_INTERVAL_MS = 30000`), cleanly started on `onResume()` and halted on `onPause()`.
   - Full notifications screen (`NotificationsActivity`) with filter chips for "All" and "Unread".
   - Relative time formatting ("Just now", "5 min ago", "2 hr ago", "Yesterday", "18 Aug 2026").
   - Individual "Mark as Read" action and toolbar menu "Mark All as Read".
   - Deep linking: tapping a notification automatically marks it read and routes to the relevant section (`mentorship`, `events`, `opportunities`).

9. **Profile & Edit Profile:**
   - Profile tab displaying role badge, email, and dynamic rows depending on user type.
   - Edit Profile screen pre-filled from current profile data.
   - Role-specific inputs: Alumni sees Company, Job Role, Department, Graduation Year, Location, LinkedIn, GitHub, Mentorship Availability checkbox; Student sees Branch, Academic Year, Skills, Career Interests, Bio.
   - Immediate synchronization of updated user profile name into local session manager without requiring re-login.

10. **Build & Release Pipelines:**
    - Gradle clean, assembleDebug, assembleRelease, and lintVitalRelease compile without a single error.

---

## B. CONFIRMED BUGS

### 1. [CRITICAL] FastAPI 422 Validation Error JSON Parsing Exception
- **Affected Files:** `LoginActivity.java`, `RegisterActivity.java`, `EditProfileActivity.java`, `CreateEventActivity.java`, `CreateOpportunityActivity.java`, `AlumniDetailsActivity.java`, `EventDetailsActivity.java`
- **Root Cause:** When FastAPI rejects a request with HTTP 422 Unprocessable Entity, the `detail` JSON field contains an **Array of Objects** (`[{"loc": ["body", "field"], "msg": "Field required", "type": "missing"}]`), NOT a String. The app's error parsing methods execute `jsonObject.getString("detail")`. This throws an unhandled `org.json.JSONException: Value [...] at detail of type org.json.JSONArray cannot be converted to JSONObject/String`. The code then jumps straight to the `catch` block and falls back to a misleading message (e.g. "Invalid email or password" or "Server error (HTTP 422)").
- **Impact:** Users receiving validation errors are shown inaccurate, confusing error messages and cannot diagnose what field was rejected.

### 2. [HIGH] Splash Screen Network Failure Automatically Ejects Authenticated Users
- **Affected File:** `SplashActivity.java` (Lines 55–58)
- **Root Cause:** In `SplashActivity.checkAuthenticationSession()`, if the user has an existing saved token, the app queries `GET /auth/me`. In the `onFailure(Call<User> call, Throwable t)` callback, the app unconditionally calls `navigateToLogin()`.
- **Impact:** If a user opens the app while offline (or during Render free-tier cold-start latency exceeding the timeout), they are forcefully booted to the Login screen. The app fails to utilize the local cached session or allow retry.

### 3. [HIGH] Silent Global 401 Session Clearance without Navigation Redirect
- **Affected File:** `AuthInterceptor.java` (Lines 34–37)
- **Root Cause:** When an API call returns HTTP 401 Unauthorized (e.g., expired JWT), `AuthInterceptor` calls `sessionManager.clearSession()`. However, it does NOT broadcast an event, invoke a navigation callback, or redirect the user to `LoginActivity`.
- **Impact:** The user remains sitting on whatever screen they were using (e.g., Directory, Event Details, Mentorship). Subsequent actions fail silently or throw errors because the token in memory is gone, leaving the app in a broken, half-authenticated state until app restart.

### 4. [MEDIUM] Mentorship Request Duplicate Dispatch on Rapid Tap
- **Affected File:** `AlumniDetailsActivity.java` (Lines 206–238)
- **Root Cause:** In `showSendRequestDialog()`, when the user taps "Send", the dialog dismisses and the API call is dispatched without a debounce guard or disabling the trigger button. Rapid double-tapping can fire multiple simultaneous `POST /mentorship/requests` calls.
- **Impact:** Can send duplicate requests before the backend transaction completes, creating duplicate entries or tripping 400 Bad Request responses.

### 5. [MEDIUM] Notification Mark-As-Read Duplicate Request on Rapid Tap
- **Affected File:** `NotificationAdapter.java` (Lines 113–116)
- **Root Cause:** The `btn_notif_mark_read` TextView button click is not disabled or debounced when tapped. Rapid tapping sends multiple concurrent `PUT /notifications/{id}/read` requests.
- **Impact:** Unnecessary network load and potential race conditions in list state updates.

### 6. [MEDIUM] Fragment Debounce Handlers Not Cleaned Up on View Destroy
- **Affected Files:** `AlumniFragment.java`, `EventsFragment.java`, `AvailableMentorsFragment.java`, `OpportunitiesFragment.java`
- **Root Cause:** Each of these fragments initializes a `Handler(Looper.getMainLooper())` for search debouncing. None of these fragments implement `onDestroyView()` to call `debounceHandler.removeCallbacksAndMessages(null)`.
- **Impact:** If a user types into the search box and quickly switches tabs or navigates away, the runnable fires against a destroyed view hierarchy, causing memory leaks and attempted updates on detached fragments.

### 7. [MEDIUM] Fragment State Discarded on Every Bottom Navigation Tab Tap
- **Affected File:** `MainActivity.java` (Lines 72–91)
- **Root Cause:** The BottomNavigationView listener executes `loadFragment(new SomeFragment())` unconditionally on every tab selection.
- **Impact:** Switching tabs destroys the existing fragment and creates a brand-new instance. The user loses their scroll position, typed search queries, selected filter chips, and triggers duplicate initial network requests every time they switch back to a tab.

### 8. [LOW] Potential "Network error: null" UI Messages
- **Affected Files:** All Activities and Fragments with `t.getMessage()` in `onFailure`.
- **Root Cause:** `t.getMessage()` is nullable in Java (for instance, on certain `IOException` or `NullPointerException` conditions).
- **Impact:** The user is presented with unhelpful text such as `"Network error: null"`.

---

## C. MISSING STANDARD FEATURES

1. **[HIGH] Proactive Network Connectivity Monitoring:**
   - The app has no global `ConnectivityManager` or network callback listener. It only discovers the device is offline when a network request fails after a 30-second delay. An offline banner or snackbar indicating "Offline — showing cached content" is missing.

2. **[HIGH] Initial Loading Progress Bars on Detail Screens:**
   - `AlumniDetailsActivity`, `EventDetailsActivity`, and `OpportunityDetailsActivity` lack initial loading indicators (ProgressBar or Skeleton shimmer) while fetching data from the backend. The screens appear blank/empty until the network response arrives.

3. **[MEDIUM] Email Format Validation:**
   - Neither `LoginActivity` nor `RegisterActivity` verifies that the entered email conforms to valid email syntax (`Patterns.EMAIL_ADDRESS`). A user can submit invalid formats like `test@` or `abc`.

4. **[MEDIUM] In-Place TextInputLayout Error Highlighting:**
   - Input forms show error messages in a single bottom `TextView` (`tv_error`) instead of highlighting the exact input field with `TextInputLayout.setError()`. Users must look at the bottom of the screen to identify input mistakes.

5. **[MEDIUM] URL Validation on Create/Edit Forms:**
   - LinkedIn URL, GitHub URL, Event Meeting URL, and Opportunity Application URL inputs accept arbitrary text without verifying proper URL syntax (`Patterns.WEB_URL` or scheme check). Attempting to open malformed URLs subsequently throws exceptions handled only by toasts.

6. **[MEDIUM] Date / Deadline Sanity Range Checks:**
   - Event creation and Opportunity creation allow selecting dates or deadlines in the past. There is no minimum date enforcement (`DatePickerDialog.getDatePicker().setMinDate(...)`).

7. **[MEDIUM] Administrator User Management UI:**
   - The backend exposes `GET /admin/users` to view registered students/alumni and toggle their status. The Android app has an Admin statistics view, but has no UI screen for User Management.

8. **[LOW] Upcoming vs. Past Events Separation:**
   - The Events view lists all events chronologically by ID descending without a tab or toggle separating upcoming future events from past events.

9. **[OPTIONAL] Device Calendar Export:**
   - No feature to export a confirmed event RSVP into the device's native Google Calendar (`CalendarContract.ACTION_INSERT`).

---

## D. UX PROBLEMS

1. **[HIGH] Tab Switch Flashing and State Loss:**
   - Due to instantiating new fragments on every BottomNav selection, the screen flashes blank progress bars every time a user returns to a tab they were just looking at.
2. **[MEDIUM] Backend Cold Start Friction:**
   - Render.com free-tier instances spin down after inactivity, requiring 30 to 50 seconds to boot on the first incoming request. During this period, the app shows a generic spinner with no user feedback explaining that the server is waking up.
3. **[MEDIUM] Off-Screen Error Notifications on Long Forms:**
   - In `EditProfileActivity`, `CreateEventActivity`, and `CreateOpportunityActivity`, the error TextView is located at the bottom of the form. If a user leaves the top field blank and clicks "Save" while scrolled down, they may not immediately realize what failed.
4. **[LOW] Hardcoded String Resources:**
   - UI strings across layouts and Java source files are hardcoded in English rather than referenced via `@string/...` in `strings.xml`. This impedes localization and centralized copy adjustments.

---

## E. API PROBLEMS

1. **[HIGH] Unhandled Pydantic 422 Validation Error Structure:**
   - Described in Bug #1. FastAPI sends an array of error objects which the Android app cannot parse with `getString("detail")`.
2. **[MEDIUM] Aggressive 30-Second Timeout for Free-Tier Backend:**
   - `ApiClient.java` sets connect, read, and write timeouts to 30 seconds. Because Render.com cold starts often take 35–45 seconds, the mobile app frequently times out with `SocketTimeoutException` just before the backend finishes waking up.
3. **[MEDIUM] Lack of Pagination on Unbounded Endpoints:**
   - `GET /alumni/`, `GET /events/`, `GET /opportunities/`, and `GET /notifications/` do not use pagination (no `limit`/`offset` or `page` parameters). As the alumni database grows past hundreds or thousands of rows, loading the entire dataset into memory in a single JSON payload will cause latency and potential out-of-memory errors on low-end devices.
4. **[LOW] Absence of Automatic Request Retries:**
   - OkHttpClient does not employ a retry interceptor for idempotent GET requests that fail due to transient connection drops.

---

## F. SECURITY FINDINGS

1. **[HIGH] Global Cleartext Traffic Enabled in Manifest:**
   - `AndroidManifest.xml` specifies `android:usesCleartextTraffic="true"` globally. While necessary for local emulator debugging (`http://10.0.2.2:8000`), in production it permits unencrypted HTTP transmission across all network calls.
   - *Remediation:* Create a `network_security_config.xml` that restricts cleartext HTTP exclusively to `10.0.2.2` and localhost, while enforcing strict HTTPS for all other endpoints.
2. **[HIGH] Plaintext JWT Storage in SharedPreferences:**
   - `SessionManager.java` stores the user's JWT access token in plain XML (`AlumniConnectPrefs.xml`). On rooted devices or via ADB backup, this token can be extracted.
   - *Remediation:* Migrate to `EncryptedSharedPreferences` provided by AndroidX Security (`androidx.security:security-crypto`).
3. **[MEDIUM] Minification and Code Obfuscation Disabled in Release:**
   - `android/app/build.gradle` has `minifyEnabled false` under `buildTypes.release`. The release APK contains full symbol names, unoptimized bytecode, and unstripped unused dependencies.
   - *Remediation:* Enable `minifyEnabled true` with appropriate ProGuard rules for Retrofit, OkHttp, and Gson models.
4. **[LOW] Unrestricted Android Backup:**
   - `AndroidManifest.xml` has `android:allowBackup="true"` without defining `android:fullBackupContent` rules, meaning local app data and preferences could be extracted via device backup tools.

---

## G. RELIABILITY FINDINGS

1. **[HIGH] Cold-Start Connection Timeouts:**
   - The 30s timeout causes high failure rates when users launch the app after backend idle periods. Increasing the connect timeout to 60s or adding an automatic single retry on cold-start timeouts will dramatically increase initial connection success.
2. **[MEDIUM] Uncancelled Asynchronous Retrofit Calls:**
   - Repositories return raw `Call<T>` objects that are enqueued directly from Activities and Fragments without tracking or cancelling them in `onDestroy()`. If an Activity finishes while a call is pending, the callback retains the Activity context in memory until the call completes.
3. **[MEDIUM] Search Debounce Leaks:**
   - Described in Bug #6. Uncancelled Handler callbacks continue running after fragment views have been destroyed.

---

## H. PERFORMANCE FINDINGS

1. **[MEDIUM] Full List Invalidation with `notifyDataSetChanged()`:**
   - All 5 adapters (`AlumniAdapter`, `EventAdapter`, `MentorshipRequestAdapter`, `NotificationAdapter`, `OpportunityAdapter`) call `notifyDataSetChanged()` on every filter change, search update, or status toggle. This forces the RecyclerView to re-bind every visible view holder and causes noticeable UI stutter on large lists.
   - *Remediation:* Implement `DiffUtil` or `ListAdapter` for incremental updates.
2. **[MEDIUM] No Response Caching:**
   - The app has no OkHttp cache configured (`OkHttpClient.Builder.cache(...)`). Every time a user switches tabs or returns to a screen, the app makes fresh network round trips over the internet.
3. **[LOW] Inline Layout Inflations:**
   - Custom field rows in `ProfileFragment` are inflated programmatically via code loops. While acceptable for a few fields, pre-inflated XML or a lightweight RecyclerView is cleaner.

---

## I. RECOMMENDED IMPROVEMENTS

1. **Centralized Error Parser (`ApiErrorUtils`):**
   - Implement a unified utility that inspects Retrofit error bodies, correctly distinguishing between a plain string detail, a nested JSON object detail, and a FastAPI 422 JSON array of errors (`[{"loc": [...], "msg": "..."}]`), extracting clean human-readable messages.
2. **Global Auth Ejection Broadcast / Event:**
   - When `AuthInterceptor` intercepts a 401 Unauthorized, dispatch an intent or callback that immediately routes the user to `LoginActivity` with a clear message: "Session expired. Please log in again."
3. **Graceful Splash Screen Offline Fallback:**
   - If `SplashActivity` fails to reach `/auth/me` due to an `IOException` or timeout, but a valid token is present in storage, allow proceeding into `MainActivity` in offline/cached mode rather than ejecting to Login.
4. **Network Security Configuration:**
   - Add `res/xml/network_security_config.xml` to enforce HTTPS for production domains while allowing cleartext HTTP only on `10.0.2.2` for debug builds.
5. **Encrypted Session Manager:**
   - Upgrade `SessionManager` to use `MasterKey` and `EncryptedSharedPreferences` for secure token storage.
6. **Fragment View Retention in `MainActivity`:**
   - Retain fragment instances or use `FragmentTransaction.show()` / `FragmentTransaction.hide()` instead of destroying and re-instantiating fragments on every bottom tab switch.
7. **Input Validation with `TextInputLayout`:**
   - Wire `addTextChangedListener` or form submit triggers directly into `TextInputLayout.setError()` for email formatting, password length, required fields, and URL syntax.
8. **Detail Screens Initial Loading Indicators:**
   - Add initial progress spinners or shimmer effects to `AlumniDetailsActivity`, `EventDetailsActivity`, and `OpportunityDetailsActivity`.
9. **RecyclerView DiffUtil & Debounce Cleanup:**
   - Implement `DiffUtil.Callback` in adapters and remove pending Handler callbacks in `onDestroyView()`.
10. **ProGuard / R8 Release Optimization:**
    - Enable `minifyEnabled true` in `buildTypes.release` with robust ProGuard rules preserving Gson models and Retrofit interfaces.

---

## J. PRIORITY CLASSIFICATION

| ID | Issue Description | Category | Priority |
|:---|:---|:---|:---|
| **BUG-01** | FastAPI 422 validation array parsing exception in error handlers | Bug | **CRITICAL** |
| **SEC-01** | Global cleartext traffic enabled in AndroidManifest | Security | **HIGH** |
| **SEC-02** | Plaintext JWT token storage in SharedPreferences | Security | **HIGH** |
| **BUG-02** | Splash screen network failure/timeout forcefully ejects user to Login | Bug / UX | **HIGH** |
| **BUG-03** | Silent 401 session clearance leaves user on broken active screen | Bug / Auth | **HIGH** |
| **UX-01** | Missing initial loading progress bars on Details screens | UX | **HIGH** |
| **UX-02** | Fragment instances destroyed & recreated on every BottomNav tab click | UX / Perf | **HIGH** |
| **NET-01** | OkHttp 30s timeout causes frequent timeouts on backend cold starts | Reliability | **MEDIUM** |
| **BUG-04** | Mentorship request duplicate dispatch on rapid tap | Reliability | **MEDIUM** |
| **BUG-05** | Notification mark-as-read duplicate request on rapid tap | Reliability | **MEDIUM** |
| **BUG-06** | Search debounce handlers not cleared in `onDestroyView()` | Reliability | **MEDIUM** |
| **VAL-01** | Missing email format validation on Login and Register forms | Validation | **MEDIUM** |
| **VAL-02** | Missing URL syntax validation on LinkedIn, GitHub, Meeting, & Apply URLs | Validation | **MEDIUM** |
| **VAL-03** | Missing past-date prevention on Event and Opportunity date pickers | Validation | **MEDIUM** |
| **VAL-04** | Generic bottom TextView errors instead of inline TextInputLayout errors | UX | **MEDIUM** |
| **SEC-03** | R8 / ProGuard minification disabled in release build | Security / Perf | **MEDIUM** |
| **PERF-01**| `notifyDataSetChanged()` used across all adapters instead of DiffUtil | Performance | **MEDIUM** |
| **FEAT-01**| Missing Administrator User Management screen | Feature | **MEDIUM** |
| **FEAT-02**| Missing proactive offline connectivity banner / detection | Reliability | **MEDIUM** |
| **BUG-07** | Potential "Network error: null" display on null exception message | Bug / UX | **LOW** |
| **FEAT-03**| Missing Upcoming vs. Past Events toggle / filter | Feature | **LOW** |
| **UX-03**   | Hardcoded UI string literals instead of `strings.xml` | Code Quality | **LOW** |
| **FEAT-04**| Native Calendar export intent for registered events | Feature | **OPTIONAL** |

---

## K. VERIFICATION SUMMARY

| Check | Expected | Result | Notes |
|:---|:---|:---|:---|
| `gradlew clean` | Exit Code 0 | **PASS** | Cleaned build cache in 6s |
| `gradlew assembleDebug` | Exit Code 0 | **PASS** | `app-debug.apk` built (6.56 MB) |
| `gradlew assembleRelease` | Exit Code 0 | **PASS** | `app-release-unsigned.apk` built (5.30 MB) |
| `lintVitalRelease` | Exit Code 0 | **PASS** | No fatal lint violations detected |
| Exported Components | Restricted | **PASS** | Only `SplashActivity` is exported |
| Hardcoded Secrets | None | **PASS** | No API keys, DB credentials, or JWT secrets in code |
| Release Logging | Disabled | **PASS** | `HttpLoggingInterceptor.Level.NONE` on release |
