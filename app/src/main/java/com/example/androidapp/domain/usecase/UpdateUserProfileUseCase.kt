package com.example.androidapp.domain.usecase.user

import com.example.androidapp.domain.repository.AuthRepository

class UpdateUserProfileUseCase(
    private val authRepository: AuthRepository
) {

    suspend operator fun invoke(
        displayName: String,
        avatarUrl: String?
    ) {
        authRepository.updateUserProfile(
            displayName = displayName,
            avatarUrl = avatarUrl
        )
    }
}