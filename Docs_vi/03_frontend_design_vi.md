# Thiết Kế & Triển Khai Frontend (Firebase + Kotlin)

## 1. Kiến Trúc UI

### 1.1 Cấu Trúc Navigation

```mermaid
graph TD
    A[App Entry] --> B{Đã Đăng Nhập?}
    B -->|Có| C[Main Activity]
    B -->|Không| C
    
    C --> D[Bottom Nav]
    D --> E[Trang Chủ]
    D --> F[Tìm Kiếm]
    D --> G[Hồ Sơ]
    
    E --> H[Chi Tiết Quiz]
    E --> I[Tạo Quiz]
    H --> J[Làm Quiz]
    J --> K[Màn Hình Kết Quả]
    
    F --> H
    G --> L[Cài Đặt]
    G --> M[Lịch Sử]
    G --> N[Thùng Rác]
```

<details>
<summary>📊 Xem dạng Text (nếu Mermaid không hiển thị)</summary>

```
                        App Entry
                            │
                            ▼
                    Đã Đăng Nhập?
                      /         \
                    Có          Không
                      \         /
                       ▼       ▼
                    Main Activity
                          │
                          ▼
                      Bottom Nav
           ┌──────────────┼──────────────┐
           ▼              ▼              ▼
       Trang Chủ      Tìm Kiếm        Hồ Sơ
           │              │              │
     ┌─────┴─────┐        │     ┌────────┼────────┐
     ▼           ▼        │     ▼        ▼        ▼
Chi Tiết Quiz Tạo Quiz    │  Cài Đặt  Lịch Sử  Thùng Rác
     │                    │
     ▼                    │
  Làm Quiz ◄──────────────┘
     │
     ▼
Màn Hình Kết Quả
```

</details>


### 1.2 Danh Sách Màn Hình

| Màn Hình | Route | Yêu Cầu Xác Thực |
|----------|-------|------------------|
| Trang Chủ | `/home` | Không |
| Tìm Kiếm/Khám Phá | `/search` | Không |
| Hồ Sơ | `/profile` | Có* |
| Chi Tiết Quiz | `/quiz/{id}` | Không |
| Làm Quiz | `/quiz/{id}/play` | Không |
| Kết Quả Quiz | `/quiz/{id}/result` | Không |
| Tạo Quiz | `/quiz/create` | Có |
| Chỉnh Sửa Quiz | `/quiz/{id}/edit` | Có |
| Cài Đặt | `/settings` | Có |
| Lịch Sử Làm Bài | `/history` | Có |
| Thùng Rác | `/trash` | Có |

> *Hiển thị yêu cầu đăng nhập nếu chưa xác thực

---

## 2. Thư Viện Component

### 2.1 Các Component Chính

```
┌─────────────────────────────────────────────────────────────┐
│                  Các Component Tái Sử Dụng                   │
├─────────────────────────────────────────────────────────────┤
│  Navigation (Điều Hướng)                                     │
│  ├─ BottomNavBar (Trang Chủ, Tìm Kiếm, Hồ Sơ)              │
│  ├─ TopAppBar (Quay Lại, Tiêu Đề, Hành Động)               │
│  └─ FloatingActionButton (Tạo Quiz)                         │
├─────────────────────────────────────────────────────────────┤
│  Hiển Thị Quiz                                               │
│  ├─ QuizCard (Thumbnail, Tiêu Đề, Thống Kê)                │
│  ├─ QuestionCard (Nội Dung, Media, Lựa Chọn)               │
│  ├─ DynamicChoiceList (hiển thị 2-10 lựa chọn động)       │
│  ├─ ChoiceButton (Text, Trạng Thái, Chế độ đa chọn)      │
│  ├─ ChoiceCounter (hiển thị "X trên Y lựa chọn")           │
│  └─ TagChip (Tag phân loại dạng pill)                       │
├─────────────────────────────────────────────────────────────┤
│  Phản Hồi                                                    │
│  ├─ LoadingSpinner                                          │
│  ├─ SkeletonLoader                                          │
│  ├─ ErrorState (Thông báo + Thử lại)                       │
│  └─ EmptyState (Thông báo + CTA)                           │
├─────────────────────────────────────────────────────────────┤
│  Biểu Mẫu                                                    │
│  ├─ TextInputField                                          │
│  ├─ CodeInputField (6 ký tự)                               │
│  ├─ DropdownSelector                                        │
│  └─ SwitchToggle                                            │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 Component QuizCard

```kotlin
@Composable
fun QuizCard(
    quiz: Quiz,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            AsyncImage(
                model = quiz.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = quiz.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                Text(
                    text = "bởi ${quiz.authorName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    quiz.tags.take(3).forEach { tag ->
                        TagChip(text = tag)
                    }
                }
            }
            
            // Thống kê
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${quiz.questionCount} câu",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = "${quiz.attemptCount} lượt làm",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
```

### 2.3 Component DynamicChoiceList

```kotlin
/**
 * Render danh sách nút lựa chọn động dựa trên số lượng lựa chọn của câu hỏi.
 * Hỗ trợ 2-10 lựa chọn mỗi câu với chế độ đa chọn tùy chọn.
 */
