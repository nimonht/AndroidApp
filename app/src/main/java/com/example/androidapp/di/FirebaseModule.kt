package com.example.androidapp.di

import android.content.Context
import androidx.room.Room
import com.example.androidapp.data.local.AppDatabase
import com.example.androidapp.data.local.dao.AttemptDao
import com.example.androidapp.data.local.dao.ChoiceDao
import com.example.androidapp.data.local.dao.QuestionDao
import com.example.androidapp.data.local.dao.QuizDao
import com.example.androidapp.data.local.dao.UserDao
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storage

/**
 * Implementation of the application-wide dependency injection container.
 * All dependencies are lazily initialized as singletons.
 */
class AppContainerImpl(override val context: Context) : AppContainer {

    // ==================== Firebase ====================

    /**
     * Provides the singleton instance of FirebaseAuth.
     * Used for user authentication (email/password, Google Sign-In).
     */
    override val firebaseAuth: FirebaseAuth by lazy {
        Firebase.auth
    }

    /**
     * Provides the singleton instance of FirebaseFirestore.
     * Used for cloud database operations (quizzes, users, attempts).
     */
    override val firebaseFirestore: FirebaseFirestore by lazy {
        Firebase.firestore
    }

    /**
     * Provides the singleton instance of FirebaseStorage.
     * Used for media file uploads (images, videos).
     */
    override val firebaseStorage: FirebaseStorage by lazy {
        Firebase.storage
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
}
