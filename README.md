# Android Mobile Application

![Android](https://img.shields.io/badge/Platform-Android-green)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue)
![Firebase](https://img.shields.io/badge/Backend-Firebase-orange)

## AI transparency 

This project was developed with the assistance of AI tools, including code generation and documentation. The AI contributions were guided and reviewed by human developers to ensure quality and accuracy. All AI-generated content has been verified and edited as necessary to meet project standards.

## Overview

Android mobile application that enables users to create, share, and take multiple-choice quizzes. The app supports both online (cloud-synced) and offline (local-first) modes.

### Key Features

- **Quiz Creation** - Manual entry or bulk import from Excel/CSV
- **Question Shuffling** - Randomizes questions and choices per attempt
- **Share Codes** - 6-digit codes for private quiz sharing
- **Public Library** - Browse and search community quizzes
- **Guest Mode** - Take quizzes without account registration
- **Question Pool** - Contribute questions for auto-generated quizzes
- **Cloud Sync** - Automatic backup with integrity verification
- **URL-based Avatars** - Random anime images from Wallhaven or custom URL

## Architecture

This project follows **Clean Architecture** with **MVVM** pattern:

```
app/src/main/java/com/example/androidapp/
├── di/                  # Dependency Injection (Manual DI)
├── domain/              # Business Logic Layer
│   ├── model/           # Domain Models
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
| Media / Avatars | External URLs (Wallhaven API / user-provided URL) |
| DI | Manual DI |
| Async | Kotlin Coroutines + Flow |

## Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 36
- Firebase project configured

### Option 1: Traditional Setup

Follow the standard Android development setup with Android Studio and manual SDK installation.

### Option 2: Nix Setup (Recommended for Nix Users)

If you're using Nix or NixOS, you can use the provided `flake.nix` for a reproducible development environment.

#### Using with `nix develop`

```bash
# Enter development shell
nix develop

# Or with direnv (recommended)
direnv allow
```

#### Using with Home Manager

Add to your Home Manager configuration:

```nix
{
  imports = [
    # Path to the AndroidApp flake
    inputs.androidapp.homeManagerModules.default
  ];

  programs.androidapp.enable = true;
}
```

#### Using with devenv

This project includes `devenv.nix` configuration:

```bash
# Install devenv if not already installed
nix profile install nixpkgs#devenv

# Enter devenv shell
devenv shell

# Or use direnv (a .envrc file is already present)
direnv allow
```

The Nix environment includes:
- JDK 17
- Android SDK (API levels 34, 35, 36)
- Gradle build tool
- Firebase CLI
- Node.js 20 (for Firebase tools)

Available devenv scripts:
- `build-debug` - Build debug APK
- `build-release` - Build release APK
- `test` - Run unit tests
- `lint` - Run lint checks
- `clean` - Clean build artifacts
- `firebase-emulators` - Start Firebase emulators

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

### 3. Build and Run

```bash
./gradlew assembleDebug
```

Or open in Android Studio and run.

## Documentation

| Document                                                   | Description                       |
|------------------------------------------------------------|-----------------------------------|
| [Project Overview](Docs_en/01_project_overview.md)         | High-level project description    |
| [Backend Design](Docs_en/02_backend_database_design.md)    | Firebase & database structure     |
| [Frontend Design](Docs_en/03_frontend_design.md)           | UI architecture & components      |
| [Application Behavior](Docs_en/04_application_behavior.md) | Business logic & flows            |
| [Code Rules](CODE_RULES.md)                                | Coding standards & conventions    |
| [Centralize theming](Docs_en/ui-theme.md)                          | Design pattern for user interface |

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
### Run options

```bash
Debug → emulator (default)
./gradlew assembleDebug

Debug → real Firebase
./gradlew assembleDebug -PuseFirebaseEmulator=false

Release → real Firebase (default)
./gradlew assembleRelease

Release → emulator (rare)
./gradlew assembleRelease -PuseFirebaseEmulator=true
```

### Samples data for testing

```python
# Create a virtual environment
python3 -m venv scripts/.venv 

# Activate the venv
source scripts/.venv/bin/activate

# Install dependencies
pip install -r scripts/requirements.txt

# Run the script against the local Firestore emulator
python scripts/generate-sample-data.py --clean --count 100 --seed 42

# Deactivate when done
deactivate

# For more options, run
scripts/generate-sample-data.py --help
```

## Version

App version 11.4.26

## License

GNU, see license at [LICENSE](LICENSE.md).

---
