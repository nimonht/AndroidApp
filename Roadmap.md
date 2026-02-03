### 1. Project Roadmap

### 2.1 Recommended Approach: **Parallel Development with Milestones**

Rather than completing all Frontend tasks before Backend (or vice versa), I recommend a **milestone-based parallel approach** where both Frontend and Backend progress together, enabling early integration testing.

```
┌─────────────────────────────────────────────────────────────────────┐
│                    DEVELOPMENT TIMELINE                             │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  Sprint 1    │ Sprint 2    │ Sprint 3    │ Sprint 4-5  │ Sprint 6-7 │
│  Foundation  │ Core Data   │ Integration │ Features    │ Polish     │
│              │             │             │             │            │
│  ┌─────────┐ │ ┌─────────┐ │ ┌─────────┐ │ ┌─────────┐ │ ┌───────┐  │
│  │Frontend │ │ │Frontend │ │ │   Both  │ │ │Frontend │ │ │Testing│  │
│  │ M1, M2  │ │ │ M3, M4  │ │ │  Sync   │ │ │M8-M11   │ │ │M18    │  │
│  └─────────┘ │ └─────────┘ │ │  & Test │ │ └─────────┘ │ └───────┘  │
│              │             │ └─────────┘ │             │            │
│  ┌─────────┐ │ ┌─────────┐ │             │ ┌─────────┐ │ ┌───────┐  │
│  │Backend  │ │ │Backend  │ │             │ │Backend  │ │ │Deploy │  │
│  │ M1, M2  │ │ │M4-M6    │ │             │ │M11-M14  │ │ │Final  │  │
│  └─────────┘ │ └─────────┘ │             │ └─────────┘ │ └───────┘  │
│              │             │             │             │            │
└─────────────────────────────────────────────────────────────────────┘
```

---

### 2.2 Sprint-by-Sprint Breakdown

#### **Sprint 1: Foundation (Weeks 1-2)**

**Goal:** Set up project infrastructure for both Frontend and Backend

**Frontend Tasks:**
```
Module 1: Project Setup & Architecture
├── Setup Jetpack Compose (3h)
├── Setup Kotlin DSL (2h)
├── Setup Firebase SDK (3h)
├── Setup Room Database (3h)
├── Setup Navigation Component (3h)
├── Create Base Project Structure (2h)
├── Setup Theme & Design System (5h)
├── Configure Build Variants (3h)
└── Setup Code Quality Tools (3h)

Total: 32 hours
```

**Backend Tasks:**
```
Module 1: Firebase Project Setup
├── Create Firebase Project (2h)
├── Configure Firebase Authentication (2h)
├── Setup Cloud Firestore (2h)
├── Setup Firebase Storage (2h)
├── Generate google-services.json (1h)
├── Configure SHA Keys (2h)
├── Setup Firebase Emulators (3h)
└── Create Test Environment (3h)

Module 2: Firestore Database Schema
├── Create Users Collection Schema (2h)
├── Create Quizzes Collection Schema (3h)
├── Create Questions Subcollection Schema (3h)
├── Create Choices Subcollection Schema (2h)
├── Create Attempts Collection Schema (3h)
├── Create QuestionPool Collection Schema (3h)
├── Create ShareCodes Collection Schema (2h)
└── Document Schema Relationships (2h)

Total: 37 hours
```

**Sprint 1 Deliverables:**
- [ ] Project compiles and runs
- [ ] Firebase connected (Auth sign-in works)
- [ ] Navigation scaffolding with placeholder screens
- [ ] Firestore emulator running with test data

---

#### **Sprint 2: Core UI Components & Data Layer (Weeks 3-4)**

**Goal:** Build reusable UI components and local data layer

