package com.example.androidapp.data.repository

import com.example.androidapp.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl(
    // Xin công cụ Xác thực từ FirebaseModule
    private val auth: FirebaseAuth
) : AuthRepository {

    override suspend fun login(email: String, pass: String): Result<Boolean> {
        return try {
            // Gọi Firebase kiểm tra email/pass. .await() là đợi mạng trả lời.
            auth.signInWithEmailAndPassword(email, pass).await()
            Result.success(true) // Báo thành công
        } catch (e: Exception) {
            Result.failure(e) // Báo thất bại (sai pass hoặc rớt mạng)
        }
    }

    override suspend fun signUp(email: String, pass: String): Result<Boolean> {
        return try {
            // Gọi Firebase tạo tài khoản mới
            auth.createUserWithEmailAndPassword(email, pass).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            auth.signOut() // Thoát tài khoản
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}