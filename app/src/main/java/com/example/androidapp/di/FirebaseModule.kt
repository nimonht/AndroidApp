package com.example.androidapp.di

import android.content.Context
import androidx.work.WorkManager
import androidx.room.Room
import com.example.androidapp.BuildConfig
import com.example.androidapp.data.local.AppDatabase
import com.example.androidapp.data.local.dao.AttemptDao
import com.example.androidapp.data.local.dao.ChoiceDao
import com.example.androidapp.data.local.dao.PendingSyncDao
import com.example.androidapp.data.local.dao.QuestionDao
import com.example.androidapp.data.local.dao.QuizDao
import com.example.androidapp.data.local.dao.UserDao
import com.example.androidapp.data.logging.LogCollector
import com.example.androidapp.data.ml.ModelManager
import com.example.androidapp.data.ml.TFLiteEmbeddingService
import com.example.androidapp.data.network.NetworkMonitor
import com.example.androidapp.data.preferences.SettingsPreferences
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
import com.example.androidapp.data.repository.SearchRepositoryImpl
import com.example.androidapp.data.repository.ShareCodeRepositoryImpl
import com.example.androidapp.data.search.EmbeddingCache
import com.example.androidapp.data.worker.EmbeddingIndexWorker
import com.example.androidapp.data.sync.QuizInvalidationManager
import com.example.androidapp.data.sync.SyncManager
import com.example.androidapp.domain.console.CommandContext
import com.example.androidapp.domain.console.CommandExecutor
import com.example.androidapp.domain.console.CommandRegistry
import com.example.androidapp.domain.console.RepositoryBundle
import com.example.androidapp.domain.console.commands.AliasCommand
import com.example.androidapp.domain.console.commands.BanCommand
import com.example.androidapp.domain.console.commands.CacheCommand
import com.example.androidapp.domain.console.commands.ClearCommand
import com.example.androidapp.domain.console.commands.ConfigCommand
import com.example.androidapp.domain.console.commands.CountCommand
import com.example.androidapp.domain.console.commands.DeleteAttemptCommand
import com.example.androidapp.domain.console.commands.DeleteCommand
import com.example.androidapp.domain.console.commands.DeletePoolItemCommand
import com.example.androidapp.domain.console.commands.DeleteQuizCommand
import com.example.androidapp.domain.console.commands.DeleteUserCommand
import com.example.androidapp.domain.console.commands.EchoCommand
import com.example.androidapp.domain.console.commands.EmbeddingCommand
import com.example.androidapp.domain.console.commands.ExportCommand
import com.example.androidapp.domain.console.commands.GrepCommand
import com.example.androidapp.domain.console.commands.HeadTailCommand
import com.example.androidapp.domain.console.commands.HelpCommand
import com.example.androidapp.domain.console.commands.HistoryCommand
import com.example.androidapp.domain.console.commands.LogCommand
import com.example.androidapp.domain.console.commands.LsCommand
import com.example.androidapp.domain.console.commands.MyCommand
import com.example.androidapp.domain.console.commands.PermCommand
import com.example.androidapp.domain.console.commands.PingCommand
import com.example.androidapp.domain.console.commands.PublishCommand
import com.example.androidapp.domain.console.commands.PurgeCommand
import com.example.androidapp.domain.console.commands.QuizInfoCommand
import com.example.androidapp.domain.console.commands.RestoreCommand
import com.example.androidapp.domain.console.commands.RoleCommand
import com.example.androidapp.domain.console.commands.SearchCommand
import com.example.androidapp.domain.console.commands.SortCommand
import com.example.androidapp.domain.console.commands.StatsCommand
import com.example.androidapp.domain.console.commands.SyncCommand
import com.example.androidapp.domain.console.commands.UnbanCommand
import com.example.androidapp.domain.console.commands.UnpublishCommand
import com.example.androidapp.domain.console.commands.UserInfoCommand
import com.example.androidapp.domain.console.commands.WhoamiCommand
import com.example.androidapp.domain.console.ServiceBundle
import com.example.androidapp.domain.model.User
import com.example.androidapp.domain.model.UserRole
import com.example.androidapp.domain.repository.AdminRepository
import com.example.androidapp.domain.repository.AttemptRepository
import com.example.androidapp.domain.repository.AuthRepository
import com.example.androidapp.domain.repository.PoolRepository
import com.example.androidapp.domain.repository.QuizRepository
import com.example.androidapp.domain.repository.SearchRepository
import com.example.androidapp.domain.repository.ShareCodeRepository
import com.example.androidapp.domain.service.EmbeddingIndex
import com.example.androidapp.domain.service.EmbeddingService
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.functions
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

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
            settingsPreferences,
            lazy { quizRepository }
        )
    }

    override val quizInvalidationManager: QuizInvalidationManager by lazy {
        QuizInvalidationManager(
            context,
            quizDao,
            questionDao,
            choiceDao,
            quizRemoteDataSource,
            networkMonitor
        )
    }

    override val quizRemoteDataSource: QuizRemoteDataSource by lazy {
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
        AdminRemoteDataSource(firebaseFirestore, firebaseFunctions, firebaseAuth)
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
            shareCodeRepository,
            embeddingService,
            embeddingIndex
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

    override val logCollector: LogCollector by lazy {
        LogCollector(CoroutineScope(SupervisorJob() + Dispatchers.IO))
    }

    override val modelManager: ModelManager by lazy {
        ModelManager(context)
    }

    override val embeddingService: EmbeddingService by lazy {
        TFLiteEmbeddingService(context, modelManager)
    }

    override val embeddingIndex: EmbeddingIndex by lazy {
        EmbeddingCache(
            quizDao = quizDao,
            reindexTrigger = {
                EmbeddingIndexWorker.enqueueIfNeeded(
                    WorkManager.getInstance(context)
                )
            }
        )
    }

    override val commandRegistry: CommandRegistry by lazy {
        CommandRegistry().apply {
            // Register all commands
            registerAll(
                HelpCommand(this@apply),
                WhoamiCommand(),
                PingCommand(),
                EchoCommand(),
                ClearCommand(),
                HistoryCommand(),
                ConfigCommand(),
                CacheCommand(),
                SyncCommand(),
                MyCommand(),
                LogCommand(),
                GrepCommand(),
                SortCommand(),
                HeadTailCommand(isHead = true),
                HeadTailCommand(isHead = false),
                CountCommand(),
                AliasCommand(),
                BanCommand(),
                UnbanCommand(),
                RoleCommand(),
                PermCommand(),
                UserInfoCommand(),
                DeleteCommand(
                    DeleteUserCommand(),
                    DeleteQuizCommand(),
                    DeleteAttemptCommand(),
                    DeletePoolItemCommand()
                ),
                QuizInfoCommand(),
                PublishCommand(),
                UnpublishCommand(),
                RestoreCommand(),
                LsCommand(),
                StatsCommand(),
                SearchCommand(),
                ExportCommand(),
                PurgeCommand(),
                EmbeddingCommand(
                    embeddingService, embeddingIndex
                )
            )
        }
    }

    override val commandExecutor: CommandExecutor by lazy {
        CommandExecutor(
            registry = commandRegistry,
            contextProvider = {
                // Use the cached StateFlow value from AuthRepository to avoid
                // blocking the calling thread (autocomplete runs on the UI thread).
                val user = (authRepository.currentUser as? StateFlow<User?>)?.value
                    ?: User(
                        id = "guest",
                        email = "",
                        displayName = "Guest",
                        role = UserRole.GUEST
                    )
                CommandContext(
                    currentUser = user,
                    repositories = RepositoryBundle(
                        adminRepository = adminRepository,
                        authRepository = authRepository,
                        quizRepository = quizRepository,
                        attemptRepository = attemptRepository,
                        shareCodeRepository = shareCodeRepository,
                        poolRepository = poolRepository,
                        searchRepository = searchRepository
                    ),
                    services = ServiceBundle(
                        syncService = syncManager,
                        networkService = networkMonitor,
                        settingsService = settingsPreferences,
                        logService = logCollector
                    )
                )
            }
        )
    }
}
