# AGENTS.md — Quizzez Android App

## Project Overview

Android quiz app (Kotlin + Jetpack Compose + Firebase) named **Quizzez**. Users create, share, and take multiple-choice quizzes. Package: `com.example.androidapp`.

## Architecture

**Clean Architecture + MVVM** with three strict layers — never skip or cross-layer:

```
domain/   ← Pure Kotlin. Models, repository interfaces, utilities. No Android/Firebase imports.
            model/      ← Domain models (Quiz, Question, Choice, Attempt, User,
                          QuestionPoolItem, UserRole, SystemStats, AdminPermission,
                          PaginatedResult, LogEntry + LogLevel enum)
            repository/ ← Repository interfaces (QuizRepository, AttemptRepository, AuthRepository,
                          ShareCodeRepository, PoolRepository,
                          AdminRepository, SearchRepository)
            util/       ← ScoreUtil (star-rating + percentage helpers), ChecksumUtil (SHA-256 quiz
                          integrity), CsvParser + CsvValidator (CSV bulk import), QuizValidator
                          (min 1 question / 2-10 choices / ≥1 correct), ScoreCalculator (exact
                          set equality grading), QuestionShuffler, SearchFilterLogic (tag/
                          visibility/date filtering), ShareCodeUtil (6-char alphanumeric gen),
                          TimeFormatter (HH:MM:SS / MM:SS + timestamp formatting),
                          InputSanitizer, TagValidator,
                          SafeCall (try/catch → Result<T> wrapper for repository impls),
                          VectorSimilarity (cosine similarity for embeddings)
            console/    ← In-app console engine: Command interface, CommandLexer, CommandParser,
                          CommandExecutor, CommandRegistry, CommandResult, CommandContext,
                          CommandToken, CommandFormatUtils, ConsoleJsonBuilder;
                          commands/ sub-package has ~35 command implementations
            service/    ← Domain service interfaces: LogService, NetworkService,
                          SettingsService, SyncService,
                          EmbeddingService (MediaPipe-backed; 100-dim output; CURRENT_MODEL_VERSION
                          constant triggers re-index on upgrade),
                          EmbeddingIndex (read-only in-memory vector index interface)
data/     ← Firebase DTOs (remote/model/), remote data sources (remote/firebase/),
            Room entities (local/entity/), mapper extensions, repository impls.
ui/       ← Compose screens + ViewModels. Screens are stateless; ViewModels own all state.
```