**Frontend Tasks:**
```
Module 2: Core UI Components Library
├── Create QuizCard Component (5h)
├── Create TagChip Component (2h)
├── Create ChoiceButton Component (5h)
├── Create DynamicChoiceList Component (5h)
├── Create TextInputField Component (3h)
├── Create CodeInputField Component (5h)
├── Create LoadingSpinner Component (1h)
├── Create SkeletonLoader Component (3h)
├── Create ErrorState Component (2h)
├── Create EmptyState Component (2h)
├── Create TopAppBar Component (3h)
├── Create BottomNavBar Component (3h)
├── Create FloatingActionButton Component (2h)
├── Create ProgressIndicator Component (2h)
├── Create TimerDisplay Component (3h)
├── Create ScoreCard Component (3h)
├── Create MediaDisplay Component (5h)
├── Create DropdownSelector Component (3h)
├── Create SwitchToggle Component (1h)
├── Create AlertDialog Component (2h)
└── Create BottomSheet Component (3h)

Module 3: Navigation & Screen Structure
├── Setup Navigation Graph (3h)
├── Create Home Screen Structure (3h)
├── Create Search Screen Structure (3h)
├── Create Profile Screen Structure (3h)
├── Create Quiz Detail Screen Structure (3h)
├── Create Take Quiz Screen Structure (5h)
├── Create Quiz Result Screen Structure (3h)
├── Create Create Quiz Screen Structure (8h)
├── Create Edit Quiz Screen Structure (3h)
├── Create Settings Screen Structure (3h)
├── Create History Screen Structure (3h)
├── Create Recycle Bin Screen Structure (3h)
├── Create Login Screen (5h)
├── Create Signup Screen (5h)
└── Implement Deep Linking (5h)

Total: 114 hours
```

**Backend Tasks:**
```
Module 4: Firebase Data Models
├── Create User Data Model (2h)
├── Create Quiz Data Model (3h)
├── Create Question Data Model (3h)
├── Create Choice Data Model (2h)
├── Create Attempt Data Model (3h)
├── Create QuestionPoolItem Data Model (2h)
├── Create ShareCode Data Model (1h)
├── Create Firestore Converters (3h)
└── Create Domain Mappers (3h)

Module 5: Room Database Setup
├── Create QuizEntity (3h)
├── Create QuestionEntity (3h)
├── Create ChoiceEntity (2h)
├── Create AttemptEntity (3h)
├── Create UserEntity (2h)
├── Create PendingSyncEntity (3h)
├── Create QuizDao (5h)
├── Create QuestionDao (3h)
├── Create ChoiceDao (2h)
├── Create AttemptDao (3h)
├── Create UserDao (2h)
├── Create PendingSyncDao (3h)
├── Create AppDatabase (3h)
├── Create Type Converters (3h)
└── Setup Database Migrations (5h)

Module 15: Manual Dependency Injection
├── Create AppContainer Interface (3h)
├── Implement AppContainerImpl with Firebase (3h)
├── Implement Database Dependencies (3h)
├── Create Network Dependencies (2h)
├── Setup Dispatchers & Context (2h)
└── Integrate with Application & Composables (2h)

Total: 87 hours
```

**Sprint 2 Deliverables:**
- [ ] All UI components working in isolation (storybook/preview)
- [ ] All screens navigable with mock data
- [ ] Room database operational
- [ ] DI modules configured

---

#### **Sprint 3: Repositories & ViewModels (Weeks 5-6)**

**Goal:** Connect UI to data sources, implement business logic

**Frontend Tasks:**
```
Module 4: ViewModels & State Management
├── Create HomeViewModel (5h)
├── Create SearchViewModel (5h)
├── Create ProfileViewModel (3h)
├── Create QuizDetailViewModel (3h)
├── Create TakeQuizViewModel (13h)
├── Create QuizResultViewModel (3h)
├── Create CreateQuizViewModel (8h)
├── Create EditQuizViewModel (5h)
├── Create SettingsViewModel (3h)
├── Create HistoryViewModel (3h)
├── Create RecycleBinViewModel (3h)
├── Create AuthViewModel (5h)
└── Create SharedQuizViewModel (3h)

Module 5: Data Layer - Local Database (if not done)
(Complete remaining from Sprint 2)

Total: 62 hours
```

