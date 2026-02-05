# Thiết Kế Backend & Cơ Sở Dữ Liệu (Firebase + Kotlin)

## 1. Kiến Trúc Firebase

### 1.1 Các Dịch Vụ Firebase Sử Dụng

| Dịch Vụ | Mục Đích |
|---------|----------|
| **Firebase Authentication** | Đăng nhập/đăng ký người dùng (Email, Google) |
| **Cloud Firestore** | Cơ sở dữ liệu NoSQL cho quiz, người dùng, lượt làm bài |
| **Firebase Storage** | File media (hình ảnh, video) |
| **Cloud Functions** (tùy chọn) | Logic phía server (tạo mã chia sẻ, dọn dẹp) |

### 1.2 Sơ Đồ Kiến Trúc

```
┌─────────────────────────────────────────────────────────────┐
│                   Ứng Dụng QuizCode Android                  │
│                       (Kotlin)                               │
├─────────────────────────────────────────────────────────────┤
│  Tầng UI (Jetpack Compose)                                   │
│         ↓                                                    │
│  Tầng ViewModel                                              │
│         ↓                                                    │
│  Tầng Repository                                             │
│         ↓                                                    │
│  ┌──────────────────┐    ┌──────────────────┐               │
│  │    Room DB       │    │  Firebase SDK    │               │
│  │ (Cache Ngoại     │    │  (Đồng Bộ        │               │
│  │  Tuyến)          │    │   Đám Mây)       │               │
│  └────────┬─────────┘    └────────┬─────────┘               │
└───────────┼───────────────────────┼─────────────────────────┘
            │                       │
    ┌───────▼───────┐       ┌───────▼───────┐
    │  Local SQLite │       │   Firebase    │
    │    Storage    │       │    Cloud      │
    └───────────────┘       └───────────────┘
                                    │
                    ┌───────────────┼───────────────┐
                    ▼               ▼               ▼
              ┌──────────┐   ┌──────────┐   ┌──────────┐
              │Firestore │   │  Auth    │   │ Storage  │
              │(Database)│   │(Đăng     │   │(Media)   │
              │          │   │ nhập)    │   │          │
              └──────────┘   └──────────┘   └──────────┘
```

---

## 2. Cấu Trúc Cơ Sở Dữ Liệu Firestore

### 2.1 Tổng Quan Collections

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
│       ├── shareCode: string (6 ký tự)
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
│               ├── choiceCount: number (2-10, linh hoạt)
│               ├── allowMultipleCorrect: boolean
│               │
│               └── choices/ (subcollection) [2-10 mục]
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
│       ├── answers: map<questionId, choiceId> (đáp án đơn)
│       ├── multiAnswers: map<questionId, array<choiceId>> (nhiều đáp án)
│       ├── score: number
│       ├── maxScore: number
│       ├── startedAt: timestamp
│       └── finishedAt: timestamp | null
│
├── questionPool/
│   └── {poolId}/
│       ├── content: string
│       ├── choices: array<{content, isCorrect}> (2-10 mục)
│       ├── correctIndices: array<number> (hỗ trợ nhiều đáp án đúng)
│       ├── tags: array<string>
│       ├── mediaUrl: string | null
│       ├── points: number
│       ├── allowMultipleCorrect: boolean
│       ├── contributorId: string | null (null nếu ẩn danh)
│       ├── sourceQuizId: string
│       ├── isActive: boolean
│       ├── usageCount: number
│       └── createdAt: timestamp
│
└── shareCodes/ (để tra cứu nhanh)
    └── {shareCode}/
        └── quizId: string
