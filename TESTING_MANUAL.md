# AlumniConnect — Comprehensive Web & APK Testing Manual

**Document Version:** 1.0  
**Target Environment:** Production Cloud (Vercel + Render + PostgreSQL) & Android Review APK  
**Last Verified:** September 2026  

---

## 1. Environment & Target Endpoints

| Platform | URL / File Path | Target Environment |
| :--- | :--- | :--- |
| **Production Web** | [https://alumni-connect-azure-mu.vercel.app/](https://alumni-connect-azure-mu.vercel.app/) | Production Cloud (Vercel) |
| **Local Web Dev** | `http://localhost:5173/` | Local Development (Vite) |
| **Cloud Backend** | `https://alumniconnect-bwoi.onrender.com/` | Production Cloud (Render + PostgreSQL) |
| **Local Backend** | `http://127.0.0.1:8000/` | Local Development (Uvicorn + SQLite) |
| **Review APK** | `AlumniConnect-ProjectReview-v1.0.apk` | Production Cloud (Render) |
| **Review APK (Alt)** | `AlumniConnect-Review.apk` | Production Cloud (Render) |

---

## 2. Master Credentials Matrix

| Role | Email Address | Password | Primary Purpose |
| :--- | :--- | :--- | :--- |
| **System Admin** | `admin@alumni.edu` | `AdminPassword123!` | Institutional governance, data management, CSV import, moderation |
| **Student (Personal)** | `sujaltotala123@gmail.com` | `Password123!` | Live student verification, profile editing, mentorship seeker |
| **Demo Student** | `student@alumni.edu` | `StudentPassword123!` | Academic demo, event RSVP, community discussions |
| **Demo Alumni** | `alumni@alumni.edu` | `AlumniPassword123!` | Mentorship availability, job opportunity posting, referrals |

> [!TIP]
> **Pre-Test Backend Warmup**:
> Free-tier cloud instances on Render spin down after 15 minutes of inactivity. Before beginning a live evaluation, open `https://alumniconnect-bwoi.onrender.com/` in your browser. When it returns `{"status":"online", ...}`, the backend and database are hot and responses will be instantaneous.

---

## 3. PART 1: Web Platform Testing Manual

### TC-W01: New User Registration (Sign Up)
- **Objective**: Verify that students and alumni can self-register with automatic input sanitization.
- **Steps**:
  1. Navigate to [https://alumni-connect-azure-mu.vercel.app/](https://alumni-connect-azure-mu.vercel.app/).
  2. Click **"Create Account"** at the bottom of the login card (or navigate to `/signup`).
  3. Fill in the form:
     - **Full Name**: `Test User 2026`
     - **Email Address**: `test_user_2026@alumni.edu` *(try typing with a leading space or capital letter)*
     - **Role**: Select **Student** (or **Alumni**)
     - **Password**: `Password123!`
  4. Click **Create Account**.
- **Expected Result**:
  - HTTP `201 Created` is received.
  - The JWT token and user profile are saved to `localStorage`.
  - The browser automatically navigates to `/dashboard` displaying `"Welcome, Test User 2026"`.

---

### TC-W02: Student Login & Dashboard Navigation
- **Objective**: Verify authentication, session persistence, and student portal widgets.
- **Steps**:
  1. Open `/login` (or log out if already authenticated).
  2. Enter:
     - **Email**: `sujaltotala123@gmail.com`
     - **Password**: `Password123!`
  3. Click **Login**.
- **Expected Result**:
  - Redirects to `/dashboard`.
  - Top role badge displays **`Student`**.
  - Student quick widgets are visible: Upcoming Events, Mentorship Status, Career Opportunities, Activity Feed.
  - Refreshing the browser preserves the session without returning to the login page.

---

### TC-W03: Alumni Directory & Advanced Search
- **Objective**: Verify filtering and search capabilities across the institutional alumni network.
- **Steps**:
  1. Click **Alumni** in the main navigation bar (`/alumni`).
  2. Type `Google` or `Engineer` into the search bar.
  3. Filter by department or graduation year.
  4. Toggle the **"Available for Mentorship"** checkbox.
  5. Click on an Alumni profile card (e.g., Demo Alumni).
- **Expected Result**:
  - Search results filter in real-time.
  - Profile modal or details view opens, showing academic batch, current company, skills, and social links.

---

### TC-W04: Mentorship Proposal Submission
- **Objective**: Verify student-to-alumni mentorship request dispatching.
- **Steps**:
  1. On the selected alumnus card who is open to mentorship, click **"Request Mentorship"** (or open `/mentorship`).
  2. Enter proposal details:
     - **Topic**: `Career Advice & Interview Preparation`
     - **Message**: `Hi, I am preparing for software engineering interviews and would love your guidance.`
  3. Click **Submit Request**.
- **Expected Result**:
  - A success confirmation is displayed.
  - Navigating to **Sent Requests** shows the request status as **`PENDING`**.

---

### TC-W05: Events Browsing & RSVP Registration
- **Objective**: Verify event discovery and participation registration.
- **Steps**:
  1. Click **Events** in the navigation bar (`/events`).
  2. Select an upcoming event (e.g., *Annual Alumni Tech Summit*).
  3. Click **RSVP / Register**.
- **Expected Result**:
  - Button state transitions to **"Registered"** with a confirmation toast.
  - Registration count on the event card increments by 1.

---

### TC-W06: Opportunities & Career Referrals
- **Objective**: Verify job and internship browsing.
- **Steps**:
  1. Click **Opportunities** in the navigation bar (`/opportunities`).
  2. Filter by type: **Full-Time**, **Internship**, or **Referral**.
  3. Click on a listing to review company name, job description, requirements, and application link.
- **Expected Result**:
  - Listings render cleanly with salary/stipend tags and application deadlines.

---

### TC-W07: Communities & Success Stories
- **Objective**: Verify interest groups and student-alumni knowledge sharing.
- **Steps**:
  1. Click **Communities** (`/communities`) $\rightarrow$ join an interest channel (e.g., *AI & Machine Learning Group*).
  2. Click **Success Stories** (`/stories` or via dashboard) $\rightarrow$ read published career journeys.
- **Expected Result**:
  - Community threads and stories load without network delays.

---

### TC-W08: Administrative Portal & System Telemetry
- **Objective**: Verify admin-exclusive controls and platform metrics.
- **Steps**:
  1. Click **Logout** in the top navigation.
  2. Log in using the **Admin Demo Account**:
     - **Email**: `admin@alumni.edu`
     - **Password**: `AdminPassword123!`
  3. Verify that the **Admin Portal** banner appears.
  4. Navigate to `/admin` (or click **Admin Dashboard**).
- **Expected Result**:
  - Telemetry cards display: **Total Users**, **Students**, **Alumni**, **Active Mentorships**, **Events**.
  - All users (including any newly registered test accounts) are visible in the user management table.

---

### TC-W09: Institutional Alumni Data Management (Pass 3A Suite)
- **Objective**: Test centralized CSV data ingestion, preview, quality audit, and segmentation.
- **Steps**:
  1. From the Admin Dashboard, navigate to **Alumni Data Management** (`/admin/alumni-data`).
  2. **Data Quality Dashboard**: Inspect the profile completeness score and unverified records.
  3. **CSV Import Preview**:
     - Upload a sample alumni CSV.
     - Observe the pre-validation table showing valid records, warnings, and duplicate emails.
     - Select commit mode: *Insert New Only* or *Safe Update Existing*.
  4. **Segmentation & Cohort Export**:
     - Filter alumni by Department (e.g., *Computer Science*) and Graduation Year.
     - Click **Export Segment CSV** to download the generated file.
- **Expected Result**:
  - Pre-import preview displays validated rows before committing to the database.
  - Exported CSV downloads cleanly to your browser.

---

### TC-W10: Announcements Broadcast
- **Objective**: Broadcast institutional notices to students and alumni.
- **Steps**:
  1. Navigate to **Announcements** (`/announcements`).
  2. Click **Create Announcement**.
  3. Enter Title, Content, and Target Audience (`All`, `Students`, or `Alumni`).
  4. Click **Publish**.
- **Expected Result**:
  - Announcement appears at the top of the feed and displays on student dashboards.

---

## 4. PART 2: Android APK Testing Manual

### TC-A01: APK Installation & Hardware Permissions
- **Target File**: `AlumniConnect-ProjectReview-v1.0.apk` (or `AlumniConnect-Review.apk`)
- **Steps**:
  1. Transfer the APK to an Android phone (via USB, Google Drive, or WhatsApp).
  2. Tap the APK file in **Files / Downloads**.
  3. If prompted, allow *"Install from this source"*.
  4. Complete installation and tap **Open**.
- **Expected Result**:
  - App installs cleanly without signature or corrupt package errors.
  - App icon and title **"AlumniConnect"** appear in the application drawer.

---

### TC-A02: Splash Screen & Auto-Login Logic
- **Steps**:
  1. Launch the app.
  2. Observe the initial Splash Screen.
- **Expected Result**:
  - Displays the AlumniConnect branding for 1.5 seconds.
  - If no prior session exists, seamlessly transitions to the **Login Activity**.

---

### TC-A03: Mobile Authentication & Sanitization
- **Steps**:
  1. On the mobile Login screen, enter:
     - **Email**: `sujaltotala123@gmail.com` *(notice how mobile keyboards auto-capitalize the first letter)*
     - **Password**: `Password123!`
  2. Tap **Login**.
- **Expected Result**:
  - Automatic case-normalization (`.toLowerCase().trim()`) prevents capital-letter rejection.
  - Authenticates with the Render cloud backend.
  - Stores JWT token in encrypted SharedPreferences and opens the **Main Activity**.

---

### TC-A04: Mobile Navigation & Core Tabs
- **Steps**:
  1. Explore the bottom navigation / drawer options:
     - **Alumni Directory**
     - **Events**
     - **Mentorship**
     - **Opportunities**
     - **Communities**
- **Expected Result**:
  - Fast, responsive tab switching.
  - Material Design cards display with smooth scrolling and swipe-to-refresh.

---

### TC-A05: Mobile Alumni Directory Search
- **Steps**:
  1. Tap the **Alumni** tab.
  2. Tap the search bar and type `Google` or `Alumni`.
  3. Tap a profile card to view full details.
- **Expected Result**:
  - Matches filter instantaneously.
  - Alumni details screen displays graduation year, company, job role, and contact buttons.

---

### TC-A06: Hardware Camera QR Code Scanner (Event Attendance)
- **Steps**:
  1. Tap the **Events** tab.
  2. Select an active event.
  3. Tap the **"Scan QR Code"** button.
  4. When prompted, tap **"Allow while using app"** for Camera permission.
  5. Point the viewfinder at any sample event QR code (or monitor screen).
- **Expected Result**:
  - Hardware camera launches instantly within the integrated ZXing scanner viewfinder.
  - Scans barcode/QR data and sends attendance check-in request to the backend.

---

### TC-A07: Mobile Mentorship Response (Alumni Role)
- **Steps**:
  1. Open the drawer and tap **Logout**.
  2. Log in using the **Demo Alumni Account**:
     - **Email**: `alumni@alumni.edu`
     - **Password**: `AlumniPassword123!`
  3. Tap **Mentorship Requests** $\rightarrow$ **Received Requests**.
  4. Locate the pending request sent in **TC-W04**.
  5. Tap **Accept** and enter a note: `"Looking forward to guiding you on system design!"`.
- **Expected Result**:
  - Status updates to **`ACCEPTED`**.
  - Toast confirms action to the user.

---

## 5. PART 3: Live Cross-Platform Synchronization Test

This test demonstrates the **cloud integration** between the Web platform and the Android mobile app.

```
+------------------------------------+          +------------------------------------+
|             REACT WEB              |          |            ANDROID APK             |
|   (Laptop / Projector Display)     |          |       (Physical Smartphone)        |
+-----------------+------------------+          +------------------+-----------------+
                  |                                                |
                  |                                                |
                  +-------------> CLOUD BACKEND <------------------+
                             (Render + PostgreSQL)
```

### Demonstration Steps:
1. **Side-by-Side Setup**:
   - Have the Web app open on your laptop ([https://alumni-connect-azure-mu.vercel.app](https://alumni-connect-azure-mu.vercel.app)) logged in as **Admin** (`admin@alumni.edu`).
   - Have your Android phone open on the **AlumniConnect APK** logged in as **Student** (`sujaltotala123@gmail.com`).
2. **Action on Web**:
   - On the Web browser, create a new Announcement titled:
     `"Project Review Demo: Real-Time Cloud Sync Verified"`
   - Click **Publish**.
3. **Verification on Android**:
   - On your phone, pull down on the Home feed to swipe-to-refresh.
   - **Result**: The new announcement created on the Web immediately appears on the phone.
4. **Action on Android**:
   - On the phone, navigate to **Events** and RSVP for an event.
5. **Verification on Web**:
   - Refresh the Web browser on the Admin / Events page.
   - **Result**: The attendee count increases by 1, demonstrating bidirectional synchronization.

---

## 6. PART 4: Troubleshooting & Fallback Cheat Sheet

| Symptom | Cause | Instant Fix |
| :--- | :--- | :--- |
| **"Login failed. Please check your credentials" on Web** | First request after inactivity; Render backend is spinning up from sleep. | Wait 30 seconds for Render to wake up, or open `https://alumniconnect-bwoi.onrender.com/` in a new tab to confirm `{"status":"online"}`. |
| **Mobile keyboard capitalization** | Auto-capitalization turned `admin@...` into `Admin@...`. | The APK automatically normalizes inputs to lowercase. If using an older build, manually lower-case the first letter. |
| **Android APK "Blocked by Play Protect"** | Self-signed debug keystore on brand-new APK. | Tap **"More details"** $\rightarrow$ **"Install anyway"**. |
| **No Internet in Android Emulator** | DNS resolution issue on emulator virtual Wi-Fi. | In emulator settings, set DNS to `8.8.8.8` or test on a physical phone using Wi-Fi / Mobile Data. |
| **Need Local Offline Backup for Demo** | Internet connection dropped in review hall. | Run local backend (`venv\Scripts\python.exe -m uvicorn app.main:app --port 8000`) and local web dev server (`npm run dev`). |
