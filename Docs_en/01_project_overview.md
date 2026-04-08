# Mobile Quiz Application

## 1. Project Overview

**Quizzez** is an Android mobile application that enables users to create, share, and take multiple-choice quizzes. The app supports both online (cloud-synced) and offline (local-first) modes.

### Key Features

| Feature | Description |
|---------|-------------|
| Quiz Creation | Manual entry or bulk import from Excel/CSV |
| Question Shuffling | Randomizes questions and choices per attempt while preserving correct answers |
| Share Codes | 6-digit codes for private quiz sharing |
| Public Library | Browse and search community quizzes |
| Guest Mode | Take quizzes without account registration |
| Question Pool | Contribute questions anonymously for auto-generated quizzes |
| Cloud Sync | Automatic backup with integrity verification |

---

## 2. Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Kotlin |
| Local Database | SQLite (Room) - offline cache |
| UI Framework | Jetpack Compose |
| Backend | Firebase (serverless) |
| Cloud Database | Cloud Firestore |
| Authentication | Firebase Auth |
| Storage | Firebase Storage (quiz media) — **not currently implemented**; `storage.rules` exists for future use but no SDK dependency or app code references Firebase Storage. Profile avatars are URL-based (external URLs / Wallhaven API). |

---

## 3. System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     Quizzez Mobile App                          │
│                         (Kotlin)                                │
├─────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐           │
│  │   UI Layer   │  │  ViewModel   │  │  Repository  │           │
│  │   (Compose)  │◄─┤   (MVVM)     │◄─┤   Pattern    │           │
│  └──────────────┘  └──────────────┘  └──────┬───────┘           │
│                                             │                   │
│  ┌──────────────────────────────────────────┴───────────────┐   │
│  │                    Data Sources                          │   │
│  │  ┌─────────────────┐        ┌─────────────────┐          │   │
│  │  │  Room Database  │        │  Firebase SDK   │          │   │
│  │  │ (Offline Cache) │        │  (Cloud Sync)   │          │   │
│  │  └────────┬────────┘        └────────┬────────┘          │   │
│  └───────────┼──────────────────────────┼───────────────────┘   │
└──────────────┼──────────────────────────┼───────────────────────┘
               │                          │
       ┌───────▼───────┐          ┌───────▼───────┐
       │  Local SQLite │          │    Firebase   │
       │    Storage    │          │     Cloud     │
       └───────────────┘          └───────────────┘
                                          │
                          ┌───────────────┼───────────────┐
                          ▼               ▼               ▼
                    ┌──────────┐   ┌──────────┐   ┌──────────┐
                    │Firestore │   │   Auth   │   │ Storage  │
                    │(Database)│   │ (Login)  │   │ (Media)  │
                    └──────────┘   └──────────┘   └──────────┘
```

---

## 4. User Roles

### 4.1 Owner (Authenticated User)
- Create/edit/delete quizzes
- Import from Excel/CSV
- Generate 6-digit share codes
- View attempt statistics
- Contribute questions to public pool
- Auto-generate quizzes from pool

### 4.2 Guest (Unauthenticated)
- Join quiz via 6-digit code
- Browse public quizzes
- Take quizzes and view own scores
- No account required

---

## 5. Core Business Rules

### Quiz Sharing
| Mode | Access Method |
|------|---------------|
| Private | 6-digit share code only |
| Public | Searchable without code |

### Tags & Metadata
- Tags set at quiz level
- Questions inherit quiz tags by default
- Per-question tag override (optional)

### Media Support
| Type | Formats |
|------|---------|
| Image | JPG, PNG, WebP |
| Video | MP4, Stream URL |

> Media displays above question text when `media_url` is not null.

### Flexible Choice Architecture

The quiz system supports **dynamic number of answer choices** (2 to 10 per question):

| Question Type | Choices | Use Case |
|---------------|---------|----------|
| True/False | 2 | Simple binary questions |
| Standard MCQ | 4 | Traditional multiple choice |
| Extended MCQ | 5-10 | Complex questions with more options |

**Key Features:**
- **Minimum**: 2 choices required per question
- **Maximum**: 10 choices allowed per question
- **Multiple Correct**: Optional support for questions with multiple correct answers
- **Dynamic UI**: Choice buttons render based on question's choice count
- **Validation**: Server and client validate choice count constraints

```
┌─────────────────────────────────────────────────────────┐
│                FLEXIBLE CHOICE MODEL                    │
├─────────────────────────────────────────────────────────┤
│  Question                                               │
│  ├── choiceCount: 2-10 (flexible)                       │
│  ├── allowMultipleCorrect: boolean                      │
│  │                                                      │
│  └── choices[] (2 to 10 items)                          │
│      ├── Choice A (isCorrect: true/false)               │
│      ├── Choice B (isCorrect: true/false)               │
│      ├── ...                                            │
│      └── Choice N (max 10)                              │
└─────────────────────────────────────────────────────────┘
```

---

## 6. Sync Algorithm (Cloud Backup)

The app uses a local-first sync strategy: writes go to Room immediately (with `syncStatus = PENDING`), then sync to Firestore in the background. A SHA-256 checksum is computed over quiz content and verified after upload to detect corruption or partial writes.

> See [02_backend_database_design.md, Section 5](02_backend_database_design.md#5-sync-with-checksum-verification) for the full `ChecksumUtil` and `SyncManager` implementation code.
>
> See [04_application_behavior.md, Section 3.4](04_application_behavior.md#34-cloud-sync-with-checksum-verification) for the behavioral flow diagram and rationale.

---

## 7. Feature Dependencies

```mermaid
graph TD
    A[Cloud Sync] --> B[Quiz Sharing]
    A --> C[Quiz Recovery]
    B --> D[Share Codes]
    B --> E[Public Search]
    A --> F[Question Pool]
    F --> G[Auto-Generate Quiz]
    C --> H[30-Day Recycle Bin]
```

> **Note:** If Mermaid doesn't render, view this diagram:
> - Cloud Sync enables Quiz Sharing and Quiz Recovery
> - Quiz Sharing enables Share Codes and Public Search
> - Cloud Sync enables Question Pool
> - Question Pool enables Auto-Generate Quiz
> - Quiz Recovery enables 30-Day Recycle Bin

---

## 8. Project Structure

Quizzez is a **single-module** Android app (`:app`). Code is organized into packages under `com.example.androidapp`:

| Package | Responsibility |
|---------|----------------|
| `ui/` | Compose screens, ViewModels, components, theme, navigation |
| `data/` | Room entities & DAOs, Firebase DTOs & remote data sources, repository implementations, network monitor, sync manager, WorkManager workers |
| `domain/` | Pure Kotlin models, repository interfaces, utility classes (checksum, CSV parsing, scoring, shuffling, validation) |
| `di/` | Manual DI container (`AppContainer` interface + `AppContainerImpl`) |

There are no separate `firebase`, `sync`, or `utils` Gradle modules — all Firebase SDK access, sync logic, and utilities live within the packages above inside the single `:app` module.

---

## 9. Firebase Services Summary

> See [Section 2 (Tech Stack)](#2-tech-stack) above for the Firebase services used. For backend-specific details (Firestore schema, security rules, Cloud Functions), see [02_backend_database_design.md](02_backend_database_design.md).