@Composable
fun DynamicChoiceList(
    choices: List<Choice>,
    selectedChoiceIds: Set<String>,
    allowMultipleCorrect: Boolean = false,
    onChoiceSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Xác thực số lượng lựa chọn (2-10)
    require(choices.size in 2..10) { 
        "Câu hỏi phải có 2-10 lựa chọn, đã nhận ${choices.size}" 
    }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Hiển thị bộ đếm lựa chọn cho câu hỏi có nhiều tùy chọn
        if (choices.size > 4) {
            Text(
                text = "${selectedChoiceIds.size} trên ${choices.size} lựa chọn",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        choices.forEachIndexed { index, choice ->
            val label = ('A' + index).toString()
            val isSelected = choice.id in selectedChoiceIds
            
            ChoiceButton(
                label = label,
                content = choice.content,
                isSelected = isSelected,
                isMultiSelect = allowMultipleCorrect,
                onClick = { onChoiceSelected(choice.id) }
            )
        }
    }
}

@Composable
fun ChoiceButton(
    label: String,
    content: String,
    isSelected: Boolean,
    isMultiSelect: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) 
                MaterialTheme.colorScheme.primary 
            else MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Chỉ báo chọn (checkbox cho đa chọn, radio cho đơn chọn)
            if (isMultiSelect) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = null
                )
            } else {
                RadioButton(
                    selected = isSelected,
                    onClick = null
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = "$label. $content",
                style = MaterialTheme.typography.bodyLarge
            )
            
            if (isSelected) {
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Đã chọn",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
```

---

## 3. Mockup Màn Hình

### 3.1 Trang Chủ Dashboard

```
┌─────────────────────────────────────────────────────────────┐
│  [←]              QuizCode                           [👤]   │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Xin chào, Thanh! 👋                                        │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Nhập mã quiz    [______]  [Tham gia →]             │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  Đã Làm Gần Đây                                             │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐                       │
│  │ Quiz 1  │ │ Quiz 2  │ │ Quiz 3  │ ──►                   │
│  └─────────┘ └─────────┘ └─────────┘                       │
│                                                             │
│  Quiz Của Tôi                                  [Xem Tất Cả →]│
│  ┌─────────────────────────────────────────────────────┐   │
│  │ [📷] Toán Học 101          10 câu   │   45 lượt    │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  Quiz Thịnh Hành                               [Xem Tất Cả →]│
│  ┌─────────────────────────────────────────────────────┐   │
│  │ [📷] Khoa Học Vui           ★4.5   │   500+ lượt  │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│        [🔍]              [🏠]              [👤]            │
│      Tìm Kiếm         Trang Chủ          Hồ Sơ            │
│                          [+]                                │
│                         (FAB)                               │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 Màn Hình Làm Quiz

```
┌─────────────────────────────────────────────────────────────┐
│  [✕]                                           [Nộp Bài]   │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Câu hỏi 4/10                                ⏱ 12:45       │
│  ████████████░░░░░░░░░░░░░░░░░░░                           │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              [Hình ảnh/Video Media]                 │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  Thủ đô của Việt Nam là gì?                                 │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  A. Thành phố Hồ Chí Minh                           │   │
│  └─────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  B. Hà Nội                              ✓ Đã chọn  │   │
│  └─────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  C. Đà Nẵng                                         │   │
│  └─────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  D. Huế                                             │   │
│  └─────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  E. Cần Thơ                                         │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  Lưu ý: Câu hỏi hỗ trợ 2-10 lựa chọn (linh hoạt)           │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│           [← Câu Trước]              [Câu Tiếp →]          │
└─────────────────────────────────────────────────────────────┘
```

### 3.3 Màn Hình Kết Quả Quiz

```
┌─────────────────────────────────────────────────────────────┐
│  [←]              Hoàn Thành Quiz                    [📤]  │
├─────────────────────────────────────────────────────────────┤
│                         🎉                                  │
│                    Điểm Của Bạn                             │
│                      8/10                                   │
│                      80%                                    │
│                    ★★★★☆                                   │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  ✅ Đúng: 8       ❌ Sai: 2       ⏱ 8:32            │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │               [Xem Lại Đáp Án]                       │   │
│  └─────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │               [Làm Lại]                              │   │
│  └─────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │               [Về Trang Chủ]                         │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  Đánh giá quiz:  ☆ ☆ ☆ ☆ ☆                                 │
└─────────────────────────────────────────────────────────────┘
```

---

## 4. Mẫu ViewModel

### 4.1 ViewModel Tạo Quiz

```kotlin
@HiltViewModel
class CreateQuizViewModel @Inject constructor(
    private val quizRepository: QuizRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateQuizUiState())
    val uiState: StateFlow<CreateQuizUiState> = _uiState.asStateFlow()

    fun updateTitle(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun addQuestion(question: QuestionDraft) {
        _uiState.update { it.copy(questions = it.questions + question) }
    }

    fun saveQuiz() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            
            try {
                val quiz = createQuizFromState()
                val quizId = quizRepository.createQuiz(quiz, _uiState.value.questions)
                
                _uiState.update { 
                    it.copy(isSaving = false, savedQuizId = quizId.getOrNull()) 
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    fun importFromCsv(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true) }
            val result = csvImporter.import(uri)
            _uiState.update { 
                it.copy(
                    isImporting = false,
                    questions = result.questions,
                    importErrors = result.errors
                ) 
            }
        }
    }
}

data class CreateQuizUiState(
    val title: String = "",
    val description: String = "",
    val tags: List<String> = emptyList(),
    val isPublic: Boolean = false,
    val questions: List<QuestionDraft> = emptyList(),
    val shareToPool: Boolean = false,
    val isSaving: Boolean = false,
    val isImporting: Boolean = false,
    val savedQuizId: String? = null,
    val error: String? = null,
    val importErrors: List<ImportError> = emptyList()
)
```

### 4.2 ViewModel Làm Quiz

```kotlin
@HiltViewModel
class TakeQuizViewModel @Inject constructor(
    private val quizRepository: QuizRepository,
    private val attemptRepository: AttemptRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val quizId: String = savedStateHandle["quizId"]!!
    
    private val _uiState = MutableStateFlow<TakeQuizUiState>(TakeQuizUiState.Loading)
    val uiState: StateFlow<TakeQuizUiState> = _uiState.asStateFlow()

    private lateinit var attempt: Attempt
    private val answers = mutableMapOf<String, String>()

    init { loadQuiz() }

    private fun loadQuiz() {
        viewModelScope.launch {
            val quizData = quizRepository.getQuizWithQuestions(quizId)
            
            // Xáo trộn câu hỏi và lựa chọn
            val shuffledQuestions = quizData.questions.shuffled()
            val shuffledChoices = shuffledQuestions.associate { q ->
                q.id to q.choices.shuffled()
            }
            
            // Tạo attempt trong Firestore
            attempt = attemptRepository.createAttempt(
                quizId = quizId,
                userId = Firebase.auth.currentUser?.uid,
                questions = shuffledQuestions
            )
            
            _uiState.value = TakeQuizUiState.Active(
                questions = shuffledQuestions,
                choiceOrders = shuffledChoices,
                currentIndex = 0,
                answers = emptyMap(),
                startTime = System.currentTimeMillis()
            )
        }
    }

    fun selectAnswer(questionId: String, choiceId: String) {
        answers[questionId] = choiceId
        updateState { it.copy(answers = answers.toMap()) }
    }

    fun submitQuiz() {
        viewModelScope.launch {
            val activeState = _uiState.value as? TakeQuizUiState.Active ?: return@launch
            
            // Tính điểm
            var score = 0
            activeState.questions.forEach { q ->
                val selectedChoiceId = answers[q.id]
                val correctChoice = q.choices.find { it.isCorrect }
                if (selectedChoiceId == correctChoice?.id) {
                    score += q.points
                }
            }
            
            // Lưu vào Firestore
            attemptRepository.submitAttempt(attempt.id, answers, score)
            
            _uiState.value = TakeQuizUiState.Completed(
                score = score,
                maxScore = activeState.questions.sumOf { it.points },
                timeTaken = System.currentTimeMillis() - activeState.startTime
            )
        }
    }
}

sealed class TakeQuizUiState {
    object Loading : TakeQuizUiState()
    data class Active(
        val questions: List<Question>,
        val choiceOrders: Map<String, List<Choice>>,
        val currentIndex: Int,
        val answers: Map<String, String>,
        val startTime: Long
    ) : TakeQuizUiState()
    data class Completed(
        val score: Int,
        val maxScore: Int,
        val timeTaken: Long
    ) : TakeQuizUiState()
}
```

---

## 5. Tích Hợp Firebase

### 5.1 Cài Đặt Firebase (Hilt Module)

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {
    
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = Firebase.auth
    
    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = Firebase.firestore
    
    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage = Firebase.storage
}
```

### 5.2 Repository với Firebase

```kotlin
class QuizRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val quizDao: QuizDao // Room cho cache ngoại tuyến
) {
    private val quizzesRef = firestore.collection("quizzes")
    
    // Cập nhật real-time với cache ngoại tuyến
    fun getMyQuizzes(userId: String): Flow<List<Quiz>> = callbackFlow {
        val listener = quizzesRef
            .whereEqualTo("ownerId", userId)
            .whereEqualTo("deletedAt", null)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val quizzes = snapshot?.toObjects(Quiz::class.java) ?: emptyList()
                trySend(quizzes)
                
                // Cache vào Room cho ngoại tuyến
                launch { quizDao.insertAll(quizzes.map { it.toEntity() }) }
            }
        awaitClose { listener.remove() }
    }.catch {
        // Fallback về cache Room khi ngoại tuyến
        emitAll(quizDao.getAllQuizzes().map { it.map { e -> e.toDomain() } })
    }
    
    // Lấy quiz theo mã chia sẻ
    suspend fun getQuizByShareCode(code: String): Quiz? {
        val codeDoc = firestore.collection("shareCodes")
            .document(code).get().await()
        if (!codeDoc.exists()) return null
        
        val quizId = codeDoc.getString("quizId") ?: return null
        return quizzesRef.document(quizId).get().await().toObject(Quiz::class.java)
    }
    
    // Tạo quiz với tất cả câu hỏi
    suspend fun createQuiz(quiz: Quiz, questions: List<QuestionDraft>): Result<String> {
        return try {
            val batch = firestore.batch()
            val quizRef = quizzesRef.document()
            
            // Tính checksum
            val checksum = ChecksumUtil.computeChecksum(quiz.title, questions)
            
            val quizData = quiz.copy(
                id = quizRef.id,
                shareCode = generateShareCode(),
                checksum = checksum,
                questionCount = questions.size
            )
            batch.set(quizRef, quizData)
            
            // Tạo tra cứu mã chia sẻ
            quizData.shareCode?.let { code ->
                val codeRef = firestore.collection("shareCodes").document(code)
                batch.set(codeRef, mapOf("quizId" to quizRef.id))
            }
            
            // Tạo câu hỏi
            questions.forEachIndexed { idx, q ->
                val qRef = quizRef.collection("questions").document()
                batch.set(qRef, q.copy(id = qRef.id, position = idx))
                
                // Tạo lựa chọn
                q.choices.forEachIndexed { cIdx, c ->
                    val cRef = qRef.collection("choices").document()
                    batch.set(cRef, c.copy(id = cRef.id, position = cIdx))
                }
            }
            
            batch.commit().await()
            Result.success(quizRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun generateShareCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6).map { chars.random() }.joinToString("")
    }
}
```

---

## 6. Hỗ Trợ Ngoại Tuyến

### 6.1 Firestore Offline Persistence

```kotlin
// Bật trong lớp Application
class QuizCodeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Bật offline persistence của Firestore (mặc định đã bật)
        Firebase.firestore.firestoreSettings = firestoreSettings {
            isPersistenceEnabled = true
            cacheSizeBytes = FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED
        }
    }
}
```

### 6.2 Theo Dõi Mạng

```kotlin
class NetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val connectivityManager = 
        context.getSystemService<ConnectivityManager>()!!
    
    val isOnline: StateFlow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(true) }
            override fun onLost(network: Network) { trySend(false) }
        }
        connectivityManager.registerDefaultNetworkCallback(callback)
        trySend(connectivityManager.activeNetwork != null)
        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }.stateIn(CoroutineScope(Dispatchers.IO), SharingStarted.Eagerly, false)
}
```

### 6.3 Chế Độ Tiết Kiệm Dữ Liệu

```kotlin
class SyncPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val dataSaverKey = booleanPreferencesKey("data_saver_enabled")
    
    val dataSaverEnabled: Flow<Boolean> = dataStore.data
        .map { it[dataSaverKey] ?: false }
    
    suspend fun setDataSaverEnabled(enabled: Boolean) {
        dataStore.edit { it[dataSaverKey] = enabled }
    }
}
```

---

## 7. Nhập CSV/Excel

### 7.1 Quy Trình Nhập

```mermaid
sequenceDiagram
    participant User as Người Dùng
    participant UI as UI
    participant Importer as Importer
    participant Validator as Validator
    participant Firestore as Firestore

    User->>UI: Chọn file
    UI->>Importer: Parse file
    Importer->>Validator: Xác thực các dòng
    Validator-->>Importer: Kết quả xác thực
    Importer-->>UI: Xem trước (thành công/lỗi)
    User->>UI: Nhập metadata quiz
    User->>UI: Xác nhận nhập
    UI->>Firestore: Tạo quiz + câu hỏi
    Firestore-->>UI: Thành công
    UI-->>User: Hiển thị tóm tắt
