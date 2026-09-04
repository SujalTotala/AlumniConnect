# ALUMNICONNECT — PRODUCTION-GRADE ANDROID ROADMAP

**Project:** AlumniConnect  
**Package:** `com.alumniconnect.app`  
**Target:** Production-Grade Hardening & Polished APK Release  
**Current Phase:** Production Hardening Pass 1 Completed (Critical + High Priority Fixes)  

---

## ROADMAP OVERVIEW

This roadmap translates the findings from [ANDROID_PRODUCTION_AUDIT.md](file:///d:/Projects/AlumniConnect/android/ANDROID_PRODUCTION_AUDIT.md) into a structured, sequential implementation schedule. Work is phased to address critical reliability and security first, followed by professional UX polish, feature completeness, and final release verification.

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
|  * Handler lifecycle cleanup in Fragments (onDestroyView)        [PENDING] |
|  * Safe error message fallbacks (no "Network error: null")        [DONE]    |
+-----------------------------------------------------------------------------+
                                       |
                                       v
+-----------------------------------------------------------------------------+
|  PHASE C: PROFESSIONAL UX & STATE MANAGEMENT                      [PARTIAL] |
|  * Initial loading indicators on Detail Activities                [DONE]    |
|  * Bottom navigation tab instance retention (no state loss)       [PENDING] |
|  * Inline TextInputLayout error reporting                         [PENDING] |
|  * Form input validators (Email syntax done)                      [PARTIAL] |
+-----------------------------------------------------------------------------+
                                       |
                                       v
+-----------------------------------------------------------------------------+
|  PHASE D: FEATURE COMPLETION                                      [PENDING] |
|  * Offline state banner & network callback listener               [PENDING] |
|  * Past vs. Upcoming events segmentation                          [PENDING] |
|  * Administrator user management interface (GET /admin/users)     [PENDING] |
+-----------------------------------------------------------------------------+
                                       |
                                       v
+-----------------------------------------------------------------------------+
|  PHASE E: SECURITY & PERFORMANCE HARDENING                        [PARTIAL] |
|  * NetworkSecurityConfig: restrict cleartext to emulator debug    [DONE]    |
|  * EncryptedSharedPreferences for JWT token storage               [DONE]    |
|  * ProGuard / R8 minification & keep rules in release build       [DEFERRED]|
|  * DiffUtil incremental updates in RecyclerView adapters          [PENDING] |
+-----------------------------------------------------------------------------+
                                       |
                                       v
+-----------------------------------------------------------------------------+
|  PHASE F: FINAL APK QA & SIGNING VERIFICATION                     [PENDING] |
|  * Release APK assembly & shrink testing                          [PARTIAL] |
|  * Real-device test matrix (orientation, network drop, cold boot) [MANUAL]  |
|  * Final production APK handoff                                   [PENDING] |
+-----------------------------------------------------------------------------+
```

---

## IMPLEMENTATION STATUS (PASS 1 AUDIT AUDIT ITEMS)

| Audit Item | Scope | Status | Notes |
|:---|:---|:---|:---|
| **ApiErrorUtils Centralization** | Core Network | **DONE** | Crash-proof parser handling FastAPI 422 arrays (`[{"loc": [...], "msg": "..."}]`), status codes (400, 401, 403, 404, 409, 422, 429, 500+), and network exceptions. Tested via build. |
| **FastAPI 422 Array Parsing** | Forms / API | **DONE** | Extracts field names and messages (`"Email: value is not a valid email address"`), cleans details, sanitizes output. |
| **Splash Screen Session Hardening** | Auth / Splash | **DONE** / **MANUAL TEST REQUIRED** | Offline and 5xx failures no longer clear session or force login; presents retry UI and continue-offline option. Physical device testing required. |
| **Global 401 Session Expiry Redirect** | Auth / Navigation | **DONE** / **MANUAL TEST REQUIRED** | `AuthInterceptor` atomically triggers single intent to `LoginActivity` with `FLAG_ACTIVITY_NEW_TASK \| FLAG_ACTIVITY_CLEAR_TASK` and loop protection. Device testing required. |
| **Encrypted Session Storage (JWT)** | Storage / Security | **DONE** / **MANUAL TEST REQUIRED** | `SessionManager` upgraded to `EncryptedSharedPreferences` with Keystore `MasterKey.KeyScheme.AES256_GCM` and automatic legacy migration. Device testing required. |
| **Network Security Configuration** | Network / Security | **DONE** | `network_security_config.xml` restricts cleartext HTTP to `10.0.2.2`/localhost for debug builds; production enforces HTTPS. Verified in manifest. |
| **OkHttp 60-Second Timeouts** | Network | **DONE** | `ApiClient` configured with 60s `connectTimeout`, `readTimeout`, and `writeTimeout` for Render cold starts. |
| **Duplicate Write Action Protection** | UI / Actions | **DONE** / **MANUAL TEST REQUIRED** | Added debounce/locks to Login, Register, Edit Profile, RSVP, Cancel RSVP, Mentorship Request, Accept/Reject/Complete, Create Event, Create Opportunity, Delete Opportunity, Mark Read, Mark All Read. |
| **Detail Loading States** | UI / UX | **DONE** / **MANUAL TEST REQUIRED** | Added LOADING, SUCCESS, and ERROR states with Retry to `AlumniDetailsActivity`, `EventDetailsActivity`, and `OpportunityDetailsActivity`. |
| **Compilation Verification** | Build | **DONE** | `gradlew.bat assembleDebug` and `gradlew.bat assembleRelease` (with `lintVitalRelease`) compile with 0 errors. |
| **Security Scan** | Source Scan | **DONE** | No backend secrets, DATABASE_URL, or hardcoded JWT found. Release OkHttp logging confirmed `NONE`. |
| **R8 / Minification** | Release Build | **DEFERRED** | Kept `minifyEnabled false` for Pass 1 to prevent obfuscation issues during stabilization. |

---

## DETAILED STATUS BY COMPONENT

### 1. PHASE A — CRITICAL BUGS & DATA INTEGRITY
- **Universal API Error Parser (`ApiErrorUtils`):** **DONE**
  - Created `com.alumniconnect.app.utils.ApiErrorUtils`.
  - Parses string, object, and FastAPI 422 JSON array detail formats.
  - Replaces manual parsers in `LoginActivity`, `RegisterActivity`, `EditProfileActivity`, `CreateEventActivity`, `CreateOpportunityActivity`, `AlumniDetailsActivity`, `EventDetailsActivity`, `OpportunityDetailsActivity`, `ReceivedRequestsFragment`, `SentRequestsFragment`, `NotificationsActivity`.
- **Splash Screen Offline / Timeout Handling:** **DONE** / **MANUAL TEST REQUIRED**
  - Updated `SplashActivity` and `activity_splash.xml`.
  - Does NOT destroy token on transient network timeout or 5xx.
  - Displays retry dialog / UI with "Retry" and "Continue Offline" buttons.
- **Global 401 Session Clearance with UI Redirect:** **DONE** / **MANUAL TEST REQUIRED**
  - Updated `AuthInterceptor` with atomic redirect flag and loop guard against `/auth/login` and `/auth/register`.
  - `LoginActivity` displays session expired alert message.

### 2. PHASE B — NETWORK RELIABILITY & DUPLICATE ACTION PREVENTION
- **Double-Tap Action Guards:** **DONE** / **MANUAL TEST REQUIRED**
  - `AlumniDetailsActivity`: Mentorship positive button disabled upon dispatch.
  - `EventDetailsActivity`: `isRegistrationInProgress` guard on RSVP / Cancel RSVP.
  - `OpportunityDetailsActivity`: `isDeleting` guard on Delete Post.
  - `EditProfileActivity`: `isSaving` guard on Save Profile.
  - `CreateEventActivity`: `isSubmitting` guard on Event creation.
  - `CreateOpportunityActivity`: `isSubmitting` guard on Opportunity creation.
  - `ReceivedRequestsFragment`: `isActionInProgress` guard on Accept, Reject, Complete.
  - `SentRequestsFragment`: `isActionInProgress` guard on Complete.
  - `NotificationsActivity`: Debounce lock on mark-read and mark-all-read.
  - `LoginActivity` & `RegisterActivity`: Progress bars, disabled inputs, and submission locks.
- **Render Cold Start Accommodation:** **DONE**
  - `ApiClient` sets 60s timeouts across connect, read, and write operations.
- **Handler Lifecycle Cleanup:** **PENDING (Pass 2)**
- **Safe Error Fallbacks:** **DONE**
  - `ApiErrorUtils.parseThrowable()` maps `SocketTimeoutException`, `UnknownHostException`, `ConnectException` to friendly text.

### 3. PHASE C — PROFESSIONAL UX & STATE MANAGEMENT
- **Initial Loading Indicators on Detail Screens:** **DONE** / **MANUAL TEST REQUIRED**
  - `AlumniDetailsActivity`: Full loading spinner, content container, error state with retry.
  - `EventDetailsActivity`: Full loading spinner, content container, error state with retry.
  - `OpportunityDetailsActivity`: Full loading spinner, content container, error state with retry.
- **Bottom Navigation Tab State Retention:** **PENDING (Pass 2)**
- **Inline TextInputLayout Error Reporting:** **PENDING (Pass 2)**
- **Client-Side Form Validation:** **PARTIAL**
  - Email syntax validation using `Patterns.EMAIL_ADDRESS` implemented in Login and Register.

### 4. PHASE D — FEATURE COMPLETION
- **Proactive Network Connectivity Monitoring:** **PENDING (Pass 2)**
- **Upcoming vs. Past Events Segmentation:** **PENDING (Pass 2)**
- **Administrator User Management:** **PENDING (Pass 2)**

### 5. PHASE E — SECURITY & PERFORMANCE HARDENING
- **Network Security Configuration:** **DONE**
  - `res/xml/network_security_config.xml` implemented and hooked into `AndroidManifest.xml`.
  - Cleartext HTTP allowed only for `10.0.2.2`, `localhost`, `127.0.0.1`.
- **Encrypted Token Storage:** **DONE** / **MANUAL TEST REQUIRED**
  - Upgraded `SessionManager` to `EncryptedSharedPreferences` via `androidx.security:security-crypto:1.1.0-alpha06`.
  - Automatic migration from plaintext SharedPreferences included.
- **R8 Minification:** **DEFERRED (Pass 2)**
- **DiffUtil in Adapters:** **PENDING (Pass 2)**

### 6. PHASE F — FINAL APK QA & SIGNING VERIFICATION
- **Compilation:** **DONE** (`assembleDebug` and `assembleRelease` pass).
- **Device QA:** **MANUAL TEST REQUIRED** (requires physical device or Android emulator).
