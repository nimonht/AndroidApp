package com.example.androidapp.di

import android.content.Context
import com.example.androidapp.data.local.AppDatabase
import com.example.androidapp.data.local.dao.AttemptDao
import com.example.androidapp.data.local.dao.ChoiceDao
import com.example.androidapp.data.local.dao.PendingSyncDao
import com.example.androidapp.data.local.dao.QuestionDao
import com.example.androidapp.data.local.dao.QuizDao
import com.example.androidapp.data.local.dao.UserDao
import com.example.androidapp.domain.repository.AttemptRepository
import com.example.androidapp.domain.repository.AuthRepository
import com.example.androidapp.domain.repository.QuizRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Application-wide dependency injection container interface.
 * Provides access to all app dependencies.
 */
interface AppContainer {
    val context: Context

    // Firebase
    val firebaseAuth: FirebaseAuth
    val firebaseFirestore: FirebaseFirestore

    // Database
    val appDatabase: AppDatabase
    val quizDao: QuizDao
    val questionDao: QuestionDao
    val choiceDao: ChoiceDao
    val attemptDao: AttemptDao
    val userDao: UserDao
    val pendingSyncDao: PendingSyncDao

    // Repositories
    val authRepository: AuthRepository
    val quizRepository: QuizRepository
    val attemptRepository: AttemptRepository
}
