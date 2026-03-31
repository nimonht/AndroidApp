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
import com.example.androidapp.data.session.GuestSessionManager
import com.example.androidapp.data.sync.SyncManager
import com.example.androidapp.domain.repository.AttemptRepository
import com.example.androidapp.domain.repository.AuthRepository
import com.example.androidapp.domain.repository.PoolRepository
import com.example.androidapp.domain.repository.QuestionRepository
import com.example.androidapp.domain.repository.QuizRepository
import com.example.androidapp.domain.repository.SearchRepository
import com.example.androidapp.domain.repository.ShareCodeRepository
import com.example.androidapp.domain.repository.StorageRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

/**
 * Application-wide dependency injection container interface.
 */
interface AppContainer {
    val context: Context

    val firebaseAuth: FirebaseAuth
    val firebaseFirestore: FirebaseFirestore
    val firebaseStorage: FirebaseStorage

    val appDatabase: AppDatabase
    val quizDao: QuizDao
    val questionDao: QuestionDao
    val choiceDao: ChoiceDao
    val attemptDao: AttemptDao
    val userDao: UserDao
    val pendingSyncDao: PendingSyncDao

    val networkMonitor: NetworkMonitor
    val syncManager: SyncManager
    val guestSessionManager: GuestSessionManager

    val authRepository: AuthRepository
    val quizRepository: QuizRepository
    val attemptRepository: AttemptRepository
    val questionRepository: QuestionRepository
    val shareCodeRepository: ShareCodeRepository
    val poolRepository: PoolRepository
    val storageRepository: StorageRepository
    val searchRepository: SearchRepository
}
