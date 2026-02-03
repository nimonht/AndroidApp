# QuizCode - Code Style & Project Rules

## 1. Project Structure

```
app/src/main/java/com/example/androidapp/
├── di/                          # Dependency Injection (Hilt modules)
│   ├── AppModule.kt
│   ├── DatabaseModule.kt
│   ├── FirebaseModule.kt
│   └── RepositoryModule.kt
│
├── domain/                      # Business logic layer (pure Kotlin)
│   ├── model/                   # Domain models
│   │   ├── Quiz.kt
│   │   ├── Question.kt
│   │   ├── Choice.kt
│   │   ├── User.kt
│   │   ├── Attempt.kt
│   │   └── PoolQuestion.kt
│   │
│   ├── usecase/                 # Business logic use cases
│   │   ├── quiz/
│   │   │   ├── CreateQuizUseCase.kt
│   │   │   ├── GetQuizByCodeUseCase.kt
│   │   │   └── ...
│   │   ├── auth/
│   │   │   ├── LoginUseCase.kt
│   │   │   └── ...
│   │   └── ...
│   │
│   └── util/                    # Pure utility functions
│       ├── ChecksumUtil.kt
│       ├── ShareCodeGenerator.kt
│       ├── QuestionShuffler.kt
│       └── ScoreCalculator.kt
│
├── data/                        # Data layer
│   ├── local/                   # Room database
│   │   ├── dao/                 # Data Access Objects
│   │   │   ├── QuizDao.kt
│   │   │   ├── QuestionDao.kt
│   │   │   └── ...
│   │   ├── entity/              # Room entities
│   │   │   ├── QuizEntity.kt
│   │   │   └── ...
│   │   ├── converter/           # Type converters
│   │   │   └── Converters.kt
│   │   └── AppDatabase.kt
│   │
│   ├── remote/                  # Firebase
│   │   ├── firebase/            # Firebase service wrappers
│   │   │   ├── FirebaseAuthService.kt
│   │   │   ├── FirestoreService.kt
│   │   │   └── StorageService.kt
│   │   └── model/               # Firebase DTOs
│   │       ├── QuizDto.kt
│   │       └── ...
│   │
│   └── repository/              # Repository implementations
│       ├── QuizRepositoryImpl.kt
│       ├── AuthRepositoryImpl.kt
│       └── ...
│
└── ui/                          # Presentation layer
    ├── theme/                   # Material Design theme
    │   ├── Color.kt
    │   ├── Typography.kt
    │   ├── Shape.kt
    │   └── Theme.kt
    │
    ├── components/              # Reusable UI components
    │   ├── common/              # General components
    │   │   ├── LoadingSpinner.kt
    │   │   ├── ErrorState.kt
    │   │   └── EmptyState.kt
    │   ├── quiz/                # Quiz-specific components
    │   │   ├── QuizCard.kt
    │   │   ├── QuestionCard.kt
    │   │   ├── ChoiceButton.kt
    │   │   └── DynamicChoiceList.kt
    │   ├── forms/               # Form components
    │   │   ├── TextInputField.kt
    │   │   ├── CodeInputField.kt
    │   │   └── DropdownSelector.kt
    │   ├── feedback/            # Feedback components
    │   │   ├── SkeletonLoader.kt
    │   │   └── ScoreCard.kt
    │   └── navigation/          # Navigation components
    │       ├── BottomNavBar.kt
    │       ├── TopAppBar.kt
    │       └── QuizCodeNavHost.kt
    │
    └── screens/                 # Screen composables + ViewModels
        ├── home/
        │   ├── HomeScreen.kt
        │   └── HomeViewModel.kt
        ├── search/
        │   ├── SearchScreen.kt
        │   └── SearchViewModel.kt
        ├── profile/
        │   ├── ProfileScreen.kt
        │   └── ProfileViewModel.kt
        ├── quiz/
        │   ├── detail/
        │   │   ├── QuizDetailScreen.kt
        │   │   └── QuizDetailViewModel.kt
        │   ├── take/
        │   │   ├── TakeQuizScreen.kt
        │   │   └── TakeQuizViewModel.kt
        │   ├── result/
        │   │   ├── QuizResultScreen.kt
        │   │   └── QuizResultViewModel.kt
        │   └── create/
        │       ├── CreateQuizScreen.kt
        │       └── CreateQuizViewModel.kt
        ├── auth/
        │   ├── LoginScreen.kt
        │   ├── SignupScreen.kt
        │   └── AuthViewModel.kt
        ├── settings/
        │   ├── SettingsScreen.kt
        │   └── SettingsViewModel.kt
        ├── history/
        │   ├── HistoryScreen.kt
        │   └── HistoryViewModel.kt
        └── trash/
            ├── RecycleBinScreen.kt
            └── RecycleBinViewModel.kt
```

