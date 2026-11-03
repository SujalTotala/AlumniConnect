# AlumniConnect — RESTful API Documentation

**Project Title:** AlumniConnect – Digital Platform for Centralized Alumni Data Management and Engagement  
**Document ID:** `FINAL_SUBMISSION/05_API_Documentation.md`  
**Base URL (Local Dev):** `http://127.0.0.1:8000/`  
**Base URL (Production Cloud):** `https://alumniconnect-bwoi.onrender.com/`  
**Specification Standard:** OpenAPI 3.0 / FastAPI  

---

## 1. Authentication & Security Scheme

All protected endpoints require an HTTP Authorization header containing a valid JWT Bearer token:
```http
Authorization: Bearer <access_token>
```

Tokens are obtained by authenticating against `/auth/login` or registering via `/auth/register`. Token expiration is set to 24 hours (`1440 minutes`).

Role-Based Access Control (RBAC) enforces three privilege levels:
- **`student`**: Enrolled student member.
- **`alumni`**: Graduated alumnus member.
- **`admin`**: Institutional system administrator.

---

## 2. API Endpoints by Functional Group

### 2.1 Authentication (`/auth`)

| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/auth/register` | Public | Registers a new student or alumni account. Returns token and user profile. |
| `POST` | `/auth/login` | Public | Authenticates credentials and returns a signed JWT Bearer token. |
| `GET` | `/auth/me` | Authenticated | Retrieves current authenticated user record and active role. |

#### Request / Response Example: `POST /auth/login`
**Request Payload:**
```json
{
  "email": "student@alumni.edu",
  "password": "StudentPassword123!"
}
```
**Response (200 OK):**
```json
{
  "message": "Login successful",
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "bearer",
  "user": {
    "id": 12,
    "name": "Demo Student",
    "email": "student@alumni.edu",
    "role": "student"
  }
}
```

---

### 2.2 Profile Management (`/profile`)

| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/profile/me` | Authenticated | Returns current profile details with role-specific student/alumni extensions. |
| `PUT` | `/profile/me` | Authenticated | Updates current user profile (skills, bio, current company, social links). |
| `GET` | `/profile/completion-suggestions` | Authenticated | Returns missing profile fields and completion percentage. |

---

### 2.3 Alumni Directory (`/alumni`)

| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/alumni/` | Authenticated | Searches and filters alumni by keyword, department, batch, company, and mentorship. |
| `GET` | `/alumni/{id}` | Authenticated | Retrieves full profile details for a specific alumnus. |
| `POST` | `/alumni/{id}/verify` | `admin` | Toggles or confirms institutional verification badge for an alumnus. |

---

### 2.4 Mentorship Engine (`/mentorship`)

| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/mentorship/requests` | `student` | Dispatches a formal mentorship request to an alumnus. |
| `GET` | `/mentorship/requests` | Authenticated | Lists inbound (for alumni) or outbound (for students) mentorship requests. |
| `PUT` | `/mentorship/requests/{id}/status` | `alumni` | Updates proposal status (`ACCEPTED`, `REJECTED`, `COMPLETED`). |
| `GET` | `/mentorship/recommendations` | `student` | Returns recommended mentors matching student department and skills. |

---

### 2.5 Professional Connections & Networking (`/connections`)

| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/connections/request` | Authenticated | Sends a networking connection request to another member. |
| `GET` | `/connections/status/{user_id}` | Authenticated | Checks mutual connection status (`NONE`, `PENDING`, `CONNECTED`). |
| `PUT` | `/connections/{id}/accept` | Authenticated | Target user accepts the incoming connection request. |
| `GET` | `/connections/my-network` | Authenticated | Lists all confirmed connections with user summaries. |
| `GET` | `/connections/suggestions` | Authenticated | Provides intelligent connection suggestions based on mutual batch/branch. |
| `DELETE`| `/connections/{id}` | Authenticated | Removes an existing connection or cancels a pending request. |

---

### 2.6 Job Opportunities & Referrals (`/opportunities`, `/referrals`)

| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/opportunities/` | Authenticated | Lists active job and internship postings with search filters. |
| `POST` | `/opportunities/` | `alumni`, `admin` | Publishes a new career opportunity with job role and requirements. |
| `POST` | `/referrals/request` | `student` | Submits a referral request to a connected alumnus for an opportunity. |
| `GET` | `/referrals/my-requests` | `student` | Lists outgoing referral applications submitted by the student. |
| `GET` | `/referrals/inbox` | `alumni` | Lists incoming referral requests submitted to the alumnus. |
| `PUT` | `/referrals/{id}/status` | `alumni` | Updates referral request status (`ACCEPTED`, `REJECTED`, `COMPLETED`). |

---

### 2.7 Events & QR Attendance Tracking (`/events`)

| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/events/` | Authenticated | Lists published campus events and workshops. |
| `POST` | `/events/` | `admin`, `alumni` | Creates a new campus event with capacity and schedule details. |
| `POST` | `/events/{id}/register` | Authenticated | Submits an RSVP registration for an upcoming event. |
| `GET` | `/events/{id}/qr-token` | `admin` | Generates a cryptographically signed, timed QR token for event check-in. |
| `POST` | `/events/{id}/check-in` | Authenticated | Records attendee check-in via scanned QR token or manual check-in. |
| `GET` | `/events/{id}/attendance` | `admin` | Retrieves attendance statistics and verified attendee roster. |

---

### 2.8 Community Hubs (`/communities`)

| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/communities/` | Authenticated | Lists all available interest and departmental communities. |
| `POST` | `/communities/` | Authenticated | Creates a new community hub. |
| `POST` | `/communities/{id}/join` | Authenticated | Joins the specified community. |
| `GET` | `/communities/{id}/posts` | Authenticated | Retrieves discussion feed for the community. |
| `POST` | `/communities/{id}/posts` | Authenticated | Publishes a new discussion post within the community. |

---

### 2.9 Success Stories & Achievements (`/success-stories`, `/achievements`)

| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/success-stories/` | Public / Auth | Lists approved inspirational alumni success stories. |
| `POST` | `/success-stories/` | `alumni` | Submits a new success story for administrative approval. |
| `PUT` | `/success-stories/{id}/approve` | `admin` | Approves or rejects a submitted success story. |
| `GET` | `/achievements/` | Public / Auth | Lists verified achievements, patents, and awards. |
| `POST` | `/achievements/` | `alumni` | Submits an achievement milestone for institutional recognition. |

---

### 2.10 Institutional Data Governance & Segments (`/admin`)

| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/admin/statistics` | `admin` | Aggregates system metrics (users, alumni, events, mentorships, attendance). |
| `GET` | `/admin/data-quality` | `admin` | Evaluates email, phone, company, and LinkedIn data completeness scores. |
| `POST` | `/admin/import/preview` | `admin` | Dry-runs a bulk alumni CSV file and returns error rows and validation metrics. |
| `POST` | `/admin/import/commit` | `admin` | Commits verified CSV data into the database with duplicate resolution modes. |
| `GET` | `/admin/segments` | `admin` | Lists saved dynamic alumni cohort segments. |
| `POST` | `/admin/segments` | `admin` | Creates a new dynamic cohort segment with criteria rules. |
| `GET` | `/admin/segments/{id}/preview` | `admin` | Previews alumni currently matching the segment's criteria. |
| `GET` | `/admin/audit-logs` | `admin` | Retrieves paginated administrative action audit trails. |
| `GET` | `/admin/export/alumni` | `admin` | Generates a sanitized CSV export of the verified alumni directory. |

