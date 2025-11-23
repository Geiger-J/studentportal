# Student Portal - Comprehensive Test Procedure

This document provides a detailed, step-by-step testing procedure to validate all functionality of the Student Portal application. Follow these steps in order to thoroughly test the system.

## Prerequisites

### Setup
1. Ensure Java 17+ is installed
2. PostgreSQL database is running (or use H2 for testing)
3. Configure database connection in `application.properties`
4. Build the application: `./mvnw clean install`
5. Run the application: `./mvnw spring-boot:run`
6. Access the application at: `http://localhost:8080`

### Test Data Requirements
- At least 2 student accounts
- At least 1 admin account
- Multiple subjects (automatically seeded)
- Test various request scenarios

---

## Part 1: User Registration and Authentication

### Test 1.1: Student Registration
**Objective:** Verify student users can register successfully

**Steps:**
1. Navigate to the registration page
2. Fill in the registration form with the following data:
   - Full Name: `John Smith`
   - Email: `123john@example.com` (must start with a number for STUDENT role)
   - Password: `Password123!`
   - Confirm Password: `Password123!`
3. Click "Register"

**Expected Result:**
- User is registered successfully
- User is redirected to login page or profile completion page
- Student role is automatically assigned (email starts with number)

### Test 1.2: Admin Registration
**Objective:** Verify admin users can register successfully

**Steps:**
1. Navigate to the registration page
2. Fill in the registration form:
   - Full Name: `Admin User`
   - Email: `admin@example.com` (does NOT start with a number for ADMIN role)
   - Password: `AdminPass123!`
   - Confirm Password: `AdminPass123!`
3. Click "Register"

**Expected Result:**
- User is registered successfully
- Admin role is automatically assigned (email does not start with number)
- User can access admin dashboard

### Test 1.3: Login
**Objective:** Verify users can log in

**Steps:**
1. Navigate to login page
2. Enter student credentials from Test 1.1
3. Click "Login"

**Expected Result:**
- User is logged in successfully
- Redirected to profile completion page (first login for students)

---

## Part 2: Student Profile Management

### Test 2.1: Complete Profile - Basic Information
**Objective:** Verify students can complete their profile

**Steps:**
1. Log in as student (`123john@example.com`)
2. You should be redirected to profile completion page
3. Select Year Group: `Year 12`
4. Select Exam Board: `A-Level` or `IB`
5. Continue to next step

**Expected Result:**
- Year group and exam board saved successfully
- User proceeds to subject selection

### Test 2.2: Complete Profile - Subject Selection
**Objective:** Verify students can select subjects from curated groups

**Steps:**
1. Continue from Test 2.1 or navigate to profile page
2. Select subjects from each category:
   - **Languages:** English, German
   - **STEM:** Mathematics, Physics, Chemistry
   - **Social Sciences:** Economics
3. Selected subjects should be visually highlighted

**Expected Result:**
- All selected subjects are highlighted with active state
- Subjects are stored in user profile

### Test 2.3: Complete Profile - Availability Selection
**Objective:** Verify students can select their availability

**Steps:**
1. Continue from Test 2.2 or navigate to profile page
2. Click on timeslot cells in the availability grid:
   - Monday: P1, P3, P5
   - Tuesday: P2, P4
   - Wednesday: P1, P3, P5
   - Thursday: P2, P4, P6
   - Friday: P1, P7
3. Selected cells should be visually highlighted
4. Click "Save Profile" or "Complete Profile"

**Expected Result:**
- Profile marked as complete
- User redirected to dashboard
- Selected timeslots saved correctly

### Test 2.4: Edit Profile
**Objective:** Verify students can edit their profile after completion

**Steps:**
1. Log in as student with completed profile
2. Navigate to Profile page
3. Add new subject: French
4. Remove subject: Economics
5. Add new availability: Saturday P1
6. Save changes

**Expected Result:**
- Changes are saved successfully
- Dashboard reflects updated profile
- Can create requests with new subjects and timeslots

---

## Part 3: Request Creation

### Test 3.1: Create TUTOR Request
**Objective:** Verify students can create tutoring offers

**Steps:**
1. Log in as student with completed profile
2. Navigate to "Create Request" page
3. Select Request Type: `TUTOR` (Offering Tutoring)
4. Select Subject: `Mathematics`
5. Select Timeslots from available times:
   - Monday P1
   - Wednesday P3
   - Friday P1
