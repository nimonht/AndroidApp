# Dependency Injection 

## Overview

The DI system consists of:
- `AppContainer`: Interface defining all dependencies
- `AppContainerImpl`: Implementation that lazily initializes all dependencies
- `QuizzezApplication`: Holds the singleton `appContainer` instance
- `AppContainerExt.kt`: Provides composable helper for accessing the container

## Accessing Dependencies

### From Composables

Use the `LocalAppContainer` property:

```kotlin
@Composable
fun MyScreen() {
    val container = LocalAppContainer
    val firebaseAuth = container.firebaseAuth
    val quizDao = container.quizDao
    // ... use dependencies
}
```

### From Activities

Access the container from the application:

```kotlin
class MyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as QuizzezApplication).appContainer
        val firebaseAuth = container.firebaseAuth
        // ... use dependencies
    }
}
```

### From ViewModels

Pass dependencies through constructor:

```kotlin
class MyViewModel(
    private val quizDao: QuizDao,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {
    // ... implementation
}
```

Create ViewModels with a factory:

```kotlin
@Composable
fun MyScreen() {
    val container = LocalAppContainer
    val viewModel: MyViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return MyViewModel(
                    quizDao = container.quizDao,
                    firebaseAuth = container.firebaseAuth
                ) as T
            }
        }
    )
    // ... use viewModel
}
```

## Adding New Dependencies

### Step 1: Add to AppContainer interface

Edit `di/AppModule.kt`:

```kotlin
interface AppContainer {
    // ... existing properties
    
    // Add your new dependency
    val myNewService: MyNewService
}
```

### Step 2: Implement in AppContainerImpl

Edit `di/FirebaseModule.kt` (which contains AppContainerImpl):

```kotlin
class AppContainerImpl(override val context: Context) : AppContainer {
    // ... existing implementations
    
    override val myNewService: MyNewService by lazy {
        MyNewServiceImpl(/* dependencies */)
    }
}
```

### Step 3: Use the dependency

Access it through the container as shown in the examples above.

## Available Dependencies

### Firebase
- `firebaseAuth: FirebaseAuth` - Authentication service
- `firebaseFirestore: FirebaseFirestore` - Firestore database

### Room Database
- `appDatabase: AppDatabase` - Room database instance
- `quizDao: QuizDao` - Quiz data access
- `questionDao: QuestionDao` - Question data access
- `choiceDao: ChoiceDao` - Choice data access
- `attemptDao: AttemptDao` - Attempt data access
- `userDao: UserDao` - User data access

### Context
- `context: Context` - Application context

### Repositories
- `authRepository: AuthRepository` - Authentication and user profile operations
- `quizRepository: QuizRepository` - Quiz CRUD operations
- `attemptRepository: AttemptRepository` - Quiz attempt tracking
- `shareCodeRepository: ShareCodeRepository` - Share code management
- `poolRepository: PoolRepository` - Community question pool
- `searchRepository: SearchRepository` - Search history and recent searches
- `adminRepository: AdminRepository` - Admin panel operations (user/quiz management, reports)

### Preferences
- `settingsPreferences: SettingsPreferences` - Local app settings (theme, notifications, etc.)

### Network & Sync
- `networkMonitor: NetworkMonitor` - Network connectivity state
- `syncManager: SyncManager` - Background sync coordinator
- `pendingSyncDao: PendingSyncDao` - Pending sync operations tracking

