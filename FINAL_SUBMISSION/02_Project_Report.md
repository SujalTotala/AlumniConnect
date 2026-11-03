# AlumniConnect — Comprehensive Project Report

**Project Title:** AlumniConnect – Digital Platform for Centralized Alumni Data Management and Engagement  
**Document ID:** `FINAL_SUBMISSION/02_Project_Report.md`  
**Academic Submission:** Final Year Engineering Project Report  
**Department:** Computer Science & Engineering  

---

## 1. Introduction

Educational institutions worldwide recognize that alumni are vital stakeholders in institutional reputation, accreditation, career placement, and student development. However, maintaining long-term relationships with alumni remains one of higher education's most persistent administrative challenges.

Traditional alumni engagement models rely on ad-hoc spreadsheets, occasional alumni association reunions, and sporadic email broadcasts. These traditional methods lack bi-directional value: students cannot easily reach relevant graduates for mentorship, alumni receive irrelevant generic broadcasts, and administrators struggle with deteriorating data quality.

**AlumniConnect** is an integrated full-stack digital platform engineered to unify alumni data governance, student career enablement, and multi-tier institutional engagement. Built upon a unified FastAPI REST architecture supporting both a React 19 web portal and a native Android application, AlumniConnect delivers automated data curation, secure cross-platform networking, event attendance verification, and targeted communication.

---

## 2. Existing System Analysis

### 2.1 Characterization of the Legacy Ecosystem
In most colleges and universities, alumni relations are managed through:
- Scattered departmental Microsoft Excel or Google Sheets files.
- Manual phone or WhatsApp groups managed by alumni volunteers.
- Third-party social networks (e.g., LinkedIn) where institutional context, student status, and academic records are unavailable.
- Paper-based guestbooks and attendance registers at reunions and campus seminars.

### 2.2 Shortcomings and Deficiencies
1. **No Single Source of Truth:** Student records, graduate employment changes, and contact information are siloed across departments with zero automated synchronization.
2. **Data Staleness & Deterioration:** Over 60% of alumni email addresses and phone numbers become outdated within two years of graduation without user-driven self-service profile maintenance.
3. **Absence of Student-Alumni Synergy:** Current students have no authenticated, college-verified directory to filter seniors by current company, graduation batch, or mentorship willingness.
4. **Administrative Vulnerability:** Batch spreadsheets containing student personal data (names, phone numbers, personal emails) are often circulated insecurely among faculty without audit logs.
5. **No Engagement Metrics:** Institutions cannot assess whether alumni are actively participating, mentoring, or attending events.

---

## 3. Proposed System Architecture & Philosophy

The proposed AlumniConnect platform introduces a cohesive, secure, multi-tier software system:

1. **Centralized Data Repository:** A normalized relational database (PostgreSQL in production, SQLite in local development) accessed exclusively through an asynchronous FastAPI REST engine.
2. **Unified REST Interface:** Both the React web application and native Android mobile application communicate through identical, authenticated endpoints. Android never accesses the database directly.
3. **Bi-Directional Value Proposition:**
   - **For Students:** Verified alumni directory, mentorship proposals, connection requests, job referrals, community forums, and event RSVPs.
   - **For Alumni:** Professional recognition, company job posting, mentee guidance, success story publishing, and badge achievements.
   - **For Administrators:** High-speed CSV data onboarding, automated conflict detection, dynamic alumni segmentation, targeted announcements, and tamper-evident audit trails.

---

## 4. Requirements Specification

### 4.1 Functional Requirements (FR)