6. Week start date should default to next Monday
7. Click "Create Request"

**Expected Result:**
- Request created successfully
- Status: PENDING
- Redirected to dashboard
- Request appears in "My Requests" table
- Success message displayed

### Test 3.2: Create TUTEE Request
**Objective:** Verify students can create tutoring requests

**Steps:**
1. Create second student account: `456jane@example.com` / `Password123!`
2. Complete profile with overlapping subjects and availability
3. Navigate to "Create Request"
4. Select Request Type: `TUTEE` (Seeking Tutoring)
5. Select Subject: `Mathematics`
6. Select Timeslots:
   - Monday P1
   - Wednesday P3
7. Click "Create Request"

**Expected Result:**
- Request created successfully
- Status: PENDING
- Request visible on dashboard

### Test 3.3: Validate Timeslot Restrictions
**Objective:** Verify only available timeslots can be selected

**Steps:**
1. Log in as student
2. Navigate to "Create Request"
3. Attempt to select timeslots not in your availability

**Expected Result:**
- Unavailable timeslots are disabled
- Tooltip suggests editing profile to enable those slots
- Cannot submit request with unavailable timeslots

### Test 3.4: Prevent Duplicate Requests
**Objective:** Verify system prevents duplicate pending requests

**Steps:**
1. Log in as student who already has a PENDING Mathematics TUTOR request
2. Navigate to "Create Request"
3. Try to create another Mathematics TUTOR request
4. Click "Create Request"

**Expected Result:**
- Error message displayed
- Request not created
- Message indicates duplicate active request exists

### Test 3.5: Create Multiple Requests (Different Subjects)
**Objective:** Verify students can have multiple requests for different subjects

**Steps:**
1. Log in as student
2. Create TUTOR request for Mathematics (if not exists)
3. Create TUTEE request for Physics
4. Create TUTOR request for English

**Expected Result:**
- All three requests created successfully
- All visible on dashboard
- Each has correct subject and type

---

## Part 4: Request Cancellation

### Test 4.1: Cancel PENDING Request
**Objective:** Verify students can cancel their pending requests

**Steps:**
1. Log in as student with PENDING request
2. Navigate to dashboard
3. Locate a PENDING request
4. Click "Cancel" button
5. Confirm cancellation in dialog

**Expected Result:**
- Request status changes to CANCELLED
- Success message displayed
- Request still visible on dashboard with CANCELLED status
- Cancel button no longer available for that request

### Test 4.2: Cancel MATCHED Request
**Objective:** Verify students can cancel matched requests (both parties cancelled)

**Pre-requisites:**
- Create TUTOR request (Student A)
- Create matching TUTEE request (Student B)
- Run matching algorithm (admin)
- Verify both requests are MATCHED

**Steps:**
1. Log in as Student A (who has MATCHED request)
2. Navigate to dashboard
3. Locate the MATCHED request
4. Click "Cancel" button
5. Confirm cancellation

**Expected Result:**
- Student A's request status changes to CANCELLED
- Student B's matched request also changes to CANCELLED
- Both students see CANCELLED status on their dashboards
- Success message displayed

### Test 4.3: Attempt to Cancel Non-Cancellable Request
**Objective:** Verify system prevents cancellation of non-cancellable requests

**Steps:**
1. Create a request and let it complete (status: DONE)
2. Try to access cancel endpoint directly or find cancel button

**Expected Result:**
- Cancel button not visible for DONE requests
- If attempted via direct API call, error message displayed
- Request status remains unchanged

---

## Part 5: Dashboard Functionality

### Test 5.1: View All Requests
**Objective:** Verify dashboard displays all user requests

**Steps:**
1. Log in as student with multiple requests
2. View dashboard

**Expected Result:**
- All requests displayed in table format
- Columns visible:
  - Subject
  - Type (with color coding: TUTOR/TUTEE)
  - Timeslots (count + labels)
  - Status (with badge styling)
  - Matched Partner (name or "Not matched")
  - Created date
  - Week Start date
  - Actions (Cancel button when applicable)

### Test 5.2: View Request Details
**Objective:** Verify request information is accurate

