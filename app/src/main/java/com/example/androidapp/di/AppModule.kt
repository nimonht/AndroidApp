package com.example.androidapp.di

import android.content.Context
import com.example.androidapp.data.local.AppDatabase
import com.example.androidapp.data.local.dao.AttemptDao
import com.example.androidapp.data.local.dao.ChoiceDao
import com.example.androidapp.data.local.dao.PendingSyncDao
import com.example.androidapp.data.local.dao.QuestionDao
import com.example.androidapp.data.local.dao.QuizDao
import com.example.androidapp.data.local.dao.UserDao
import com.example.androidapp.data.network.NetworkMonitor
import com.example.androidapp.data.sync.QuizInvalidationManager
import com.example.androidapp.data.sync.SyncManager
import com.example.androidapp.domain.repository.AdminRepository
import com.example.androidapp.domain.repository.AttemptRepository
import com.example.androidapp.domain.repository.AuthRepository
import com.example.androidapp.domain.repository.PoolRepository
import com.example.androidapp.domain.repository.QuizRepository
import com.example.androidapp.domain.repository.SearchRepository
import com.example.androidapp.domain.repository.ShareCodeRepository
import com.example.androidapp.data.preferences.SettingsPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Application-wide dependency injection container interface.
 */
interface AppContainer {
    val context: Context

    val firebaseAuth: FirebaseAuth
    val firebaseFirestore: FirebaseFirestore

    val appDatabase: AppDatabase
    val quizDao: QuizDao
    val questionDao: QuestionDao
    val choiceDao: ChoiceDao
    val attemptDao: AttemptDao
    val userDao: UserDao
    val pendingSyncDao: PendingSyncDao

    val networkMonitor: NetworkMonitor
    val syncManager: SyncManager

    /**
     * Manages incremental invalidation of locally-cached quizzes that
     * have been permanently deleted from Firestore by other users or
     * maintenance tasks. Uses a lightweight tombstone-based approach.
     */
    val quizInvalidationManager: QuizInvalidationManager

    val authRepository: AuthRepository
    val quizRepository: QuizRepository
    val attemptRepository: AttemptRepository
    val shareCodeRepository: ShareCodeRepository
    val poolRepository: PoolRepository
    val adminRepository: AdminRepository
    val searchRepository: SearchRepository
    val settingsPreferences: SettingsPreferences
}