| ID | Module | Functional Requirement Description |
| :--- | :--- | :--- |
| **FR-01** | Authentication | Self-registration with email normalization, password hashing via bcrypt, and role assignment (`student`, `alumni`, `admin`). |
| **FR-02** | Session & RBAC | Stateless JWT bearer token issuance with role-based endpoint authorization guards. |
| **FR-03** | Profile Management | Full profile lifecycle for students and alumni including skills, bio, company, job role, and social handles. |
| **FR-04** | Directory & Search | Real-time multi-criteria filtering across alumni by name, department, batch, company, and mentorship willingness. |
| **FR-05** | Mentorship | Two-way mentorship proposals with status updates (`PENDING`, `ACCEPTED`, `REJECTED`, `COMPLETED`). |
| **FR-06** | Networking | Connection requests, mutual status checks, network listings, and connection suggestions. |
| **FR-07** | Job Opportunities | Alumni-posted internships and job openings with student referral applications and eligibility checks. |
| **FR-08** | Events & Attendance | Event creation, RSVP registration, dynamic QR check-in token issuance, and manual attendance marking. |
| **FR-09** | Communities | Topic-specific discussion communities with membership management and discussion post threads. |
| **FR-10** | Success Stories | Curated inspirational stories published by alumni with admin approval moderation. |
| **FR-11** | Achievements | Alumni achievement submissions with verification workflows and public recognition badges. |
| **FR-12** | Data Management | Administrative bulk CSV upload, preview verification, duplicate resolution modes, and data health scoring. |
| **FR-13** | Dynamic Segments | Custom alumni cohort segmentation based on department, batch, company, and mentorship filters. |
| **FR-14** | Targeted Broadcasts | Announcement dispatching targeting specific dynamic alumni segments with audit logging. |
| **FR-15** | Notifications | In-app notification dispatching with unread counts and customizable notification preferences. |

### 4.2 Non-Functional Requirements (NFR)

1. **Security & Privacy:**
   - Passwords hashed with high-entropy salted bcrypt (`passlib`).
   - Secret keys and database credentials strictly externalized in environment variables.
   - Zero exposure of passwords or cryptographic hashes in API responses or CSV exports.
   - Android sessions stored in `EncryptedSharedPreferences` using AES-256-GCM.
2. **Performance & Responsiveness:**
   - Database queries indexed on foreign keys, email lookups, and status filters.
   - Sub-100ms API response time under normal development workloads.
   - Gzip-compressed frontend bundles (<140 kB transfer) for sub-second web page loads.
3. **Portability & Cross-Platform Parity:**
   - Responsive web layouts rendering seamlessly across desktop, tablet, and mobile browsers.
   - Native Android client supporting devices from Android 7.0 (API 24) through Android 14 (API 34).
4. **Reliability & Data Consistency:**
   - ACID transaction guarantees with automatic rollbacks on database operation failures.
   - Linear, version-controlled Alembic schema migrations preventing schema drift.

---

## 5. Technology Stack

| Layer | Technology | Version | Justification |
| :--- | :--- | :--- | :--- |
| **Frontend Web** | React | 19.x | Declarative component model, virtual DOM performance, modern hooks |
| **Frontend Build** | Vite | 8.x | Near-instant hot module replacement (HMR), optimized production Rollup bundling |
| **Web Styling** | Tailwind CSS | 4.x | Utility-first responsive styling, zero CSS bloat |
| **HTTP Client (Web)**| Axios | 1.x | Interceptor support for automatic JWT attachment and 401 error handling |
| **Routing (Web)** | React Router DOM | 7.x | Declarative client-side routing with role-based route wrappers |
| **Backend Framework**| FastAPI | 0.136.x | Asynchronous execution, automatic OpenAPI documentation, Pydantic data validation |
| **ASGI Server** | Uvicorn | 0.47.x | High-throughput asynchronous server gateway interface |
| **ORM** | SQLAlchemy | 2.0.x | Enterprise Object Relational Mapping, typed query builder, connection pooling |
| **Database Migrations**| Alembic | 1.19.x | Linear versioned schema tracking, automated DDL execution |
| **Production Database**| PostgreSQL | 16.x | Robust ACID compliance, relational integrity, enterprise cloud hosting |
| **Dev Database** | SQLite | 3.x | Zero-configuration file-backed local database for rapid development |
| **Mobile Platform** | Native Android (Java) | SDK 24–34 | Full hardware integration (camera, KeyStore), smooth 60fps native UI |
| **Mobile Networking**| Retrofit + OkHttp | 2.9 / 4.12 | Type-safe REST client, request interceptors, connection timeout management |
| **QR Code Scanner** | ZXing Embedded | 4.3.x | Battle-tested barcode/QR scanning with integrated camera permissions |
| **Mobile Security** | AndroidX Security Crypto| 1.1.0 | Hardware-backed KeyStore encryption for tokens and sessions |
| **Cloud Hosting** | Render & Vercel | Cloud | Automated Git deployments, HTTPS termination, global CDN |

