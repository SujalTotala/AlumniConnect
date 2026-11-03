# AlumniConnect — System User Manual

**Project Title:** AlumniConnect – Digital Platform for Centralized Alumni Data Management and Engagement  
**Document ID:** `FINAL_SUBMISSION/07_User_Manual.md`  
**Target Audience:** Students, Alumni, Faculty Administrators  

---

## 1. Getting Started

### 1.1 Accessing the Platform
- **Web Portal:** Open [https://alumni-connect-azure-mu.vercel.app/](https://alumni-connect-azure-mu.vercel.app/) in any modern web browser (Chrome, Firefox, Safari, Edge).
- **Android App:** Install `AlumniConnect-ProjectReview-v1.0.apk` onto an Android smartphone running Android 7.0 (Nougat) or higher.

### 1.2 User Registration
1. On the Login screen, click **"Create Account"**.
2. Enter your **Full Name**, **Email Address**, and choose a secure **Password**.
3. Select your institutional role:
   - **Student**: Current enrolled undergraduate or postgraduate student.
   - **Alumni**: Graduated alumnus of the institution.
4. Click **Create Account**. You will be authenticated immediately and redirected to the Member Dashboard.

---

## 2. Student User Guide

### 2.1 Exploring the Alumni Directory & Seeking Mentorship
1. Navigate to **Alumni Directory** (`/alumni`) from the sidebar or bottom navigation.
2. Use the search bar to locate alumni by name, employer (e.g., `"Google"`), or technical domain (e.g., `"Machine Learning"`).
3. Toggle the **"Available for Mentorship"** filter to display seniors actively offering guidance.
4. Click on an alumnus profile card to view their degree, graduation batch, company, and bio.
5. Click **"Request Mentorship"**, select your topic (e.g., `"Resume Review"`, `"Interview Guidance"`), write a brief introductory message, and submit.
6. Track your request status in the **Mentorship** section (`/mentorship`).

### 2.2 Building Your Professional Network
1. On any alumnus or peer card, click **"Connect"**.
2. The recipient receives an instant notification. Once accepted, your mutual status transitions to **Connected**.
3. Open **My Network** (`/network`) to view your confirmed connections and browse smart connection suggestions.

### 2.3 Job Opportunities & Seeking Referrals
1. Navigate to **Opportunities** (`/opportunities`) to view internal job postings.
2. When you find an appealing position at a company where you have a confirmed connection:
   - Click **"Request Referral"**.
   - Attach your hosted resume URL and an introductory note.
   - The alumnus receives your application directly in their **Referral Inbox**.

### 2.4 Event Participation & QR Check-In
1. Navigate to **Events** (`/events`) to view upcoming workshops and reunions.
2. Click **"RSVP / Register"** on the event card.
3. On event day:
   - **Web View:** Open the event details to view the live session stream or location.
   - **Android App:** Tap **"Scan QR Code"**, grant camera permission, and scan the organizer's QR code screen to instantly record your attendance.

---

## 3. Alumni User Guide

### 3.1 Managing Profile & Mentorship Availability
1. Navigate to **My Profile** (`/profile`).
2. Update your **Current Company**, **Job Role**, **Location**, and **LinkedIn URL**.
3. Toggle **"Available for Mentorship"** to `ON` so aspiring students can discover you.
4. Review your profile completeness score to ensure your contact card is comprehensive.

### 3.2 Handling Inbound Mentorship & Referral Requests
1. Open **Mentorship** (`/mentorship`) to review inbound student proposals.
   - Click **Accept** to share your contact schedule or meeting link.
   - Click **Decline** if you are at capacity.
2. Open **Opportunities** → **Referrals** to evaluate student referral requests.
   - Inspect the student's resume and message.
   - Mark the request as **Accepted** once you have submitted their referral internally.

### 3.3 Posting Opportunities & Sharing Stories
1. **Post an Opportunity:** On the Opportunities page, click **"Post Opportunity"**. Specify job title, company, location, requirements, and application link.
2. **Publish a Success Story:** Navigate to **Success Stories** (`/stories`) and click **"Share Your Story"**. Describe your career journey and tips for juniors. Once approved by administrators, your story will be featured on the portal.
3. **Submit an Achievement:** Submit notable patents, startup milestones, or promotions for verified institutional badges.

---

## 4. Administrator Guide

### 4.1 System Dashboard & Health Metrics
1. Log in with your administrator credentials (`admin@alumni.edu`).
2. Navigate to **Admin Portal** (`/admin`).
3. View high-level metrics: Total Registered Users, Total Alumni, Student Counts, Published Events, Active Mentorships, and Network Connections.

### 4.2 Alumni Data Quality Dashboard
1. Open **Alumni Data** (`/admin/alumni-data`) → **Data Quality**.
2. Review real-time data health scores:
   - **Email Completeness %**
   - **Company & Job Role Coverage %**
   - **Phone & Contact Availability %**
   - **LinkedIn Profile Integration %**
3. Identify records needing institutional updates.

### 4.3 Bulk CSV Alumni Ingestion
1. In the **Alumni Data** section, click **"Import Alumni CSV"**.
2. Download the standard CSV template.
3. Prepare your spreadsheet with columns: `name`, `email`, `graduation_year`, `degree`, `department`, `company`, `job_role`.
4. Upload the file to trigger **Instant Preview & Validation**. The system checks for malformed rows and duplicate emails.
5. Choose resolution mode:
   - **Update Existing:** Overwrites outdated employment fields with new values.
   - **Skip Existing:** Inserts only new graduates, preserving current records.
6. Click **"Commit Import"**. The records are ingested into the database and an audit record is logged.

### 4.4 Dynamic Cohort Segmentation & Targeted Announcements
1. Navigate to **Dynamic Segments** (`/admin/alumni-data#segments`).
2. Click **"Create Segment"**.
3. Define filters (e.g., Department: `"Computer Science"`, Batch: `"2020-2024"`, Company: `"Microsoft"`).
4. Save the segment.
5. In the **Announcements** section, compose an official notice and select your target segment. Only matching alumni will receive the broadcast.

### 4.5 Audit Trail & Governance
1. Navigate to **Audit Logs** (`/admin/alumni-data#audit`).
2. Review immutable log entries recording the timestamp, administrator ID, action type (`IMPORT_ALUMNI`, `APPROVE_STORY`), and affected entity ID.

