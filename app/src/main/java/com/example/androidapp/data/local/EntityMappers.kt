package com.example.androidapp.data.local

import com.example.androidapp.data.local.entity.AttemptEntity
import com.example.androidapp.data.local.entity.ChoiceEntity
import com.example.androidapp.data.local.entity.QuestionEntity
import com.example.androidapp.data.local.entity.QuizEntity
import com.example.androidapp.data.local.entity.SyncStatus
import com.example.androidapp.data.local.entity.UserEntity
import com.example.androidapp.domain.model.Attempt
import com.example.androidapp.domain.model.Choice
import com.example.androidapp.domain.model.Question
import com.example.androidapp.domain.model.Quiz
import com.example.androidapp.domain.model.AdminPermission
import com.example.androidapp.domain.model.User
import com.example.androidapp.domain.model.UserRole
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

private val gson = Gson()

// --- QUIZ ---

/** Maps [QuizEntity] to domain [Quiz]. Tags are stored as comma-separated string. */
fun QuizEntity.toDomain(): Quiz = Quiz(
    id = id,
    ownerId = ownerId,
    title = title,
    description = description,
    authorName = authorName,
    thumbnailUrl = thumbnailUrl,
    tags = if (tags.isBlank()) emptyList() else tags.split(",").map { it.trim() },
    questionCount = questionCount,
    attemptCount = attemptCount,
    isPublic = isPublic,
    isDraft = isDraft,
    shareCode = shareCode,
    checksum = checksum,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    isRemovedFromCloud = isRemovedFromCloud
)

/**
 * Maps domain [Quiz] to [QuizEntity] for Room storage.
 *
 * **Warning:** This mapper sets [QuizEntity.embedding] to `null` and
 * [QuizEntity.embeddingVersion] to `0`. Using `@Upsert` with the result
 * will overwrite locally-computed embeddings. Prefer [QuizDao.updateQuizMetadata]
 * for updates that should preserve embeddings.
 *
 * @see com.example.androidapp.data.local.dao.QuizDao.updateQuizMetadata
 */
fun Quiz.toEntity(syncStatus: String = SyncStatus.SYNCED.name): QuizEntity = QuizEntity(
    id = id,
    ownerId = ownerId,
    title = title,
    description = description,
    authorName = authorName,
    thumbnailUrl = thumbnailUrl,
    isPublic = isPublic,
    isDraft = isDraft,
    shareCode = shareCode,
    tags = tags.joinToString(","),
    checksum = checksum,
    questionCount = questionCount,
    attemptCount = attemptCount,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    // Embeddings are computed locally by EmbeddingIndexWorker and are not part
    // of the Quiz domain model. Setting null here means @Upsert will overwrite
    // existing embeddings on sync. The worker automatically re-indexes affected
    // quizzes in the next batch pass (triggered at app startup and after sync).
    embedding = null,
    embeddingVersion = 0,
    syncStatus = syncStatus,
    isRemovedFromCloud = isRemovedFromCloud
)

// --- QUESTION ---

/** Maps [QuestionEntity] to domain [Question] (choices must be provided separately). */
fun QuestionEntity.toDomain(choices: List<Choice> = emptyList()): Question = Question(
    id = id,
    quizId = quizId,
    content = content,
    choices = choices,
    isMultiSelect = allowMultipleCorrect,
    explanation = explanation,
    mediaUrl = mediaUrl,
    points = points,
    position = position
)

/** Maps domain [Question] to [QuestionEntity] for Room storage. */
fun Question.toEntity(): QuestionEntity = QuestionEntity(
    id = id,
    quizId = quizId,
    content = content,
    mediaUrl = mediaUrl,
    explanation = explanation,
    points = points,
    position = position,
    choiceCount = choices.size,
    allowMultipleCorrect = isMultiSelect
)

// --- CHOICE ---

/** Maps [ChoiceEntity] to domain [Choice]. */
fun ChoiceEntity.toDomain(): Choice = Choice(
    id = id,
    content = content,
    isCorrect = isCorrect,
    position = position
)

/** Maps domain [Choice] to [ChoiceEntity] for Room storage. */
fun Choice.toEntity(questionId: String): ChoiceEntity = ChoiceEntity(
    id = id,
    questionId = questionId,
    content = content,
    isCorrect = isCorrect,
    position = position
)

// --- ATTEMPT ---

/** Maps [AttemptEntity] to domain [Attempt]. Multi-answers JSON is preferred over single-answer. */
fun AttemptEntity.toDomain(): Attempt {
    val multiAnswerType = object : TypeToken<Map<String, List<String>>>() {}.type
    val answers: Map<String, List<String>> = if (multiAnswers.isNotBlank()) {
        try {
            gson.fromJson(multiAnswers, multiAnswerType) ?: emptyMap()
        } catch (_: Exception) {
            emptyMap()
        }
    } else emptyMap()

    val questionOrder: List<String> = questionOrder.split(",").filter { it.isNotBlank() }

    return Attempt(
        id = id,
        userId = userId,
        quizId = quizId,
        score = score,
        maxScore = maxScore,
        answers = answers,
        startTimeMillis = startedAt,
        endTimeMillis = finishedAt,
        questionOrder = questionOrder
    )
}

/** Maps domain [Attempt] to [AttemptEntity] for Room storage. */
fun Attempt.toEntity(): AttemptEntity = AttemptEntity(
    id = id,
    userId = userId,
    quizId = quizId,
    score = score,
    maxScore = maxScore,
    multiAnswers = gson.toJson(answers),
    startedAt = startTimeMillis,
    finishedAt = endTimeMillis,
    questionOrder = questionOrder.joinToString(",")
)

// --- USER ---

/** Maps [UserEntity] to domain [User]. Parses permissions from comma-separated string. */
fun UserEntity.toDomain(): User = User(
    id = id,
    email = email,
    displayName = displayName ?: username,
    username = username,
    photoUrl = photoUrl,
    role = UserRole.fromString(role),
    isBanned = deletedAt != null,
    permissions = if (permissions.isBlank()) {
        emptySet()
    } else {
        permissions.split(",").mapNotNull { AdminPermission.fromString(it.trim()) }.toSet()
    }
)

/**
 * Maps domain [User] to [UserEntity] for Room storage.
 * Serializes permissions as comma-separated string.
 *
 * @param existingEntity Optional existing entity to preserve fields not present
 *   in the domain model (e.g. [UserEntity.createdAt]). When null, [createdAt]
 *   defaults to [System.currentTimeMillis].
 */
fun User.toEntity(existingEntity: UserEntity? = null): UserEntity = UserEntity(
    id = id,
    username = username,
    email = email,
    displayName = displayName,
    photoUrl = photoUrl,
    role = role.toStorageValue(),
    permissions = permissions.joinToString(",") { it.toStorageValue() },
    createdAt = existingEntity?.createdAt ?: System.currentTimeMillis(),
    deletedAt = if (isBanned) (existingEntity?.deletedAt ?: System.currentTimeMillis()) else null
)
