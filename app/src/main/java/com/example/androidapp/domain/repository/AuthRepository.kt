package com.example.androidapp.domain.repository

interface AuthRepository {
    suspend fun login(email: String, pass: String): Result<Boolean>
    suspend fun signUp(email: String, pass: String): Result<Boolean>
    suspend fun signOut(): Result<Unit>
}