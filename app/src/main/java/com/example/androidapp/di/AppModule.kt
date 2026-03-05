package com.example.androidapp.di

import android.content.Context
import com.example.androidapp.data.local.AppDatabase
import com.example.androidapp.data.local.dao.AttemptDao
import com.example.androidapp.data.local.dao.ChoiceDao
import com.example.androidapp.data.local.dao.QuestionDao
import com.example.androidapp.data.local.dao.QuizDao
import com.example.androidapp.data.local.dao.UserDao
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.example.androidapp.domain.repository.*
import com.example.androidapp.data.repository.*

/**
 * Application-wide dependency injection container interface.
 * Provides access to all app dependencies.
 */
interface AppContainer {
    val context: Context

    // Firebase
    val firebaseAuth: FirebaseAuth
    val firebaseFirestore: FirebaseFirestore
    val firebaseStorage: FirebaseStorage

    // Database
    val appDatabase: AppDatabase

    // Repositories
    val authRepository: AuthRepository
    val quizRepository: QuizRepository
    val questionRepository: QuestionRepository

    // DAOs
    val quizDao: QuizDao
    val questionDao: QuestionDao
    val choiceDao: ChoiceDao
    val attemptDao: AttemptDao
    val userDao: UserDao
}

/**
 * Implementation for the Dependency Injection container.
 */
class DefaultAppContainer(private val applicationContext: Context) : AppContainer {

    override val context: Context
        get() = applicationContext

    // 1. LẤY ĐỒ NGHỀ FIREBASE VỀ
    override val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    override val firebaseFirestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    override val firebaseStorage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }


    // Tự tay gọi thợ xây (Room.databaseBuilder) để tạo Database luôn
    override val appDatabase: AppDatabase by lazy {
        androidx.room.Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME // Lấy cái tên database nhóm má cấu hình sẵn
        ).build()
    }


    override val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(firebaseAuth)
    }

    override val quizRepository: QuizRepository by lazy {
        QuizRepositoryImpl(firebaseFirestore)
    }

    override val questionRepository: QuestionRepository by lazy {
        QuestionRepositoryImpl(firebaseFirestore)
    }


    override val quizDao: QuizDao by lazy { appDatabase.quizDao() }
    override val questionDao: QuestionDao by lazy { appDatabase.questionDao() }
    override val choiceDao: ChoiceDao by lazy { appDatabase.choiceDao() }
    override val attemptDao: AttemptDao by lazy { appDatabase.attemptDao() }
    override val userDao: UserDao by lazy { appDatabase.userDao() }
}