Data layer internal structure:
```
data/
  local/
    AppDatabase.kt          ← Room DB definition (v9); uses fallbackToDestructiveMigration (no explicit migrations)
    EntityMappers.kt        ← Extension fns: Entity ↔ Domain (toDomain / toEntity)
    LocalQuizPurger.kt      ← Shared utility: purges quiz + questions + choices from Room in correct
                              deletion order (choices → questions → quiz); used by QuizInvalidationManager,
                              SyncManager, and QuizRepositoryImpl to avoid duplication
    dao/                    ← DAOs (QuizDao, QuestionDao, ChoiceDao, AttemptDao, UserDao, PendingSyncDao)
    entity/                 ← Room entities + SyncStatus / PendingSyncStatus / SyncEntityType / SyncOperation enums;
                              QuizEntity includes `embedding: ByteArray?`, `embeddingVersion: Int`, and
                              `isRemovedFromCloud: Boolean` fields; tags stored as comma-separated string;
                              no separate Converters.kt — complex fields are plain strings/bytes
  remote/
    AppMappers.kt           ← Extension fns: DTO ↔ Domain (toDomain / toDto)
    firebase/               ← Remote data sources (QuizRemoteDataSource, AttemptRemoteDataSource,
                              UserRemoteDataSource, QuestionRemoteDataSource,
                              ShareCodeRemoteDataSource, PoolRemoteDataSource,
                              AdminRemoteDataSource) + FirestoreCollections.kt (collection/field constants)
                              + FirestoreCascadeHelper.kt (shared helper for cascade-delete operations:
                              collectQuizSubcollectionRefs, cascadeDeleteUserData, buildTombstoneData)
    model/                  ← Firestore DTOs: `QuizDtoModels.kt` (QuizDto + QuestionDto + ChoiceDto),
                              `AttemptDto.kt`, `UserDto.kt`, `ShareCodeDto.kt`, `QuestionPoolItemDto.kt`
  repository/               ← Repository implementations: `QuizRepositoryImpl.kt`, `AttemptRepositoryImpl.kt`,
                              `AuthRepositoryImpl.kt` (wraps FirebaseAuth + UserDao + UserRemoteDataSource),
                              `ShareCodeRepositoryImpl.kt`,
                              `PoolRepositoryImpl.kt`, `AdminRepositoryImpl.kt`,
                              `SearchRepositoryImpl.kt` (SharedPreferences-backed recent searches)
  logging/
    LogCollector.kt         ← Logcat ring buffer collector (10k entries); implements LogService;
                              spawns coroutine reading `logcat -v threadtime --pid`; batched StateFlow emission
  ml/
    ModelManager.kt         ← MediaPipe model file lifecycle (bundled asset + future OTA updates via Firebase ML)
    TFLiteEmbeddingService.kt ← Implements EmbeddingService; produces 100-dim float vectors via MediaPipe TextEmbedder
  network/
    NetworkMonitor.kt       ← ConnectivityManager wrapper; exposes `isOnline: StateFlow<Boolean>`
  preferences/
    SettingsPreferences.kt  ← Local app settings (theme, notifications, etc.)
  search/
    EmbeddingCache.kt       ← In-memory vector index; implements EmbeddingIndex; populated by EmbeddingIndexWorker
  sync/
    SyncManager.kt          ← Active sync coordinator; `SyncState` enum (IDLE/SYNCING/PENDING/ERROR);
                              `enqueueSync()`, `processPendingOperations()`, `retryFailedOperations()`
    QuizInvalidationManager.kt ← Incremental tombstone-based invalidation of locally-cached quizzes
                              deleted on Firestore. Three tiers: lazy validateQuizExists() (1 read),
                              periodic checkForDeletedQuizzes() (tombstone sweep), full stale cleanup
                              in SyncManager. Tracks last-check timestamp in SharedPreferences.
  worker/                   ← Background workers (WorkManager): `BackendMaintenanceWorker.kt`,
                              `BackgroundSyncWorker.kt`,
                              `EmbeddingIndexWorker.kt` (computes embeddings in batches; re-indexes
                              when embeddingVersion < EmbeddingService.CURRENT_MODEL_VERSION)
```

Repositories generally delegate Firestore access to `*RemoteDataSource` classes in `data/remote/firebase/`. Current exception: `PoolRepositoryImpl.kt` also receives `FirebaseFirestore` for batch writes in `contributeQuestions()`.

**Semantic Search Architecture (TFLite + FTS4)**: The app uses hybrid search combining full-text search with vector similarity:
- **FTS4**: `QuizFtsEntity` virtual table synced with `QuizEntity` for fast `MATCH` queries on title/description/tags.
- **Embeddings**: 100-dim float vectors generated by on-device MediaPipe Universal Sentence Encoder (`universal_sentence_encoder.tflite`), stored as `ByteArray` in `QuizEntity.embedding`.
- **Indexing**: `EmbeddingIndexWorker` computes embeddings in batches; re-indexes when `embeddingVersion < EmbeddingService.CURRENT_MODEL_VERSION`.
- **Search**: Combines FTS results with cosine similarity (`VectorSimilarity`) for relevance ranking. In-memory index lives in `EmbeddingCache` (implements `EmbeddingIndex`).

**Local-first with cloud sync**: Room-backed flows (notably quiz/question/attempt) write to Room first (`syncStatus = PENDING` or queued via `PendingSyncEntity`), then sync to Firestore in the background. Reads emit Room data immediately and refresh from Firestore when online. Some repositories are intentionally remote-only (`ShareCodeRepositoryImpl`, `PoolRepositoryImpl`, `AdminRepositoryImpl`).

