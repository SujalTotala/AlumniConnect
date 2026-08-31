# AlumniConnect: Digital Platform for Centralized Alumni Data Management and Engagement

## Overview
**AlumniConnect** is an enterprise-grade, cross-platform alumni engagement and mentorship platform designed for modern higher education institutions. The system bridges the gap between active students, graduates (alumni), and university administrators through a unified cloud architecture accessed simultaneously via a **modern React Web Portal** and a **native Android Mobile Application**.

---

## Problem Statement
Traditional higher education institutions struggle with fragmented, obsolete alumni records spread across isolated spreadsheets, social media groups, and departmental silos. Active students face significant hurdles discovering relevant industry mentors, securing verified career opportunities, and attending institutional events. Furthermore, university administration lacks real-time visibility into alumni engagement metrics, career trajectories, and cross-platform institutional networking.

---

## Solution
AlumniConnect resolves institutional fragmentation by establishing a **single source of truth** hosted on a cloud-native PostgreSQL relational database, served by a high-throughput FastAPI REST backend, and consumed seamlessly across desktop web and mobile environments:
- **Centralized Directory**: Real-time verified records of alumni searchable by department, graduation year, skill set, and corporate employer.
- **Structured Mentorship Engine**: Asynchronous, student-to-alumni connection workflows with formal proposal messages, status auditing, and acceptance tracking.
- **Opportunity & Event Dispatcher**: Direct posting of job/internship openings and campus/virtual events with live attendee rosters and instant notifications.
- **Cross-Platform Parity**: Full biometric/JWT session synchronicity between web browsers and native mobile smartphones.

---

## Key Features

### Student Portal
- **Alumni Discovery**: Filter graduates by industry skills, target companies, branch/department, and graduation year.
- **Mentorship Requests**: Submit formal mentorship applications to verified graduates with custom introductory messaging.
- **Career Opportunities**: Browse full-time roles and internships posted directly by alumni with direct application links.
- **Event Registrations**: RSVP for upcoming reunions, tech talks, guest lectures, and webinars with instant attendance confirmation.
- **Profile Customization**: Maintain academic branch, year of study, technical skills, and career interests.

### Alumni Portal
- **Mentorship Availability**: One-click toggle to declare mentorship availability to the student body.
- **Mentorship Management**: Review incoming mentorship proposals, evaluate student background, and accept or decline with personalized guidance notes.
- **Opportunity Broadcasting**: Post job openings, internship vacancies, and referral pathways directly to students.
- **Event Hosting**: Organize departmental reunions, webinars, and professional workshops with real-time attendee tracking.
- **Professional Identity**: Highlight current employer, job title, industry experience, LinkedIn/GitHub profiles, and portfolio.

### Administrator Portal
- **Platform Analytics**: High-level telemetry covering total registered users, verified alumni, active mentors, published events, and open mentorship requests.
- **User Governance**: Audit user accounts across student, alumni, and administrative roles; activate or suspend accounts as necessary.
- **Institutional Oversight**: Ensure content integrity, manage campus-wide event schedules, and prevent unauthorized role escalation.

---

## Web + Android Cross-Platform Architecture
AlumniConnect implements a **decoupled client-server architecture** where neither client communicates directly with the other. Both clients interact strictly through standardized REST API contracts:

```
+-------------------------------------------------------------+
|                     PostgreSQL Database                     |
|                   (Single Source of Truth)                  |
+-------------------------------------------------------------+
                              ^
                              | SQLAlchemy ORM
+-------------------------------------------------------------+
|                 FastAPI Backend Service                     |
|        (JWT Bearer Auth + RBAC + CORS Validation)           |
+-------------------------------------------------------------+
            ^                                     ^
            | HTTPS / JSON                        | HTTPS / JSON
            v                                     v
+-----------------------+             +-----------------------+
|   React 19 Web App    |             |  Native Android App   |
|   (Vite 8 + Tailwind) |             |  (Java 17 + Retrofit) |
+-----------------------+             +-----------------------+
```

