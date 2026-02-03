# Android Mobile Application

![Android](https://img.shields.io/badge/Platform-Android-green)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue)
![Firebase](https://img.shields.io/badge/Backend-Firebase-orange)

## Overview

Android mobile application that enables users to create, share, and take multiple-choice quizzes. The app supports both online (cloud-synced) and offline (local-first) modes.

### Key Features

- **Quiz Creation** - Manual entry or bulk import from Excel/CSV
- **Question Shuffling** - Randomizes questions and choices per attempt
- **Share Codes** - 6-digit codes for private quiz sharing
- **Public Library** - Browse and search community quizzes
- **Guest Mode** - Take quizzes without account registration
- **Question Pool** - Contribute questions for auto-generated quizzes
- ☁**Cloud Sync** - Automatic backup with integrity verification

## Architecture

This project follows **Clean Architecture** with **MVVM** pattern:

```
app/src/main/java/com/example/androidapp/
├── di/                  # Dependency Injection (Hilt)
├── domain/              # Business Logic Layer
│   ├── model/           # Domain Models
│   ├── usecase/         # Use Cases
│   └── util/            # Utilities
├── data/                # Data Layer
│   ├── local/           # Room Database
│   ├── remote/          # Firebase Services
│   └── repository/      # Repository Implementations
└── ui/                  # Presentation Layer
    ├── theme/           # Material Design Theme
    ├── components/      # Reusable UI Components
    └── screens/         # Screen Composables + ViewModels
```

## Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Kotlin |
| UI Framework | Jetpack Compose |
| Local Database | SQLite (Room) |
| Backend | Firebase (Serverless) |
| Cloud Database | Cloud Firestore |
| Authentication | Firebase Auth |
| Storage | Firebase Storage |
| DI | Hilt |
| Async | Kotlin Coroutines + Flow |

## Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 34
- Firebase project configured

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/nimonht/AndroidApp
cd AndroidApp
```

### 2. Firebase Setup

1. Create a Firebase project at [Firebase Console](https://console.firebase.google.com)
2. Add an Android app with package name `com.example.androidapp`
3. Download `google-services.json` and place it in `app/` directory
4. Enable Authentication (Email/Password, Google Sign-In)
5. Create Firestore Database
6. Set up Firebase Storage

### 3. Build and Run

```bash
./gradlew assembleDebug
```

Or open in Android Studio and run.

## Documentation

| Document | Description |
|----------|-------------|
| [Project Overview](Docs_en/01_project_overview.md) | High-level project description |
| [Backend Design](Docs_en/02_backend_database_design.md) | Firebase & database structure |
| [Frontend Design](Docs_en/03_frontend_design.md) | UI architecture & components |
| [Application Behavior](Docs_en/04_application_behavior.md) | Business logic & flows |
| [Code Rules](CODE_RULES.md) | Coding standards & conventions |

## Development

### Branch Naming

- Feature: `feature/{task-id}-{description}`
- Bugfix: `bugfix/{task-id}-{description}`
- Hotfix: `hotfix/{description}`

### Commit Convention

```
<type>(<scope>): <subject>

Types: feat, fix, docs, style, refactor, test, chore
```

### Code Quality

```bash
# Run lint
./gradlew lint

# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest
```
## Roadmap 

See development plan at [Roadmap.md](Roadmap.md).

## License

GNU, see license at [LICENSE](LICENSE.md).

---
