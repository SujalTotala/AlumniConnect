# AlumniConnect — Academic Project Review Feature Specification

---

## 1. Project Title
**AlumniConnect: Centralized Institutional Alumni Data Management and Cross-Platform Engagement System**

---

## 2. Problem Statement
Higher education institutions frequently struggle with fragmented alumni databases, outdated graduate employment records, and disjointed communication channels. Graduating students face significant barriers when attempting to seek career mentorship, industry referrals, and authentic professional guidance from graduated seniors. Conventional approaches (static spreadsheets, decentralized alumni associations, and third-party social networks) fail to offer colleges institutional data ownership, verified alumni status verification, targeted event management, and structured mentorship workflows.

---

## 3. Objective
To design, implement, and validate a cloud-native, cross-platform institutional platform that unites alumni, enrolled students, and college administrators. AlumniConnect delivers:
1. A single authoritative database for institutional graduate records and verified credentials.
2. Structured student-alumni networking channels, including formal mentorship requests, opportunity referrals, and campus event coordination.
3. Centralized institutional data governance for university administrators, featuring bulk CSV ingest, pre-validation preview, dynamic segmentation, data hygiene auditing, and targeted announcements.
4. Seamless real-time synchronization between a responsive modern Web single-page application and a native Android mobile application.

---

## 4. User Roles & Access Control (RBAC)

AlumniConnect enforces strict server-side Role-Based Access Control (RBAC) across three distinct user roles:

| Role | Permissions & Operational Scope |
| :--- | :--- |
| **Student** | • Browse verified alumni directory with multi-criteria filtering.<br>• Submit structured mentorship proposals with custom notes.<br>• Track incoming and outgoing mentorship request statuses.<br>• Explore job and internship opportunities.<br>• Register (RSVP) for campus workshops, reunions, and webinars.<br>• Participate in collaborative interest communities.<br>• Maintain personal profile (branch, graduation year, skills, bio). |
| **Alumni** | • Manage professional profile (company, role, experience, industry skills, mentorship availability).<br>• Review, accept, or reject incoming student mentorship requests with feedback notes.<br>• Post job vacancies, internships, and referral opportunities.<br>• Organize events and webinars; review real-time attendee rosters.<br>• Author success stories and career milestone reflections.<br>• Participate in and create discussion communities. |
| **Administrator** | • System-wide telemetry dashboard (user demographics, engagement ratios, event registrations).<br>• Centralized alumni record management: bulk CSV ingestion with pre-commit preview and duplicate detection.<br>• Data quality and hygiene monitoring (profile completeness, unverified accounts).<br>• Dynamic cohort segmentation with one-click CSV export.<br>• Targeted broadcast announcements with role and cohort audience filters.<br>• Immutable chronological administrative audit logging. |

---

## 5. System Architecture

AlumniConnect utilizes a cloud-native, decoupled, client-server distributed architecture:

```
+------------------------------------+       +------------------------------------+
|          React Web SPA             |       |     Native Android Mobile App      |
|    - Vite Single-Page Application  |       |   - Java 17, Android SDK 34        |
|    - Axios HTTP Interceptors       |       |   - Retrofit 2 + OkHttp 3 Clients  |
|    - Responsive Modern Design      |       |   - Material Design 3 Components   |
|    - Deployed on Vercel CDN        |       |   - Camera & ZXing QR Scanning     |
+------------------------------------+       +------------------------------------+
                   \                                   /
                    \   HTTPS REST API (Bearer JWT)   /
                     \                               /
                      v                             v
       +---------------------------------------------------------------+
       |                     FastAPI REST Engine                       |
       |  - Stateless Bearer JWT Authentication & Role Validation     |
       |  - Domain Routers (Auth, Profiles, Events, Mentorship, Admin) |
       |  - Strict CORS Policy Enforcement                             |
       |  - Pydantic v2 Request/Response Data Validation               |
       |  - SQLAlchemy 2.0 Async/Sync ORM Engine                       |
       |  - Deployed on Render Cloud Linux Container Service           |
       +---------------------------------------------------------------+
                                       |
                                       | SSL / Connection Pooling
                                       v
       +---------------------------------------------------------------+
       |                   Cloud PostgreSQL Database                   |
       |  - Relational Schema with Foreign Key Constraints             |
       |  - Single-Head Alembic Migration Chain                        |
       |  - Hosted on Managed Cloud Database Infrastructure            |
       +---------------------------------------------------------------+
```

---

## 6. Technology Stack

| Layer | Component / Technology | Justification & Role |
| :--- | :--- | :--- |
| **Backend REST API** | Python 3.11, FastAPI | High-performance asynchronous REST API framework with native OpenAPI/Swagger documentation. |
| **Database ORM** | SQLAlchemy 2.0, Alembic | Declarative data modeling, robust relationship mappings, and deterministic database migration versioning. |
| **Database** | PostgreSQL | Robust ACID-compliant relational storage supporting foreign key cascades, unique indexes, and high-throughput queries. |
| **Web Frontend** | React 19, Vite, Vanilla CSS | Fast virtual DOM single-page architecture with customized modern typography, responsive CSS grid layouts, and zero bloat. |
| **Mobile Application** | Java 17, Android SDK (API 24–34) | Native Android client utilizing Material 3 design, Retrofit 2 REST networking, and ZXing barcode scanning. |
| **Authentication** | Passlib (bcrypt), PyJWT | Industry-standard salted password hashing with stateless cryptographic JSON Web Tokens. |
| **Cloud Hosting** | Render (Backend & DB), Vercel (Web) | Cloud infrastructure with continuous deployment pipelines and edge caching. |