### Key Architectural Principles:
1. **No Client-to-Client Coupling**: The Web portal and Android app operate completely independently.
2. **Atomic Single Source of Truth**: All operations (mentorship approvals, RSVP counts, job posts) commit to PostgreSQL immediately.
3. **Stateless JWT Authentication**: Both clients exchange login credentials for cryptographic JWT bearer tokens.
4. **Backend-Enforced RBAC**: Every endpoint enforces role authorization at the controller dependency level, independent of client-side UI guards.

---

## Technology Stack

### Backend
- **Language**: Python 3.11 / 3.14
- **Framework**: FastAPI (ASGI)
- **Web Server**: Uvicorn
- **ORM**: SQLAlchemy 2.0
- **Database Driver**: pg8000 (Pure Python PostgreSQL driver) & SQLite (local dev)
- **Migrations**: Alembic
- **Security & Cryptography**: python-jose (HS256 JWT), passlib & bcrypt (salted hashing)
- **Data Validation**: Pydantic v2 & email-validator

### Web Client
- **Core**: React 19, JavaScript (ES2022)
- **Build Tool**: Vite 8
- **Styling**: Tailwind CSS v4
- **Routing**: React Router v7 (Client-side routing with role-based Route Guards)
- **HTTP Client**: Axios (with JWT injection & 401 response interceptors)

### Mobile Client (Android)
- **Language**: Java (Targeting Java 8/17 compatibility)
- **SDK**: compileSdk 34 (Android 14), minSdk 24 (Android 7.0)
- **Networking**: Square Retrofit 2, OkHttp 3, OkHttp Logging Interceptor
- **Serialization**: Google Gson
- **UI Components**: Google Material Components 3, AndroidX, RecyclerView, SwipeRefreshLayout, CardView

### Deployment Infrastructure
- **API Hosting**: Render (Linux Web Service with Uvicorn)
- **Web Hosting**: Vercel (Edge CDN with SPA route rewrites)
- **Database Hosting**: Cloud PostgreSQL with SSL enforcement

---

## Authentication & Security
- **Salted Password Hashing**: Passwords hashed using standard bcrypt algorithms; plaintext passwords never touch database persistence layers.
- **Cryptographic Tokens**: Access tokens issued with configurable expiration, containing subject (`sub`), email, and institutional role claims.
- **Admin Privilege Escalation Lockdown**: The registration controller enforces restrictions preventing arbitrary users from self-assigning administrative privileges.
- **CORS Whitelist Protection**: Production backend explicitly denies wildcard (`*`) origins when credentials are transmitted, enforcing whitelisted institutional domain origins.
- **Tenant Notification Isolation**: Database queries strictly filter notifications by user identity, preventing cross-user data leakage.

---

## Core Modules & API Architecture

| Domain | Route Prefix | Primary Endpoints | Access Level |
| :--- | :--- | :--- | :--- |
| **Auth** | `/auth` | `POST /register`, `POST /login`, `GET /me` | Public / Bearer |
| **Profiles** | `/profile` | `GET /me`, `PUT /me` | Authenticated |
| **Alumni** | `/alumni` | `GET /`, `GET /{id}` | Public / Authenticated |
| **Events** | `/events` | `GET /`, `POST /`, `GET /{id}`, `POST /{id}/register`, `GET /{id}/registrations` | Authenticated (Role Filtered) |
| **Mentorship** | `/mentorship` | `GET /mentors`, `POST /request`, `GET /requests/sent`, `GET /requests/received`, `PUT /requests/{id}/status` | Student / Alumni |
| **Opportunities** | `/opportunities` | `GET /`, `POST /`, `GET /{id}`, `DELETE /{id}` | Authenticated |
| **Notifications** | `/notifications`| `GET /`, `GET /unread-count`, `PUT /{id}/read`, `PUT /mark-all-read` | Authenticated (Owner Only) |
| **Admin** | `/admin` | `GET /statistics`, `GET /users`, `PUT /users/{id}/status` | Admin Only |

