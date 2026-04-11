# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Quizzez** is an Android quiz application built with Kotlin, Jetpack Compose, and Firebase. It allows users to create, share, and take multiple-choice quizzes with both online (cloud-synced) and offline (local-first) modes.

Package: `com.example.androidapp`

## Build System

### Gradle Commands

```bash
# Build debug APK (default: uses Firebase emulator at 10.0.2.2)
./gradlew assembleDebug

# Build debug with real Firebase
./gradlew assembleDebug -PuseFirebaseEmulator=false

# Build release APK
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Run lint checks
./gradlew lint

# Generate Dokka API documentation
./gradlew :app:dokkaHtml

# Clean build artifacts
./gradlew clean
```

### Nix/devenv Shortcuts (if using Nix)

```bash
# Enter development shell
nix develop
# or: direnv allow

# Available scripts
build-debug        # Build debug APK
build-release      # Build release APK
test               # Run unit tests
lint               # Run lint checks
clean              # Clean build artifacts
firebase-emulators # Start Firebase emulators
```

## Architecture

### Clean Architecture + MVVM (3 Strict Layers)

```
domain/   ← Pure Kotlin. No Android/Firebase imports.
  model/      ← Domain models (Quiz, Question, Choice, Attempt, User,
                QuestionPoolItem, UserRole, SystemStats, etc.)
  repository/ ← Repository interfaces (QuizRepository, AttemptRepository, etc.)
  util/       ← Pure utilities: ScoreUtil, ChecksumUtil, CsvParser,
                QuizValidator, ScoreCalculator, QuestionShuffler,
                TimeFormatter, ShareCodeUtil, SafeCall wrapper
  console/    ← In-app console engine (~35 commands)
  service/    ← Domain service interfaces: LogService, NetworkService, etc.

data/     ← Firebase DTOs, Room entities, repository implementations
  local/        ← Room database (v5), DAOs, entities, mappers
  remote/       ← Firestore data sources, DTOs, mappers
  repository/   ← Repository implementations
  logging/      ← LogCollector implements LogService
  network/      ← NetworkMonitor with StateFlow<Boolean>
  preferences/  ← SettingsPreferences for local settings
  sync/         ← SyncManager (SyncState: IDLE/SYNCING/PENDING/ERROR)
  worker/       ← WorkManager: BackendMaintenanceWorker, BackgroundSyncWorker

ui/       ← Compose screens + ViewModels
  theme/        ← Material Design theme (QuizzezTheme)
  components/   ← Reusable UI components (organized by category)
  navigation/   ← Routes.kt, QuizzezNavHost
  screens/      ← Screen composables + ViewModels (one package per screen)
```

### Dependency Injection (Manual - No Hilt/Dagger)

1. `AppContainer` interface (`di/AppModule.kt`) — declares all singletons
2. `AppContainerImpl` (`di/FirebaseModule.kt`) — lazy `by lazy` implementations
3. `QuizzezApplication.appContainer` — single instance at startup
4. Access in Composables via `LocalAppContainer` (`di/AppContainerExt.kt`)
5. Pass to ViewModels via anonymous `ViewModelProvider.Factory`

To add a dependency: Add to `AppContainer` → implement in `AppContainerImpl` → inject where needed.

### Repository Pattern (Local-First with Cloud Sync)

Room-backed flows write to Room first (`syncStatus = PENDING` or `PendingSyncEntity` queue), then sync to Firestore in background. Reads emit Room data immediately and refresh from Firestore when online.

Remote-only repositories: `ShareCodeRepositoryImpl`, `PoolRepositoryImpl`, `AdminRepositoryImpl`.

## Key Conventions

### ViewModel Pattern

```kotlin
// Private mutable, public immutable StateFlow
private val _uiState = MutableStateFlow(HomeUiState())
val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

// Events funnel through single onEvent() method
fun onEvent(event: HomeEvent) { ... }
```

Use `sealed class` for states with distinct phases (e.g., `TakeQuizUiState`). Use `data class` for simple flag-bearing states (e.g., `HomeUiState`).