**Backend Tasks:**
```
Module 3: Firestore Security Rules
├── Write Users Security Rules (3h)
├── Write Quizzes Security Rules (5h)
├── Write Questions Security Rules (3h)
├── Write Choices Security Rules (2h)
├── Write Attempts Security Rules (5h)
├── Write QuestionPool Security Rules (3h)
├── Write ShareCodes Security Rules (2h)
├── Test Security Rules (5h)
└── Deploy Security Rules (2h)

Module 6: Authentication Repository
├── Implement AuthRepository Interface (2h)
├── Implement Email/Password Sign Up (5h)
├── Implement Email/Password Sign In (3h)
├── Implement Google Sign In (5h)
├── Implement Sign Out (2h)
├── Implement Auth State Observer (3h)
├── Implement Session Validation (3h)
├── Implement Password Reset (2h)
├── Implement Email Verification (3h)
├── Implement Delete Account (5h)
└── Create Guest Session Manager (3h)

Module 7: Quiz Repository
├── Implement QuizRepository Interface (2h)
├── Implement Create Quiz (8h)
├── Implement Update Quiz (8h)
├── Implement Soft Delete Quiz (3h)
├── Implement Restore Quiz (2h)
├── Implement Permanent Delete (5h)
├── Implement Get Quiz By ID (5h)
├── Implement Get Quiz By Share Code (3h)
├── Implement Get My Quizzes (5h)
├── Implement Get Public Quizzes (5h)
├── Implement Get Trending Quizzes (3h)
├── Implement Get Deleted Quizzes (3h)
├── Implement Search Quizzes (8h)
└── Implement Quiz Count Increment (2h)

Module 8: Question Repository
├── Implement QuestionRepository Interface (2h)
├── Implement Add Question (5h)
├── Implement Update Question (5h)
├── Implement Delete Question (3h)
├── Implement Reorder Questions (3h)
├── Implement Get Questions By Quiz (5h)
└── Implement Validate Question (3h)

Module 9: Attempt Repository
├── Implement AttemptRepository Interface (2h)
├── Implement Create Attempt (5h)
├── Implement Update Attempt Progress (3h)
├── Implement Submit Attempt (5h)
├── Implement Get Attempt By ID (3h)
├── Implement Get My Attempts (5h)
├── Implement Get Quiz Attempts (3h)
├── Implement Calculate Score (5h)
└── Implement Link Guest Attempts (5h)

Total: 172 hours
```

**Sprint 3 Deliverables:**
- [ ] Authentication flow working end-to-end
- [ ] Quiz CRUD operations functional
- [ ] Basic quiz taking working
- [ ] Security rules deployed and tested

---

#### **Sprint 4: Core Features Integration (Weeks 7-8)**

**Goal:** Complete main user flows with Firebase integration

**Frontend Tasks:**
```
Module 6: Data Layer - Firebase Integration
├── Create Firebase Module (3h)
├── Create AuthRepository (5h)
├── Create QuizRepository (8h)
├── Create QuestionRepository (5h)
├── Create AttemptRepository (5h)
├── Create ShareCodeRepository (3h)
├── Create PoolRepository (5h)
├── Create StorageRepository (5h)
├── Create NetworkMonitor (3h)
└── Create SyncManager (8h)

Module 7: Business Logic & Use Cases
├── Create Checksum Utility (3h)
├── Create Share Code Generator (2h)
├── Create Question Shuffler (3h)
├── Create Score Calculator (3h)
├── Create CSV Parser (8h)
├── Create CSV Validator (5h)
├── Create Quiz Validator (3h)
├── Create Time Formatter (2h)
├── Create Search Filter Logic (3h)
└── Create Auto-Generate Quiz Logic (5h)

Module 8: Home Screen Features
├── Implement Join Quiz by Code (5h)
├── Implement Recently Played Section (5h)
├── Implement My Quizzes Section (5h)
├── Implement Trending Quizzes Section (5h)
├── Implement Pull-to-Refresh (2h)
└── Implement Offline Banner (2h)

Total: 104 hours
```