---

## 2. Naming Conventions

### 2.1 Files & Classes

| Type | Convention | Example |
|------|------------|---------|
| Composable Screen | `{Name}Screen.kt` | `HomeScreen.kt` |
| ViewModel | `{Name}ViewModel.kt` | `HomeViewModel.kt` |
| Composable Component | `{Name}.kt` | `QuizCard.kt` |
| Repository Interface | `{Name}Repository.kt` | `QuizRepository.kt` |
| Repository Implementation | `{Name}RepositoryImpl.kt` | `QuizRepositoryImpl.kt` |
| Room Entity | `{Name}Entity.kt` | `QuizEntity.kt` |
| Room DAO | `{Name}Dao.kt` | `QuizDao.kt` |
| Firebase DTO | `{Name}Dto.kt` | `QuizDto.kt` |
| Use Case | `{Action}{Entity}UseCase.kt` | `CreateQuizUseCase.kt` |
| Hilt Module | `{Name}Module.kt` | `DatabaseModule.kt` |

### 2.2 Functions

| Type | Convention | Example |
|------|------------|---------|
| Composable | PascalCase | `@Composable fun QuizCard()` |
| Regular function | camelCase | `fun calculateScore()` |
| Suspend function | camelCase with verb | `suspend fun fetchQuizzes()` |
| Flow producer | camelCase | `fun observeQuizzes(): Flow<List<Quiz>>` |
| Event handler | `on{Event}` | `onClick`, `onValueChange` |
| Boolean getter | `is{Adjective}` | `isLoading`, `isValid` |

### 2.3 Variables & Properties

| Type | Convention | Example |
|------|------------|---------|
| MutableStateFlow | `_variableName` (private) | `private val _uiState` |
| StateFlow (exposed) | `variableName` | `val uiState: StateFlow<...>` |
| Composable state | descriptive name | `val isExpanded = remember { mutableStateOf(false) }` |
| Constants | SCREAMING_SNAKE_CASE | `const val MAX_CHOICES = 10` |
| Sealed class | PascalCase | `sealed class UiState` |

### 2.4 Resources

| Type | Convention | Example |
|------|------------|---------|
| String | snake_case | `quiz_title_hint` |
| Color | camelCase | `primaryContainer` |
| Drawable | snake_case with prefix | `ic_home`, `bg_card` |
| Layout | snake_case with prefix | `activity_main`, `item_quiz` |

---

## 3. Architecture Guidelines

### 3.1 MVVM Pattern

```kotlin
// ✅ CORRECT: ViewModel manages UI state
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val quizRepository: QuizRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.LoadQuizzes -> loadQuizzes()
            is HomeEvent.RefreshData -> refreshData()
        }
    }
    
    private fun loadQuizzes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            quizRepository.getMyQuizzes()
                .catch { e -> _uiState.update { it.copy(error = e.message) } }
                .collect { quizzes ->
                    _uiState.update { it.copy(isLoading = false, quizzes = quizzes) }
                }
        }
    }
}

// ❌ WRONG: UI logic in composable
@Composable
fun HomeScreen() {
    val quizzes = remember { mutableStateListOf<Quiz>() }
    LaunchedEffect(Unit) {
        // Don't do API calls directly in composables
        Firebase.firestore.collection("quizzes").get()...
    }
}
```

### 3.2 Repository Pattern

```kotlin
// Interface in domain layer
interface QuizRepository {
    fun getMyQuizzes(userId: String): Flow<List<Quiz>>
    suspend fun createQuiz(quiz: Quiz): Result<String>
    suspend fun getQuizByShareCode(code: String): Quiz?
}

// Implementation in data layer
class QuizRepositoryImpl @Inject constructor(
    private val firestoreService: FirestoreService,
    private val quizDao: QuizDao
) : QuizRepository {
    
    override fun getMyQuizzes(userId: String): Flow<List<Quiz>> = flow {
        // Try remote first
        firestoreService.getMyQuizzes(userId)
            .collect { quizzes ->
                // Cache locally
                quizDao.insertAll(quizzes.map { it.toEntity() })
                emit(quizzes)
            }
    }.catch {
        // Fallback to local cache
        emitAll(quizDao.getAllQuizzes().map { it.map { e -> e.toDomain() } })
    }
}
```