```

### 2.2 Quy Tắc Bảo Mật Firestore

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Collection users
    match /users/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth.uid == userId;
    }
    
    // Collection quizzes
    match /quizzes/{quizId} {
      // Ai cũng có thể đọc quiz công khai
      allow read: if resource.data.isPublic == true 
                  || request.auth.uid == resource.data.ownerId;
      
      // Chỉ chủ sở hữu mới được ghi
      allow create: if request.auth != null;
      allow update, delete: if request.auth.uid == resource.data.ownerId;
      
      // Subcollection questions
      match /questions/{questionId} {
        allow read: if get(/databases/$(database)/documents/quizzes/$(quizId)).data.isPublic == true
                    || request.auth.uid == get(/databases/$(database)/documents/quizzes/$(quizId)).data.ownerId;
        allow write: if request.auth.uid == get(/databases/$(database)/documents/quizzes/$(quizId)).data.ownerId;
        
        // Subcollection choices
        match /choices/{choiceId} {
          allow read: if true; // Kế thừa từ parent
          allow write: if request.auth.uid == get(/databases/$(database)/documents/quizzes/$(quizId)).data.ownerId;
        }
      }
    }
    
    // Collection attempts
    match /attempts/{attemptId} {
      allow read: if request.auth.uid == resource.data.userId 
                  || request.auth.uid == get(/databases/$(database)/documents/quizzes/$(resource.data.quizId)).data.ownerId;
      allow create: if true; // Cho phép khách tạo attempt
      allow update: if request.auth.uid == resource.data.userId || resource.data.userId.matches('guest_.*');
    }
    
    // Tra cứu mã chia sẻ (chỉ đọc cho client)
    match /shareCodes/{code} {
      allow read: if true;
      allow write: if false; // Chỉ Cloud Functions mới được ghi
    }
    
    // Kho câu hỏi
    match /questionPool/{poolId} {
      allow read: if resource.data.isActive == true;
      allow create: if request.auth != null;
      allow update: if request.auth.uid == resource.data.contributorId;
    }
  }
}
```

---

## 3. Cài Đặt Firebase trong Android (Kotlin)

### 3.1 Dependencies (build.gradle.kts)

```kotlin
plugins {
    id("com.google.gms.google-services")
}

dependencies {
    // Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    
    // Các dịch vụ Firebase
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    
    // Hỗ trợ Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
}
```

### 3.2 Data Models

```kotlin
// Model User
data class User(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val displayName: String? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val deletedAt: Timestamp? = null
)

// Model Quiz
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

// Model Question
data class Question(
    val id: String = "",
    val content: String = "",
    val mediaUrl: String? = null,
    val explanation: String? = null,
    val points: Int = 1,
    val position: Int = 0,
    val minChoices: Int = 2,  // Số lựa chọn tối thiểu cho phép
    val maxChoices: Int = 10, // Số lựa chọn tối đa cho phép
    val allowMultipleCorrect: Boolean = false, // Hỗ trợ nhiều đáp án đúng
    val choices: List<Choice> = emptyList() // Load riêng (2-10 lựa chọn, linh hoạt)
)

// Model Choice
data class Choice(
    val id: String = "",
    val content: String = "",
    val isCorrect: Boolean = false,
    val position: Int = 0
)

// Model Attempt
data class Attempt(
    val id: String = "",
    val quizId: String = "",
    val userId: String = "",
    val questionOrder: List<String> = emptyList(),
    val choiceOrders: Map<String, List<String>> = emptyMap(),
    // Đáp án đơn cho mỗi câu hỏi (tương thích ngược)
    val answers: Map<String, String> = emptyMap(),
    // Nhiều đáp án cho mỗi câu hỏi (cho câu hỏi có allowMultipleCorrect=true)
    val multiAnswers: Map<String, List<String>> = emptyMap(),
    val score: Int = 0,
    val maxScore: Int = 0,
    val startedAt: Timestamp = Timestamp.now(),
    val finishedAt: Timestamp? = null
)

```

---

