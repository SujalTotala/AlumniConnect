# AlumniConnect — Installation & Deployment Guide

**Project Title:** AlumniConnect – Digital Platform for Centralized Alumni Data Management and Engagement  
**Document ID:** `FINAL_SUBMISSION/09_Installation_Deployment_Guide.md`  
**Target Audience:** System Administrators, Developers, DevOps Evaluators  

---

## 1. Prerequisites & System Requirements

### 1.1 Development Workstation Requirements
- **Operating System:** Windows 10/11, macOS (12+), or Ubuntu Linux (20.04+).
- **Python:** Python 3.11 to 3.14 (with `pip` and `virtualenv`).
- **Node.js:** Node.js 18.x or 20.x LTS (with `npm`).
- **Java Development Kit (JDK):** JDK 17 (Temurin 17 recommended for Android Gradle).
- **Android Studio:** Android Studio Iguana / Jellyfish (or command-line Android SDK 34).
- **Git:** Version 2.30+.

---

## 2. Local Backend & Database Setup

### 2.1 Virtual Environment & Dependency Installation
Open PowerShell or bash in the project root directory:

```bash
# Navigate to backend directory
cd backend

# Create Python virtual environment
python -m venv venv

# Activate virtual environment
# Windows PowerShell:
.\venv\Scripts\Activate.ps1
# Linux / macOS:
source venv/bin/activate

# Install required dependencies
pip install -r requirements.txt
pip install pytest
```

### 2.2 Local Environment Variables Configuration
Create a `.env` file inside the `backend/` directory (or verify `backend/.env`):

```ini
# Local file-backed database
DATABASE_URL=sqlite:///./alumni.db

# Environment mode
ENVIRONMENT=development

# JWT Security
SECRET_KEY=local_development_super_secret_key_change_in_production_123456
ALGORITHM=HS256
ACCESS_TOKEN_EXPIRE_MINUTES=1440

# Allowed CORS origins
ALLOWED_ORIGINS=http://localhost:5173,http://127.0.0.1:5173
```

### 2.3 Database Migrations (Alembic)
Run the migration suite to initialize the local SQLite database schema to head:

```bash
# Verify single migration head
alembic heads

# Apply all migrations to head revision (d4e5f6a7b8c9)
alembic upgrade head
```

### 2.4 Running the Local API Server
Start the Uvicorn ASGI development server with auto-reload:

```bash
uvicorn app.main:app --host 127.0.0.1 --port 8000 --reload
```
- Interactive API Documentation (Swagger): [http://127.0.0.1:8000/docs](http://127.0.0.1:8000/docs)
- Alternative API Documentation (ReDoc): [http://127.0.0.1:8000/redoc](http://127.0.0.1:8000/redoc)

---

## 3. Local Web Frontend Setup

### 3.1 Dependency Installation & Dev Server
Open a second terminal window:

```bash
# Navigate to frontend directory
cd frontend

# Install Node modules
npm install

# Start Vite development server
npm run dev
```
The web application will launch at: [http://localhost:5173/](http://localhost:5173/)

### 3.2 Compiling the Production Build
To test the production optimized asset bundle:

```bash
npm run build
```
The compiled SPA will be output to `frontend/dist/`.

---

## 4. Android Mobile Application Setup

### 4.1 Opening in Android Studio
1. Launch **Android Studio**.
2. Select **Open** and select the `android/` directory (`d:\Projects\AlumniConnect\android`).
3. Allow Gradle to sync dependencies automatically.
4. Verify project SDK settings:
   - Compile SDK: `34`
   - Target SDK: `34`
   - Minimum SDK: `24`
   - JDK: `JDK 17`

### 4.2 Building Android Variants via CLI
Run the Gradle wrapper from the `android/` directory:

```bash
# 1. Build Debug APK (connects to local emulator http://10.0.2.2:8000/)
.\gradlew.bat assembleDebug
# Output: android/app/build/outputs/apk/debug/app-debug.apk

# 2. Build Project Review APK (debug-signed, connects to production https://alumniconnect-bwoi.onrender.com/)
.\gradlew.bat assembleReview
# Output: android/app/build/outputs/apk/review/AlumniConnect-ProjectReview-v1.0.apk

# 3. Build Unsigned Release APK (connects to production, ready for private release keystore signing)
.\gradlew.bat assembleRelease
# Output: android/app/build/outputs/apk/release/app-release-unsigned.apk
```

### 4.3 Installing on a Physical Device or Emulator
```bash
adb install -r android/app/build/outputs/apk/review/AlumniConnect-ProjectReview-v1.0.apk
```

---

## 5. Cloud Production Deployment Architecture

### 5.1 Backend Deployment (Render)
1. **Repository Link:** Connect your GitHub repository `SujalTotala/AlumniConnect`.
2. **Root Directory:** Set to `backend`.
3. **Build Command:**
   ```bash
   pip install -r requirements.txt && alembic upgrade head
   ```
4. **Start Command:**
   ```bash
   uvicorn app.main:app --host 0.0.0.0 --port $PORT
   ```
5. **Environment Variables Configured in Render Dashboard:**
   - `ENVIRONMENT`: `production`
   - `PYTHON_VERSION`: `3.11.9`
   - `SECRET_KEY`: `<cryptographically_random_256bit_hex_string>`
   - `ALGORITHM`: `HS256`
   - `ACCESS_TOKEN_EXPIRE_MINUTES`: `1440`
   - `DATABASE_URL`: `postgresql://<user>:<password>@<host>:5432/<dbname>`
   - `ALLOWED_ORIGINS`: `http://localhost:5173,https://alumni-connect-azure-mu.vercel.app`

### 5.2 Frontend Deployment (Vercel)
1. Link the repository to Vercel.
2. Root configuration in `vercel.json`:
   - `installCommand`: `npm --prefix frontend install`
   - `buildCommand`: `npm --prefix frontend run build`
   - `outputDirectory`: `frontend/dist`
3. Reverse proxy rewrites in `vercel.json` forward `/auth/*`, `/alumni/*`, etc., directly to the Render backend, providing zero-CORS latency.

