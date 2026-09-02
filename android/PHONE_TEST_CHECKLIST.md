# AlumniConnect — Physical Phone Testing Checklist

This checklist provides the step-by-step procedure for verifying the pre-release physical phone test APK connected to the production cloud backend (`https://alumniconnect-bwoi.onrender.com/`).

---

## Pre-Requisites & Target Build
- **APK Name:** `AlumniConnect-PhoneTest-v1.0.apk`
- **Path:** `android/app/build/outputs/apk/phone-test/AlumniConnect-PhoneTest-v1.0.apk`
- **Application ID:** `com.alumniconnect.app`
- **Backend API:** `https://alumniconnect-bwoi.onrender.com/`
- **Database:** Production PostgreSQL (Render)

---

## Practical Testing Sequence

### A. INSTALLATION
- [ ] Transfer and install `AlumniConnect-PhoneTest-v1.0.apk` on a physical Android device
- [ ] Allow "Install Unknown Apps" from file manager / browser if prompted
- [ ] Open **AlumniConnect**
- [ ] Verify splash screen displays smoothly and transitions to login / dashboard

### B. AUTHENTICATION
- [ ] Login using existing Student account credentials
- [ ] Verify Dashboard loads live data from production backend
- [ ] Logout via toolbar menu
- [ ] Login again to confirm persistent session capability

### C. STUDENT FLOWS
- [ ] **Profile**: Navigate to Profile tab and verify personal details
- [ ] **Edit Profile**: Modify bio, skills, or student details and save successfully
- [ ] **Alumni Directory**: Browse directory; search and filter alumni by name/department/company
- [ ] **Events**: Browse available events list
- [ ] **Event RSVP**: Open an event, RSVP / register, verify attendee counter increments
- [ ] **Mentorship**: Browse Available Mentors tab
- [ ] **Send Mentorship Request**: Send a mentorship connection request with a personalized message
- [ ] **Opportunities**: View posted jobs and internships; test application URL redirection
- [ ] **Notifications**: Open notification bell; verify notification items and unread count badge

### D. ALUMNI FLOWS
- [ ] Login as an Alumni account
- [ ] **Profile**: Verify corporate info, job title, and mentorship toggle status
- [ ] **Mentorship Received**: Open Mentorship tab -> Received Requests
- [ ] **Accept Student Request**: Accept pending mentorship request from Student with optional note
- [ ] **Create Event**: Create an event (if permitted by role)
- [ ] **Create Opportunity**: Post a career opportunity or job listing
- [ ] **Notifications**: Verify real-time notification alerts

### E. WEB ↔ APK SYNC (CROSS-PLATFORM INTEGRATION)

#### Test 1: Profile Synchronization
- Update student or alumni profile from Android mobile app
- Refresh React Web application at production URL
- Verify updated bio and details reflect instantly on the Web UI

#### Test 2: Event RSVP Synchronization
- Register / RSVP for an event on the React Web application
- Open or pull-to-refresh Events in the Android mobile app
- Verify registration badge displays "Registered" on Android

#### Test 3: Mentorship Lifecycle Synchronization
- Student submits a mentorship request on Web
- Alumni logs in on Android mobile app, opens Received Requests tab, and clicks **Accept**
- Student refreshes Web Mentorship tab and confirms status is **ACCEPTED**

### F. NETWORK RESILIENCE
- [ ] Test application behavior on Wi-Fi
- [ ] Switch to Cellular / Mobile data (4G/5G)
- [ ] Reopen application from recent apps / background
- [ ] Verify backend data loads without timeout or socket disconnection

### G. FINAL VERIFICATION
- [ ] Perform Logout
- [ ] Force close application
- [ ] Reopen application
- [ ] Perform fresh login to confirm clean state handling
