# AlumniConnect — Physical Phone Testing Checklist

This checklist provides the complete, structured QA procedure for testing the pre-release physical phone test APK connected to the production cloud backend (`https://alumniconnect-bwoi.onrender.com/`).

---

## Pre-Requisites & Target Build
- **APK Name:** `AlumniConnect-PhoneTest-v1.0.apk`
- **Location:** `android/app/build/outputs/apk/phone-test/AlumniConnect-PhoneTest-v1.0.apk`
- **Application ID:** `com.alumniconnect.app`
- **Version:** `1.0 (versionCode 1)`
- **minSdk / targetSdk:** `24 / 34`
- **Backend API:** `https://alumniconnect-bwoi.onrender.com/`
- **Database:** Production PostgreSQL (Render)
- **Signature:** Valid debug signature (Scheme v2) for testing installation without production keystore

---

## 1. INSTALLATION
- [ ] Transfer and install `AlumniConnect-PhoneTest-v1.0.apk` on physical Android device (API 24 through API 34).
- [ ] Allow "Install Unknown Apps" if prompted by OS package installer.
- [ ] Open **AlumniConnect** from home screen launcher.
- [ ] **Splash Screen**: Verify logo, title, and smooth transition to Login screen or Dashboard (if already authenticated).
- [ ] **Login Screen**: Verify email, password fields, password toggle, IME Next/Done actions, and Register navigation.

---

## 2. FUNCTIONAL MODULES
- [ ] **Dashboard (Home)**:
  - [ ] User greeting, role badge, profile summary card.
  - [ ] Upcoming events preview card and quick action navigation.
  - [ ] Pull-to-refresh syncs user profile and notifications.
- [ ] **Profile**:
  - [ ] Profile strength completion indicator (0-100%).
  - [ ] Correct student (branch, graduation year) or alumni (company, job title) fields.
  - [ ] No literal "null" values displayed for empty fields.
  - [ ] Clickable LinkedIn and GitHub links open safely in browser via `UrlUtils`.
  - [ ] "About & Status" button opens app version, purpose, and backend status.
- [ ] **Edit Profile**:
  - [ ] Edit bio, company, role, LinkedIn, GitHub URLs.
  - [ ] Keyboard dismisses on save via `KeyboardUtils`.
  - [ ] Success banner feedback and profile refresh.
- [ ] **Alumni Directory**:
  - [ ] List displays alumni with avatar, name, degree, and current company/role.
  - [ ] Search input debounced (400ms); clearing query restores full list.
  - [ ] Filter chips work in combination with search.
  - [ ] Empty state ("No alumni found.") and error retry states work.
- [ ] **Events**:
  - [ ] Event list displays title, date, location, and attendee counts.
  - [ ] Event details view with hero header and long description scrolling.
  - [ ] RSVP / Register action increments attendee count.
  - [ ] Cancel RSVP confirms via `MaterialAlertDialogBuilder`.
  - [ ] Virtual meeting link opens safely in browser via `UrlUtils`.
- [ ] **Mentorship**:
  - [ ] **Available Mentors**: Student views available alumni mentors; client search filters accurately without crashing.
  - [ ] **Send Request**: Student submits mentorship request with note; prevents rapid double-send.
  - [ ] **Received Requests**: Alumni views incoming requests; status badges (`PENDING`, `ACCEPTED`, `REJECTED`, `COMPLETED`).
  - [ ] **Accept / Reject**: Alumni accepts or rejects request with optional note; confirm dialog prevents accidental decline.
  - [ ] **Sent Requests**: Student tracks request status; complete mentorship prompts confirmation.
- [ ] **Opportunities**:
  - [ ] Career opportunities list (Internships, Full-Time, Referral).
  - [ ] Opportunity details view; Apply link opens external job portal safely via `UrlUtils`.
  - [ ] Authorized user can create opportunity; keyboard dismisses on submit.
  - [ ] Delete opportunity prompts Material confirmation dialog.
- [ ] **Notifications**:
  - [ ] Unread badge count in toolbar updates accurately.
  - [ ] Notification list with human-readable timestamps.
  - [ ] Mark one notification read updates UI immediately.
  - [ ] Mark all as read updates badge and item appearances.
  - [ ] Empty state displays "You're all caught up."

---

