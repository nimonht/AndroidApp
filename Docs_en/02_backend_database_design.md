# Backend & Database Design (Firebase + Kotlin)

## 1. Firebase Architecture

### 1.1 Firebase Services Used

The app uses Firebase Authentication, Cloud Firestore, and (optionally) Cloud Functions. Firebase Storage rules exist as scaffolding but the SDK is not yet integrated.

> See [01_project_overview.md, Section 2](01_project_overview.md#2-tech-stack) for the full tech stack table including all Firebase services and their usage.

### 1.2 Architecture Diagram

The app follows a Clean Architecture + MVVM pattern: UI (Compose) -> ViewModel -> Repository -> Data Sources (Room offline cache + Firebase SDK cloud sync).

> See [01_project_overview.md, Section 3](01_project_overview.md#3-system-architecture) for the architecture diagram.

---

## 2. Firestore Database Structure

### 2.1 Collections Overview

```
firestore-root/
├── users/
│   └── {userId}/
│       ├── username: string
│       ├── email: string
│       ├── displayName: string
│       ├── createdAt: timestamp
│       └── deletedAt: timestamp | null
│
├── quizzes/
│   └── {quizId}/
│       ├── ownerId: string (userId)
│       ├── title: string
│       ├── description: string
│       ├── isPublic: boolean
│       ├── shareCode: string (6 chars)
│       ├── tags: array<string>
│       ├── checksum: string
│       ├── questionCount: number
│       ├── attemptCount: number
│       ├── createdAt: timestamp
│       ├── updatedAt: timestamp
│       ├── deletedAt: timestamp | null
│       │
│       └── questions/ (subcollection)
│           └── {questionId}/
│               ├── content: string
│               ├── mediaUrl: string | null
│               ├── explanation: string | null
│               ├── points: number
│               ├── position: number
│               ├── choiceCount: number (2-10, flexible)
│               ├── allowMultipleCorrect: boolean
│               │
│               └── choices/ (subcollection) [2-10 items]
│                   └── {choiceId}/
│                       ├── content: string
│                       ├── isCorrect: boolean
│                       └── position: number
│
├── attempts/
│   └── {attemptId}/
│       ├── quizId: string
│       ├── userId: string | "guest_xxx"
│       ├── questionOrder: array<string>
│       ├── choiceOrders: map<questionId, array<choiceId>>
│       ├── answers: map<questionId, choiceId> (single answer)
│       ├── multiAnswers: map<questionId, array<choiceId>> (multiple answers)
│       ├── score: number
│       ├── maxScore: number
│       ├── startedAt: timestamp
│       └── finishedAt: timestamp | null
│
├── questionPool/
│   └── {poolId}/
│       ├── content: string
│       ├── choices: array<{content, isCorrect}> (2-10 items)
│       ├── correctIndices: array<number> (supports multiple correct)
│       ├── tags: array<string>
│       ├── mediaUrl: string | null
│       ├── points: number
│       ├── allowMultipleCorrect: boolean
│       ├── contributorId: string | null (null if anonymized)
│       ├── sourceQuizId: string
│       ├── isActive: boolean
│       ├── usageCount: number
│       └── createdAt: timestamp
│
└── shareCodes/ (for quick lookup)
    └── {shareCode}/
        └── quizId: string
```

### 2.2 Firestore Security Rules

> **Source of truth:** [`firestore.rules`](../firestore.rules) in the repository root.
> Do not duplicate the full rules here -- they will drift. Always consult the file directly for the authoritative version.

Key points of the current security rules:

- **Helper functions** -- reusable checks (`isSignedIn`, `isOwner`, `isAdmin`, `isQuizOwner`, `notDeleted`, `isQuizAccessible`) keep the rules DRY. The `isAdmin()` helper reads the caller's Firestore user document and verifies `role == 'admin'` and that the account is not soft-deleted.
- **Admin role** -- admins have elevated read/write access across all collections (users, quizzes, questions, choices, attempts). Admin checks are enforced in the rules themselves, not only in application code.
- **Users** -- any authenticated user can read profiles. Users can create their own profile and update it, but **cannot** change their own `role` or `deletedAt` fields (prevents self-escalation). Admins can update or delete any user profile.
- **Quizzes** -- owners can always read their quizzes (including soft-deleted ones for trash/restore). Non-owners can read non-deleted quizzes that are public or shared via share code. Only the owner can create (must set `ownerId` to their own UID), update, or delete. An exception allows **any** authenticated user to increment `attemptCount` by exactly 1 (for quiz-taking). Admins can update/delete any quiz but `ownerId` remains immutable.
- **Questions & Choices** (subcollections of quizzes) -- readable by the quiz owner, anyone with access to an accessible quiz, or admins. Writable only by the quiz owner or admins.
- **Attempts** -- readable by the attempt owner, the quiz owner, or admins. Authenticated users create attempts for themselves; guest attempts (UID matching `guest_*`) are also allowed. Only the attempt owner can update. Deletion is allowed for the attempt owner, quiz owner, or admins.
- **Share codes** -- readable by anyone (for quiz joining). **Any authenticated user** can create, update, or delete share codes. Share codes are generated client-side by `ShareCodeUtil`, not by Cloud Functions.
- **Question pool** -- readable by anyone. Authenticated users can contribute (create). Only the contributor can update or delete their own entries; anonymous contributions (no `contributorId`) can be modified by any authenticated user.

---

## 3. Firebase Setup in Android (Kotlin)

### 3.1 Dependencies (build.gradle.kts)

```kotlin
plugins {
    id("com.google.gms.google-services")
}

dependencies {
    // Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    
    // Firebase services
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")  // NOTE: not in actual build.gradle.kts — Firebase Storage is not currently implemented
    
    // Coroutines support
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
}
```

### 3.2 Data Models

```kotlin
// User model
data class User(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val displayName: String? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val deletedAt: Timestamp? = null
)

// Quiz model
data class Quiz(
    val id: String = "",
    val ownerId: String = "",
    val title: String = "",
    val description: String? = null,
    val isPublic: Boolean = false,
    val shareCode: String? = null,
    val tags: List<String> = emptyList(),
    val checksum: String? = null,
    val questionCount: Int = 0,
    val attemptCount: Int = 0,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val deletedAt: Timestamp? = null
)

// Question model
data class Question(
    val id: String = "",
    val content: String = "",
    val mediaUrl: String? = null,
    val explanation: String? = null,
    val points: Int = 1,
    val position: Int = 0,
    val minChoices: Int = 2,  // Minimum choices allowed
    val maxChoices: Int = 10, // Maximum choices allowed
    val allowMultipleCorrect: Boolean = false, // Support multiple correct answers
    val choices: List<Choice> = emptyList() // Loaded separately (2-10 choices, flexible)
)

// Choice model
data class Choice(
    val id: String = "",
    val content: String = "",
    val isCorrect: Boolean = false,
    val position: Int = 0
)

// Attempt model
data class Attempt(
    val id: String = "",
    val quizId: String = "",
    val userId: String = "",
    val questionOrder: List<String> = emptyList(),
    val choiceOrders: Map<String, List<String>> = emptyMap(),
    // Single answer per question (for backward compatibility)
    val answers: Map<String, String> = emptyMap(),
    // Multiple answers per question (for questions with allowMultipleCorrect=true)
    val multiAnswers: Map<String, List<String>> = emptyMap(),
    val score: Int = 0,
    val maxScore: Int = 0,
    val startedAt: Timestamp = Timestamp.now(),
    val finishedAt: Timestamp? = null
)
```

> **Implementation Note:** The data models above represent the original design-phase schemas. The actual implementation uses Room entities (`data/local/entity/`) for local storage and Firestore DTOs (`data/remote/model/`) for cloud sync, both mapped to domain models (`domain/model/`) via extension functions in `EntityMappers.kt` and `AppMappers.kt`. Refer to the source code for the latest field definitions.

---

## 4. Repository Implementation

### 4.1 Authentication Repository

```kotlin
class AuthRepository() {
    private val auth = Firebase.auth
    
    val currentUser: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }
    
    suspend fun signUp(email: String, password: String, username: String): Result<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user!!
            
            // Create user document in Firestore
            val user = User(
                id = firebaseUser.uid,
                username = username,
                email = email,
                displayName = username
            )
            Firebase.firestore.collection("users")
                .document(firebaseUser.uid)
                .set(user)
                .await()
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun signOut() = auth.signOut()
}
```

### 4.2 Quiz Repository

```kotlin
class QuizRepository(
    private val quizDao: QuizDao // Room DAO for offline cache
) {
    private val db = Firebase.firestore
    private val quizzesRef = db.collection("quizzes")
    
    // Get user's quizzes (with offline support)
    fun getMyQuizzes(userId: String): Flow<List<Quiz>> = callbackFlow {
        val listener = quizzesRef
            .whereEqualTo("ownerId", userId)
            .whereEqualTo("deletedAt", null)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val quizzes = snapshot?.toObjects(Quiz::class.java) ?: emptyList()
                trySend(quizzes)
                
                // Cache to Room
                launch { quizDao.insertAll(quizzes.map { it.toEntity() }) }
            }
        awaitClose { listener.remove() }
    }.catch {
        // Fallback to local cache if offline
        emitAll(quizDao.getAllQuizzes().map { it.map { e -> e.toDomain() } })
    }
    
    // Get quiz by share code
    suspend fun getQuizByShareCode(code: String): Quiz? {
        val codeDoc = db.collection("shareCodes").document(code).get().await()
        if (!codeDoc.exists()) return null
        
        val quizId = codeDoc.getString("quizId") ?: return null
        return quizzesRef.document(quizId).get().await().toObject(Quiz::class.java)
    }
    
    // Create quiz with questions
    suspend fun createQuiz(quiz: Quiz, questions: List<Question>): Result<String> {
        return try {
            val batch = db.batch()
            
            // Create quiz document
            val quizRef = quizzesRef.document()
            val quizWithId = quiz.copy(
                id = quizRef.id,
                shareCode = generateShareCode(),
                questionCount = questions.size
            )
            batch.set(quizRef, quizWithId)
            
            // Create share code lookup
            quizWithId.shareCode?.let { code ->
                val codeRef = db.collection("shareCodes").document(code)
                batch.set(codeRef, mapOf("quizId" to quizRef.id))
            }
            
            // Create questions and choices
            questions.forEachIndexed { index, question ->
                val questionRef = quizRef.collection("questions").document()
                val questionWithId = question.copy(id = questionRef.id, position = index)
                batch.set(questionRef, questionWithId.toFirestoreMap())
                
                question.choices.forEachIndexed { choiceIndex, choice ->
                    val choiceRef = questionRef.collection("choices").document()
                    batch.set(choiceRef, choice.copy(id = choiceRef.id, position = choiceIndex))
                }
            }
            
            batch.commit().await()
            Result.success(quizRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Soft delete (30-day retention)
    suspend fun deleteQuiz(quizId: String): Result<Unit> {
        return try {
            quizzesRef.document(quizId)
                .update("deletedAt", Timestamp.now())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Restore from recycle bin
    suspend fun restoreQuiz(quizId: String): Result<Unit> {
        return try {
            quizzesRef.document(quizId)
                .update("deletedAt", FieldValue.delete())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Get quiz with all questions and choices
    suspend fun getQuizWithQuestions(quizId: String): QuizWithQuestions {
        val quiz = quizzesRef.document(quizId).get().await().toObject(Quiz::class.java)!!
        
        val questionsSnapshot = quizzesRef.document(quizId)
            .collection("questions")
            .orderBy("position")
            .get()
            .await()
        
        val questions = questionsSnapshot.documents.map { doc ->
            val question = doc.toObject(Question::class.java)!!
            
            val choicesSnapshot = doc.reference.collection("choices")
                .orderBy("position")
                .get()
                .await()
            
            question.copy(
                choices = choicesSnapshot.toObjects(Choice::class.java)
            )
        }
        
        return QuizWithQuestions(quiz, questions)
    }
    
    private fun generateShareCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6).map { chars.random() }.joinToString("")
    }
}

data class QuizWithQuestions(
    val quiz: Quiz,
    val questions: List<Question>
)
```

### 4.3 Attempt Repository

```kotlin
class AttemptRepository() {
    private val db = Firebase.firestore
    private val attemptsRef = db.collection("attempts")
    
    suspend fun createAttempt(
        quizId: String,
        userId: String?,
        questions: List<Question>
    ): Attempt {
        // Shuffle questions
        val shuffledQuestions = questions.shuffled()
        val questionOrder = shuffledQuestions.map { it.id }
        
        // Shuffle choices for each question
        val choiceOrders = shuffledQuestions.associate { q ->
            q.id to q.choices.shuffled().map { it.id }
        }
        
        val attempt = Attempt(
            quizId = quizId,
            userId = userId ?: "guest_${UUID.randomUUID()}",
            questionOrder = questionOrder,
            choiceOrders = choiceOrders,
            maxScore = questions.sumOf { it.points }
        )
        
        val docRef = attemptsRef.document()
        val attemptWithId = attempt.copy(id = docRef.id)
        docRef.set(attemptWithId).await()
        
        return attemptWithId
    }
    
    suspend fun submitAttempt(
        attemptId: String,
        answers: Map<String, String>,
        score: Int
    ) {
        attemptsRef.document(attemptId).update(
            mapOf(
                "answers" to answers,
                "score" to score,
                "finishedAt" to Timestamp.now()
            )
        ).await()
        
        // Increment quiz attempt count
        val attempt = attemptsRef.document(attemptId).get().await()
            .toObject(Attempt::class.java)!!
        
        db.collection("quizzes").document(attempt.quizId)
            .update("attemptCount", FieldValue.increment(1))
            .await()
    }
    
    fun getMyAttempts(userId: String): Flow<List<Attempt>> = callbackFlow {
        val listener = attemptsRef
            .whereEqualTo("userId", userId)
            .orderBy("startedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.toObjects(Attempt::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }
}
```

---

## 5. Sync with Checksum Verification

### 5.1 Checksum Utility

```kotlin
object ChecksumUtil {
    fun computeQuizChecksum(quiz: Quiz, questions: List<Question>): String {
        val data = buildString {
            append(quiz.title)
            append(quiz.description ?: "")
            questions.sortedBy { it.position }.forEach { q ->
                append(q.content)
                append(q.mediaUrl ?: "")
                q.choices.sortedBy { it.position }.forEach { c ->
                    append(c.content)
                    append(c.isCorrect)
                }
            }
        }
        
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(data.toByteArray())
            .fold("") { str, byte -> str + "%02x".format(byte) }
    }
}
```

### 5.2 Sync Manager

```kotlin
class SyncManager(
    private val quizRepository: QuizRepository,
    private val quizDao: QuizDao,
    private val workManager: WorkManager
) {
    // Check if local and remote are in sync
    suspend fun verifySyncStatus(quizId: String): SyncStatus {
        val localQuiz = quizDao.getQuizById(quizId) ?: return SyncStatus.NOT_FOUND
        val remoteQuiz = quizRepository.getQuizWithQuestions(quizId)
        
        val localChecksum = localQuiz.checksum
        val remoteChecksum = ChecksumUtil.computeQuizChecksum(
            remoteQuiz.quiz, 
            remoteQuiz.questions
        )
        
        return if (localChecksum == remoteChecksum) {
            SyncStatus.SYNCED
        } else {
            SyncStatus.OUT_OF_SYNC
        }
    }
    
    // Upload local changes to Firebase
    suspend fun uploadQuiz(quizId: String): Result<Unit> {
        val localData = quizDao.getQuizWithQuestions(quizId)
        val checksum = ChecksumUtil.computeQuizChecksum(
            localData.quiz.toDomain(),
            localData.questions.map { it.toDomain() }
        )
        
        return try {
            quizRepository.updateQuiz(
                localData.quiz.toDomain().copy(checksum = checksum),
                localData.questions.map { it.toDomain() }
            )
            
            // Verify upload
            val remoteData = quizRepository.getQuizWithQuestions(quizId)
            val remoteChecksum = remoteData.quiz.checksum
            
            if (checksum == remoteChecksum) {
                quizDao.updateSyncStatus(quizId, SyncStatus.SYNCED)
                Result.success(Unit)
            } else {
                quizDao.updateSyncStatus(quizId, SyncStatus.FAILED)
                Result.failure(ChecksumMismatchException())
            }
        } catch (e: Exception) {
            quizDao.updateSyncStatus(quizId, SyncStatus.FAILED)
            Result.failure(e)
        }
    }
}

enum class SyncStatus {
    PENDING, SYNCING, SYNCED, FAILED, NOT_FOUND, OUT_OF_SYNC
}
```

---

## 6. Cloud Functions

The project uses a single Cloud Function defined in `functions/src/index.ts`.

### 6.1 `deleteUserAuth` (Callable)

An HTTPS callable function that deletes a user from Firebase Authentication. This exists because Firestore security rules cannot trigger Auth account deletion -- only server-side code with the Admin SDK can do that.

**Caller requirements:**
- Must be authenticated.
- Must have `role == "admin"` in their Firestore `/users/{uid}` document.

**Input:** `{ userId: string }` -- the UID of the user to remove from Firebase Auth.

**Behavior:**
1. Verifies the caller is authenticated; throws `unauthenticated` otherwise.
2. Reads the caller's Firestore user document and checks `role == "admin"`; throws `permission-denied` if not.
3. Validates that `userId` is a non-empty string; throws `invalid-argument` if missing.
4. Prevents self-deletion (caller cannot delete their own account via this function).
5. Calls `admin.auth().deleteUser(targetUserId)`.
6. If the target user is already absent from Auth (`auth/user-not-found`), returns success anyway.

**Returns:** `{ success: true }` on success, or throws an `HttpsError`.

> **Note:** Share code generation and soft-deleted quiz cleanup are handled client-side (via `ShareCodeUtil` and the recycle bin UI), not by Cloud Functions. There are no scheduled or Firestore-triggered functions in this project.

---

## 7. Local Room Database (Offline Cache)

### 7.1 Room Entities (for offline support)

```kotlin
@Entity(tableName = "quizzes_cache")
data class QuizEntity(
    @PrimaryKey val id: String,
    val ownerId: String,
    val title: String,
    val description: String?,
    val isPublic: Boolean,
    val shareCode: String?,
    val tags: String, // JSON array
    val checksum: String?,
    val syncStatus: String = "SYNCED",
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null
)

@Entity(tableName = "questions_cache")
data class QuestionEntity(
    @PrimaryKey val id: String,
    val quizId: String,
    val content: String,
    val mediaUrl: String?,
    val explanation: String?,
    val points: Int,
    val position: Int
)

@Entity(tableName = "choices_cache")  
data class ChoiceEntity(
    @PrimaryKey val id: String,
    val questionId: String,
    val content: String,
    val isCorrect: Boolean,
    val position: Int
)

@Dao
interface QuizDao {
    @Query("SELECT * FROM quizzes_cache WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun getAllQuizzes(): Flow<List<QuizEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(quizzes: List<QuizEntity>)
    
    @Query("UPDATE quizzes_cache SET syncStatus = :status WHERE id = :quizId")
    suspend fun updateSyncStatus(quizId: String, status: String)
}
```

---