---

## 6. System Architecture

The AlumniConnect platform employs a strict 3-tier client-server architecture:

```
+──────────────────────────+     +──────────────────────────+
|  React 19 Web Client     |     |   Android Mobile App     |
|  (Desktop, Tablet, Phone)|     |   (Java / Material 3)    |
+────────────┬─────────────+     +────────────┬─────────────+
             │                                │
             │ HTTPS / JSON                   │ HTTPS / JSON
             │ (Bearer JWT)                   │ (Bearer JWT)
             ▼                                ▼
+───────────────────────────────────────────────────────────+
|               FastAPI REST API Gateway                    |
|    - CORS & Security Middleware                           |
|    - Pydantic Schema Validation                           |
|    - Role-Based Authorization Guard                       |
|    - 19 Modular Route Handlers                            |
+─────────────────────────────┬─────────────────────────────+
                              │
                              │ SQLAlchemy 2.0 ORM
                              ▼
+───────────────────────────────────────────────────────────+
|               Relational Database Engine                  |
|    - PostgreSQL (Production Cloud: Render)               |
|    - SQLite (Local Development: alumni.db)                |
|    - 21 Normalized Tables / Linear Alembic Migrations     |
+───────────────────────────────────────────────────────────+
```

### Architectural Guardrails:
- **No Direct Database Access:** The Android mobile client communicates exclusively with FastAPI over HTTPS; it has no database credentials or drivers.
- **Single Source of Truth:** Web and mobile clients interact with the exact same API routes and database state.
- **Decoupled Stateless Architecture:** JWT bearer tokens eliminate server-side session stickiness, allowing frictionless horizontal scaling.

---

## 7. Modular System Description

### 7.1 Authentication & Profile Engine
- Handles registration, authentication, password verification, and session renewal.
- Sanitizes incoming email addresses to lowercase and trims whitespace to prevent casing mismatch issues.
- Creates linked `Alumni` or `StudentProfile` records automatically upon registration.

### 7.2 Networking & Peer Referral Module
- Enables mutual professional connection requests between students and alumni.
- Allows students to request internal job referrals from connected alumni for specific opportunities.
- Prevents self-connections and duplicate requests through database-level constraints.

### 7.3 Event Attendance & Dynamic QR Check-In
- Organizers create institutional events with dates, locations, and capacity limits.
- Attendees RSVP and receive dynamic QR check-in tokens.
- Organizers scan QR tokens or manually record attendance, tracking real-time attendance ratios.

### 7.4 Knowledge Communities & Social Engagement
- Departmental and interest-based community hubs (e.g., "AI & Machine Learning", "Civil Engineering Circle").
- Threaded discussions, community post creation, and member count aggregation.
- Alumni success stories and verified achievement showcases.

### 7.5 Institutional Alumni Data Management (Pass 3A)
- **CSV Bulk Import:** Uploads thousands of legacy alumni records. Features dry-run validation, field type checking, preview generation, and duplicate resolution modes (`update_existing` vs `skip_existing`).
- **Data Quality Dashboard:** Computes automated completeness scores evaluating personal email, corporate company, valid phone, and LinkedIn profile coverage.
- **Dynamic Segments:** Generates named cohorts matching dynamic filter criteria for targeted institutional announcements.
- **Audit Logging:** Every administrative data action (import, approval, broadcast) is logged with administrative user ID, target entity, and timestamp.

---

## 8. Database Design & Relational Modeling

The database comprises 21 normalized relational tables designed to Third Normal Form (3NF):

