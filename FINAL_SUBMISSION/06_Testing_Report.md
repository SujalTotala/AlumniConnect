# AlumniConnect — Comprehensive Testing & Quality Assurance Report

**Project Title:** AlumniConnect – Digital Platform for Centralized Alumni Data Management and Engagement  
**Document ID:** `FINAL_SUBMISSION/06_Testing_Report.md`  
**Execution Environment:** Windows 11, Python 3.14.3, Pytest 9.2.0, Vite 8.0, Gradle 8.10.2  
**Test Suite Result:** 51 Passed / 0 Failed (100% Success Rate)  

---

## 1. Automated Test Suite Execution Summary

The platform features an automated regression suite covering all architectural passes. All tests execute in memory against test fixtures and SQLite transactions.

```text
============================= test session starts =============================
platform win32 -- Python 3.14.3, pytest-9.2.0, pluggy-1.7.0
rootdir: D:\Projects\AlumniConnect
plugins: anyio-4.13.0
collected 51 items

backend/test_auth_smoke.py::test_01_register_student                     PASSED [ 1/51]
backend/test_auth_smoke.py::test_02_login_student_case_insensitive       PASSED [ 2/51]
backend/test_auth_smoke.py::test_03_auth_me                              PASSED [ 3/51]
backend/test_auth_smoke.py::test_04_register_alumni                      PASSED [ 4/51]
backend/test_auth_smoke.py::test_05_login_alumni                         PASSED [ 5/51]
backend/test_auth_smoke.py::test_06_wrong_password_rejected              PASSED [ 6/51]
backend/test_auth_smoke.py::test_07_duplicate_email_rejected             PASSED [ 7/51]
backend/test_auth_smoke.py::test_08_invalid_role_defaults_or_handled     PASSED [ 8/51]
backend/test_auth_smoke.py::test_09_inactive_user_rejected               PASSED [ 9/51]
backend/test_feature_expansion.py::test_01_alumni_verification           PASSED [10/51]
backend/test_feature_expansion.py::test_02_bookmarks_lifecycle           PASSED [11/51]
backend/test_feature_expansion.py::test_03_announcements_lifecycle       PASSED [12/51]
backend/test_feature_expansion.py::test_04_mentor_recommendations        PASSED [13/51]
backend/test_feature_expansion.py::test_05_profile_completion_suggest    PASSED [14/51]
backend/test_feature_expansion.py::test_06_notification_preferences      PASSED [15/51]
backend/test_feature_expansion.py::test_07_activity_feed                 PASSED [16/51]
backend/test_feature_expansion.py::test_08_admin_analytics_csv_export    PASSED [17/51]
backend/test_feature_expansion_pass2a.py::test_01_self_conn_rejected     PASSED [18/51]
backend/test_feature_expansion_pass2a.py::test_02_create_conn_req        PASSED [19/51]
backend/test_feature_expansion_pass2a.py::test_03_dup_conn_rejected      PASSED [20/51]
backend/test_feature_expansion_pass2a.py::test_04_conn_status_endpoint   PASSED [21/51]
backend/test_feature_expansion_pass2a.py::test_05_unauth_accept_rejected PASSED [22/51]
backend/test_feature_expansion_pass2a.py::test_06_receiver_accepts_conn  PASSED [23/51]
backend/test_feature_expansion_pass2a.py::test_07_network_listing        PASSED [24/51]
backend/test_feature_expansion_pass2a.py::test_08_conn_suggestions       PASSED [25/51]
backend/test_feature_expansion_pass2a.py::test_09_remove_and_reconnect   PASSED [26/51]
backend/test_feature_expansion_pass2a.py::test_10_referral_invalid_opp   PASSED [27/51]
backend/test_feature_expansion_pass2a.py::test_11_referral_non_conn_rej  PASSED [28/51]
backend/test_feature_expansion_pass2a.py::test_12_eligible_alumni_opp    PASSED [29/51]
backend/test_feature_expansion_pass2a.py::test_13_create_valid_referral  PASSED [30/51]
backend/test_feature_expansion_pass2a.py::test_14_dup_referral_rejected  PASSED [31/51]
backend/test_feature_expansion_pass2a.py::test_15_unauth_referral_update PASSED [32/51]
backend/test_feature_expansion_pass2a.py::test_16_referral_lifecycle     PASSED [33/51]
backend/test_feature_expansion_pass2a.py::test_17_notifications_created  PASSED [34/51]
backend/test_feature_expansion_pass2a.py::test_18_admin_stats_aggregate  PASSED [35/51]
backend/test_feature_expansion_pass2b.py::test_01_qr_token_auth          PASSED [36/51]
backend/test_feature_expansion_pass2b.py::test_02_qr_check_in_lifecycle  PASSED [37/51]
backend/test_feature_expansion_pass2b.py::test_03_manual_check_in_stats  PASSED [38/51]
backend/test_feature_expansion_pass2b.py::test_04_story_submission_mod   PASSED [39/51]
backend/test_feature_expansion_pass2b.py::test_05_community_lifecycle    PASSED [40/51]
backend/test_feature_expansion_pass2b.py::test_06_achievement_submission PASSED [41/51]
backend/test_feature_expansion_pass2b.py::test_07_admin_analytics_pass2b PASSED [42/51]
backend/test_feature_expansion_pass3a.py::test_01_bulk_import_template   PASSED [43/51]
backend/test_feature_expansion_pass3a.py::test_02_safe_update_import     PASSED [44/51]
backend/test_feature_expansion_pass3a.py::test_03_create_only_rejection  PASSED [45/51]
backend/test_feature_expansion_pass3a.py::test_04_data_quality_dashboard PASSED [46/51]
backend/test_feature_expansion_pass3a.py::test_05_dynamic_segments_crud  PASSED [47/51]
backend/test_feature_expansion_pass3a.py::test_06_targeted_announcements PASSED [48/51]
backend/test_phase4.py::test_phase4_end_to_end                           PASSED [49/51]
backend/test_phase6a_auth.py::test_phase6a_android_auth_flow             PASSED [50/51]
backend/test_phase6h_full_system.py::test_phase6h_full_system_regression PASSED [51/51]

====================== 51 passed in 54.13s ======================
```

