package com.example.androidapp.di

import android.content.Context
import androidx.room.Room
import com.example.androidapp.BuildConfig
import com.example.androidapp.data.local.AppDatabase
import com.example.androidapp.data.local.dao.AttemptDao
import com.example.androidapp.data.local.dao.ChoiceDao
import com.example.androidapp.data.local.dao.PendingSyncDao
import com.example.androidapp.data.local.dao.QuestionDao
import com.example.androidapp.data.local.dao.QuizDao
import com.example.androidapp.data.local.dao.UserDao
import com.example.androidapp.data.network.NetworkMonitor
import com.example.androidapp.data.remote.firebase.AdminRemoteDataSource
import com.example.androidapp.data.remote.firebase.AttemptRemoteDataSource
import com.example.androidapp.data.remote.firebase.PoolRemoteDataSource
import com.example.androidapp.data.remote.firebase.QuestionRemoteDataSource
import com.example.androidapp.data.remote.firebase.QuizRemoteDataSource
import com.example.androidapp.data.remote.firebase.ShareCodeRemoteDataSource
import com.example.androidapp.data.remote.firebase.UserRemoteDataSource
import com.example.androidapp.data.repository.AdminRepositoryImpl
import com.example.androidapp.data.repository.AttemptRepositoryImpl
import com.example.androidapp.data.repository.AuthRepositoryImpl
import com.example.androidapp.data.repository.PoolRepositoryImpl
import com.example.androidapp.data.repository.QuizRepositoryImpl
import com.example.androidapp.data.repository.ShareCodeRepositoryImpl
import com.example.androidapp.data.preferences.SettingsPreferences
import com.example.androidapp.data.repository.SearchRepositoryImpl
import com.example.androidapp.data.sync.SyncManager
import com.example.androidapp.domain.repository.AdminRepository
import com.example.androidapp.domain.repository.AttemptRepository
import com.example.androidapp.domain.repository.AuthRepository
import com.example.androidapp.domain.repository.PoolRepository
import com.example.androidapp.domain.repository.QuizRepository
import com.example.androidapp.domain.repository.ShareCodeRepository
import com.example.androidapp.domain.repository.SearchRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.functions

class AppContainerImpl(override val context: Context) : AppContainer {

    private val emulatorHost: String = BuildConfig.FIREBASE_EMULATOR_HOST

    override val firebaseAuth: FirebaseAuth by lazy {
        Firebase.auth.also { auth ->
            if (BuildConfig.USE_FIREBASE_EMULATOR) {
                auth.useEmulator(emulatorHost, 9099)
            }
        }
    }

    override val firebaseFirestore: FirebaseFirestore by lazy {
        Firebase.firestore.also { firestore ->
            if (BuildConfig.USE_FIREBASE_EMULATOR) {
                firestore.useEmulator(emulatorHost, 8080)
            }
        }
    }

    private val firebaseFunctions: FirebaseFunctions by lazy {
        Firebase.functions.also { functions ->
            if (BuildConfig.USE_FIREBASE_EMULATOR) {
                functions.useEmulator(emulatorHost, 5001)
            }
        }
    }

    override val appDatabase: AppDatabase by lazy {
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    override val quizDao: QuizDao by lazy { appDatabase.quizDao() }
    override val questionDao: QuestionDao by lazy { appDatabase.questionDao() }
    override val choiceDao: ChoiceDao by lazy { appDatabase.choiceDao() }
    override val attemptDao: AttemptDao by lazy { appDatabase.attemptDao() }
    override val userDao: UserDao by lazy { appDatabase.userDao() }
    override val pendingSyncDao: PendingSyncDao by lazy { appDatabase.pendingSyncDao() }

    override val networkMonitor: NetworkMonitor by lazy { NetworkMonitor(context) }

    override val syncManager: SyncManager by lazy {
        SyncManager(
            pendingSyncDao,
            quizDao,
            questionDao,
            choiceDao,
            attemptDao,
            quizRemoteDataSource,
            questionRemoteDataSource,
            attemptRemoteDataSource,
            networkMonitor,
            settingsPreferences
        )
    }

    private val quizRemoteDataSource: QuizRemoteDataSource by lazy {
        QuizRemoteDataSource(firebaseFirestore)
    }

    private val attemptRemoteDataSource: AttemptRemoteDataSource by lazy {
        AttemptRemoteDataSource(firebaseFirestore)
    }

    private val userRemoteDataSource: UserRemoteDataSource by lazy {
        UserRemoteDataSource(firebaseFirestore)
    }

    private val questionRemoteDataSource: QuestionRemoteDataSource by lazy {
        QuestionRemoteDataSource(firebaseFirestore)
    }

    private val shareCodeRemoteDataSource: ShareCodeRemoteDataSource by lazy {
        ShareCodeRemoteDataSource(firebaseFirestore)
    }

    private val poolRemoteDataSource: PoolRemoteDataSource by lazy {
        PoolRemoteDataSource(firebaseFirestore)
    }

    private val adminRemoteDataSource: AdminRemoteDataSource by lazy {
        AdminRemoteDataSource(firebaseFirestore, firebaseFunctions)
    }

    override val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(firebaseAuth, userDao, userRemoteDataSource, firebaseFirestore)
    }

    override val quizRepository: QuizRepository by lazy {
        QuizRepositoryImpl(
            quizDao,
            questionDao,
            choiceDao,
            quizRemoteDataSource,
            questionRemoteDataSource,
            syncManager,
            shareCodeRepository
        )
    }

    override val attemptRepository: AttemptRepository by lazy {
        AttemptRepositoryImpl(attemptDao, syncManager)
    }

    override val shareCodeRepository: ShareCodeRepository by lazy {
        ShareCodeRepositoryImpl(shareCodeRemoteDataSource)
    }

    override val poolRepository: PoolRepository by lazy {
        PoolRepositoryImpl(poolRemoteDataSource, firebaseFirestore)
    }

    override val adminRepository: AdminRepository by lazy {
        AdminRepositoryImpl(adminRemoteDataSource)
    }

    override val searchRepository: SearchRepository by lazy {
        SearchRepositoryImpl(context)
    }

    override val settingsPreferences: SettingsPreferences by lazy {
        SettingsPreferences(context)
    }
}