---

## 7. Core Functional Modules

### 7.1 Authentication & Authorization
- Salted bcrypt password hashing with configurable complexity requirements.
- Cryptographically signed JSON Web Tokens (`HS256`) carrying subject ID, role, and expiration timestamp.
- Dependency-injected endpoint security (`get_current_user`, `require_role(["admin"])`).
- Single initial admin self-bootstrap protection guard preventing unauthorized administrative registrations.

### 7.2 Dynamic Alumni Directory
- Comprehensive directory listing verified alumni members.
- Multi-parameter live search: query by name, organization/company, graduation year, academic department, and location.
- Toggle filter to surface only alumni actively accepting mentorship proposals.

### 7.3 Mentorship Management Workflow
- Formal mentorship proposal dispatch with custom message payload.
- Server-enforced prevention of duplicate concurrent pending requests between the same student and mentor.
- Tabbed request tracking:
  - Students view *Sent Requests* with current status badges (`PENDING`, `ACCEPTED`, `REJECTED`, `COMPLETED`).
  - Alumni mentors review *Received Requests* with the ability to accept (with a response note) or decline.

### 7.4 Opportunities & Referrals
- Multi-category opportunity board: Full-Time Jobs, Internships, Campus Webinars, and Referral Openings.
- Role-restricted posting: only verified alumni and college administrators may publish listings.
- Detailed listing metadata including company name, application deadline, compensation/type, and direct external application URL.

### 7.5 Events & QR Attendance System
- Campus workshops, technical masterclasses, alumni reunions, and virtual webinars.
- One-click student RSVP registration with server-side duplicate registration guards.
- Organizer access to real-time attendee rosters.
- Event attendance check-in architecture featuring QR barcode scanning integration via ZXing embedded library.

### 7.6 Communities & Success Stories
- Specialized interest channels (e.g., Cloud & Distributed Systems, AI/ML, Competitive Coding).
- Student and alumni community discovery and membership participation.
- Success story publication and spotlighting to inspire undergraduate cohorts.

---

## 8. Advanced Institutional Data Management (Pass 3A Suite)

### 8.1 Bulk Alumni CSV Ingestion
- Upload institutional alumni records via standardized CSV templates.
- Immediate client/server validation before committing records to permanent storage.
- Intelligent duplicate detection identifying existing alumni by institutional email address.
- Flexible ingest strategy: **Insert New Only** (skips duplicates) or **Safe Update Existing** (updates outdated employment details while preserving user keys).

### 8.2 Data Quality & Hygiene Dashboard
- Institutional completeness scoring across alumni cohorts.
- Visual breakdown of missing fields (unverified employment, missing contact numbers, blank LinkedIn URLs).
- Administrative verification queues for newly onboarded alumni.

### 8.3 Cohort Segmentation & Targeted Export
- Dynamic query builder filtering alumni by graduation year range, degree department, and current employer.
- Real-time preview of eligible cohort records.
- One-click CSV export of dynamically filtered segments for alumni association campaigns.

### 8.4 Targeted Institutional Announcements
- Administrative publication of priority news, reunion dates, and campus initiatives.
- Audience targeting filters: broadcast to *All Users*, *Students Only*, *Alumni Only*, or *Specific Cohort Segments*.
- Pinned announcement banners on web and mobile dashboards.

### 8.5 Institutional Audit Logging
- Immutable administrative action ledger tracking CSV ingest events, user role elevations, and account suspensions.
- Chronological timestamping and administrator user attribution for complete compliance.

---

## 9. Security & Governance Implementation

1. **Stateless Security**: No session state maintained on backend servers; every request is verified through cryptographic JWT signature and expiration checks.
2. **Credential Sanitization**: Passwords, JWT tokens, and connection strings are strictly excluded from API response schemas, application logs, and source control.
3. **Privilege Escalation Protection**: Student and Alumni accounts attempting to invoke administrative endpoints receive immediate `403 Forbidden` responses.
4. **CORS Boundary Enforcement**: Strict CORS headers prevent unauthorized third-party origins from dispatching authenticated credentialed requests.

---

## 10. Verification & Quality Assurance Summary

| Test Suite | Scope | Result |
| :--- | :--- | :--- |
| `test_feature_expansion_pass3a.py` | CSV Ingestion, Preview, Segmentation, Data Quality, Targeted Announcements | **8 / 8 PASSED (100%)** |
| `test_feature_expansion_pass2b.py` | Referrals, Communities, Stories, Moderation, Profile Enhancements | **7 / 7 PASSED (100%)** |
| `test_feature_expansion_pass2a.py` | Mentorship Requests, Events RSVP, Opportunities, Notifications | **18 / 18 PASSED (100%)** |
| `test_feature_expansion.py` | Core Authentication, Directory Search, Profile Read/Write | **8 / 8 PASSED (100%)** |
| `test_phase6h_full_system.py` | Full-stack regression and integration suite | **100% PASSED** |
| `test_final_production_cross_platform_sync.py` | Live cloud backend cross-platform synchronization (28 flows) | **28 / 28 PASSED (100%)** |

---

## 11. Future Scope
- Native push notification delivery via Firebase Cloud Messaging (FCM) for real-time mobile push notifications.
- WebRTC-based one-on-one direct audio/video mentorship sessions.
- AI-driven mentorship matching algorithms based on student project interests and alumni industry specializations.
