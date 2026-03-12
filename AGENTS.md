# AGENTS.md — QuizCode Android App

## Project Overview

Android quiz app (Kotlin + Jetpack Compose + Firebase) named **QuizCode**. Users create, share, and take multiple-choice quizzes. Package: `com.example.androidapp`.

## Architecture

**Clean Architecture + MVVM** with three strict layers — never skip or cross-layer:

```
domain/   ← Pure Kotlin. Models, use cases, utilities. No Android/Firebase imports.
data/     ← Firebase DTOs (remote/model/), remote data sources (remote/firebase/),
            Room entities (local/entity/), mapper extensions, repository impls.
ui/       ← Compose screens + ViewModels. Screens are stateless; ViewModels own all state.
```

Data layer internal structure:
```
data/
  local/
    AppDatabase.kt          ← Room DB definition (v2) + migrations
    EntityMappers.kt        ← Extension fns: Entity ↔ Domain (toDomain / toEntity)
    converter/Converters.kt ← Room TypeConverters using Gson
    dao/                    ← DAOs (QuizDao, QuestionDao, ChoiceDao, AttemptDao, UserDao, PendingSyncDao)
    entity/                 ← Room entities + SyncStatus / PendingSyncStatus enums
  remote/
    AppMappers.kt           ← Extension fns: DTO ↔ Domain (toDomain / toDto)
    firebase/               ← Remote data sources (QuizRemoteDataSource, AttemptRemoteDataSource, UserRemoteDataSource)
    model/                  ← Firestore DTOs (QuizDto, QuestionDto, ChoiceDto, AttemptDto, UserDto, ShareCodeDto)
  repository/               ← Repository implementations delegating to DAOs + remote data sources
```

Repositories do **not** touch Firestore directly — they delegate to `*RemoteDataSource` classes in `data/remote/firebase/`.

**Local-first with cloud sync**: Every write saves to Room first (`syncStatus = PENDING`), then syncs to Firestore in the background. Reads emit Room data immediately and refresh from Firestore when online. See `data/repository/` for the pattern.

`PendingSyncEntity` (`data/local/entity/PendingSyncEntity.kt`) provides a separate retry queue (`pending_sync_operations` table) with `retryCount`, `maxRetries`, and `PendingSyncStatus` (PENDING / IN_PROGRESS / FAILED / COMPLETED). `PendingSyncDao` exposes `observePendingCount(): Flow<Int>` for UI sync indicators.

## Dependency Injection

**Manual DI — no Hilt/Dagger.** The container chain is:
1. `AppContainer` interface (`di/AppModule.kt`) — declares all singletons
2. `AppContainerImpl` (`di/FirebaseModule.kt`) — lazy `by lazy` implementations
3. `QuizCodeApplication.appContainer` — single instance created at startup
4. Access in Composables via `LocalAppContainer` (`di/AppContainerExt.kt`)
5. Pass to ViewModels via an anonymous `ViewModelProvider.Factory`

To add a dependency: add property to `AppContainer` → implement in `AppContainerImpl` → inject where needed.

## ViewModel Pattern

```kotlin
// Private mutable, public immutable StateFlow
private val _uiState = MutableStateFlow(HomeUiState())
val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

// Events funnel through a single onEvent() method
fun onEvent(event: HomeEvent) { ... }
```

Use `sealed class` for states with distinct phases (e.g., `TakeQuizUiState`); use `data class` for simple flag-bearing states (e.g., `HomeUiState`).

**Shared ViewModel**: For cross-screen state within a navigation sub-graph, use a dedicated shared ViewModel scoped to the `NavBackStackEntry`. Example: `SharedQuizViewModel` (`ui/screens/create/`) holds `editingIndex` and `editingDraft` shared between `CreateQuizScreen` and a question editor without passing state through nav arguments.

**Draft models**: Form-state data classes used only in the UI layer live alongside the ViewModel that owns them, not in `domain/`. Example: `QuestionDraft` and `ChoiceDraft` are defined in `ui/screens/create/CreateQuizViewModel.kt`.

## Compose Rules

- Every composable **must** accept `modifier: Modifier = Modifier` as a parameter.
- Components are **stateless** — hoist all state to ViewModel.
- Add both light + dark `@Preview` for every component.
- **All UI text must be in Vietnamese**, defined in `res/values/strings.xml`, accessed via `stringResource(R.string.*)`. Never hardcode strings.
- Never hardcode colors — use `MaterialTheme.colorScheme.*` only.
- Use `collectAsStateWithLifecycle()` (not `collectAsState()`) to collect `StateFlow` in composables.