`PendingSyncEntity` (`data/local/entity/PendingSyncEntity.kt`) provides a separate retry queue (`pending_sync_operations` table) with `retryCount`, `maxRetries`, and `PendingSyncStatus` (PENDING / IN_PROGRESS / FAILED / COMPLETED). `PendingSyncDao` exposes `observePendingCount(): Flow<Int>` for UI sync indicators.

## Dependency Injection

**Manual DI — no Hilt/Dagger.** The container chain is:
1. `AppContainer` interface (`di/AppModule.kt`) — declares all singletons
2. `AppContainerImpl` (`di/FirebaseModule.kt`) — lazy `by lazy` implementations
3. `QuizzezApplication.appContainer` — single instance created at startup
4. Access in Composables via `LocalAppContainer` (`di/AppContainerExt.kt`)
5. Pass to ViewModels via an anonymous `ViewModelProvider.Factory`

To add a dependency: add property to `AppContainer` → implement in `AppContainerImpl` → inject where needed.

`AppContainer` also exposes: `quizInvalidationManager` (tombstone-based cache invalidation), `quizRemoteDataSource` (shared by workers and the invalidation manager), `modelManager` (TFLite model lifecycle), `embeddingService` (on-device inference), and `embeddingIndex` (in-memory vector index).

## ViewModel Pattern

```kotlin
// Private mutable, public immutable StateFlow
private val _uiState = MutableStateFlow(HomeUiState())
val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

// Events funnel through a single onEvent() method
fun onEvent(event: HomeEvent) { ... }
```

Use `sealed class` for states with distinct phases (e.g., `TakeQuizUiState`); use `data class` for simple flag-bearing states (e.g., `HomeUiState`).

**Shared ViewModel**: For cross-screen state within a navigation sub-graph, use a dedicated shared ViewModel scoped to the `NavBackStackEntry`, shared between composables via the parent back-stack entry without passing state through nav arguments.

**Draft models**: Form-state data classes used only in the UI layer live alongside the ViewModel that owns them, not in `domain/`. Example: `QuestionDraft` and `ChoiceDraft` are defined in `ui/screens/create/CreateQuizViewModel.kt`.

