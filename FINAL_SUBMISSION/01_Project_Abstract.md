# AlumniConnect — Project Abstract

**Project Title:** AlumniConnect – Digital Platform for Centralized Alumni Data Management and Engagement  
**Document ID:** `FINAL_SUBMISSION/01_Project_Abstract.md`  
**Academic Year:** 2026–2027  
**Degree / Program:** Bachelor / Master of Technology (Computer Science & Engineering)  
**Author:** Sujal Totala & Project Team  

---

## 1. Project Introduction

Higher education institutions generate extensive alumni networks with every graduating class. These alumni represent invaluable social, professional, and financial capital for colleges and universities. However, institutions historically lose contact with their graduates within two to three years following graduation due to fragmented communication channels, outdated contact details, and lack of engaging incentives.

**AlumniConnect** is a centralized, cross-platform digital ecosystem engineered to bridge the divide between academic institutions, enrolled students, and graduated alumni. Combining a modern Single Page Application (React 19), a native mobile application (Android Java / SDK 34), and an asynchronous RESTful backend (FastAPI / SQLAlchemy), AlumniConnect delivers centralized data governance, verifiable credential networking, two-way mentorship, real-time event attendance tracking, and dynamic engagement segmentation.

---

## 2. Problem Statement

Contemporary institutional alumni operations suffer from several acute challenges:

1. **Data Fragmentation & Stale Records:** Alumni contact details are scattered across legacy spreadsheets, departmental records, and disparate databases with no automated deduplication, normalization, or data quality metrics.
2. **One-Way Institutional Communication:** Institutions typically engage alumni only for administrative fundraising, lacking interactive value propositions such as peer mentorship, career referral requests, and specialized knowledge communities.
3. **Student Isolation from Industry Insights:** Current students lack verified, authenticated channels to seek guidance, mock interviews, and career referrals from verified graduates working in target companies.
4. **Attendance Tracking Inefficiencies:** College reunions, workshops, and guest lectures rely on manual attendance sheets, resulting in long queues, missing attendee analytics, and unverified participation records.
5. **Lack of Cross-Platform Continuity:** Disparate tools prevent users from switching seamlessly between desktop administration and on-the-go mobile engagement.

---

## 3. Objectives

The primary objectives of the AlumniConnect project are:

1. **Centralize Alumni Data Governance:** Provide institution administrators with an audit-logged management portal for CSV data ingestion, preview validation, automated conflict resolution, and real-time data health scoring.
2. **Facilitate Verified Academic & Career Networking:** Establish secure connection workflows, role-based visibility, and targeted referral request pipelines connecting verified alumni with aspiring students.
3. **Automate Event Management & Contactless Check-In:** Implement cryptographically timed QR codes and manual fallback check-in systems for campus events with live attendance analytics.
4. **Deliver Native Cross-Platform Parity:** Ensure identical business logic, data models, and authorization constraints across Web (React 19) and Mobile (Native Android Java).
5. **Ensure Enterprise Security & Compliance:** Protect sensitive institutional and personal information via bcrypt salted password hashing, JWT bearer token session management, Android KeyStore AES-256 encryption, and SQL injection prevention.

---

## 4. Proposed Solution

AlumniConnect resolves these systemic challenges through a modular, service-oriented architecture:

- **Unified FastAPI REST Engine:** A high-performance Python backend serving both web and mobile clients from a single authoritative source of truth.
- **Hierarchical Role-Based Access Control (RBAC):** Distinct capability levels for **Students** (mentorship seeking, event RSVP, job applications, community discussions), **Alumni** (profile showcase, mentor availability, job posting, referral review, badge submission), and **Administrators** (data quality analytics, CSV batch import, community moderation, announcement dispatch, audit logs).
- **Dynamic Segmentation Engine:** Institutional filtering allowing administrators to query alumni dynamically by department, graduation batch, current company, domain, and mentorship status to deliver targeted announcements.
- **Mobile Native Parity:** A dedicated Android client offering offline-safe encrypted credentials, native QR scanning via ZXing, Material 3 UI design, and background notification management.

---

## 5. Scope of the System

### In-Scope Functional Capabilities
- Self-registration with email sanitization and case-insensitive normalization.
- JWT-based authentication with expiration handling and session revocation.
- Comprehensive user profile management (academic batch, skills, bio, social links, company, role).
- Alumni directory with multi-field search, mentor filtering, and detailed profile viewing.
- Bidirectional student-alumni mentorship requests with status tracking (`PENDING`, `ACCEPTED`, `REJECTED`, `COMPLETED`).
- Professional peer connections and opportunity referral request lifecycles.
- Institutional campus events with RSVP, QR token generation, and real-time attendance logging.
- Community discussion hubs with topic threads, post authoring, and member participation.
- Success stories showcase and verified alumni achievement portfolios.
- Administrative bulk CSV alumni import with pre-commit validation, error reporting, and safe upsert modes.
- Alumni data quality dashboard evaluating email validity, company placement, phone records, and LinkedIn coverage.
- Full administrative audit trail logging administrative data actions with timestamps and operator IDs.

### Out-of-Scope (Future Enhancements)
- Direct end-to-end video calling within the application (external meeting links are utilized).
- In-app payment processing for alumni donations or ticketing (can be integrated with payment gateways).
- Automated web scraping of external social platforms (manual CSV or profile update is prioritized for data privacy compliance).

---

## 6. Expected Outcomes

Upon deployment, the AlumniConnect system yields:
1. **90%+ Reduction in Administrative Ingestion Time:** Bulk CSV processing replaces manual entry with automated validation and duplicate prevention.
2. **Instant Contactless Event Check-In:** QR code verification processes attendees in under two seconds per participant.
3. **Measurable Student Career Acceleration:** Direct referral request and mentorship pipelines dramatically improve student industry access.
4. **Audited Institutional Governance:** Institutional administrators maintain full visibility over data quality, user status, and administrative modifications.

