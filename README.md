# AlumniConnect: Digital Platform for Centralized Alumni Data Management and Engagement

## Overview
**AlumniConnect** is an enterprise-grade, cross-platform institutional alumni engagement, mentorship, and centralized data management platform designed for modern higher education institutions. The system unites active students, graduated alumni, and university administrators through a unified cloud architecture accessed simultaneously via a **modern React Web Portal** and a **native Android Mobile Application**.

---

## Problem Statement
Traditional higher education institutions struggle with fragmented, obsolete alumni records spread across isolated spreadsheets, departmental silos, and decentralized social networks. Active students face significant hurdles discovering verified industry mentors, securing verified job referrals, and participating in institutional events. Concurrently, university administrators lack centralized institutional data governance, duplicate detection, automated cohort segmentation, and verifiable engagement telemetry.

---

## Solution
AlumniConnect resolves institutional fragmentation by establishing a **single authoritative source of truth** hosted on a cloud-native PostgreSQL relational database, served by a high-throughput FastAPI REST backend, and consumed seamlessly across desktop web and mobile environments:
- **Centralized Directory**: Real-time verified records of alumni searchable by department, graduation year, skill set, and corporate employer.
- **Structured Mentorship Engine**: Asynchronous, student-to-alumni connection workflows with formal proposal messages, status auditing, and acceptance tracking.
- **Opportunity & Referral Board**: Direct publishing of job vacancies, internships, and referral pathways by verified alumni and administrators.
- **Centralized Alumni Data Management**: High-speed bulk CSV ingestion, pre-commit validation preview, duplicate detection, dynamic segmentation, data hygiene auditing, and targeted announcements.
- **Events & QR Attendance**: Campus workshops, webinars, and reunions with live attendee rosters and QR check-in capabilities.
- **Communities & Success Stories**: Collaborative interest clubs, milestone publications, and gamified achievement recognition.
- **Cross-Platform Parity**: Full session synchronicity and data integrity between React Web browsers and native Android mobile smartphones.

---

## Academic Review Documentation
- [Project Review Demo Flow](PROJECT_REVIEW_DEMO_FLOW.md): Step-by-step 7–10 minute faculty presentation guide covering Student, Alumni, and Admin workflows.
- [Project Review Features Specification](PROJECT_REVIEW_FEATURES.md): Detailed technical specification including problem statement, objectives, architecture, security, and verification matrix.
- [Final Demo Readiness Audit](FINAL_DEMO_READINESS.md): Comprehensive 31-phase project state audit and status matrix.

---

## Core Modules & Functional Architecture

| Module | Purpose & Scope | Target Roles |
| :--- | :--- | :--- |
| **Authentication & RBAC** | Stateless JWT authentication (`HS256`), salted bcrypt password hashing, and role-based route guards. | Public, Student, Alumni, Admin |
| **Alumni Directory** | Directory of verified graduates with live multi-parameter search (name, company, batch, skills). | All Users |
| **Mentorship System** | Proposal dispatch, proposal notes, duplicate prevention, and status tracking (`PENDING`, `ACCEPTED`, `REJECTED`, `COMPLETED`). | Student & Alumni |
| **Opportunities & Referrals** | Full-time jobs, internships, and referral postings with direct application links and expiration tracking. | Student & Alumni |
| **Events & QR Attendance** | Campus workshops and webinars with RSVP registration, attendee rosters, and QR check-in concept. | All Users |
| **Communities** | Discussion channels and interest groups for peer-to-peer and alumni-student collaboration. | All Users |
| **Success Stories** | Published career milestones and inspirational achievements authored by graduates. | All Users |
| **Alumni Data Management** | Bulk CSV ingest with preview, duplicate detection, dynamic cohort segmentation, data hygiene dashboard, and targeted announcements. | Admin Only |
| **Notification Center** | In-app notification inbox with read tracking and unread count badges. | All Authenticated Users |

---

## Web + Android Cross-Platform Architecture

AlumniConnect implements a **decoupled client-server architecture** where neither client communicates directly with the other. Both clients interact strictly through standardized REST API contracts:

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

### Key Architectural Principles:
1. **Zero Client-to-Client Coupling**: The Web portal and Android app operate completely independently.
2. **Atomic Single Source of Truth**: All operations (mentorship approvals, RSVP counts, job posts, CSV imports) commit to PostgreSQL immediately.
3. **Stateless JWT Authentication**: Both clients exchange login credentials for cryptographic JWT bearer tokens.
4. **Backend-Enforced RBAC**: Every endpoint enforces role authorization at the controller dependency level, independent of client-side UI guards.

---

## Technology Stack

### Backend
- **Language**: Python 3.11
- **Framework**: FastAPI (ASGI)
- **Web Server**: Uvicorn
- **ORM**: SQLAlchemy 2.0
- **Database Driver**: pg8000 (Pure Python PostgreSQL driver) & SQLite (local dev)
- **Migrations**: Alembic (deterministic single head)
- **Security & Cryptography**: python-jose (HS256 JWT), passlib & bcrypt (salted hashing)
- **Data Validation**: Pydantic v2 & email-validator