```

<details>
<summary>📊 Xem dạng Text (nếu Mermaid không hiển thị)</summary>

```
Người Dùng      UI          Importer      Validator      Firestore
     │           │              │              │              │
     │──Chọn────►│              │              │              │
     │   file    │              │              │              │
     │           │──Parse file─►│              │              │
     │           │              │──Xác thực───►│              │
     │           │              │◄──Kết quả────│              │
     │           │◄──Xem trước──│              │              │
     │──Nhập────►│              │              │              │
     │  metadata │              │              │              │
     │──Xác─────►│              │              │              │
     │  nhận     │──────────Tạo quiz──────────►│              │
     │           │◄─────────Thành công─────────│              │
     │◄──Tóm tắt─│              │              │              │
```

</details>

### 7.2 Bộ Phân Tích CSV

```kotlin
class CsvImporter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val requiredColumns = setOf(
        "question", "option_0", "option_1", "option_2", "option_3", "correct_option"
    )
    
    suspend fun import(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val questions = mutableListOf<QuestionDraft>()
        val errors = mutableListOf<ImportError>()
        
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val reader = BufferedReader(InputStreamReader(stream))
            val headers = reader.readLine()?.split(",") ?: return@use
            
            val missingHeaders = requiredColumns - headers.toSet()
            if (missingHeaders.isNotEmpty()) {
                return@withContext ImportResult(
                    emptyList(),
                    listOf(ImportError(0, "Thiếu cột: $missingHeaders"))
                )
            }
            
            var lineNumber = 1
            reader.forEachLine { line ->
                lineNumber++
                try {
                    questions.add(parseQuestion(headers, line.split(",")))
                } catch (e: Exception) {
                    errors.add(ImportError(lineNumber, e.message ?: "Lỗi parse"))
                }
            }
        }
        ImportResult(questions, errors)
    }
}
```

---

## 8. Kho Câu Hỏi & Tạo Tự Động

### 8.1 Pool Repository

```kotlin
class PoolRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val poolRef = firestore.collection("questionPool")
    
    suspend fun contributeQuestions(
        questions: List<Question>,
        sourceQuizId: String,
        isAnonymized: Boolean
    ) {
        val batch = firestore.batch()
        questions.forEach { q ->
            val poolDoc = poolRef.document()
            batch.set(poolDoc, PoolQuestion(
                id = poolDoc.id,
                content = q.content,
                choices = q.choices.map { PoolChoice(it.content, it.isCorrect) },
                correctIndex = q.choices.indexOfFirst { it.isCorrect },
                tags = q.tags,
                mediaUrl = q.mediaUrl,
                points = q.points,
                sourceQuizId = sourceQuizId,
                contributorId = if (isAnonymized) null else Firebase.auth.currentUser?.uid,
                isActive = true
            ))
        }
        batch.commit().await()
    }
    
    suspend fun getRandomQuestions(
        tags: List<String>,
        count: Int
    ): List<PoolQuestion> {
        return poolRef
            .whereEqualTo("isActive", true)
            .whereArrayContainsAny("tags", tags)
            .limit(count.toLong() * 3) // Lấy nhiều hơn, rồi lọc
            .get()
            .await()
            .toObjects(PoolQuestion::class.java)
            .shuffled()
            .take(count)
    }
}
```
