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
    val quizDao: QuizDao
    val questionDao: QuestionDao
    val choiceDao: ChoiceDao
    val attemptDao: AttemptDao
    val userDao: UserDao

    // Add repositories and other dependencies here as needed
}