**UI-layer helper data classes**: Non-form helpers that combine or adapt domain data also live with the owning ViewModel. Examples: `AttemptWithQuiz` in `HistoryViewModel.kt` (pairs `Attempt` with its quiz title), `QuestionReview` in `AnswerReviewViewModel.kt` (pairs a question with the user's answer and correctness). Do not move these to `domain/`.

## Compose Rules

- Every composable **must** accept `modifier: Modifier = Modifier` as a parameter.
- Components are **stateless** — hoist all state to ViewModel.
- Add both light + dark `@Preview` for every component.
- **All UI text must be in Vietnamese**, defined in `res/values/strings.xml`, accessed via `stringResource(R.string.*)`. Never hardcode strings.
- Never hardcode colors — use `MaterialTheme.colorScheme.*` only.
- Use `collectAsStateWithLifecycle()` (not `collectAsState()`) to collect `StateFlow` in composables.

Reusable components live in `ui/components/` organized by category:
- Standalone — `ShareCodeSection`, `TagSuggestionDialog`
- `admin/` — AdminChart, AdminInsightCard, AdminQuizCard, AdminUserCard, RoleSelector, StatisticCard
- `common/` — AlertDialog, BottomSheet, MediaDisplay, TagChip, LoginPromptDialog
- `feedback/` — EmptyState, ErrorState, LoadingSpinner, ScoreCard, SkeletonLoader (`shimmerEffect()` Modifier extension)
- `forms/` — CodeInputField, DropdownSelector, SwitchToggle, TextInputField, QuizSearchBar
- `navigation/` — AppTopBar, BottomNavBar, CreateQuizFAB
- `quiz/` — ChoiceButton, DynamicChoiceList, QuizCard, QuizProgressIndicator, QuizThumbnail, TimerDisplay

Use **Coil** (`AsyncImage` from `coil.compose`) for all network image loading.

## Theming

`design-tokens.json` (repo root) is the single source of truth for colors, typography, spacing, radius, and elevation. When changing a visual value, update the token file first, then mirror into `ui/theme/`. Theme class is `QuizzezTheme`.

`QuizzezTheme` supports dynamic color on Android 12+, but it is **disabled by default** (`dynamicColor = false`). An extra `FullShape = RoundedCornerShape(50.dp)` value (pill/capsule) is exported from `ui/theme/Shape.kt` alongside the standard `Shapes` object.

Typography uses a dual-font system loaded via **Google Fonts** (`compose.ui.text.google.fonts`): **Playfair Display** (Serif) for display/headline text, and **Inter** (Sans-Serif) for labels, buttons, and body copy. Font families are `PlayfairDisplayFamily` and `InterFamily` declared in `ui/theme/Type.kt`.

## Firebase & Emulator

Firebase emulator is **disabled by default** in all build types. Enable it explicitly via Gradle property:

```bash
./gradlew assembleDebug -PuseFirebaseEmulator=true
# Override host (e.g. for a remote emulator)
./gradlew assembleDebug -PuseFirebaseEmulator=true -PfirebaseEmulatorHost=192.168.1.100
```

Firestore operations: use batch writes for multi-document mutations; use `callbackFlow` + `addSnapshotListener` for real-time streams. Collection names are in `FirestoreCollections` object (see `CODE_RULES.md` §5.1).

**Sample Data Generation**:
Populate the local Firestore emulator using the provided Python script:
```bash
python3 -m venv scripts/.venv
source scripts/.venv/bin/activate
pip install -r scripts/requirements.txt
python scripts/generate-sample-data.py --clean --count 100 --seed 42
```

## Navigation

Routes are string constants in `ui/navigation/Routes.kt`. Typed destinations live in the `NavigationDestination` sealed class. The single `NavHost` entry point is `QuizzezNavHost` (rendered from `MainActivity`). Bottom nav shows only on `HOME`, `SEARCH`, `PROFILE` routes.

Existing screen directories under `ui/screens/`:
- `auth/` — `LoginScreen`, `RegisterScreen`, `AuthViewModel` (shared auth state), `AuthFragment` (XML Fragment with TabLayout; wraps Login/Register tabs and "Continue as Guest")
- `home/` — `HomeScreen`, `HomeViewModel`
- `search/` — `SearchScreen`, `SearchViewModel`; state and events are split into standalone files (`SearchUiState.kt`, `SearchEvent.kt`); display model `QuizCardDraft` and sub-composables (`SearchControlsRow`, `TagFilterRow`, `SearchResultsGrid`, `SearchResultsList`, `DiscoverSection`) also live in this package; `SortOption` enum (DATE / POPULARITY / RELEVANCE) is defined in `SearchUiState.kt`
- `profile/` — `ProfileScreen`, `ProfileViewModel`, `EditProfileScreen`, `EditProfileViewModel`.
  `EditProfileViewModel` depends only on `AuthRepository`. Profile avatars
  are URL-based: users paste a URL manually or fetch a random avatar from the Wallhaven API.
  Events: `AvatarUrlChanged(url: String)`, `FetchRandomAvatar` (replaced the old `AvatarSelected(uri: Uri)`).
  `EditProfileUiState` includes `isLoadingAvatar: Boolean` for tracking Wallhaven fetch progress.
- `quiz/` — `QuizDetailScreen`/`ViewModel`, `TakeQuizScreen`/`ViewModel`, `QuizResultScreen`/`ViewModel`
- `create/` — `CreateQuizScreen`/`ViewModel`, `EditQuizScreen`/`EditQuizViewModel`, `CsvImportScreen`/`CsvImportViewModel`, `QuizPreviewScreen`/`QuizPreviewViewModel`, `QuizFormContent`, `QuizFormEvent`, `QuizFormHelper` (shared form helpers).
  Quiz lifecycle uses a 3-state model:
    - **Draft**: `isDraft=true`, `isPublic=false` (forced) — work-in-progress, not visible to others.
    - **Published (Private)**: `isDraft=false`, `isPublic=false` (toggle off) — finalized but only accessible via share code.
    - **Public**: `isDraft=false`, `isPublic=true` (toggle on, explicit user choice) — discoverable in search.
  Publishing a draft sets `isDraft=false` but does **not** force `isPublic=true`; visibility is a separate, explicit toggle.
- `history/` — `HistoryScreen`, `HistoryViewModel`
- `review/` — `AnswerReviewScreen`, `AnswerReviewViewModel`
- `attempt/` — `AttemptDetailScreen`, `AttemptDetailViewModel`
- `trash/` — `TrashScreen`, `RecycleBinViewModel`
- `settings/` — `SettingsScreen`, `SettingsViewModel`
- `pool/` — `QuestionPoolScreen`, `QuestionPoolViewModel`
- `admin/` — Admin panel (accessed from Profile, not bottom nav); role-guarded in NavHost + Firestore rules.
  Root: `BaseAdminStatsViewModel.kt` (shared base class for admin sub-ViewModels).
  Sub-packages: `dashboard/` (`AdminDashboardScreen`/`ViewModel`/`UiState`),
  `users/` (`AdminUserManagementScreen`/`ViewModel`/`UiState`),
  `quizzes/` (`AdminQuizManagementScreen`/`ViewModel`/`UiState`),
  `reports/` (`AdminReportsScreen`/`ViewModel`/`UiState`).
  Note: `users/` and `quizzes/` management screens have collapsible filter/search sections (collapsed by default, toggled via `AnimatedVisibility`).
  Admin routes: `admin/dashboard`, `admin/users`, `admin/quizzes`, `admin/reports`.
- `advanced/` — Developer Tools (tabbed: Console + Log Viewer). Entry from Profile, accessible to all
  logged-in users. `AdvancedScreen`/`AdvancedViewModel` (tab container).
  Sub-packages: `console/` (`ConsoleScreen`/`ConsoleViewModel`/`ConsoleViewModelFactory`;
  components: `ConsoleInputField`, `ConsoleOutputLine`, `SuggestionDropdown`, `TokenHighlighter`),
  `logviewer/` (`LogViewerScreen`/`LogViewerViewModel`).
  Console commands are defined in `domain/console/commands/` and registered via DI in `FirebaseModule.kt`.
  **MCP Server**: An MCP server (`mcp_server/`) is available to connect AI agents directly to this in-app console via ADB broadcasts. It exposes tools like `execute_command`, `list_commands`, and `get_command_help`.

## Build & Test Commands

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew test                   # Unit tests
./gradlew connectedAndroidTest   # Instrumented tests
./gradlew lint                   # Lint checks
./gradlew :app:dokkaHtml         # Generate API docs (Dokka)
./gradlew clean                  # Clean build

# Nix/devenv users (equivalent shortcuts)
build-debug | build-release | test | lint | clean | firebase-emulators
```

## Key Conventions

- **No emojis** anywhere in source code, comments, scripts, or configs.
- Naming: `{Name}ViewModel`, `{Name}RepositoryImpl`, `{Name}Entity`, `{Name}Dao`, `{Name}Dto`, `{Action}{Entity}UseCase`.
- Exception: the Trash screen ViewModel is named `RecycleBinViewModel` (not `TrashViewModel`) — match this when editing that file.
- `domain/usecase/` does not exist — no use-case classes are implemented. Business logic lives directly in ViewModels.
- **Profile avatars** are served via URL (user-pasted or randomly fetched from the Wallhaven API), **not** uploaded to Firebase Storage. There is no `StorageRepository`; `EditProfileViewModel` depends only on `AuthRepository`.
- Private `MutableStateFlow` prefixed with `_`; event handlers prefixed with `on`.
- KDoc required for all public APIs (see `CODE_RULES.md` §10.1 for format).
- Branch pattern: `feature/{task-id}-{description}` / `bugfix/{task-id}-{description}`.
- Commit format: `feat(scope): subject` (conventional commits).
- Annotation processing uses **KSP** (not KAPT) — add processors with `ksp(...)` in `build.gradle.kts`.
- **Gson** is used in `EntityMappers.kt` for JSON serialization of multi-answer fields (e.g. `multiAnswers: Map<questionId, List<choiceId>>`). There is no separate `Converters.kt`; complex entity fields are stored as plain strings or bytes. Do not add a second JSON library.
- **`UiError` enum** (`ui/common/UiError.kt`): ViewModels emit typed `UiError` values instead of raw Vietnamese strings in their `UiState`. The composable layer maps them to localised messages via the `@Composable` extension `UiError.toMessage(detail?)`. This keeps ViewModels free of Android resource imports while guaranteeing every user-facing error message lives in `strings.xml`.
- **`QrCodeUtil`** (`ui/util/QrCodeUtil.kt`): Generates QR code bitmaps from text content (e.g. share codes) via ZXing. Use `QrCodeUtil.generateQrBitmap(content)` — returns a nullable `Bitmap`.
- **MCP Server Sync**: When adding, removing, or modifying console commands in `domain/console/commands/`,
  the MCP server metadata in `mcp_server/command_registry.py` MUST be updated to match. Add a corresponding
  `CommandInfo` entry with matching name, aliases, usage, flags, examples, role, and category. Also update
  `_NL_KEYWORDS` and `_build_concrete` in `mcp_server/server.py` if the new command has subcommands or
  Vietnamese keywords. Failure to keep these in sync will cause the MCP tools (help, validation, suggestions)
  to be unaware of new commands.

## Key Files

| File | Purpose |
|------|---------|
| `di/AppModule.kt` | DI container interface |
| `di/FirebaseModule.kt` | DI container implementation + Firebase/Room init |
| `ui/navigation/Routes.kt` | All route strings + helper builders |
| `ui/navigation/QuizzezNavHost.kt` | Full app navigation graph |
| `design-tokens.json` | Source of truth for all design values |
| `CODE_RULES.md` | Full coding standards with examples |
| `DOKKA_SETUP.md` | Dokka generation setup (`./gradlew :app:dokkaHtml`) |
| `Docs_en/` | Architecture, backend, frontend, and behavior docs |
| `data/local/AppDatabase.kt` | Room DB v9 definition; `fallbackToDestructiveMigration` — no explicit migrations |
| `data/local/EntityMappers.kt` | Entity ↔ Domain extension functions (`toDomain` / `toEntity`) |
| `data/local/LocalQuizPurger.kt` | Shared Room purge utility (choices → questions → quiz deletion order) |
| `data/sync/QuizInvalidationManager.kt` | Tombstone-based invalidation of locally-cached deleted quizzes |
| `data/remote/firebase/FirestoreCascadeHelper.kt` | Cascade-delete user data and write quiz tombstones on Firestore |
| `domain/console/Command.kt` | Console command interface + CompletionSuggestion |
| `domain/console/CommandExecutor.kt` | Console command execution pipeline |
| `data/logging/LogCollector.kt` | Logcat ring buffer collector (implements LogService) |
| `ui/common/UiError.kt` | Typed error enum + `toMessage()` extension for all ViewModel error states |
| `ui/util/QrCodeUtil.kt` | ZXing-backed QR code bitmap generator |
| `mcp_server/server.py` | MCP server connecting AI agents to the in-app console via ADB |
| `mcp_server/command_registry.py` | MCP server command metadata -- must stay in sync with in-app commands |
| `scripts/generate-sample-data.py` | Populates local Firestore emulator with test data |