### 3.3 UI State Pattern

```kotlin
// Use sealed class for complex states
sealed class TakeQuizUiState {
    object Loading : TakeQuizUiState()
    data class Active(
        val currentQuestion: Question,
        val questionIndex: Int,
        val totalQuestions: Int,
        val selectedAnswer: String?,
        val timeRemaining: Long
    ) : TakeQuizUiState()
    data class Completed(val score: Int, val maxScore: Int) : TakeQuizUiState()
    data class Error(val message: String) : TakeQuizUiState()
}

// Use data class for simple states
data class HomeUiState(
    val isLoading: Boolean = false,
    val quizzes: List<Quiz> = emptyList(),
    val recentAttempts: List<Attempt> = emptyList(),
    val error: String? = null
)
```

---

## 4. Compose Guidelines

### 4.1 Component Structure

```kotlin
/**
 * Displays a quiz card with thumbnail, title, and stats.
 * 
 * @param quiz The quiz data to display
 * @param onClick Callback when card is clicked
 * @param modifier Modifier for styling/layout
 */
@Composable
fun QuizCard(
    quiz: Quiz,
    onClick: () -> Unit,
    modifier: Modifier = Modifier  // Always accept modifier parameter
) {
    // Implementation
}
```

### 4.2 State Hoisting

```kotlin
// ✅ CORRECT: Stateless component with hoisted state
@Composable
fun CodeInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
    )
}

// ❌ WRONG: Component manages its own state
@Composable
fun CodeInputField(
    modifier: Modifier = Modifier
) {
    var value by remember { mutableStateOf("") }  // Don't do this
    OutlinedTextField(value = value, onValueChange = { value = it })
}
```

### 4.3 Preview Annotations

```kotlin
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun QuizCardPreview() {
    QuizCodeTheme {
        QuizCard(
            quiz = Quiz(title = "Sample Quiz", questionCount = 10),
            onClick = {}
        )
    }
}
```

---

## 5. Firebase Guidelines

### 5.1 Collection References

```kotlin
object FirestoreCollections {
    const val USERS = "users"
    const val QUIZZES = "quizzes"
    const val QUESTIONS = "questions"
    const val CHOICES = "choices"
    const val ATTEMPTS = "attempts"
    const val SHARE_CODES = "shareCodes"
    const val QUESTION_POOL = "questionPool"
}
```

### 5.2 Firestore Operations

```kotlin
// Use batch writes for multiple operations
suspend fun createQuizWithQuestions(quiz: Quiz, questions: List<Question>) {
    val batch = firestore.batch()
    
    val quizRef = firestore.collection(QUIZZES).document()
    batch.set(quizRef, quiz.copy(id = quizRef.id))
    
    questions.forEach { question ->
        val qRef = quizRef.collection(QUESTIONS).document()
        batch.set(qRef, question.copy(id = qRef.id))
    }
    
    batch.commit().await()
}

// Use callbackFlow for real-time listeners
fun observeQuizzes(userId: String): Flow<List<Quiz>> = callbackFlow {
    val listener = firestore.collection(QUIZZES)
        .whereEqualTo("ownerId", userId)
        .addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            val quizzes = snapshot?.toObjects(Quiz::class.java) ?: emptyList()
            trySend(quizzes)
        }
    awaitClose { listener.remove() }
}
```

---

## 6. Room Database Guidelines

### 6.1 Entity Definition

```kotlin
@Entity(tableName = "quizzes")
data class QuizEntity(
    @PrimaryKey
    val id: String,
    val ownerId: String,
    val title: String,
    val description: String?,
    @ColumnInfo(name = "is_public")
    val isPublic: Boolean,
    @ColumnInfo(name = "share_code")
    val shareCode: String?,
    val tags: List<String>,  // Needs TypeConverter
    @ColumnInfo(name = "question_count")
    val questionCount: Int,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long?,
    @ColumnInfo(name = "sync_status")
    val syncStatus: SyncStatus = SyncStatus.SYNCED
)

enum class SyncStatus { PENDING, SYNCING, SYNCED, FAILED }
```