## 3. SESSION LIFECYCLE
- [ ] **Restart App**: Force quit app from recents and restart; verify encrypted session persists and auto-logs into MainActivity.
- [ ] **Background / Resume**: Background app during active use and resume; verify no state loss or polling leak.
- [ ] **Logout**: Click Logout in toolbar or Profile; verify Material confirmation dialog, session clearance in `SessionManager`, and navigation to LoginActivity.
- [ ] **Back-Navigation Guard**: After logout, press Android system Back button; verify user cannot re-enter authenticated MainActivity.
- [ ] **Re-Login**: Re-login with valid credentials to confirm clean session reconstruction.
- [ ] **Expired Token (401)**: When server returns HTTP 401, verify session is cleared and user is redirected to LoginActivity with "Your session has expired. Please log in again."

---

## 4. NETWORK RESILIENCE
- [ ] **Wi-Fi**: Full functionality test over Wi-Fi.
- [ ] **Mobile Data**: Full functionality test over Cellular (4G/5G).
- [ ] **Wi-Fi → Mobile Data**: Start action on Wi-Fi, toggle Wi-Fi off to switch to Mobile Data; verify connection smoothly migrates.
- [ ] **Mobile Data → Wi-Fi**: Re-enable Wi-Fi during usage; verify connectivity recovery.
- [ ] **Airplane Mode (Offline)**: Turn on Airplane mode; verify friendly offline error and Retry button; verify JWT session is NEVER deleted on network failure.
- [ ] **Reconnect**: Turn off Airplane mode; tap Retry or pull-to-refresh; verify data reloads immediately.
- [ ] **Slow Network**: Test on throttled 2G/3G; verify 60-second OkHttp timeouts and absence of infinite spinners.
- [ ] **Backend Cold Start**: Test initial API call when Render backend spins up; verify graceful wait up to 60s without premature crashes.

---

## 5. UI RESPONSIVENESS & ACCESSIBILITY
- [ ] **Small Display**: Test on ~5-inch / narrow device; verify all forms scroll, buttons remain accessible, and no text clips.
- [ ] **Large Display / Tablet**: Verify cards, dialogs, and navigation stretch and align cleanly.
- [ ] **Large Font Scaling (130%-150%)**: Increase system font size in Android Settings; verify text flows, dialogs don't clip, and touch targets remain accessible.
- [ ] **System Dark Mode**: Enable system dark mode; verify app enforces clean Light theme legibility (no white-on-white or dark-on-dark text).
- [ ] **Long Values**: Test user profiles with long names (30+ characters), long company names, long bios, and multi-paragraph event descriptions.
- [ ] **Keyboard & Forms**: Verify keyboard `actionNext` jumps between inputs, `actionDone` submits, and `ScrollView` prevents keyboard from hiding submit buttons.

---

## 6. CROSS-PLATFORM (WEB ↔ ANDROID) SYNCHRONIZATION
Verify seamless data synchronization between the Android mobile app and the React Web application against the centralized PostgreSQL backend:

- [ ] **Test A: Android → Web Profile**:
  1. Edit profile in Android app (update bio or company).
  2. Refresh React Web application profile page.
  3. Verify updated details appear immediately on Web.
- [ ] **Test B: Web → Android Profile**:
  1. Edit profile on React Web application.
  2. Pull-to-refresh Profile or Dashboard on Android app.
  3. Verify updated details reflect on Android.
- [ ] **Test C: Android → Web RSVP**:
  1. Register / RSVP for an event in Android app.
  2. Refresh event page on Web.
  3. Verify attendee list and registration state reflect on Web.
- [ ] **Test D: Web → Android RSVP**:
  1. RSVP / Cancel RSVP on Web.
  2. Pull-to-refresh Events on Android app.
  3. Verify attendee count and registration badge update on Android.
- [ ] **Test E: Web → Android Mentorship**:
  1. Student sends mentorship request from Web.
  2. Alumni on Android pulls-to-refresh Received Requests tab.
  3. Verify pending request appears with student's message.
- [ ] **Test F: Android → Web Mentorship**:
  1. Alumni accepts mentorship request on Android app.
  2. Student on Web refreshes Mentorship page.
  3. Verify status changes to **ACCEPTED** on Web.
- [ ] **Test G: Android → Web Opportunity**:
  1. Post a new opportunity from Android app.
  2. Refresh Opportunities page on Web; verify posting appears.
- [ ] **Test H: Web → Android Opportunity**:
  1. Post a new opportunity from Web.
  2. Pull-to-refresh Opportunities on Android app; verify posting appears.