---

## Local Development Setup

### 1. Backend Setup
```bash
cd backend

# Create virtual environment
python -m venv venv

# Activate virtual environment
# On Windows (PowerShell):
.\venv\Scripts\Activate.ps1
# On Linux/macOS:
source venv/bin/activate

# Install dependencies
pip install -r requirements.txt

# Start development server
uvicorn app.main:app --reload --host 127.0.0.1 --port 8000
```
API Documentation: Interactive Swagger UI is available at `http://127.0.0.1:8000/docs`.

### 2. Frontend Setup
```bash
cd frontend

# Install Node dependencies
npm install

# Start development server
npm run dev
```
Web Application: Accessible locally at `http://localhost:5173`.

### 3. Android Setup
1. Open the `android/` directory in **Android Studio**.
2. Ensure Android SDK 34 is installed via SDK Manager.
3. Configure `android/local.properties`:
   ```properties
   sdk.dir=C\:\\Users\\<Username>\\AppData\\Local\\Android\\Sdk
   ```
4. Build APK:
   ```bash
   cd android
   .\gradlew.bat assembleDebug
   ```
5. Deploy to connected emulator or physical device directly from Android Studio. Note: Emulator builds automatically connect to `http://10.0.2.2:8000/`.

---

## Environment Variables Reference

### Backend (`backend/.env`)
- `ENVIRONMENT`: Application runtime mode (`development`, `testing`, `production`).
- `DATABASE_URL`: Connection string (`sqlite:///./alumni.db` for local dev; `postgresql://...` for production).
- `SECRET_KEY`: High-entropy cryptographic secret for signing JWTs (>= 32 chars in production).
- `ALGORITHM`: Signature algorithm (Default: `HS256`).
- `ACCESS_TOKEN_EXPIRE_MINUTES`: Expiration lifetime for session tokens.
- `ALLOWED_ORIGINS`: Comma-separated list of allowed browser client domains.

### Frontend (`frontend/.env`)
- `VITE_API_BASE_URL`: Base HTTPS/HTTP URL of the FastAPI backend.

---

## Production Deployment

### Backend (Render)
Configured using Infrastructure-as-Code in `render.yaml`:
- **Build Command**: `pip install -r requirements.txt && alembic upgrade head`
- **Start Command**: `uvicorn app.main:app --host 0.0.0.0 --port $PORT`

### Frontend (Vercel)
Configured for single-page routing in `vercel.json`:
- Rewrites all dynamic client routes (`/(.*)`) to `/index.html` for clean HTML5 history navigation.

---

## Testing & Quality Assurance
The codebase includes comprehensive automated test suites:
- `backend/test_phase6a_auth.py`: Verifies Android & Web authentication contracts, token issuance, and user session retrieval.
- `backend/test_phase6h_full_system.py`: End-to-end multi-role test covering all modules: auth, profile updating, event registration, mentorship lifecycles, notification dispatches, and administrative RBAC gates.
- `backend/scripts/verify_production_full_suite.py`: Remote verification suite auditing production HTTPS endpoints, preflight CORS, and transactional database integrity.

---

## Future Scope
1. **Push Notifications**: Integration with Firebase Cloud Messaging (FCM) for real-time mobile push delivery.
2. **Direct In-App Messaging**: WebSocket-based real-time chat between accepted mentors and mentees.
3. **Alumni Donation & Endowment Gateway**: Secure payment portal integration for institutional alumni fundraising campaigns.
4. **Resume Parsing & AI Matching**: Automated profile matching between student career preferences and alumni job openings using natural language processing.

---

## Academic Project Information
- **Project Title**: AlumniConnect: Digital Platform for Centralized Alumni Data Management and Engagement
- **Domain**: Web & Mobile Distributed Systems, Educational Informatics
- **Target Audience**: Universities, Alumni Associations, Student Placement Offices