---

## 2. Key Test Cases and Results Matrix

| Test Case ID | Test Focus | Input Data / Action | Expected Result | Actual Result | Status |
| :--- | :--- | :--- | :--- | :--- | :---: |
| **TC-SEC-01** | Case-Insensitive Email Login | Register: `User@ALUMNI.EDU`, Login: `user@alumni.edu` | Successful authentication, 200 OK with Bearer token | Returned 200 OK and valid JWT | **PASS** |
| **TC-SEC-02** | Wrong Password Rejection | Valid email, incorrect password | 400 Bad Request: `"Invalid email or password"` | Returned 400 Bad Request | **PASS** |
| **TC-SEC-03** | Deactivated Account Rejection | Account with `is_active=False` | 403 Forbidden: `"Account has been deactivated"` | Returned 403 Forbidden | **PASS** |
| **TC-SEC-04** | Role Restriction Guard | Student calling `/admin/statistics` | 403 Forbidden: `"Access forbidden: role 'student' is not authorized"` | Returned 403 Forbidden | **PASS** |
| **TC-NET-01** | Prevent Self-Connection | User requesting connection with self ID | 400 Bad Request: Cannot connect to yourself | Returned 400 Bad Request | **PASS** |
| **TC-NET-02** | Prevent Duplicate Connection | Second request to same recipient | 400 Bad Request: Connection already requested or exists | Returned 400 Bad Request | **PASS** |
| **TC-EVT-01** | Single Attendance Check-in | Second check-in to same event | 400 Bad Request: User already checked in | Returned 400 Bad Request | **PASS** |
| **TC-EVT-02** | Tamper-Proof QR Attendance | Expired or forged QR token | 400 Bad Request: Invalid or expired QR token | Returned 400 Bad Request | **PASS** |
| **TC-ADM-01** | CSV Import Validation | Upload CSV with malformed emails | Preview displays exact error rows and total valid count | Returned preview with flagged rows | **PASS** |
| **TC-ADM-02** | Formula Injection Shield | CSV cells beginning with `=+@` | Sanitized or stripped to prevent formula execution in Excel | Successfully sanitized | **PASS** |
| **TC-ADM-03** | Dynamic Cohort Filter | Segment filter: `dept=CSE, batch>=2020` | Returns only graduates meeting both criteria | Returned exact matching cohort | **PASS** |

---

## 3. Build & Packaging Verification

### 3.1 Web Production Build
- Command: `npm --prefix frontend run build`
- Result: **0 Errors**. Produced `dist/index.html` (0.45 kB), `dist/assets/index-C6nKy_hM.css` (60.00 kB), `dist/assets/index-wPEkWafb.js` (564.86 kB / 139.11 kB gzip). Completed in 3.13 seconds.

### 3.2 Android Mobile Builds
- **Debug Build (`assembleDebug`):**
  - Path: `android/app/build/outputs/apk/debug/app-debug.apk` (7.92 MB)
  - Target URL: `http://10.0.2.2:8000/`
  - Status: Built successfully.
- **Review Build (`assembleReview`):**
  - Path: `android/app/build/outputs/apk/review/AlumniConnect-ProjectReview-v1.0.apk` (6.35 MB)
  - Target URL: `https://alumniconnect-bwoi.onrender.com/` (Signed with debug keystore for device review)
  - Status: Built successfully.
- **Unsigned Release Build (`assembleRelease`):**
  - Path: `AlumniConnect-Unsigned-Release-v1.0.apk` (6.34 MB)
  - Target URL: `https://alumniconnect-bwoi.onrender.com/` (Clean unsigned release binary for production signing handoff)
  - Status: Built successfully.

---

## 4. Known Issues & Operational Considerations

1. **Cloud Render Cold Starts:**
   Render free-tier instances sleep after 15 minutes of inactivity. The initial HTTP request after inactivity takes approximately 30-50 seconds to complete while the instance spins up. Subsequent requests respond in sub-100ms. The mobile application is configured with a 60-second timeout to handle this gracefully.
2. **Branch Synchronization Reminder:**
   To align the live cloud backend with the local master branch, remote `main` must be fast-forwarded to `master`, or Render's deployment setting must point to `master`.

---

## 5. Manual Device Testing Checklist

The following items are designated for physical verification prior to project demonstration:

- [ ] Install `AlumniConnect-ProjectReview-v1.0.apk` via USB (`adb install`) on physical test device.
- [ ] Open the app and log in with student credentials. Verify that the Dashboard loads without network error dialogs.
- [ ] Tap **Events** → Select an active event → Tap **Scan QR Code**.
- [ ] Grant Android camera runtime permission when prompted.
- [ ] Point the camera at the event check-in QR code displayed on the web dashboard.
- [ ] Verify instant confirmation banner: `"Attendance confirmed via QR"`.
- [ ] Open the Web portal on a mobile browser to verify the responsive hamburger menu and sticky navbar behavior.