**Steps:**
1. View dashboard with multiple requests
2. Verify each request shows:
   - Correct subject name
   - Correct type (TUTOR vs TUTEE with proper styling)
   - Accurate timeslot count and names
   - Appropriate status badge color
   - Partner name if matched

**Expected Result:**
- All information displays correctly
- Status badges have appropriate colors:
  - PENDING: yellow/orange
  - MATCHED: green
  - CANCELLED: red/grey
  - DONE: blue

### Test 5.3: Success and Error Messages
**Objective:** Verify flash messages display correctly

**Steps:**
1. Create a request (should see success message)
2. Cancel a request (should see success message)
3. Try to create duplicate request (should see error message)

**Expected Result:**
- Success messages appear in green
- Error messages appear in red
- Messages auto-dismiss or can be dismissed

---

## Part 6: Admin Dashboard

### Test 6.1: Access Admin Dashboard
**Objective:** Verify admin users can access admin features

**Steps:**
1. Log in as admin user (`admin@example.com`)
2. Navigate to Admin Dashboard

**Expected Result:**
- Admin dashboard visible
- Statistics displayed:
  - Total Requests
  - Pending Requests
  - Matched Requests
  - Total Users
  - Student Count
  - Administrator Count
- Action buttons visible:
  - Manage Requests
  - Manage Users
  - Run Matching Algorithm
  - Archive Old Requests

### Test 6.2: View All Requests (Admin)
**Objective:** Verify admin can view all system requests

**Steps:**
1. Log in as admin
2. Click "Manage Requests"
3. View all requests in system

**Expected Result:**
- All requests from all users displayed
- Can filter by status (PENDING, MATCHED, etc.)
- Shows request ID, user name, subject, type, status, matched partner, created date

### Test 6.3: View All Users (Admin)
**Objective:** Verify admin can view all system users

**Steps:**
1. Log in as admin
2. Click "Manage Users"
3. View all users

**Expected Result:**
- All users displayed in table
- Shows user ID, full name, email, role, profile completion status
- Delete button available for each user

### Test 6.4: Delete User
**Objective:** Verify admin can delete users

**Steps:**
1. Log in as admin
2. Navigate to "Manage Users"
3. Select a test user to delete
4. Click "Delete" button
5. Confirm deletion

**Expected Result:**
- User deleted successfully
- All user's requests also deleted or handled appropriately
- Success message displayed
- User no longer appears in user list

---

## Part 7: Matching Algorithm

### Test 7.1: Basic Matching
**Objective:** Verify matching algorithm pairs compatible requests

**Pre-requisites:**
- Student A: Create TUTOR request for Mathematics with timeslots [MON_P1, WED_P3]
- Student B: Create TUTEE request for Mathematics with timeslots [MON_P1, WED_P3]

**Steps:**
1. Log in as admin
2. Click "Run Matching Algorithm"
3. Confirm action
4. Check results

**Expected Result:**
- Success message: "Matching completed. 1 requests matched."
- Student A's request status: MATCHED, matched partner: Student B
- Student B's request status: MATCHED, matched partner: Student A
- Both can see partner's name on dashboard

### Test 7.2: No Match Scenario
**Objective:** Verify system handles no compatible matches

**Pre-requisites:**
- Student A: TUTOR request for Mathematics with timeslots [MON_P1]
- Student B: TUTEE request for Mathematics with timeslots [FRI_P7] (no overlap)

**Steps:**
1. Log in as admin
2. Run matching algorithm

**Expected Result:**
- Success message: "Matching completed. 0 requests matched."
- Both requests remain PENDING
- No matched partners assigned

### Test 7.3: Multiple Matches
**Objective:** Verify algorithm handles multiple simultaneous matches

**Pre-requisites:**
- Create 2 TUTOR requests and 2 TUTEE requests
- Ensure overlapping subjects and timeslots
- Pair 1: Math tutors/tutees
- Pair 2: Physics tutors/tutees

**Steps:**
1. Log in as admin
2. Run matching algorithm

**Expected Result:**
- Multiple pairs matched
- Success message shows correct count
- Each request matched with appropriate partner
- No conflicts or duplicate matches

### Test 7.4: Already Matched Requests
**Objective:** Verify algorithm skips already matched requests