### 6.2 DAO Pattern

```kotlin
@Dao
interface QuizDao {
    @Query("SELECT * FROM quizzes WHERE owner_id = :userId AND deleted_at IS NULL ORDER BY updated_at DESC")
    fun getQuizzesByOwner(userId: String): Flow<List<QuizEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(quizzes: List<QuizEntity>)
    
    @Query("UPDATE quizzes SET sync_status = :status WHERE id = :quizId")
    suspend fun updateSyncStatus(quizId: String, status: SyncStatus)
}
```

---

## 7. Testing Guidelines

### 7.1 ViewModel Testing

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    
    private lateinit var viewModel: HomeViewModel
    private val fakeRepository = FakeQuizRepository()
    
    @Before
    fun setup() {
        viewModel = HomeViewModel(fakeRepository)
    }
    
    @Test
    fun `loadQuizzes updates state with quizzes`() = runTest {
        fakeRepository.setQuizzes(listOf(testQuiz))
        
        viewModel.onEvent(HomeEvent.LoadQuizzes)
        
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.quizzes.size)
    }
}
```

### 7.2 Compose UI Testing

```kotlin
class QuizCardTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun quizCard_displaysTitle() {
        composeTestRule.setContent {
            QuizCard(
                quiz = Quiz(title = "Test Quiz"),
                onClick = {}
            )
        }
        
        composeTestRule.onNodeWithText("Test Quiz").assertIsDisplayed()
    }
}
```

---

## 8. Code Quality Rules

### 8.1 Required for All PRs

- [ ] Code compiles without errors
- [ ] No compiler warnings (or documented exceptions)
- [ ] Unit tests pass
- [ ] New features have corresponding tests
- [ ] KDoc comments for public APIs
- [ ] No hardcoded strings (use strings.xml)
- [ ] No hardcoded colors (use theme)
- [ ] Follows naming conventions above

## 9. Git Conventions

### 9.1 Branch Naming

| Type | Pattern | Example |
|------|---------|---------|
| Feature | `feature/{task-id}-{description}` | `feature/FE-001-quiz-card-component` |
| Bugfix | `bugfix/{task-id}-{description}` | `bugfix/FE-042-timer-crash` |
| Hotfix | `hotfix/{description}` | `hotfix/auth-crash` |
| Release | `release/{version}` | `release/1.0.0` |

### 9.2 Commit Messages

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation
- `style`: Formatting
- `refactor`: Code restructuring
- `test`: Adding tests
- `chore`: Maintenance

**Examples:**
```
feat(quiz): add QuizCard component

- Implement QuizCard with thumbnail, title, stats
- Add preview for light/dark mode
- Support click callback

Closes FE-001
```

### 9.3 Pull Request Template

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] New feature
- [ ] Bug fix
- [ ] Refactor
- [ ] Documentation

## Testing
- [ ] Unit tests added/updated
- [ ] UI tests added/updated
- [ ] Manually tested on device

## Screenshots (if applicable)

## Checklist
- [ ] Code follows style guidelines
- [ ] Self-review completed
- [ ] Documentation updated
```

---

## 10. Documentation Requirements

### 10.1 KDoc for Public APIs

```kotlin
/**
 * Calculates the quiz score based on user answers.
 *
 * Supports both single-answer and multiple-answer questions.
 * For multiple-answer questions, partial credit is not given.
 *
 * @param questions List of questions with correct answers
 * @param answers Map of questionId to selected choiceId(s)
 * @return Total score (sum of points for correct answers)
 * @throws IllegalArgumentException if any questionId in answers is not in questions
 */
fun calculateScore(questions: List<Question>, answers: Map<String, Set<String>>): Int
```

### 10.2 README Files

Each major module should have a README.md explaining:
- Purpose of the module
- Key classes/files
- Usage examples
- Dependencies

---

## Quick Reference Card

```
Package Structure: domain → data → ui
ViewModel: Handle all business logic, expose StateFlow
Composable: Stateless, accept Modifier, use Preview
Firebase: Use batch writes, callbackFlow for listeners  
Room: Use Flow for reactive queries
Test: ViewModel with fakes, UI with compose test rules
Git: feature/{id}-{name}, conventional commits
Docs: KDoc for all public APIs
```