**Backend Tasks:**
```
Module 10: Share Code Repository
├── Implement ShareCodeRepository Interface (2h)
├── Implement Generate Share Code (3h)
├── Implement Lookup Share Code (2h)
├── Implement Regenerate Share Code (3h)
├── Implement Validate Share Code (2h)
└── Implement Delete Share Code (2h)

Module 13: Sync Manager
├── Implement NetworkMonitor (3h)
├── Implement SyncManager (8h)
├── Implement Checksum Computation (3h)
├── Implement Checksum Verification (3h)
├── Implement Pending Operations Queue (5h)
├── Implement Conflict Resolution (8h)
├── Implement Background Sync Worker (5h)
├── Implement Sync Status Tracking (3h)
├── Implement Data Saver Mode (3h)
└── Implement Retry Strategy (3h)

Module 16: Data Validation & Business Rules
├── Implement Quiz Validator (2h)
├── Implement Question Validator (3h)
├── Implement User Input Sanitizer (3h)
├── Implement Tag Validator (2h)
├── Implement Score Calculator (5h)
├── Implement CSV Parser (8h)
├── Implement CSV Validator (5h)
└── Implement Question Shuffler (3h)

Total: 89 hours
```

**Sprint 4 Deliverables:**
- [ ] Complete quiz creation flow (manual + CSV)
- [ ] Share code system working
- [ ] Home screen fully functional
- [ ] Sync mechanism operational

---

#### **Sprint 5: Quiz Taking & Creation Features (Weeks 9-10)**

**Goal:** Complete quiz-taking experience and creation features

**Frontend Tasks:**
```
Module 9: Quiz Taking Features
├── Implement Question Navigation (5h)
├── Implement Answer Selection (5h)
├── Implement Quiz Timer (5h)
├── Implement Quiz Submission (5h)
├── Implement Answer Review (5h)
├── Implement Quiz Exit Confirmation (2h)
├── Implement Progress Persistence (5h)
└── Implement Media Display in Questions (5h)

Module 10: Quiz Creation Features
├── Implement Quiz Metadata Form (5h)
├── Implement Add Question Flow (8h)
├── Implement Choice Editor (8h)
├── Implement Question Reordering (5h)
├── Implement CSV Import UI (8h)
├── Implement Quiz Preview (3h)
├── Implement Share to Pool Toggle (2h)
├── Implement Draft Save (5h)
└── Implement Publish Quiz (3h)

Module 11: Search & Discovery Features
├── Implement Search Bar (5h)
├── Implement Tag Filter (3h)
├── Implement Search Results Grid (5h)
├── Implement Search Results List (3h)
├── Implement Sort Options (3h)
└── Implement No Results State (2h)

Total: 105 hours
```

**Backend Tasks:**
```
Module 11: Question Pool Repository
├── Implement PoolRepository Interface (2h)
├── Implement Contribute Questions (5h)
├── Implement Get Pool Questions By Tags (5h)
├── Implement Get My Contributions (3h)
├── Implement Revoke Contribution (3h)
├── Implement Increment Usage Count (2h)
└── Implement Auto-Generate Quiz (8h)

Module 12: Storage Repository
├── Implement StorageRepository Interface (2h)
├── Implement Upload Image (5h)
├── Implement Upload Video (5h)
├── Implement Download Media (3h)
├── Implement Delete Media (2h)
├── Implement Generate Thumbnail (5h)
├── Configure Storage Security Rules (3h)
└── Implement Media URL Validation (2h)

Module 14: Cloud Functions
├── Setup Cloud Functions Project (3h)
├── Implement Share Code Generator Function (5h)
├── Implement Cleanup Deleted Quizzes (5h)
├── Implement Quiz Stats Aggregation (5h)
├── Implement Pool Question Cleanup (3h)
├── Implement User Cleanup (5h)
├── Implement Notification Triggers (5h) [optional]
├── Deploy Cloud Functions (2h)
└── Setup Cloud Functions CI/CD (3h)

Total: 92 hours
```

**Sprint 5 Deliverables:**
- [ ] Complete quiz-taking experience
- [ ] Quiz creation with all features
- [ ] Search working with filters
- [ ] Cloud Functions deployed

---

#### **Sprint 6: Profile, Settings & Advanced Features (Weeks 11-12)**

