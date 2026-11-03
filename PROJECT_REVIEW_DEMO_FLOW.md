# AlumniConnect — Academic Project Review & Demo Flow
**Estimated Presentation Duration:** 7 – 10 Minutes

---

## 1. Demo Overview & Strategy

This walkthrough is structured to demonstrate the complete cross-platform capabilities of **AlumniConnect** across **React Web**, **Android Mobile (APK)**, and the cloud-hosted **FastAPI + PostgreSQL** backend.

### Key Rules for Demo Presenters:
1. **Cloud Warmup**: Because the production backend is hosted on a cloud container service, trigger a single request to the backend health endpoint 2 minutes before the faculty presentation to ensure hot cache and instant responses.
2. **Dual-Screen Setup**: Display the Web Application on the primary projector/screen and the Android Application on an emulator or mirrored physical device alongside.
3. **No Hardcoded Credentials in Slides**: Use the configured student, alumni, and admin demo accounts.

---

## 2. Recommended 7–10 Minute Demo Sequence

### Act I: Student Experience & Discovery (Minutes 0:00 – 3:30)

1. **Platform Landing & Student Login**
   - Navigate to the Web frontend.
   - Log in using the **Student Demo Account**.
   - Review the personalized Student Dashboard displaying metrics, recent announcements, and quick-action shortcuts.

2. **Alumni Directory Exploration**
   - Navigate to **Alumni Directory** (`/alumni`).
   - Demonstrate real-time multi-criteria filtering:
     - Search by Name or Company (e.g., *Google Cloud*).
     - Filter by Department / Graduation Year.
     - Filter by **Mentorship Availability**.
   - Open a detailed Alumni Profile card to showcase academic background, career trajectory, social links, and skills.

3. **Initiate Mentorship Request**
   - From the Alumni profile, click **Request Mentorship**.
   - Compose a short proposal message requesting guidance on system design and interview readiness.
   - Submit the request; observe the real-time status change to **Pending** in the Sent Requests tab.

4. **Explore Opportunities & Referral Catalog**
   - Navigate to **Opportunities** (`/opportunities`).
   - Filter by type: *Full-Time*, *Internship*, *Referral*.
   - View an internship listing posted by an alumnus, highlighting requirements, deadline, and application links.

5. **Events & QR Attendance Concept**
   - Navigate to **Events** (`/events`).
   - Inspect upcoming campus webinars and tech summits.
   - Click **RSVP / Register** for an upcoming event.
   - Demonstrate registration confirmation and discuss the event QR check-in mechanism.

6. **Communities & Success Stories**
   - Navigate to **Communities** (`/communities`) to view collaborative interest clubs.
   - Navigate to **Success Stories** (`/stories`) to highlight alumni achievements and career journeys.
   - View **Profile / Achievements** (`/profile`) showcasing earned badges and activity telemetry.

---

### Act II: Android Mobile & Synchronization (Minutes 3:30 – 6:00)

7. **Launch Native Android Mobile App**
   - Launch the **AlumniConnect Review APK** (`AlumniConnect-ProjectReview-v1.0.apk`) on an Android device or emulator.
   - Log in using the **Alumni Demo Account**.

8. **Live Cross-Platform Data Sync**
   - Open the Android **Mentorship Requests** section.
   - Immediately observe the pending mentorship request dispatched moments earlier from the Web student.
   - Tap to view the student's proposal message and academic profile.

9. **Accept Mentorship with Response Note**
   - Accept the mentorship proposal with a short response note.
   - Return to the Web client; refresh or navigate to the Student Mentorship tab to demonstrate instant status update to **Accepted**.

10. **Mobile Opportunities & Community Interactions**
    - Browse posted opportunities and community channels directly within native Material Design cards.
    - Demonstrate swipe-to-refresh and asynchronous network operations powered by Retrofit + OkHttp.

---

### Act III: Administrative Governance & Institutional Data Management (Minutes 6:00 – 9:00)

11. **Administrative Login**
    - Switch to Web browser, log out of Student, and log in with the **Admin Demo Account**.
    - The navigation bar unlocks the dedicated **Admin Portal** and **Alumni Data Management** suites.

12. **System Analytics & Platform Telemetry**
    - Open **Admin Dashboard** (`/admin`).
    - Present platform health metrics: total user distribution (Students, Alumni, Admins), event registrations, active mentorship pairs, and opportunity engagement.

13. **Centralized Alumni Data Management (Pass 3A Suite)**
    - Navigate to **Alumni Data Management** (`/admin/alumni-data`).
    - **Bulk CSV Import Preview**:
      - Upload a sample institutional alumni CSV dataset.
      - Demonstrate client/server pre-validation: schema checks, duplicate email detection against existing records, and valid column mapping.
      - Highlight the dual commit modes: *Insert New Only* vs. *Safe Update Existing*.
    - **Data Quality & Hygiene Dashboard**:
      - Review completeness ratios (profiles with company, contact info, LinkedIn profiles).
      - Highlight unverified records requiring administrative review.
    - **Dynamic Alumni Segmentation**:
      - Create and preview a custom segment (e.g., *Batch 2020 Computer Science Alumni*).
      - Demonstrate live cohort count and instantaneous CSV export of the filtered segment.
    - **Targeted Institutional Announcements**:
      - Dispatch an announcement filtered to specific cohorts or user roles.
      - Demonstrate audience filtering guards so only intended recipients view the communication.
    - **Institutional Audit Log**:
      - Inspect the chronological audit trail capturing administrative data imports, role updates, and segment actions.

---

### Act IV: Faculty Q&A & Architecture Wrap-Up (Minutes 9:00 – 10:00)

14. **Architecture Summary**
    - Highlight the clean three-tier architecture:
      - **Frontend**: React 19 SPA (Vite) + Native Android Java (compileSdk 34).
      - **Backend**: FastAPI with async SQLAlchemy ORM, Pydantic schemas, and JWT RBAC.
      - **Database**: Cloud PostgreSQL with Alembic version-controlled schema migrations.
    - Conclude the demo and invite faculty questions.

---

## 3. Demo Safety Checklist

- [x] Backend verified healthy on Render (`https://alumniconnect-bwoi.onrender.com/health`).
- [x] Cloud PostgreSQL database active and connected.
- [x] Web frontend build verified clean (`npm run build` succeeds).
- [x] Android review APK compiled and verified (`AlumniConnect-ProjectReview-v1.0.apk`).
- [x] All three user roles (Student, Alumni, Admin) populated with valid records.
- [x] No sensitive passwords or database connection strings committed to version control.