1. `users` — Master account credentials, email, hashed password, role, active status.
2. `student_profiles` — Extended student academic records, branch, year, skills, bio.
3. `alumni` — Alumni directory records, graduation year, degree, company, job title, mentor flag.
4. `events` — Institutional campus events and seminars.
5. `event_registrations` — User RSVPs for upcoming events.
6. `event_attendance` — Verified check-in records, QR token timestamps, attendance mode.
7. `mentorship_requests` — Formal student proposals for alumni guidance and status lifecycle.
8. `opportunities` — Job and internship listings posted by alumni or administrators.
9. `referral_requests` — Formal referral requests tied to opportunities and connected alumni.
10. `connections` — Mutual networking connections between community members.
11. `communities` — Interest and departmental hubs.
12. `community_members` — Community membership associations and join dates.
13. `community_posts` — Discussion posts within specific communities.
14. `success_stories` — Curated alumni profiles and inspirational accounts.
15. `achievements` — Professional milestones, patents, and awards submitted for recognition.
16. `announcements` — Official campus-wide and targeted notifications.
17. `notifications` — Direct user notifications for requests, approvals, and reminders.
18. `notification_preferences` — User-defined channel toggles (email, in-app, push).
19. `user_bookmarks` — Personal bookmarks for events, jobs, and alumni profiles.
20. `alumni_import_jobs` — History and metrics of CSV bulk ingestion batches.
21. `alumni_segments` — Dynamic segment definitions and query filters.
22. `admin_audit_logs` — Immutable audit trail of administrative modifications.

---

## 9. Security & Governance

1. **Cryptographic Protection:** Passwords are hashed using salted bcrypt with high-workload factors (`passlib`). Passwords are never stored in plaintext and never included in API serialization schemas.
2. **Stateless Authorization:** Tokens are signed using HMAC-SHA256 (`HS256`) with a cryptographically random secret key. Expired or forged tokens are rejected immediately with HTTP 401.
3. **CORS Governance:** Production CORS explicitly allows verified domains (`https://alumni-connect-azure-mu.vercel.app`) with credential support and rejects wildcard access in production environments.
4. **Mobile KeyStore AES-256:** Android sessions use `EncryptedSharedPreferences` backed by the device's hardware security module (KeyStore) to safeguard JWT tokens from extraction.
5. **SQL Injection Immunity:** All database interactions utilize SQLAlchemy parameterized ORM queries; no raw string interpolation is executed.

---

## 10. Verification & Testing Summary

Testing was performed across automated unit, integration, regression, and build verification suites:

- **Automated Backend Tests:** 51 / 51 tests passed with zero failures across all feature passes.
- **Frontend Build Verification:** Production Vite build completed cleanly in 3.13s with zero syntax or bundling errors.
- **Android Gradle Assembly:**
  - `assembleDebug` completed successfully (`app-debug.apk`).
  - `assembleReview` completed successfully (`AlumniConnect-ProjectReview-v1.0.apk`).
  - `assembleRelease` completed successfully (`AlumniConnect-Unsigned-Release-v1.0.apk`).

---

## 11. Limitations & Future Scope

### 11.1 Current Limitations
- Cloud free-tier hosting on Render enters sleep mode after 15 minutes of inactivity, requiring a 30-50 second cold start on first request.
- Video call mentorship sessions require opening external links (e.g., Google Meet, Zoom) rather than streaming video in-app.

### 11.2 Future Enhancements
- WebRTC native peer-to-peer video calling for direct mentorship office hours.
- Automated LinkedIn OAuth 2.0 integration for real-time employment verification.
- Mobile push notification integration via Firebase Cloud Messaging (FCM).
- AI-driven mentor-student recommendation engine using vector similarity on skill embeddings.

---

## 12. Conclusion

AlumniConnect delivers a robust, secure, production-ready solution to the longstanding problem of fragmented alumni relations. By unifying modern web technologies, native mobile capabilities, and high-performance asynchronous APIs, the platform establishes an engaging ecosystem that empowers students, honors alumni, and provides administrators with unprecedented data governance.

