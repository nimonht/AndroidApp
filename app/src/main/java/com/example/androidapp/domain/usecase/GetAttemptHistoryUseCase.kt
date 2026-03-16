package com.example.androidapp.domain.usecase

import com.example.androidapp.domain.model.Attempt
import com.example.androidapp.domain.repository.AttemptRepository
import kotlinx.coroutines.flow.Flow

class GetAttemptHistoryUseCase(
    private val attemptRepository: AttemptRepository
) {
    operator fun invoke(userId: String): Flow<List<Attempt>> {
        return attemptRepository.getAttemptsByUser(userId)
    }
}

