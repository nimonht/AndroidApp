package com.example.androidapp.data.remote

import com.example.androidapp.data.remote.model.*
import com.example.androidapp.domain.model.*
import com.google.firebase.Timestamp
import java.util.Date

// --- USER ---
fun UserDto.toDomain() = User(
    id = id,
    email = email,
    displayName = displayName,
    username = username,
    photoUrl = photoUrl,
    role = UserRole.fromString(role),
    isBanned = deletedAt != null
)

/**
 * Converts a [User] to a [UserDto] for a create or update operation.
 * When [existingDto] is provided, preserves the original [createdAt] timestamp.
 * When [existingDto] is null (new user), [createdAt] defaults to now.
 */
fun User.toDto(existingDto: UserDto? = null): UserDto = UserDto(
    id = id,
    email = email,
    displayName = displayName,
    username = username,
    photoUrl = photoUrl,
    role = role.toStorageValue(),
    createdAt = existingDto?.createdAt ?: Timestamp.now(),
    updatedAt = Timestamp.now()
)

// --- QUIZ & QUESTIONS ---
fun ChoiceDto.toDomain() = Choice(id, content, isCorrect, position)
fun Choice.toDto() = ChoiceDto(id, content, isCorrect, position)

fun QuestionDto.toDomain() = Question(
    id = id,
    content = content,
    choices = choices.map { it.toDomain() },
    isMultiSelect = allowMultipleCorrect,
    explanation = explanation,
    mediaUrl = mediaUrl,
    points = points,
    position = position
)

fun Question.toDto() = QuestionDto(
    id = id,
    content = content,
    choices = choices.map { it.toDto() },
    allowMultipleCorrect = isMultiSelect,
    choiceCount = choices.size,
    explanation = explanation,
    mediaUrl = mediaUrl,
    points = points,
    position = position
)

fun QuizDto.toDomain() = Quiz(
    id = id,
    ownerId = ownerId,
    title = title,
    description = description,
    authorName = authorName,
    thumbnailUrl = thumbnailUrl,
    tags = tags,
    questionCount = questionCount,
    attemptCount = attemptCount,
    isPublic = isPublic,
    isDraft = isDraft,
    shareCode = shareCode,
    checksum = checksum,
    createdAt = createdAt?.toDate()?.time ?: System.currentTimeMillis(),
    updatedAt = updatedAt?.toDate()?.time ?: System.currentTimeMillis(),
    deletedAt = deletedAt?.toDate()?.time
)

fun Quiz.toDto() = QuizDto(
    id = id,
    ownerId = ownerId,
    title = title,
    description = description,
    authorName = authorName,
    thumbnailUrl = thumbnailUrl,
    tags = tags,
    questionCount = questionCount,
    attemptCount = attemptCount,
    isPublic = isPublic,
    isDraft = isDraft,
    shareCode = shareCode,
    checksum = checksum,
    createdAt = Timestamp(Date(createdAt)),
    updatedAt = Timestamp(Date(updatedAt)),
    deletedAt = deletedAt?.let { Timestamp(Date(it)) }
)

// --- ATTEMPT ---
fun AttemptDto.toDomain() = Attempt(
    id = id,
    userId = userId,
    quizId = quizId,
    score = score,
    totalQuestions = maxScore,
    answers = multiAnswers.ifEmpty { answers.mapValues { (_, v) -> listOf(v) } },
    startTimeMillis = startedAt?.toDate()?.time ?: System.currentTimeMillis(),
    endTimeMillis = finishedAt?.toDate()?.time,
    questionOrder = questionOrder
)

fun Attempt.toDto() = AttemptDto(
    id = id,
    userId = userId,
    quizId = quizId,
    questionOrder = questionOrder,
    choiceOrders = emptyMap(),
    answers = answers.mapValues { (_, v) -> v.firstOrNull() ?: "" },
    multiAnswers = answers,
    score = score,
    maxScore = totalQuestions,
    startedAt = Timestamp(Date(startTimeMillis)),
    finishedAt = endTimeMillis?.let { Timestamp(Date(it)) }
)

// --- QUESTION POOL ---
fun QuestionPoolItemDto.toDomain() = QuestionPoolItem(
    id = id,
    question = Question(
        id = "",  // Pool questions don't have a separate question ID
        content = content,
        choices = choices.mapIndexed { index, choice ->
            Choice(
                id = "",
                content = choice.content,
                isCorrect = choice.isCorrect,
                position = index
            )
        },
        isMultiSelect = allowMultipleCorrect,
        explanation = null,
        mediaUrl = mediaUrl,
        points = points,
        position = 0
    ),
    contributorId = contributorId,
    sourceQuizId = sourceQuizId,
    tags = tags,
    usageCount = usageCount,
    isActive = isActive,
    createdAtMillis = createdAt?.toDate()?.time ?: System.currentTimeMillis()
)

fun QuestionPoolItem.toDto() = QuestionPoolItemDto(
    id = id,
    content = question.content,
    choices = question.choices.map { choice ->
        PoolChoiceDto(
            content = choice.content,
            isCorrect = choice.isCorrect
        )
    },
    correctIndices = question.choices
        .mapIndexedNotNull { index, choice -> if (choice.isCorrect) index else null },
    tags = tags,
    mediaUrl = question.mediaUrl,
    points = question.points,
    allowMultipleCorrect = question.isMultiSelect,
    contributorId = contributorId,
    sourceQuizId = sourceQuizId,
    isActive = isActive,
    usageCount = usageCount,
    createdAt = Timestamp(Date(createdAtMillis))
)
