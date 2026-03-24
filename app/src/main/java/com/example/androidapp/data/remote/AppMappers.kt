package com.example.androidapp.data.remote

import com.example.androidapp.data.remote.model.*
import com.example.androidapp.domain.model.*
import com.google.firebase.Timestamp
import java.util.Date

// --- USER ---
fun UserDto.toDomain() = User(id, email, displayName, username, photoUrl)
fun User.toDto() = UserDto(id, email, displayName, username, photoUrl)

// --- QUIZ & QUESTIONS ---
fun ChoiceDto.toDomain() = Choice(id, content, isCorrect, position)
fun Choice.toDto() = ChoiceDto(id, content, isCorrect, position)

fun QuestionDto.toDomain() = Question(
    id = id,
    content = content,
    choices = choices.map { it.toDomain() },
    isMultiSelect = isMultiSelect,
    explanation = explanation,
    mediaUrl = mediaUrl,
    points = points,
    position = position
)

fun Question.toDto() = QuestionDto(
    id = id,
    content = content,
    choices = choices.map { it.toDto() },
    isMultiSelect = isMultiSelect,
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
    shareCode = shareCode,
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
    shareCode = shareCode,
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
    totalQuestions = totalQuestions,
    answers = answers,
    startTimeMillis = startTime?.toDate()?.time ?: System.currentTimeMillis(),
    endTimeMillis = endTime?.toDate()?.time,
    questionOrder = questionOrder
)

fun Attempt.toDto() = AttemptDto(
    id = id,
    userId = userId,
    quizId = quizId,
    score = score,
    totalQuestions = totalQuestions,
    answers = answers,
    startTime = Timestamp(Date(startTimeMillis)),
    endTime = endTimeMillis?.let { Timestamp(Date(it)) },
    questionOrder = questionOrder
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

// --- SHARE CODE ---
fun ShareCodeDto.toDomain() = ShareCode(
    code = code,
    quizId = quizId,
    expiresAtMillis = expiresAt?.toDate()?.time
)

fun ShareCode.toDto() = ShareCodeDto(
    code = code,
    quizId = quizId,
    expiresAt = expiresAtMillis?.let { Timestamp(Date(it)) }
)