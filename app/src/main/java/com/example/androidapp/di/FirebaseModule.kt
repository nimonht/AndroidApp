package com.example.androidapp.di

import android.content.Context
import androidx.room.Room
import com.example.androidapp.data.local.AppDatabase
import com.example.androidapp.data.local.dao.AttemptDao
import com.example.androidapp.data.local.dao.ChoiceDao
import com.example.androidapp.data.local.dao.PendingSyncDao
import com.example.androidapp.data.local.dao.QuestionDao
import com.example.androidapp.data.local.dao.QuizDao
import com.example.androidapp.data.local.dao.UserDao
import com.example.androidapp.BuildConfig
import com.example.androidapp.data.remote.firebase.AttemptRemoteDataSource
import com.example.androidapp.data.remote.firebase.QuizRemoteDataSource
import com.example.androidapp.data.remote.firebase.UserRemoteDataSource
import com.example.androidapp.data.repository.AttemptRepositoryImpl
import com.example.androidapp.data.repository.AuthRepositoryImpl
import com.example.androidapp.data.repository.QuizRepositoryImpl
import com.example.androidapp.domain.repository.AttemptRepository
import com.example.androidapp.domain.repository.AuthRepository
import com.example.androidapp.domain.repository.QuizRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore

/**
 * Implementation of the application-wide dependency injection container.
 * All dependencies are lazily initialized as singletons.
 */
class AppContainerImpl(override val context: Context) : AppContainer {

    private val emulatorHost: String = BuildConfig.FIREBASE_EMULATOR_HOST

    // ==================== Firebase ====================

    /**
     * Provides the singleton instance of FirebaseAuth.
     * Used for user authentication (email/password, Google Sign-In).
     */
    override val firebaseAuth: FirebaseAuth by lazy {
        Firebase.auth.also { auth ->
            if (BuildConfig.USE_FIREBASE_EMULATOR) {
                auth.useEmulator(emulatorHost, 9099)
            }
        }
    }

    /**
     * Provides the singleton instance of FirebaseFirestore.
     * Used for cloud database operations (quizzes, users, attempts).
     */
    override val firebaseFirestore: FirebaseFirestore by lazy {
        Firebase.firestore.also { firestore ->
            if (BuildConfig.USE_FIREBASE_EMULATOR) {
                firestore.useEmulator(emulatorHost, 8080)
            }
        }
    }

    // ==================== Room Database ====================

    /**
     * Provides the singleton instance of AppDatabase.
     */
    override val appDatabase: AppDatabase by lazy {
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .fallbackToDestructiveMigration(false)
            .build()
    }

    /**
     * Provides the QuizDao instance.
     */
    override val quizDao: QuizDao by lazy {
        appDatabase.quizDao()
    }

    /**
     * Provides the QuestionDao instance.
     */
    override val questionDao: QuestionDao by lazy {
        appDatabase.questionDao()
    }

    /**
     * Provides the ChoiceDao instance.
     */
    override val choiceDao: ChoiceDao by lazy {
        appDatabase.choiceDao()
    }

    /**
     * Provides the AttemptDao instance.
     */
    override val attemptDao: AttemptDao by lazy {
        appDatabase.attemptDao()
    }

    /**
     * Provides the UserDao instance.
     */
    override val userDao: UserDao by lazy {
        appDatabase.userDao()
    }

    /**
     * Provides the PendingSyncDao instance.
     */
    override val pendingSyncDao: PendingSyncDao by lazy {
        appDatabase.pendingSyncDao()
    }

    // ==================== Remote Data Sources ====================

    private val quizRemoteDataSource: QuizRemoteDataSource by lazy {
        QuizRemoteDataSource(firebaseFirestore)
    }

    private val attemptRemoteDataSource: AttemptRemoteDataSource by lazy {
        AttemptRemoteDataSource(firebaseFirestore)
    }

    private val userRemoteDataSource: UserRemoteDataSource by lazy {
        UserRemoteDataSource(firebaseFirestore)
    }

    // ==================== Repositories ====================

    /**
     * Provides the [AuthRepository] implementation.
     */
    override val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(firebaseAuth, userDao, userRemoteDataSource)
    }

    /**
     * Provides the [QuizRepository] implementation.
     */
    override val quizRepository: QuizRepository by lazy {
        QuizRepositoryImpl(quizDao, questionDao, choiceDao, quizRemoteDataSource)
    }

    /**
     * Provides the [AttemptRepository] implementation.
     */
    override val attemptRepository: AttemptRepository by lazy {
        AttemptRepositoryImpl(attemptDao, attemptRemoteDataSource)
    }
}