**Goal:** Complete user profile, settings, and advanced features

**Frontend Tasks:**
```
Module 12: Profile & Settings Features
├── Implement User Profile Display (3h)
├── Implement Edit Profile (5h)
├── Implement Attempt History List (5h)
├── Implement Attempt Detail (5h)
├── Implement Data Saver Toggle (2h)
├── Implement Theme Setting (3h)
├── Implement Logout (2h)
└── Implement Delete Account (5h)

Module 13: Recycle Bin Features
├── Implement Deleted Quizzes List (3h)
├── Implement Restore Quiz (3h)
├── Implement Permanent Delete (3h)
├── Implement Days Remaining Display (2h)
└── Implement Empty Trash (2h)

Module 14: Sharing Features
├── Implement Share Code Display (2h)
├── Implement Regenerate Share Code (3h)
├── Implement Share Sheet (3h)
└── Implement QR Code Generation (5h)

Module 15: Question Pool Features
├── Implement Auto-Generate Quiz UI (5h)
├── Implement Generated Quiz Preview (5h)
├── Implement My Contributions List (3h)
└── Implement Revoke Contribution (3h)

Module 16: Guest Mode Features
├── Implement Guest Session (3h)
├── Implement Guest Restrictions (3h)
├── Implement Login Prompt (2h)
└── Implement Guest to User Migration (5h)

Total: 85 hours
```

**Backend Tasks:**
```
Module 17: Error Handling & Logging
├── Create Result Wrapper (2h)
├── Create Custom Exceptions (3h)
├── Implement Error Mapper (3h)
├── Setup Firebase Crashlytics (3h)
├── Implement Analytics Events (5h)
├── Implement Debug Logging (2h)
└── Implement Release Logging (3h)

Total: 21 hours
```

**Sprint 6 Deliverables:**
- [ ] Profile fully functional
- [ ] Settings working
- [ ] Recycle bin operational
- [ ] Sharing features complete
- [ ] Guest mode working

---

#### **Sprint 7: Error Handling, Testing & Polish (Weeks 13-14)**

**Goal:** Handle edge cases, write tests, prepare for release

**Frontend Tasks:**
```
Module 17: Error Handling & Edge Cases
├── Implement Global Error Handler (3h)
├── Implement Network Error Handling (5h)
├── Implement Session Expiry Handling (3h)
├── Implement Sync Conflict Resolution (8h)
└── Implement Input Validation Messages (3h)

Module 18: Testing & Documentation
├── Write Unit Tests for ViewModels (8h)
├── Write Unit Tests for Repositories (8h)
├── Write Unit Tests for Utilities (5h)
├── Write UI Tests for Core Flows (13h)
├── Write Integration Tests (8h)
├── Create Component Documentation (5h)
├── Create Architecture Documentation (3h)
└── Create API Documentation (3h)

Total: 75 hours
```

**Backend Tasks:**
```
Module 18: Testing & Documentation
├── Write Unit Tests for Repositories (8h)
├── Write Unit Tests for Validators (5h)
├── Write Unit Tests for Sync Manager (8h)
├── Write Integration Tests (8h)
├── Write Security Rules Tests (5h)
├── Create API Documentation (5h)
├── Create Data Model Documentation (3h)
└── Create Sync Flow Documentation (3h)

Total: 45 hours
```

**Sprint 7 Deliverables:**
- [ ] All error cases handled gracefully
- [ ] Test coverage >70%
- [ ] Documentation complete
- [ ] App ready for release

---

## 3. Key Milestones & Checkpoints

| Milestone | Sprint | Criteria |
|-----------|--------|----------|
| **M1: Skeleton App** | 1 | App runs, Firebase connected, navigation works |
| **M2: Data Foundation** | 2 | All UI components done, Room + Models ready |
| **M3: Core CRUD** | 3 | Create/Read/Update/Delete quiz works end-to-end |
| **M4: Quiz Flow** | 4 | Take quiz → See results works completely |
| **M5: Feature Complete** | 5-6 | All features working, no placeholders |
| **M6: Production Ready** | 7 | Tests pass, errors handled, docs complete |

---
