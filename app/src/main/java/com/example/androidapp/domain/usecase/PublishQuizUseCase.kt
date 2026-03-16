package com.example.androidapp.domain.usecase

import com.example.androidapp.domain.model.Question
import com.example.androidapp.domain.model.Quiz
import com.example.androidapp.domain.repository.QuizRepository

class PublishQuizUseCase(
    private val repository: QuizRepository
) {

    suspend operator fun invoke(
        quiz: Quiz,
        questions: List<Question>
    ): Result<Unit> {

        if (quiz.title.isBlank()) {
            return Result.failure(Exception("Quiz phải có tiêu đề"))
        }

        if (questions.isEmpty()) {
            return Result.failure(Exception("Quiz phải có ít nhất 1 câu hỏi"))
        }

        questions.forEach { question ->

            if (question.content.isBlank()) {
                return Result.failure(Exception("Câu hỏi không được rỗng"))
            }

            if (question.choices.size < 2) {
                return Result.failure(Exception("Mỗi câu hỏi cần ít nhất 2 lựa chọn"))
            }

            if (question.choices.none { it.isCorrect }) {
                return Result.failure(Exception("Phải có ít nhất 1 đáp án đúng"))
            }
        }

        repository.publishQuiz(
            quiz = quiz,
            questions = questions
        )

        return Result.success(Unit)
    }
}