### Compose Rules

- Every composable **must** accept `modifier: Modifier = Modifier` as parameter
- Components are **stateless** — hoist all state to ViewModel
- Add both light + dark `@Preview` for every component
- **All UI text must be in Vietnamese** — defined in `res/values/strings.xml`
- Never hardcode colors — use `MaterialTheme.colorScheme.*` only
- Use `collectAsStateWithLifecycle()` (not `collectAsState()`)

### Naming Conventions

| Type | Pattern | Example |
|------|---------|---------|
| Composable Screen | `{Name}Screen.kt` | `HomeScreen.kt` |
| ViewModel | `{Name}ViewModel.kt` | `HomeViewModel.kt` |
| Repository Interface | `{Name}Repository.kt` | `QuizRepository.kt` |
| Repository Impl | `{Name}RepositoryImpl.kt` | `QuizRepositoryImpl.kt` |
| Room Entity | `{Name}Entity.kt` | `QuizEntity.kt` |
| Room DAO | `{Name}Dao.kt` | `QuizDao.kt` |
| Firebase DTO | `{Name}Dto.kt` | `QuizDto.kt` |
| MutableStateFlow | `_variableName` (private) | `private val _uiState` |
| StateFlow (exposed) | `variableName` | `val uiState` |
| Event handler | `on{Event}` | `onClick`, `onValueChange` |

**Exception**: Trash screen ViewModel is `RecycleBinViewModel` (not `TrashViewModel`).

## Firebase Configuration

### Emulator Support

Debug builds default to Firebase emulator (`10.0.2.2`). Override via:

```bash
./gradlew assembleDebug -PuseFirebaseEmulator=false
./gradlew assembleDebug -PfirebaseEmulatorHost=192.168.1.100
```

### Sample Data Generation

Populate local Firestore emulator:

```bash
cd scripts
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python generate-sample-data.py --clean --count 100 --seed 42
```

## MCP Server (AI Agent Integration)

An MCP server in `mcp_server/` connects AI agents to the in-app console via ADB:

```bash
cd mcp_server
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python server.py
```

Tools: `execute_command`, `list_commands`, `get_command_help`, `suggest_command`, `validate_command`, `build_command`, `get_command_examples`.

## Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose (BoM 2025.01.01) |
| Local DB | Room 2.8.3 |
| Backend | Firebase (Auth, Firestore, Functions) |
| DI | Manual DI (no Hilt/Dagger) |
| Async | Kotlin Coroutines + Flow |
| Images | Coil |
| Fonts | Google Fonts (Playfair Display + Inter) |
| Docs | Dokka 2.0.0 |

## Important Code Locations

| File | Purpose |
|------|---------|
| `di/AppModule.kt` | DI container interface |
| `di/FirebaseModule.kt` | DI container implementation |
| `ui/navigation/Routes.kt` | All route strings |
| `ui/navigation/QuizzezNavHost.kt` | Full navigation graph |
| `data/local/AppDatabase.kt` | Room DB v5 definition |
| `data/local/EntityMappers.kt` | Entity ↔ Domain mappers |
| `domain/console/Command.kt` | Console command interface |
| `mcp_server/server.py` | MCP server for AI agents |
| `design-tokens.json` | Source of truth for design values |

## Documentation References

- `CODE_RULES.md` — Full coding standards with examples
- `AGENTS.md` — Comprehensive architecture overview
- `Docs_en/` — Architecture, backend, frontend, behavior docs
- `DOKKA_SETUP.md` — API documentation generation
- `mcp_server/README.md` — MCP server configuration

## Strict Rules (Non-Negotiable)

- **No emojis** in code, comments, scripts, or configs
- **Vietnamese UI text only** via `strings.xml`, never hardcoded
- **No Hilt/Dagger** — use manual DI only
- **No use-case classes** — business logic lives in ViewModels
- **KSP** (not KAPT) for annotation processing
- **Gson** for JSON — do not add a second JSON library
- **Profile avatars via URL only** (Wallhaven API or manual paste) — no Firebase Storage uploads