## 4. Triển Khai Repository

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
            
            // Tạo document user trong Firestore
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
    private val quizDao: QuizDao // Room DAO cho cache ngoại tuyến
) {
    private val db = Firebase.firestore
    private val quizzesRef = db.collection("quizzes")
    
    // Lấy quiz của người dùng (với hỗ trợ ngoại tuyến)
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
                
                // Cache vào Room
                launch { quizDao.insertAll(quizzes.map { it.toEntity() }) }
            }
        awaitClose { listener.remove() }
    }.catch {
        // Fallback về cache cục bộ nếu ngoại tuyến
        emitAll(quizDao.getAllQuizzes().map { it.map { e -> e.toDomain() } })
    }
    
    // Lấy quiz theo mã chia sẻ
    suspend fun getQuizByShareCode(code: String): Quiz? {
        val codeDoc = db.collection("shareCodes").document(code).get().await()
        if (!codeDoc.exists()) return null
        
        val quizId = codeDoc.getString("quizId") ?: return null
        return quizzesRef.document(quizId).get().await().toObject(Quiz::class.java)
    }
    
    // Tạo quiz với câu hỏi
    suspend fun createQuiz(quiz: Quiz, questions: List<Question>): Result<String> {
        return try {
            val batch = db.batch()
            
            // Tạo document quiz
            val quizRef = quizzesRef.document()
            val quizWithId = quiz.copy(
                id = quizRef.id,
                shareCode = generateShareCode(),
                questionCount = questions.size
            )
            batch.set(quizRef, quizWithId)
            
            // Tạo tra cứu mã chia sẻ
            quizWithId.shareCode?.let { code ->
                val codeRef = db.collection("shareCodes").document(code)
                batch.set(codeRef, mapOf("quizId" to quizRef.id))
            }
            
            // Tạo câu hỏi và lựa chọn
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
    
    // Xóa mềm (giữ lại 30 ngày)
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
    
    // Khôi phục từ thùng rác
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
    
    // Lấy quiz với tất cả câu hỏi và lựa chọn
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
        // Xáo trộn câu hỏi
        val shuffledQuestions = questions.shuffled()
        val questionOrder = shuffledQuestions.map { it.id }
        
        // Xáo trộn lựa chọn cho mỗi câu hỏi
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
        
        // Tăng số lượt làm bài của quiz
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

## 5. Đồng Bộ với Xác Minh Checksum

### 5.1 Tiện Ích Checksum

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

### 5.2 Quản Lý Đồng Bộ

```kotlin
class SyncManager(
    private val quizRepository: QuizRepository,
    private val quizDao: QuizDao,
    private val workManager: WorkManager
) {
    // Kiểm tra local và remote có đồng bộ không
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
    
    // Upload thay đổi cục bộ lên Firebase
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
            
            // Xác minh upload
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

## 6. Cloud Functions (Tùy Chọn - cho logic phía server)

### 6.1 Tự động dọn quiz đã xóa (30 ngày)

```javascript
// functions/index.js
const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

// Chạy hàng ngày lúc nửa đêm
exports.cleanupDeletedQuizzes = functions.pubsub
  .schedule('0 0 * * *')
  .onRun(async (context) => {
    const db = admin.firestore();
    const thirtyDaysAgo = new Date();
    thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);
    
    const snapshot = await db.collection('quizzes')
      .where('deletedAt', '<', thirtyDaysAgo)
      .get();
    
    const batch = db.batch();
    snapshot.docs.forEach(doc => {
      // Xóa quiz và subcollections
      batch.delete(doc.ref);
      // Lưu ý: Subcollections cần xóa đệ quy
    });
    
    await batch.commit();
    console.log(`Đã dọn ${snapshot.size} quizzes`);
  });

// Tạo mã chia sẻ duy nhất
exports.generateShareCode = functions.firestore
  .document('quizzes/{quizId}')
  .onCreate(async (snap, context) => {
    const db = admin.firestore();
    let code;
    let isUnique = false;
    
    while (!isUnique) {
      code = generateCode();
      const existing = await db.collection('shareCodes').doc(code).get();
      isUnique = !existing.exists;
    }
    
    await db.collection('shareCodes').doc(code).set({
      quizId: context.params.quizId
    });
    
    await snap.ref.update({ shareCode: code });
  });

function generateCode() {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
  return Array.from({ length: 6 }, () => 
    chars[Math.floor(Math.random() * chars.length)]
  ).join('');
}
```

---

## 7. Cơ Sở Dữ Liệu Room Cục Bộ (Cache Ngoại Tuyến)

### 7.1 Room Entities (cho hỗ trợ ngoại tuyến)

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