**Pre-requisites:**
- Have some MATCHED requests in system
- Create new PENDING requests

**Steps:**
1. Run matching algorithm
2. Verify results

**Expected Result:**
- Only PENDING requests processed
- MATCHED requests remain unchanged
- New matches created only for PENDING requests

---

## Part 8: Archive Functionality

### Test 8.1: Archive DONE Requests
**Objective:** Verify archiving removes completed requests from active view

**Pre-requisites:**
- Set some requests to DONE status (may need database manipulation or wait for completion)
- Have some PENDING/MATCHED requests

**Steps:**
1. Log in as admin
2. Click "Archive Old Requests"
3. Confirm action

**Expected Result:**
- Success message: "Archiving completed. X requests archived."
- DONE requests no longer appear in main request views
- PENDING and MATCHED requests still visible
- Archived requests have `archived=true` in database

### Test 8.2: Archive CANCELLED Requests
**Objective:** Verify cancelled requests can be archived

**Pre-requisites:**
- Have several CANCELLED requests

**Steps:**
1. Log in as admin
2. Click "Archive Old Requests"

**Expected Result:**
- CANCELLED requests archived
- No longer visible in main views
- Count reflects archived cancelled requests

### Test 8.3: Verify Non-Archived Requests Remain
**Objective:** Verify active requests are not archived

**Steps:**
1. Have PENDING and MATCHED requests
2. Run archive function
3. Check dashboard

**Expected Result:**
- PENDING requests still visible
- MATCHED requests still visible
- Only DONE and CANCELLED requests archived

---

## Part 9: Security and Authorization

### Test 9.1: Unauthorized Access - Student to Admin
**Objective:** Verify students cannot access admin pages

**Steps:**
1. Log in as student
2. Try to navigate to `/admin/dashboard`
3. Try to access `/admin/users`
4. Try to access `/admin/requests`

**Expected Result:**
- Access denied or redirected to appropriate page
- Error message or 403 Forbidden status
- Student cannot perform admin actions

### Test 9.2: Unauthorized Request Cancellation
**Objective:** Verify users can only cancel their own requests

**Steps:**
1. Note request ID from another user
2. Log in as different student
3. Try to cancel other student's request (via direct API call if needed)

**Expected Result:**
- Error message: "You can only cancel your own requests"
- Request status unchanged
- Authorization check prevents action

### Test 9.3: Unauthenticated Access
**Objective:** Verify unauthenticated users are redirected to login

**Steps:**
1. Log out or open incognito browser
2. Try to access `/dashboard`
3. Try to access `/profile`
4. Try to access `/requests/new`
5. Try to access `/admin/dashboard`

**Expected Result:**
- All protected routes redirect to login page
- No unauthorized access granted
- After login, redirected to originally requested page

---

## Part 10: Edge Cases and Error Handling

### Test 10.1: Invalid Form Submissions
**Objective:** Verify validation prevents invalid data

**Steps:**
1. Try to create request without selecting subject
2. Try to create request without selecting timeslots
3. Try to register with mismatched passwords
4. Try to register with invalid email format

**Expected Result:**
- Appropriate error messages displayed
- Form not submitted
- Validation messages indicate specific issues

### Test 10.2: Concurrent Request Creation
**Objective:** Verify system handles simultaneous actions

**Steps:**
1. Open two browser sessions as same student
2. Try to create same request in both sessions simultaneously

**Expected Result:**
- Only one request created
- Second attempt gets duplicate error
- Data consistency maintained

### Test 10.3: Profile Incomplete Access
**Objective:** Verify incomplete profiles are redirected

**Steps:**
1. Register new student
2. Try to create request before completing profile
3. Try to access dashboard

**Expected Result:**
- Redirected to profile completion page
- Cannot create requests until profile complete
- Clear message indicating profile completion needed

---

## Part 11: Performance and Data Validation

### Test 11.1: Large Dataset Performance
**Objective:** Verify system handles larger datasets

**Steps:**
1. Create 20+ student accounts
2. Each student creates 3-5 requests
3. Run matching algorithm
4. View admin dashboard

**Expected Result:**
- System remains responsive
- Matching algorithm completes successfully
- Dashboard loads all data
- No timeout or performance issues

