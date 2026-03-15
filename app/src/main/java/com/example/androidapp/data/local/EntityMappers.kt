package com.example.androidapp.data.local

import com.example.androidapp.data.local.entity.AttemptEntity
import com.example.androidapp.data.local.entity.ChoiceEntity
import com.example.androidapp.data.local.entity.QuestionEntity
import com.example.androidapp.data.local.entity.QuizEntity
import com.example.androidapp.data.local.entity.UserEntity
import com.example.androidapp.domain.model.Attempt
import com.example.androidapp.domain.model.Choice
import com.example.androidapp.domain.model.Question
import com.example.androidapp.domain.model.Quiz
import com.example.androidapp.domain.model.User
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
    authorName = "",
    thumbnailUrl = null,
    tags = if (tags.isBlank()) emptyList() else tags.split(",").map { it.trim() },
    questionCount = questionCount,
    attemptCount = attemptCount,
    isPublic = isPublic,
    shareCode = shareCode,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

/** Maps domain [Quiz] to [QuizEntity] for Room storage. */
fun Quiz.toEntity(syncStatus: String = "SYNCED"): QuizEntity = QuizEntity(
    id = id,
    ownerId = ownerId,
    title = title,
    description = description,
    isPublic = isPublic,
    shareCode = shareCode,
    tags = tags.joinToString(","),
    questionCount = questionCount,
    attemptCount = attemptCount,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncStatus = syncStatus
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
        totalQuestions = maxScore,
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
    maxScore = totalQuestions,
    multiAnswers = gson.toJson(answers),
    startedAt = startTimeMillis,
    finishedAt = endTimeMillis,
    questionOrder = questionOrder.joinToString(",")
)

// --- USER ---

/** Maps [UserEntity] to domain [User]. */
fun UserEntity.toDomain(): User = User(
    id = id,
    email = email,
    displayName = displayName ?: username,
    username = username,
    photoUrl = null
)

/** Maps domain [User] to [UserEntity] for Room storage. */
fun User.toEntity(): UserEntity = UserEntity(
    id = id,
    username = username,
    email = email,
    displayName = displayName
)