Reusable components live in `ui/components/` organized by category:
- `common/` — AlertDialog, BottomSheet, MediaDisplay, TagChip
- `feedback/` — EmptyState, ErrorState, LoadingSpinner, ScoreCard, SkeletonLoader (`shimmerEffect()` Modifier extension)
- `forms/` — CodeInputField, DropdownSelector, SwitchToggle, TextInputField
- `navigation/` — AppTopBar, BottomNavBar, CreateQuizFAB
- `quiz/` — ChoiceButton, DynamicChoiceList, QuizCard, QuizProgressIndicator, TimerDisplay

Use **Coil** (`AsyncImage` from `coil.compose`) for all network image loading.

## Theming

`design-tokens.json` (repo root) is the single source of truth for colors, typography, spacing, radius, and elevation. When changing a visual value, update the token file first, then mirror into `ui/theme/`. Theme class is `QuizCodeTheme`.

`QuizCodeTheme` supports dynamic color on Android 12+, but it is **disabled by default** (`dynamicColor = false`). An extra `FullShape = RoundedCornerShape(50.dp)` value (pill/capsule) is exported from `ui/theme/Shape.kt` alongside the standard `Shapes` object.

## Firebase & Emulator

Firebase is **enabled by default in debug builds with the local emulator** (`useFirebaseEmulator = true`, host `10.0.2.2`). Override via Gradle property:

```bash
./gradlew assembleDebug -PuseFirebaseEmulator=false
# Override host (e.g. for a remote emulator)
./gradlew assembleDebug -PfirebaseEmulatorHost=192.168.1.100
```

Firestore operations: use batch writes for multi-document mutations; use `callbackFlow` + `addSnapshotListener` for real-time streams. Collection names are in `FirestoreCollections` object (see `CODE_RULES.md` §5.1).

## Navigation

Routes are string constants in `ui/navigation/Routes.kt`. Typed destinations live in the `NavigationDestination` sealed class. The single `NavHost` entry point is `QuizCodeNavHost` (rendered from `MainActivity`). Bottom nav shows only on `HOME`, `SEARCH`, `PROFILE` routes.

## Build & Test Commands

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew test                   # Unit tests
./gradlew connectedAndroidTest   # Instrumented tests
./gradlew lint                   # Lint checks
./gradlew clean                  # Clean build

# Nix/devenv users (equivalent shortcuts)
build-debug | build-release | test | lint | clean | firebase-emulators
```

## Key Conventions

- **No emojis** anywhere in source code, comments, scripts, or configs.
- Naming: `{Name}ViewModel`, `{Name}RepositoryImpl`, `{Name}Entity`, `{Name}Dao`, `{Name}Dto`, `{Action}{Entity}UseCase`.
- Exception: the Trash screen ViewModel is named `RecycleBinViewModel` (not `TrashViewModel`) — match this when editing that file.
- Private `MutableStateFlow` prefixed with `_`; event handlers prefixed with `on`.
- KDoc required for all public APIs (see `CODE_RULES.md` §10.1 for format).
- Branch pattern: `feature/{task-id}-{description}` / `bugfix/{task-id}-{description}`.
- Commit format: `feat(scope): subject` (conventional commits).
- Annotation processing uses **KSP** (not KAPT) — add processors with `ksp(...)` in `build.gradle.kts`.
- **Gson** is used for JSON serialization in Room `TypeConverter`s (`Converters.kt`) and in `EntityMappers.kt` for multi-answer fields. Do not add a second JSON library.

## Key Files

| File | Purpose |
|------|---------|
| `di/AppModule.kt` | DI container interface |
| `di/FirebaseModule.kt` | DI container implementation + Firebase/Room init |
| `ui/navigation/Routes.kt` | All route strings + helper builders |
| `ui/navigation/QuizCodeNavHost.kt` | Full app navigation graph |
| `design-tokens.json` | Source of truth for all design values |
| `CODE_RULES.md` | Full coding standards with examples |
| `Docs_en/` | Architecture, backend, frontend, and behavior docs |
| `data/local/AppDatabase.kt` | Room DB v2 definition + `MIGRATION_1_2` |
| `data/local/EntityMappers.kt` | Entity ↔ Domain extension functions (`toDomain` / `toEntity`) |
| `data/remote/AppMappers.kt` | DTO ↔ Domain extension functions (`toDomain` / `toDto`) |
| `data/remote/firebase/FirestoreCollections.kt` | Firestore collection/field name constants |