### Test 11.2: Data Integrity After Operations
**Objective:** Verify data remains consistent

**Steps:**
1. Create request
2. Match request
3. Cancel matched request
4. Archive request
5. Check database directly

**Expected Result:**
- All operations logged correctly
- Timestamps accurate
- Foreign keys maintained
- No orphaned records

---

## Part 12: UI/UX Verification

### Test 12.1: Responsive Design
**Objective:** Verify UI works on different screen sizes

**Steps:**
1. View dashboard on desktop (1920x1080)
2. Resize to tablet (768x1024)
3. Resize to mobile (375x667)

**Expected Result:**
- Layout adjusts appropriately
- All functionality accessible
- No broken layouts or hidden elements
- Mobile-friendly navigation

### Test 12.2: Visual Feedback
**Objective:** Verify user actions provide clear feedback

**Steps:**
1. Create request (check for loading state, success message)
2. Cancel request (check for confirmation dialog)
3. Select timeslots (check for visual selection)
4. Select subjects (check for active state)

**Expected Result:**
- Loading indicators during async operations
- Success/error messages clear and visible
- Interactive elements show hover/active states
- Confirmation dialogs for destructive actions

---

## Test Summary Checklist

Use this checklist to track testing progress:

- [ ] User Registration (Student & Admin)
- [ ] User Authentication (Login/Logout)
- [ ] Profile Creation & Editing
- [ ] Subject Selection (All categories)
- [ ] Availability Selection
- [ ] Create TUTOR Request
- [ ] Create TUTEE Request
- [ ] Cancel PENDING Request
- [ ] Cancel MATCHED Request (both parties)
- [ ] View Dashboard
- [ ] Admin Dashboard Access
- [ ] View All Requests (Admin)
- [ ] View All Users (Admin)
- [ ] Delete User (Admin)
- [ ] Run Matching Algorithm
- [ ] Archive Old Requests
- [ ] Security - Unauthorized Access Prevention
- [ ] Security - Cross-User Request Access Prevention
- [ ] Validation - Duplicate Request Prevention
- [ ] Validation - Timeslot Restrictions
- [ ] Error Handling
- [ ] Performance Testing
- [ ] UI/UX Verification

---

## Known Issues to Test For

During testing, watch for these potential issues:

1. **Matching Edge Cases:**
   - Multiple students requesting same timeslot
   - Partial timeslot overlaps
   - Subject mismatch

2. **Cancellation Edge Cases:**
   - Cancelling one side of matched pair
   - Rapid cancel/recreate cycles
   - Cancel during matching process

3. **Archive Edge Cases:**
   - Archiving matched pairs
   - Re-archiving already archived requests

4. **UI Edge Cases:**
   - Very long user names
   - Many simultaneous timeslots
   - Large number of subjects

---

## Test Environment Setup

### Quick Setup for Testing
```bash
# 1. Clone and build
git clone <repository-url>
cd studentportal
./mvnw clean install

# 2. Run with H2 (default config)
./mvnw spring-boot:run

# 3. Access application
# Main app: http://localhost:8080
# H2 Console: http://localhost:8080/h2-console
```

### Create Test Users Quickly
Register these users in order:
1. Student 1: `111student1@test.com` / `Test1234!`
2. Student 2: `222student2@test.com` / `Test1234!`
3. Student 3: `333student3@test.com` / `Test1234!`
4. Admin: `admin@test.com` / `Admin1234!`

---

## Post-Testing Activities

After completing all tests:

1. **Document Issues:** Record any bugs or unexpected behavior
2. **Verify Fixes:** Retest any issues that were fixed
3. **Performance Review:** Note any performance concerns
4. **User Experience Notes:** Document any UX improvements needed
5. **Security Audit:** Confirm all security tests passed
6. **Test Data Cleanup:** Clear test data if needed

---

## Automated Testing

While this document focuses on manual testing, consider automating:
- User registration flows
- Request creation and cancellation
- Matching algorithm verification
- Security/authorization tests

Current automated tests: Run with `./mvnw test`

---

## Support and Troubleshooting

If tests fail:
1. Check application logs for errors
2. Verify database connection
3. Ensure all dependencies installed
4. Check for concurrent user conflicts
5. Review recent code changes

For questions or issues, refer to the main README.md or contact the development team.