### Web Client
- **Core**: React 19, JavaScript (ES2022)
- **Build Tool**: Vite 8
- **Styling**: Vanilla CSS with tailored design tokens and responsive layouts
- **Routing**: React Router v7 (SPA client-side routing with role-based Route Guards)
- **HTTP Client**: Axios (with JWT injection & response interceptors)

### Mobile Client (Android)
- **Language**: Java 17
- **SDK Targets**: compileSdk 34 (Android 14), minSdk 24 (Android 7.0), targetSdk 34
- **Networking**: Square Retrofit 2, OkHttp 3, OkHttp Logging Interceptor
- **Serialization**: Google Gson
- **UI Components**: Material Components 3, AndroidX, RecyclerView, SwipeRefreshLayout, CardView
- **Barcode / Scanner**: ZXing Android Embedded

### Deployment Infrastructure
- **API Hosting**: Render (Linux Web Service with Uvicorn)
- **Web Hosting**: Vercel (Edge CDN with SPA route rewrites)
- **Database Hosting**: Cloud PostgreSQL with SSL enforcement

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

# Run migrations
alembic upgrade head

# Start development server
uvicorn app.main:app --reload --host 127.0.0.1 --port 8000
```
Interactive Swagger API documentation is available at `http://127.0.0.1:8000/docs`.

### 2. Frontend Setup
```bash
cd frontend

# Install Node dependencies
npm install

# Start development server
npm run dev

# Build for production
npm run build
```
Web Application is accessible locally at `http://localhost:5173`.

### 3. Android Setup
1. Open the `android/` directory in **Android Studio**.
2. Configure `android/local.properties`:
   ```properties
   sdk.dir=C\:\\Users\\<Username>\\AppData\\Local\\Android\\Sdk
   ```
3. Build APK variants:
   ```bash
   cd android
   # Build local debug APK (targets http://10.0.2.2:8000/)
   .\gradlew.bat assembleDebug

   # Build faculty review APK (targets cloud production backend with debug signature)
   .\gradlew.bat assembleReview

   # Build official release APK (unsigned)
   .\gradlew.bat assembleRelease
   ```
4. Output APK locations:
   - **Review APK**: `android/app/build/outputs/apk/review/AlumniConnect-ProjectReview-v1.0.apk`
   - **Debug APK**: `android/app/build/outputs/apk/debug/app-debug.apk`
   - **Release APK**: `android/app/build/outputs/apk/release/app-release-unsigned.apk`

---

## Environment Variables Reference (No Values Exposed)

### Backend (`backend/.env`)
- `ENVIRONMENT`: Application runtime mode (`development`, `production`).
- `DATABASE_URL`: Connection string (`postgresql://...` for production, `sqlite:///...` for local).
- `SECRET_KEY`: High-entropy cryptographic secret for signing JWTs.
- `ALGORITHM`: Token signing algorithm (Default: `HS256`).
- `ACCESS_TOKEN_EXPIRE_MINUTES`: Expiration lifetime for session tokens.
- `ALLOWED_ORIGINS`: Comma-separated list of allowed browser client domains.

### Frontend (`frontend/.env`)
- `VITE_API_BASE_URL`: Base HTTPS URL of the FastAPI backend.

---

## Production Deployment Configuration

### Backend (Render)
Configured using Infrastructure-as-Code in `render.yaml`:
- **Build Command**: `pip install -r requirements.txt && alembic upgrade head`
- **Start Command**: `uvicorn app.main:app --host 0.0.0.0 --port $PORT`
- **Live Target**: `https://alumniconnect-bwoi.onrender.com`

### Frontend (Vercel)
Configured for single-page routing in `vercel.json`:
- Rewrites all dynamic client routes (`/(.*)`) to `/index.html` for clean HTML5 history navigation.

---

## Automated Verification & Test Suites

Run the regression and integration test suites:
```bash
# In backend/ directory:
venv\Scripts\python.exe test_feature_expansion_pass3a.py
venv\Scripts\python.exe test_feature_expansion_pass2b.py
venv\Scripts\python.exe test_feature_expansion_pass2a.py
venv\Scripts\python.exe test_feature_expansion.py
venv\Scripts\python.exe test_phase6h_full_system.py

# Live cloud backend cross-platform synchronization test:
backend\venv\Scripts\python.exe backend\scripts\test_final_production_cross_platform_sync.py
```

---

## Academic Project Information
- **Project Title**: AlumniConnect: Digital Platform for Centralized Alumni Data Management and Engagement
- **Domain**: Web & Mobile Distributed Systems, Educational Informatics
- **Target Audience**: Universities, Alumni Associations, Student Career Placement Offices
