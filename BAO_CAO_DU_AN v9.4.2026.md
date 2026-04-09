# Báo cáo Dự án: Ứng dụng Android Quizzez

**Tên ứng dụng:** Quizzez
**Package:** `com.example.androidapp`
**Phiên bản:** 9.4.2026
**Nền tảng:** Android (minSdk 26 — Android 8.0 trở lên, targetSdk 36)

---

## MỤC LỤC

| # | Chương | Nội dung |
|---|--------|---------|
| 1 | [Tổng Quan Dự Án](#1-tổng-quan-dự-án) | Giới thiệu, công nghệ, tính năng, vai trò người dùng |
| 2 | [Kiến Trúc Hệ Thống](#2-kiến-trúc-hệ-thống) | Clean Architecture, luồng local-first, cấu trúc thư mục |
| 3 | [Tầng Domain](#3-tầng-domain-domain-layer) | Models, Repository Interfaces, Utilities (30 file) |
| 3.1 | └─ Domain Models | Quiz, Question, Choice, Attempt, User, UserRole, AdminPermission… |
| 3.2 | └─ Repository Interfaces | AuthRepository, QuizRepository, AttemptRepository… |
| 3.3 | └─ Domain Utilities | ScoreCalculator, QuizValidator, ChecksumUtil, QuestionShuffler… |
| 4 | [Tầng Dữ Liệu](#4-tầng-dữ-liệu-data-layer) | Room, Firebase, Repository Implementations, Sync (44 file) |
| 4.1 | └─ Local Room | AppDatabase, 6 Entities, 6 DAOs, EntityMappers, Converters |
| 4.2 | └─ Remote Firebase | FirestoreCollections, 8 Remote Data Sources, 7 DTOs |
| 4.3 | └─ Repository Impls | QuizRepositoryImpl, AuthRepositoryImpl… (7 file) |
| 4.4 | └─ Sync & Mạng | SyncManager, NetworkMonitor, WorkManager Workers |
| 5 | [Dependency Injection](#5-tầng-dependency-injection-di) | AppContainer, AppContainerImpl, LocalAppContainer |
| 6 | [Tầng Giao Diện](#6-tầng-giao-diện-ui-layer) | Screens, Components, Theme, Navigation (80+ file) |
| 6.1 | └─ Navigation | Routes.kt, QuizzezNavHost.kt (23 routes) |
| 6.2 | └─ Xác thực | LoginScreen, RegisterScreen, AuthViewModel, AuthFragment |
| 6.3 | └─ Trang chủ | HomeScreen, HomeViewModel |
| 6.4 | └─ Tìm kiếm | SearchScreen, SearchViewModel + 8 sub-files |
| 6.5 | └─ Hồ sơ | ProfileScreen, EditProfileScreen (Wallhaven API) |
| 6.6 | └─ Luồng Quiz | QuizDetail → TakeQuiz → QuizResult |
| 6.7 | └─ Tạo & Sửa Quiz | CreateQuiz, EditQuiz, CsvImport, QuizPreview |
| 6.8 | └─ Màn hình khác | History, AnswerReview, AttemptDetail, Trash, Settings, Pool |
| 6.9 | └─ Quản trị | AdminDashboard, AdminUsers, AdminQuizzes, AdminReports |
| 6.10 | └─ Components | 32 reusable components theo 7 nhóm |
| 6.11 | └─ Theme | QuizzezTheme, Color, Type, Shape, design-tokens.json |
| 7 | [Sơ Đồ Use Case](#7-sơ-đồ-use-case--mô-tả-chi-tiết) | 4 Actor, 30 Use Cases, 15 mô tả chi tiết |
| 8 | [Bảng Tổng Hợp File](#8-bảng-tổng-hợp-tất-cả-file-code) | 188 file phân nhóm theo tầng |
| 9 | [Quy Tắc Nghiệp Vụ](#9-quy-tắc-nghiệp-vụ-quan-trọng) | 15 Business Rules quan trọng nhất |
| 10 | [Kết Luận](#10-kết-luận) | Điểm mạnh kiến trúc, khả năng mở rộng |

---

## 1. Tổng Quan Dự Án

### 1.1 Giới thiệu

Quizzez là ứng dụng Android cho phép người dùng tạo, chia sẻ và làm bài trắc nghiệm nhiều lựa chọn.
Ứng dụng hỗ trợ cả chế độ **offline** (local-first với Room/SQLite) lẫn **đồng bộ đám mây** thời gian
thực qua Cloud Firestore. Người dùng có thể chia sẻ quiz thông qua mã 6 ký tự, nhập câu hỏi hàng loạt
từ file CSV, và theo dõi lịch sử làm bài cùng thống kê điểm số.

Ứng dụng được xây dựng theo mô hình **Clean Architecture + MVVM**, toàn bộ giao diện dùng
**Jetpack Compose** và **Firebase** làm nền tảng serverless phía back-end.

---

### 1.2 Công nghệ sử dụng

| Thành phần | Công nghệ | Phiên bản / Ghi chú |
|---|---|---|
| Ngôn ngữ | Kotlin | JVM 17 |
| Giao diện người dùng | Jetpack Compose | Material 3 |
| Kiến trúc | Clean Architecture + MVVM | 3 tầng: `domain` / `data` / `ui` |
| Cơ sở dữ liệu cục bộ | Room (SQLite) | Version 7, `fallbackToDestructiveMigration` |
| Back-end | Firebase (serverless) | Authentication + Firestore + Functions |
| Cơ sở dữ liệu đám mây | Cloud Firestore | Real-time snapshot, batch writes |
| Xác thực người dùng | Firebase Authentication | Email/Password + Anonymous |
| Tải ảnh | Coil | `AsyncImage` từ URL (không dùng Firebase Storage) |
| JSON | Gson | `TypeConverter` trong Room + `EntityMappers` |
| Font chữ | Google Fonts (Compose) | Playfair Display (Serif) + Inter (Sans-Serif) |
| Tác vụ nền | WorkManager | `BackgroundSyncWorker` + `BackendMaintenanceWorker` |
| Dependencies injection | Manual DI | Không dùng Hilt/Dagger; `AppContainer` + `AppContainerImpl`|
| Xử lý annotation | KSP | Thay thế KAPT cho Room compiler |
| Build | Gradle Kotlin DSL | Plugin `google-services`, `dokka` |

---

### 1.3 Tính năng chính

| Nhóm tính năng | Tính năng | Mô tả ngắn |
|---|---|---|
| Xác thực | Đăng ký / Đăng nhập | Tạo tài khoản và đăng nhập bằng email qua Firebase Auth |
| Xác thực | Chế độ khách (Guest) | Làm quiz công khai hoặc qua mã chia sẻ mà không cần tài khoản |
| Tạo Quiz | Tạo & chỉnh sửa | Soạn quiz với tiêu đề, mô tả, tag và ảnh thumbnail (URL) |
| Tạo Quiz | Câu hỏi nhiều lựa chọn | Mỗi câu có 2–10 đáp án; hỗ trợ nhiều đáp án đúng cùng lúc |
| Tạo Quiz | Nhập hàng loạt CSV | Import câu hỏi từ file CSV với validate định dạng tự động |
| Tạo Quiz | Xem trước | Kiểm tra toàn bộ quiz trước khi xuất bản |
| Làm bài | Giao diện làm bài | Đồng hồ đếm ngược, xáo trộn thứ tự câu hỏi ngẫu nhiên |
| Làm bài | Kết quả & đánh giá | Xem điểm, xếp hạng sao và chi tiết từng câu trả lời |
| Tìm kiếm & Chia sẻ | Tìm kiếm công khai | Tìm quiz theo từ khoá, sắp xếp theo ngày / độ phổ biến / liên quan |
| Tìm kiếm & Chia sẻ | Lọc nâng cao | Lọc theo tag, trạng thái hiển thị và khoảng thời gian |
| Tìm kiếm & Chia sẻ | Chia sẻ qua mã | Tạo và nhập mã 6 ký tự để chia sẻ quiz private với người khác |
| Quản lý nội dung | Vòng đời quiz | Ba trạng thái: Draft (nháp) / Private (riêng tư) / Public (công khai) |
| Quản lý nội dung | Thùng rác | Khôi phục hoặc xóa vĩnh viễn quiz đã xóa (`RecycleBinViewModel`) |
| Quản lý nội dung | Lịch sử làm bài | Xem lại các lượt đã làm, điểm số và thời gian hoàn thành |
| Kho câu hỏi | Đóng góp | Gửi câu hỏi lên kho câu hỏi chung của cộng đồng |
| Kho câu hỏi | Sử dụng | Chọn câu hỏi từ kho chung để thêm vào quiz đang soạn |
| Quản trị hệ thống | Dashboard | Thống kê tổng quan: số quiz, người dùng, lượt làm bài |
| Quản trị hệ thống | Quản lý người dùng | Tìm kiếm, xem và phân quyền người dùng (lọc có thể thu gọn) |
| Quản trị hệ thống | Quản lý quiz | Xét duyệt, ẩn hoặc xóa quiz (lọc có thể thu gọn) |
| Quản trị hệ thống | Báo cáo | Thống kê hệ thống và xuất báo cáo |
| Đồng bộ | Local-first | Ghi Room trước, UI cập nhật tức thì, sync nền khi có mạng |
| Đồng bộ | Hàng đợi retry | `PendingSyncEntity` theo dõi thao tác chờ với `retryCount` / `maxRetries` |

---

### 1.4 Vai trò người dùng

| Vai trò | Mã | Mô tả | Quyền hạn chính |
|---|---|---|---|
| Khách | `GUEST` | Người dùng ẩn danh, không cần tài khoản | Làm quiz công khai hoặc qua mã chia sẻ; không lưu lịch sử cá nhân |
| Người dùng | `USER` | Tài khoản đăng nhập đầy đủ tính năng | Tạo / chỉnh sửa / xóa quiz; xem lịch sử; chia sẻ; đóng góp kho câu hỏi |
| Quản trị viên | `ADMIN` | Tài khoản được cấp quyền bởi Superuser | Quản lý người dùng & quiz; xem thống kê; quyền cụ thể do Superuser cấp |
| Siêu quản trị | `SUPERUSER` | Toàn quyền hệ thống, không thể bị thay đổi | Mọi quyền của Admin; cấp / thu hồi quyền Admin; trạng thái bất biến |

---

## 2. Kiến Trúc Hệ Thống

### 2.1 Sơ đồ kiến trúc tổng thể

    +-------------------------------------------------------------------+
    |                          UI LAYER                                 |
    |  Compose Screens: auth / home / search / quiz / create /          |
    |                   profile / history / review / pool /             |
    |                   trash / settings / attempt / admin              |
    |  ViewModels: MutableStateFlow + onEvent() (sealed class events)   |
    +-----------------------------+-------------------------------------+
                                  |  goi repository interfaces (domain)
                                  v
    +-------------------------------------------------------------------+
    |                        DOMAIN LAYER                               |
    |  model/      : Quiz, Question, Choice, Attempt, User, ...         |
    |  repository/ : QuizRepository, AttemptRepository, AuthRepo, ...   |
    |  util/       : QuizValidator, ScoreUtil, CsvParser, SafeCall, ... |
    +----------------+--------------------------------------+----------+
                     | implements                           |
                     v                                      v
    +-----------------------------+       +------------------+----------+
    |        DATA LAYER           |       |   DICH VU BEN NGOAI         |
    |                             |       |                             |
    |  Room (SQLite, version 7)   | <---> |  Firebase Cloud             |
    |  +-- DAOs (6)               | sync  |  +-- Cloud Firestore        |
    |  +-- Entities (6)           |       |  +-- Firebase Auth          |
    |  +-- PendingSyncEntity      |       |  +-- Firebase Functions     |
    |                             |       +-----------------------------+
    |  Remote DataSources (7)     |
    |  Repository Impls (7)       |
    |  SyncManager                |
    |  WorkManager Workers (2)    |
    |  NetworkMonitor             |
    +-----------------------------+

---

### 2.2 Ba tầng Clean Architecture

| Tầng | Package | Trách nhiệm | Phụ thuộc vào | Không được phụ thuộc vào |
|---|---|---|---|---|
| Domain | `domain/` | Pure Kotlin — models, interfaces repository, utilities nghiệp vụ. Tuyệt đối không import Android SDK hay Firebase. | Không có | `data/`, `ui/`, Android SDK, Firebase |
| Data | `data/` | Triển khai repository, Room entities & DAOs, Firebase DTOs & remote sources, đồng bộ dữ liệu, WorkManager workers | `domain/` | `ui/` |
| UI | `ui/` | Compose screens (stateless), ViewModels (sở hữu toàn bộ state), theme, navigation graph | `domain/`, `data/` (qua DI) | — (tầng trên cùng) |

---

### 2.3 Luồng dữ liệu Local-First

    Nguoi dung thuc hien thao tac
                |
                v
        ViewModel.onEvent()
                |
                v
        RepositoryImpl.someOperation()
                |
        +-------+-----------------------------+
        |                                     |
        v                                     v
    Room DB                            PendingSyncEntity
    (ghi ngay vao SQLite,              (syncStatus = PENDING,
     phat ra Flow -> UI)                retryCount, maxRetries)
        |                                     |
        v                                     v
    UI cap nhat tuc thi              SyncManager.enqueueSync()
    (khong cho mang)                          |
                                     NetworkMonitor.isOnline = true
                                              |
                                              v
                                     processPendingOperations()
                                              |
                                      +-------+--------+
                                      |                |
                                      v                v
                                   Firestore      Thanh cong:
                                (dong bo nen)     status = COMPLETED
                                                  That bai:
                                                  retry / FAILED

---

### 2.4 Cấu trúc thư mục

| Package | Thư mục con | Số file | Mục đích |
|---|---|---|---|
| `domain/model/` | — | 10 | Domain models: `Quiz`, `Question`, `Choice`, `Attempt`, `User`, `QuestionPoolItem`, `UserRole`, `SystemStats`, `AdminPermission`, `PaginatedResult` |
| `domain/repository/` | — | 7 | Repository interfaces: `QuizRepository`, `AttemptRepository`, `AuthRepository`, `ShareCodeRepository`, `PoolRepository`, `AdminRepository`, `SearchRepository` |
| `domain/util/` | — | 13 | Utilities: `QuizValidator`, `ScoreUtil`, `ScoreCalculator`, `CsvParser`, `CsvValidator`, `ChecksumUtil`, `QuestionShuffler`, `SearchFilterLogic`, `ShareCodeUtil`, `TimeFormatter`, `InputSanitizer`, `TagValidator`, `SafeCall` |
| `data/local/` | `dao/`, `entity/`, `converter/` | 16 | `AppDatabase` (v7), 6 DAOs, 6 Entities, `Converters`, `EntityMappers`, `LocalQuizPurger` |
| `data/remote/` | `firebase/`, `model/` | 15 | 7 Remote DataSources, 5 Firestore DTOs, `AppMappers`, `FirestoreCollections`, `FirestoreCascadeHelper` |
| `data/repository/` | — | 7 | Repository implementations: `QuizRepositoryImpl`, `AttemptRepositoryImpl`, `AuthRepositoryImpl`, `ShareCodeRepositoryImpl`, `PoolRepositoryImpl`, `AdminRepositoryImpl`, `SearchRepositoryImpl` |
| `data/sync/`, `data/worker/` | — | 4 | `SyncManager`, `QuizInvalidationManager`, `BackgroundSyncWorker`, `BackendMaintenanceWorker` |
| `data/network/`, `data/preferences/` | — | 2 | `NetworkMonitor` (ConnectivityManager), `SettingsPreferences` (DataStore) |
| `di/` | — | 3 | `AppContainer` (interface), `AppContainerImpl` (Firebase + Room init), `AppContainerExt` (`LocalAppContainer`) |
| `ui/screens/` | 14 sub-packages | 50+ | Màn hình + ViewModels: `auth`, `home`, `search`, `profile`, `quiz`, `create`, `history`, `review`, `attempt`, `trash`, `settings`, `pool`, `admin/dashboard`, `admin/users`, `admin/quizzes`, `admin/reports` |
| `ui/components/` | 7 sub-packages | 32 | Components tái sử dụng: `common/`, `feedback/`, `forms/`, `navigation/`, `quiz/`, `admin/`, standalone |
| `ui/navigation/` | — | 2 | `QuizzezNavHost` (nav graph), `Routes` (route constants + `NavigationDestination`) |
| `ui/theme/` | — | 4 | `QuizzezTheme`, Color (design tokens), `Type` (Google Fonts), `Shape` (`FullShape`) |

---

# 3. TẦNG DOMAIN (DOMAIN LAYER)

Domain là tầng trung tâm của Clean Architecture trong Quizzez. **Không import Android hay Firebase** — chỉ Kotlin thuần. Định nghĩa model nghiệp vụ, giao diện repository và tiện ích dùng chung cho toàn ứng dụng.

## 3.1 Domain Models (`domain/model/`)

### 3.1.1 `Quiz.kt`
| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Thực thể trung tâm; tách biệt khỏi tầng lưu trữ để mọi tầng dùng chung mà không phụ thuộc SDK. |
| **Vị trí (WHERE)** | `domain/model/Quiz.kt` |
| **Cách hoạt động (HOW)** | `data class` bất biến; ánh xạ sang `QuizEntity` (Room) và `QuizDto` (Firestore) ở tầng Data. |
| **Khi nào dùng (WHEN)** | Tạo, sửa, tìm kiếm, hiển thị, đồng bộ quiz ở mọi màn hình. |
| Thuộc tính | Kiểu | Mô tả |
|-----------|------|-------|
| `id` | `String` | Định danh duy nhất |
| `ownerId` | `String` | UID người tạo |
| `title` | `String` | Tiêu đề bài quiz |
| `description` | `String?` | Mô tả tùy chọn |
| `authorName` | `String` | Tên hiển thị tác giả |
| `thumbnailUrl` | `String?` | URL ảnh bìa |
| `tags` | `List<String>` | Danh sách nhãn phân loại |
| `questionCount` | `Int` | Số câu hỏi |
| `attemptCount` | `Int` | Lượt làm bài tích lũy |
| `isPublic` | `Boolean` | Công khai để tìm kiếm |
| `isDraft` | `Boolean` | Trạng thái nháp |
| `shareCode` | `String?` | Mã chia sẻ 6 ký tự |
| `checksum` | `String?` | SHA-256 kiểm tra toàn vẹn |
| `createdAt` | `Long` | Thời điểm tạo (ms) |
| `updatedAt` | `Long` | Thời điểm cập nhật (ms) |
| `deletedAt` | `Long?` | Thời điểm xóa mềm (ms) |
| `isRemovedFromCloud` | `Boolean` | Admin xóa Firestore nhưng bản ghi vẫn tồn tại local |

### 3.1.2 `Question.kt`
| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Lưu nội dung câu hỏi và danh sách đáp án phục vụ hiển thị, chấm điểm, xáo trộn. |
| **Vị trí (WHERE)** | `domain/model/Question.kt` |
| **Cách hoạt động (HOW)** | Chứa `List<Choice>` lồng nhau; `isMultiSelect=true` cho phép nhiều đáp án đúng (hỗ trợ 2–10 choices). |
| **Khi nào dùng (WHEN)** | Hiển thị khi làm bài, chấm điểm, xáo trộn, tính checksum. |
| Thuộc tính | Kiểu | Mô tả |
|-----------|------|-------|
| `id` | `String` | Định danh câu hỏi |
| `quizId` | `String` | ID bài quiz cha |
| `content` | `String` | Nội dung câu hỏi |
| `choices` | `List<Choice>` | Danh sách 2–10 đáp án |
| `isMultiSelect` | `Boolean` | Cho phép nhiều đáp án đúng |
| `explanation` | `String?` | Giải thích sau khi trả lời |
| `mediaUrl` | `String?` | URL hình ảnh đính kèm |
| `points` | `Int` | Điểm số câu (mặc định 1) |
| `position` | `Int` | Thứ tự hiển thị |

### 3.1.3 `Choice.kt`
| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Tách đáp án thành model riêng để xáo trộn và lưu trữ độc lập. |
| **Vị trí (WHERE)** | `domain/model/Choice.kt` |
| **Cách hoạt động (HOW)** | `data class` đơn giản; cờ `isCorrect` bảo toàn tự nhiên khi xáo trộn theo đối tượng. |
| **Khi nào dùng (WHEN)** | Render lựa chọn trong `ChoiceButton`, chấm điểm, tính checksum. |
| Thuộc tính | Kiểu | Mô tả |
|-----------|------|-------|
| `id` | `String` | Định danh đáp án |
| `content` | `String` | Nội dung đáp án |
| `isCorrect` | `Boolean` | Đánh dấu đáp án đúng |
| `position` | `Int` | Thứ tự hiển thị gốc |

### 3.1.4 `Attempt.kt`
| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Ghi lại một lần làm bài — điểm, câu trả lời, thời gian — để hiển thị lịch sử và xem lại đáp án. |
| **Vị trí (WHERE)** | `domain/model/Attempt.kt` |
| **Cách hoạt động (HOW)** | `answers: Map<questionId, List<choiceId>>` hỗ trợ multi-select; `questionOrder` lưu thứ tự đã xáo trộn. |
| **Khi nào dùng (WHEN)** | Sau nộp bài, `HistoryScreen`, `AnswerReviewScreen`, `AttemptDetailScreen`. |
| Thuộc tính | Kiểu | Mô tả |
|-----------|------|-------|
| `id` | `String` | Định danh lần làm bài |
| `userId` | `String` | UID người làm bài |
| `quizId` | `String` | ID bài quiz |
| `score` | `Int` | Số câu đúng |
| `totalQuestions` | `Int` | Tổng số câu hỏi |
| `answers` | `Map<String, List<String>>` | questionId → danh sách choiceId được chọn |
| `startTimeMillis` | `Long` | Thời điểm bắt đầu (ms) |
| `endTimeMillis` | `Long?` | Thời điểm kết thúc; `null` nếu chưa xong |
| `questionOrder` | `List<String>` | Thứ tự câu hỏi đã xáo trộn |

### 3.1.5 `User.kt`
| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Biểu diễn người dùng với phân quyền chi tiết; tách biệt khỏi `FirebaseUser`. |
| **Vị trí (WHERE)** | `domain/model/User.kt` |
| **Cách hoạt động (HOW)** | Có phương thức trợ giúp kiểm tra quyền; SUPERUSER ngầm có toàn bộ quyền qua `AdminPermission.all()`. |
| **Khi nào dùng (WHEN)** | Kiểm tra quyền admin, hiển thị hồ sơ, guard điều hướng. |
| Thuộc tính / Phương thức | Kiểu / Trả về | Mô tả |
|--------------------------|--------------|-------|
| `id` | `String` | UID |
| `email` | `String` | Email đăng nhập |
| `displayName` | `String` | Tên hiển thị |
| `username` | `String` | Tên người dùng duy nhất |
| `photoUrl` | `String?` | URL ảnh đại diện |
| `role` | `UserRole` | Vai trò (mặc định `USER`) |
| `isBanned` | `Boolean` | Tài khoản bị cấm |
| `permissions` | `Set<AdminPermission>` | Quyền admin được cấp |
| `isAdmin()` | `Boolean` | `true` nếu `ADMIN` hoặc `SUPERUSER` |
| `isSuperuser()` | `Boolean` | `true` nếu `SUPERUSER` |
| `hasPermission(p)` | `Boolean` | Kiểm tra quyền; SUPERUSER luôn `true` |
| `effectivePermissions()` | `Set<AdminPermission>` | Tập quyền thực tế (SUPERUSER = tất cả) |

### 3.1.6 `UserRole.kt`
| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Định nghĩa bậc phân quyền rõ ràng để kiểm soát truy cập và guard điều hướng. |
| **Vị trí (WHERE)** | `domain/model/UserRole.kt` |
| **Cách hoạt động (HOW)** | Enum 4 bậc từ thấp đến cao; `fromString()` parse an toàn với fallback `USER`. |
| **Khi nào dùng (WHEN)** | Guard `NavHost` admin, kiểm tra `isAdmin()`, phân quyền Firestore. |
| Giá trị | Mô tả |
|---------|-------|
| `GUEST` | Ẩn danh — chỉ làm bài quiz công khai |
| `USER` | Đã đăng ký — đầy đủ tính năng |
| `ADMIN` | Quản trị viên với quyền do SUPERUSER cấp |
| `SUPERUSER` | Chủ ứng dụng — toàn quyền, không thể bị hạ cấp |

### 3.1.7 `AdminPermission.kt`
| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Cho phép SUPERUSER giao quyền có chọn lọc thay vì trao toàn quyền admin. |
| **Vị trí (WHERE)** | `domain/model/AdminPermission.kt` |
| **Cách hoạt động (HOW)** | Enum; `all()` trả tập đầy đủ dùng cho SUPERUSER; lưu Firestore dạng lowercase. |
| **Khi nào dùng (WHEN)** | `hasPermission()`, cập nhật quyền từ panel admin, hiển thị danh sách quyền. |
| Giá trị | Mô tả |
|---------|-------|
| `MANAGE_USERS` | Xem và quản lý tài khoản người dùng |
| `CHANGE_USER_ROLES` | Thay đổi vai trò người dùng |
| `DELETE_USERS` | Xóa vĩnh viễn tài khoản |
| `BAN_USERS` | Cấm và bỏ cấm người dùng |
| `MANAGE_QUIZZES` | Xem và quản lý tất cả bài quiz |
| `DELETE_QUIZZES` | Xóa vĩnh viễn bài quiz |
| `PUBLISH_QUIZZES` | Buộc công khai hoặc ẩn bài quiz |
| `VIEW_REPORTS` | Xem báo cáo và thống kê hệ thống |

### 3.1.8 `QuestionPoolItem.kt`
| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Hồ câu hỏi dùng chung cho phép tái sử dụng câu hỏi và sinh quiz tự động theo tag. |
| **Vị trí (WHERE)** | `domain/model/QuestionPoolItem.kt` |
| **Cách hoạt động (HOW)** | Bọc `Question`; `isActive=false` khi bị thu hồi; `usageCount` tăng mỗi lần dùng. |
| **Khi nào dùng (WHEN)** | `QuestionPoolScreen`, sinh quiz tự động từ `autoGenerateQuiz()`. |
| Thuộc tính | Kiểu | Mô tả |
|-----------|------|-------|
| `id` | `String` | Định danh mục hồ |
| `question` | `Question` | Dữ liệu câu hỏi |
| `contributorId` | `String?` | UID người đóng góp (null nếu ẩn danh) |
| `sourceQuizId` | `String` | ID bài quiz nguồn |
| `tags` | `List<String>` | Nhãn phân loại |
| `usageCount` | `Int` | Số lần đã được dùng |
| `isActive` | `Boolean` | Còn hoạt động (`false` = đã thu hồi) |
| `createdAtMillis` | `Long` | Thời điểm đóng góp (ms) |

### 3.1.9 `SystemStats.kt`
| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Snapshot thống kê toàn hệ thống cho dashboard admin, tránh truy vấn nhiều collection. |
| **Vị trí (WHERE)** | `domain/model/SystemStats.kt` |
| **Cách hoạt động (HOW)** | `data class` với 9 trường đếm và 3 thuộc tính tính toán: `averageAttemptsPerQuiz`, `activeUserPercentage`, `publicQuizPercentage`. |
| **Khi nào dùng (WHEN)** | `AdminDashboardScreen` hiển thị biểu đồ và thẻ thống kê. |
| Thuộc tính | Kiểu | Mô tả |
|-----------|------|-------|
| `totalUsers` | `Int` | Tổng người dùng đã đăng ký |
| `totalQuizzes` | `Int` | Tổng bài quiz (không tính xóa mềm) |
| `totalAttempts` | `Int` | Tổng lượt làm bài |
| `totalQuestionsInPool` | `Int` | Tổng câu hỏi trong hồ dùng chung |
| `activeUsers` | `Int` | Người dùng hoạt động 30 ngày gần nhất |
| `publicQuizzes` | `Int` | Số quiz công khai |
| `privateQuizzes` | `Int` | Số quiz riêng tư |
| `deletedQuizzes` | `Int` | Số quiz trong thùng rác |
| `adminUsers` | `Int` | Số quản trị viên |

### 3.1.10 `PaginatedResult.kt`
| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Wrapper chung cho kết quả phân trang; tránh định nghĩa lại cấu trúc trang ở mỗi repository. |
| **Vị trí (WHERE)** | `domain/model/PaginatedResult.kt` |
| **Cách hoạt động (HOW)** | Generic `data class<T>`; `hasMore=true` khi còn dữ liệu tiếp theo chưa tải. |
| **Khi nào dùng (WHEN)** | Mọi API phân trang trong `PoolRepository`, `AdminRepository`, `AttemptRepository`. |
| Thuộc tính | Kiểu | Mô tả |
|-----------|------|-------|
| `items` | `List<T>` | Danh sách mục trong trang hiện tại |
| `hasMore` | `Boolean` | `true` nếu còn trang tiếp theo |

## 3.2 Repository Interfaces (`domain/repository/`)

### 3.2.1 `AuthRepository.kt`
| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Trừu tượng hóa FirebaseAuth; ViewModel không phụ thuộc trực tiếp SDK Firebase. |
| **Vị trí (WHERE)** | `domain/repository/AuthRepository.kt` |
| **Cách hoạt động (HOW)** | `currentUser` là `Flow<User?>` real-time; thao tác bất đồng bộ trả `Result<T>`. |
| **Khi nào dùng (WHEN)** | `AuthViewModel`, `EditProfileViewModel`, mọi màn hình kiểm tra đăng nhập. |
| Phương thức / Thuộc tính | Trả về | Mô tả |
|--------------------------|--------|-------|
| `currentUser` | `Flow<User?>` | Phát người dùng hiện tại hoặc null |
| `isLoggedIn` | `Boolean` | Trạng thái đăng nhập đồng bộ |
| `login(email, password)` | `Result<User>` | Đăng nhập email/mật khẩu |
| `register(email, password, username)` | `Result<User>` | Tạo tài khoản mới |
| `logout()` | `Unit` | Đăng xuất |
| `getCurrentUser()` | `User?` | Lấy người dùng hiện tại (one-shot) |
| `sendPasswordResetEmail(email)` | `Result<Unit>` | Gửi email đặt lại mật khẩu |
| `deleteAccount()` | `Result<Unit>` | Xóa tài khoản + dọn dẹp local |
| `generateGuestId()` | `String` | Sinh UUID cho khách |
| `refreshSession()` | `Result<Unit>` | Làm mới token xác thực |
| `updateProfile(displayName, photoUrl)` | `Result<Unit>` | Cập nhật tên và ảnh đại diện |

### 3.2.2 `QuizRepository.kt`
| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Giao diện CRUD trung tâm cho bài quiz; chiến lược local-first với Room là nguồn chính. |
| **Vị trí (WHERE)** | `domain/repository/QuizRepository.kt` |
| **Cách hoạt động (HOW)** | Trả `Flow` từ Room, đồng bộ Firestore nền; ghi Room trước với `syncStatus=PENDING`. Các biến thể `*Limited(limit)` hỗ trợ phân trang động. |
| **Khi nào dùng (WHEN)** | `HomeViewModel`, `CreateQuizViewModel`, `EditQuizViewModel`, `TrashScreen`, `SearchViewModel`. |
| Phương thức | Trả về | Mô tả |
|------------|--------|-------|
| `getHomeQuizzes(userId)` | `Flow<HomeQuizzes>` | Quiz trang chủ: gần đây / của tôi / trending |
| `getMyQuizzes(userId)` | `Flow<List<Quiz>>` | Quiz của người dùng chưa xóa |
| `getPublicQuizzes()` | `Flow<List<Quiz>>` | Quiz công khai theo lượt phổ biến |
| `searchQuizzes(query)` | `Flow<List<Quiz>>` | Tìm kiếm theo tiêu đề |
| `getDeletedQuizzes(userId)` | `Flow<List<Quiz>>` | Thùng rác của người dùng |
| `getQuizById(quizId)` | `Quiz?` | Lấy quiz theo ID |
| `getQuizByShareCode(shareCode)` | `Quiz?` | Lấy quiz theo mã chia sẻ |
| `saveQuiz(quiz, questions)` | `Result<Unit>` | Tạo mới quiz + câu hỏi |
| `updateQuiz(quiz, questions)` | `Result<Unit>` | Cập nhật quiz và câu hỏi |
| `deleteQuiz(quizId)` | `Result<Unit>` | Xóa mềm — vào thùng rác |
| `restoreQuiz(quizId)` | `Result<Unit>` | Khôi phục từ thùng rác |
| `permanentlyDeleteQuiz(quizId)` | `Result<Unit>` | Xóa vĩnh viễn khỏi Room và Firestore |
| `incrementAttemptCount(quizId)` | `Result<Unit>` | Tăng lượt làm bài (atomic) |
| `getTrendingQuizzes()` | `Flow<List<Quiz>>` | Quiz phổ biến nhất |
| `emptyTrash(userId)` | `Result<Unit>` | Dọn sạch thùng rác |
| `getAllTags()` | `List<String>` | Tất cả tag phân biệt |
| `refreshQuizFromRemote(quizId)` | `Result<Quiz>` | Đồng bộ lại một quiz từ Firestore vào Room |

### 3.2.3 `AttemptRepository.kt`
| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Quản lý lịch sử làm bài; hỗ trợ liên kết attempt của khách sau khi đăng ký. |
| **Vị trí (WHERE)** | `domain/repository/AttemptRepository.kt` |
| **Cách hoạt động (HOW)** | Ghi Room trước, đồng bộ Firestore nền; `linkGuestAttempts` di chuyển `userId` hàng loạt. |
| **Khi nào dùng (WHEN)** | `TakeQuizViewModel`, `HistoryViewModel`, `QuizResultViewModel`. |
| Phương thức | Trả về | Mô tả |
|------------|--------|-------|
| `getAttemptsByUser(userId)` | `Flow<List<Attempt>>` | Toàn bộ lịch sử người dùng |
| `getAttemptsByQuiz(quizId)` | `Flow<List<Attempt>>` | Lịch sử theo bài quiz |
| `getAttemptById(attemptId)` | `Attempt?` | Lấy một lần làm bài |
| `getLatestAttempt(userId, quizId)` | `Attempt?` | Lần làm gần nhất |
| `saveAttempt(attempt)` | `Result<String>` | Lưu lần làm bài mới, trả về ID |
| `updateAttempt(attempt)` | `Result<Unit>` | Cập nhật khi nộp bài |
| `linkGuestAttempts(guestId, userId)` | `Result<Int>` | Chuyển attempt khách → tài khoản mới |
| `getAttemptsByUserLimited(userId, limit)` | `Flow<List<Attempt>>` | Phân trang động lịch sử |

### 3.2.4 `ShareCodeRepository.kt`
| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Quản lý ánh xạ mã chia sẻ → quizId; remote-only vì mã cần nhất quán toàn cầu. |
| **Vị trí (WHERE)** | `domain/repository/ShareCodeRepository.kt` |
| **Cách hoạt động (HOW)** | Sinh mã thử tối đa 10 lần đảm bảo duy nhất; xóa mã cũ sau khi mã mới được lưu thành công. |
| **Khi nào dùng (WHEN)** | Khi tạo/sửa quiz; khi người dùng nhập mã để tìm quiz. |
| Phương thức | Trả về | Mô tả |
|------------|--------|-------|
| `lookupQuizId(shareCode)` | `Result<String?>` | Tra cứu quizId từ mã |
| `generateShareCode(quizId)` | `Result<String>` | Sinh mã duy nhất (retry ≤ 10 lần) |
| `deleteShareCode(shareCode)` | `Result<Unit>` | Xóa mã khi quiz bị xóa vĩnh viễn |
| `regenerateShareCode(quizId, oldShareCode)` | `Result<String>` | Thay mã cũ bằng mã mới |
| `validateShareCode(shareCode)` | `Result<String>` | Xác thực mã và trả về quizId |

### 3.2.5 `PoolRepository.kt`
| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Hồ câu hỏi dùng chung cho tái sử dụng và sinh quiz tự động; remote-only. |
| **Vị trí (WHERE)** | `domain/repository/PoolRepository.kt` |
| **Cách hoạt động (HOW)** | `contributeQuestions` dùng batch write; `revokeContribution` chỉ đặt `isActive=false`. |
| **Khi nào dùng (WHEN)** | `QuestionPoolScreen`, `QuestionPoolViewModel`, sinh quiz tự động. |
| Phương thức | Trả về | Mô tả |
|------------|--------|-------|
| `contributeQuestion(poolItem)` | `Result<Unit>` | Đóng góp một câu hỏi |
| `contributeQuestions(questions, contributorId, sourceQuizId, tags, anonymize)` | `Result<Unit>` | Đóng góp nhiều câu hỏi (có tùy chọn ẩn danh) |
| `getPoolQuestionsByTags(tags, activeOnly)` | `Result<List<QuestionPoolItem>>` | Lấy câu hỏi theo tag |
| `getMyContributions(userId)` | `Result<List<QuestionPoolItem>>` | Câu hỏi tôi đã đóng góp |
| `revokeContribution(poolItemId)` | `Result<Unit>` | Thu hồi đóng góp (`isActive=false`) |
| `incrementUsageCount(poolItemId)` | `Result<Unit>` | Tăng bộ đếm sử dụng |
| `autoGenerateQuiz(tags, count)` | `Result<List<QuestionPoolItem>>` | Sinh quiz tự động từ hồ câu hỏi |

### 3.2.6 `AdminRepository.kt`
| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Tập trung tác vụ quản trị cần quyền nâng cao; remote-only, không cache local. |
| **Vị trí (WHERE)** | `domain/repository/AdminRepository.kt` |
| **Cách hoạt động (HOW)** | `Flow` cho danh sách real-time; `Result` cho thao tác ghi; phân trang cursor-based. |
| **Khi nào dùng (WHEN)** | `AdminDashboardScreen`, `AdminUserManagementScreen`, `AdminQuizManagementScreen`, `AdminReportsScreen`. |
| Phương thức | Trả về | Mô tả |
|------------|--------|-------|
| `getAllUsers()` | `Flow<List<User>>` | Tất cả người dùng real-time |
| `updateUserRole(userId, newRole)` | `Result<Unit>` | Thay đổi vai trò |
| `banUser(userId)` | `Result<Unit>` | Cấm tài khoản |
| `unbanUser(userId)` | `Result<Unit>` | Bỏ cấm tài khoản |
| `deleteUserPermanently(userId)` | `Result<Unit>` | Xóa vĩnh viễn người dùng |
| `getAllQuizzes(includeDeleted)` | `Flow<List<Quiz>>` | Tất cả quiz real-time |
| `deleteQuizPermanently(quizId)` | `Result<Unit>` | Xóa vĩnh viễn quiz |
| `restoreQuiz(quizId)` | `Result<Unit>` | Khôi phục quiz từ thùng rác |
| `forcePublishQuiz(quizId)` | `Result<Unit>` | Buộc công khai quiz |
| `unpublishQuiz(quizId)` | `Result<Unit>` | Ẩn quiz |
| `getSystemStats()` | `Flow<SystemStats>` | Thống kê hệ thống |
| `searchUsers(query)` | `Flow<List<User>>` | Tìm kiếm người dùng |
| `searchQuizzes(query, includeDeleted)` | `Flow<List<Quiz>>` | Tìm kiếm quiz |
| `getUsersPage(pageSize, loadMore)` | `PaginatedResult<User>` | Phân trang người dùng |
| `getQuizzesPage(pageSize, includeDeleted, loadMore)` | `PaginatedResult<Quiz>` | Phân trang quiz |
| `updateAdminPermissions(userId, permissions)` | `Result<Unit>` | Cập nhật quyền admin |
| `getCurrentAdminPermissions()` | `Set<AdminPermission>` | Quyền thực tế của admin hiện tại |

### 3.2.7 `SearchRepository.kt`
| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Quản lý lịch sử tìm kiếm cục bộ; tách biệt khỏi logic tìm kiếm Firestore. |
| **Vị trí (WHERE)** | `domain/repository/SearchRepository.kt` |
| **Cách hoạt động (HOW)** | `getRecentSearches()` phát `Flow<List<String>>`; impl dùng SharedPreferences. |
| **Khi nào dùng (WHEN)** | `SearchScreen` hiển thị gợi ý từ khóa gần đây. |
| Phương thức | Trả về | Mô tả |
|------------|--------|-------|
| `getRecentSearches()` | `Flow<List<String>>` | Danh sách từ khóa tìm kiếm gần đây |
| `addRecentSearch(query)` | `Unit` | Thêm từ khóa vào lịch sử |
| `clearRecentSearches()` | `Unit` | Xóa toàn bộ lịch sử |

## 3.3 Domain Utilities (`domain/util/`)

### 3.3.1 `ScoreCalculator.kt`
| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Tập trung logic chấm điểm point-based; hỗ trợ đơn và đa lựa chọn bằng exact set equality. |
| **Vị trí (WHERE)** | `domain/util/ScoreCalculator.kt` |
| **Cách hoạt động (HOW)** | `userChoiceIds == correctChoiceIds` → cộng `Question.points`; kết quả trả trong `PointScoreResult`. |
| **Khi nào dùng (WHEN)** | `QuizResultViewModel` sau khi người dùng nộp bài. |
| Phương thức / Kiểu | Mô tả |
|-------------------|-------|
| `calculatePointScore(questions, userAnswers: Map<String, Set<String>>)` → `PointScoreResult` | Tính tổng điểm dựa trên `Question.points` cho mỗi câu đúng |
| `PointScoreResult.earnedScore: Int` | Tổng điểm kiếm được |
| `PointScoreResult.maxScore: Int` | Tổng điểm tối đa |
| `PointScoreResult.correctCount: Int` | Số câu trả lời đúng |
| `PointScoreResult.wrongCount: Int` | Số câu trả lời sai |

### 3.3.2 `ScoreUtil.kt`
| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Chuyển điểm số thô thành sao và phần trăm để hiển thị trực quan kết quả. |
| **Vị trí (WHERE)** | `domain/util/ScoreUtil.kt` |
| **Cách hoạt động (HOW)** | Hàm thuần túy, không trạng thái; `calculateStarRating` dùng ngưỡng cố định. |
| **Khi nào dùng (WHEN)** | `ScoreCard` component, màn hình kết quả, lịch sử làm bài. |
| Phần trăm | Số sao |
|-----------|--------|
| ≥ 90% | 5 ★ |
| ≥ 80% | 4 ★ |
| ≥ 60% | 3 ★ |
| ≥ 40% | 2 ★ |
| ≥ 20% | 1 ★ |
| < 20% | 0 ★ |
| Phương thức | Trả về | Mô tả |
|------------|--------|-------|
| `calculateStarRating(percentage: Int)` | `Int` | Số sao 0–5 theo ngưỡng trên |
| `calculatePercentage(score, maxScore)` | `Int` | Phần trăm 0–100; trả 0 nếu maxScore = 0 |

### 3.3.3 `QuizValidator.kt`
| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Đảm bảo quiz hợp lệ trước khi lưu; generic để tái dùng cho cả domain model lẫn UI draft. |
| **Vị trí (WHERE)** | `domain/util/QuizValidator.kt` |
| **Cách hoạt động (HOW)** | `validate<Q, C>()` nhận selector functions; trả `QuizValidationResult` với `errorCode` để UI ánh xạ `stringResource`. |
| **Khi nào dùng (WHEN)** | `CreateQuizViewModel`, `EditQuizViewModel`, `CsvImportViewModel` trước khi lưu. |
| Quy tắc | `errorCode` | Mô tả |
|---------|-------------|-------|
| Ít nhất 1 câu hỏi | `QUIZ_TOO_FEW_QUESTIONS` | Quiz phải có ít nhất một câu |
| 2–10 đáp án mỗi câu | `QUESTION_INVALID_CHOICE_COUNT` | Số lựa chọn phải trong khoảng 2–10 |
| Nội dung câu không trống | `QUESTION_BLANK` | Nội dung câu hỏi bắt buộc |
| Nội dung đáp án không trống | `CHOICE_BLANK` | Nội dung đáp án bắt buộc |
| Ít nhất 1 đáp án đúng | `QUESTION_NO_CORRECT_CHOICE` | Mỗi câu phải có ≥ 1 đáp án đúng |

### 3.3.4 `ChecksumUtil.kt`
| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Phát hiện thay đổi nội dung quiz sau đồng bộ; tránh so sánh từng trường thủ công. |
| **Vị trí (WHERE)** | `domain/util/ChecksumUtil.kt` |
| **Cách hoạt động (HOW)** | SHA-256 trên chuỗi nối: tiêu đề + mô tả + câu hỏi (sort `position`) + đáp án (sort `position`). |
| **Khi nào dùng (WHEN)** | Sau khi đồng bộ từ Firestore để xác minh toàn vẹn dữ liệu. |
| Phương thức | Trả về | Mô tả |
|------------|--------|-------|
| `computeQuizChecksum(quiz, questions)` | `String` | Chuỗi hex SHA-256 deterministic |

### 3.3.5 `QuestionShuffler.kt`
| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Xáo trộn câu hỏi và đáp án mỗi lần làm bài để tránh ghi nhớ vị trí. |
| **Vị trí (WHERE)** | `domain/util/QuestionShuffler.kt` |
| **Cách hoạt động (HOW)** | Generic `shuffle<Q, C>()`; dùng selector functions — `isCorrect` bảo toàn tự nhiên theo đối tượng. |
| **Khi nào dùng (WHEN)** | `TakeQuizViewModel` khi bắt đầu một lần làm bài mới. |
| Phương thức | Trả về | Mô tả |
|------------|--------|-------|
| `shuffle(questions, getChoices, copyWithNewChoices)` | `List<Q>` | Câu hỏi và đáp án đã xáo trộn |

### 3.3.6 `CsvParser.kt`
| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Nhập hàng loạt câu hỏi qua file CSV; thuần Kotlin, không phụ thuộc Android I/O. |
| **Vị trí (WHERE)** | `domain/util/CsvParser.kt` |
| **Cách hoạt động (HOW)** | Dòng đầu là header; xử lý giá trị có dấu nháy kép bao quanh dấu phẩy bên trong. |
| **Khi nào dùng (WHEN)** | `CsvImportViewModel` sau khi tầng Data đọc nội dung file từ URI. |
| Phương thức | Trả về | Mô tả |
|------------|--------|-------|
| `parse(csvContent, delimiter = ',')` | `List<Map<String, String>>` | Parse CSV thành danh sách hàng (header → giá trị) |

### 3.3.7 `CsvValidator.kt`
| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Xác thực dữ liệu CSV đã parse; báo lỗi theo số dòng để người dùng dễ sửa. |
| **Vị trí (WHERE)** | `domain/util/CsvValidator.kt` |
| **Cách hoạt động (HOW)** | Kiểm tra từng hàng có đủ cột bắt buộc; trả `List<CsvValidationError>` với số dòng cụ thể. |
| **Khi nào dùng (WHEN)** | `CsvImportViewModel` sau khi gọi `CsvParser.parse()`. |
| Phương thức | Trả về | Mô tả |
|------------|--------|-------|
| `validate(parsedRows, requiredHeaders)` | `List<CsvValidationError>` | Danh sách lỗi; rỗng = hợp lệ |

### 3.3.8 `SearchFilterLogic.kt`
| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Lọc quiz theo tag, visibility và khoảng thời gian hoàn toàn phía client. |
| **Vị trí (WHERE)** | `domain/util/SearchFilterLogic.kt` |
| **Cách hoạt động (HOW)** | Tag dùng logic OR (ít nhất 1 tag khớp); visibility và date range kết hợp AND; `null` = bỏ qua bộ lọc. |
| **Khi nào dùng (WHEN)** | `SearchViewModel` khi áp dụng bộ lọc nâng cao trên kết quả đã tải về. |
| Phương thức | Trả về | Mô tả |
|------------|--------|-------|
| `filter(items, queryTags, isPublic, startDateMillis, endDateMillis, getTags, getIsPublic, getTimestampMillis)` | `List<T>` | Danh sách mục đã lọc |

### 3.3.9 `ShareCodeUtil.kt`
| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Sinh mã chia sẻ ngắn, dễ nhớ, tránh nhầm lẫn giữa các ký tự tương đồng. |
| **Vị trí (WHERE)** | `domain/util/ShareCodeUtil.kt` |
| **Cách hoạt động (HOW)** | Chọn ngẫu nhiên 6 ký tự từ bảng `A–Z` + `0–9` (chữ hoa — tránh nhầm `l`/`1`, `O`/`0`). |
| **Khi nào dùng (WHEN)** | `ShareCodeRepositoryImpl.generateShareCode()` gọi để tạo ứng viên mã mới. |
| Phương thức | Trả về | Mô tả |
|------------|--------|-------|
| `generateCode()` | `String` | Mã 6 ký tự chữ hoa + số ngẫu nhiên |

### 3.3.10 `InputSanitizer.kt`
| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Bảo vệ khỏi XSS và injection trước khi lưu dữ liệu người dùng vào Firestore. |
| **Vị trí (WHERE)** | `domain/util/InputSanitizer.kt` |
| **Cách hoạt động (HOW)** | Xóa ký tự điều khiển, escape HTML, phát hiện `<script>` / `javascript:`; `sanitizeForFirestore` cắt tại 10.000 ký tự. |
| **Khi nào dùng (WHEN)** | Repository impl trước khi ghi Firestore; ViewModel khi xử lý input từ form. |
| Phương thức | Mô tả |
|------------|-------|
| `sanitizeText(input, maxLength)` | Trim, xóa ký tự điều khiển, giới hạn độ dài (mặc định 1.000) |
| `sanitizeHtml(input)` | Escape HTML: `&`, `<`, `>`, `"`, `'` |
| `containsProhibitedContent(input)` | Phát hiện script tag / javascript: URL / data: URL |
| `sanitizeForFirestore(input)` | Làm sạch và giới hạn 10.000 ký tự cho Firestore |

### 3.3.11 `TagValidator.kt`
| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Đảm bảo tag nhất quán (lowercase, không trùng) và hợp lệ (1–50 ký tự, ký tự cho phép). |
| **Vị trí (WHERE)** | `domain/util/TagValidator.kt` |
| **Cách hoạt động (HOW)** | `normalizeTag` chuẩn hóa trước; `validateTag` kiểm tra regex `[\w\s-]`; `normalizeTags` loại trùng. |
| **Khi nào dùng (WHEN)** | `CreateQuizViewModel`, `EditQuizViewModel` khi thêm tag vào bài quiz. |
| Phương thức | Trả về | Mô tả |
|------------|--------|-------|
| `normalizeTag(tag)` | `String` | Trim, lowercase, gộp khoảng trắng/gạch dưới liên tiếp |
| `validateTag(tag)` | `TagValidationResult` | Kiểm tra độ dài và ký tự cho phép |
| `normalizeTags(tags)` | `List<String>` | Chuẩn hóa toàn bộ danh sách + loại trùng |

### 3.3.12 `TimeFormatter.kt`
| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Định dạng thời gian nhất quán cho đồng hồ đếm ngược và thời lượng làm bài. |
| **Vị trí (WHERE)** | `domain/util/TimeFormatter.kt` |
| **Cách hoạt động (HOW)** | Trả `MM:SS` khi dưới 1 giờ, `HH:MM:SS` từ 1 giờ trở lên; giá trị âm trả `"00:00"`. |
| **Khi nào dùng (WHEN)** | `TimerDisplay` component, màn hình kết quả, lịch sử làm bài. |
| Phương thức | Trả về | Mô tả |
|------------|--------|-------|
| `formatDuration(totalSeconds: Long)` | `String` | `MM:SS` hoặc `HH:MM:SS` tùy thời lượng |

### 3.3.13 `SafeCall.kt`
| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Loại bỏ boilerplate `try/catch` lặp đi lặp lại trong tất cả repository implementation. |
| **Vị trí (WHERE)** | `domain/util/SafeCall.kt` |
| **Cách hoạt động (HOW)** | Inline suspend wrapper: thành công trả `Result.success(value)`, mọi exception trả `Result.failure(e)`. |
| **Khi nào dùng (WHEN)** | Tất cả `*RepositoryImpl` khi gọi Firestore, Firebase Auth hoặc Room. |
| Phương thức | Trả về | Mô tả |
|------------|--------|-------|
| `safeCall { block }` | `Result<T>` | Bọc suspend block trong try/catch, trả về `Result` |# 4. TẦNG DỮ LIỆU (DATA LAYER)

Tầng dữ liệu triển khai các interface repository được định nghĩa ở tầng domain, đóng vai trò cầu nối giữa nguồn dữ liệu cục bộ (Room/SQLite) và nguồn dữ liệu từ xa (Firebase Firestore). Tầng này **không bao giờ** được tham chiếu trực tiếp từ UI — mọi giao tiếp đều thông qua interface domain. Chiến lược chính là **local-first**: ghi vào Room trước, đồng bộ lên Firestore ở nền sau.

---

## 4.1 Cơ sở dữ liệu cục bộ — Room (`data/local/`)

### 4.1.1 `AppDatabase.kt`

| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Cung cấp lớp SQLite cục bộ cho toàn bộ ứng dụng; là nền tảng của chiến lược local-first |
| **Vị trí (WHERE)** | `data/local/AppDatabase.kt` |
| **Cách hoạt động (HOW)** | Khai báo `@Database` với 6 entity và 6 DAO; dùng `fallbackToDestructiveMigration` vì dữ liệu local chỉ là cache Firestore, không cần migrate thủ công |
| **Khi nào dùng (WHEN)** | Khởi tạo một lần duy nhất tại `AppContainerImpl`; được inject vào tất cả repository cần Room |

```app/src/main/java/com/example/androidapp/data/local/AppDatabase.kt#L34-46
@Database(
    entities = [
        QuizEntity::class,
        QuestionEntity::class,
        ChoiceEntity::class,
        AttemptEntity::class,
        UserEntity::class,
        PendingSyncEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() { ... }
```

**Phiên bản hiện tại:** 7 — mỗi lần nâng version sẽ xóa và tạo lại toàn bộ DB cục bộ.

---

### 4.1.2 Entities (`data/local/entity/`)

| Entity | Bảng DB | Các trường quan trọng | Đặc điểm lưu trữ |
|--------|---------|----------------------|------------------|
| `QuizEntity` | `quizzes` | `id`, `ownerId`, `title`, `tags`, `syncStatus`, `deletedAt`, `isRemovedFromCloud` | `tags` lưu dạng chuỗi phân cách bằng dấu phẩy |
| `QuestionEntity` | `questions` | `id`, `quizId`, `content`, `mediaUrl`, `points`, `position`, `syncStatus` | — |
| `ChoiceEntity` | `choices` | `id`, `questionId`, `content`, `isCorrect`, `position` | — |
| `AttemptEntity` | `attempts` | `id`, `userId`, `quizId`, `score`, `answers`, `questionOrder` | `answers` và `questionOrder` lưu JSON (Gson) |
| `UserEntity` | `users` | `id`, `email`, `displayName`, `role`, `permissions`, `isBanned` | `permissions` lưu JSON (Gson) |
| `PendingSyncEntity` | `pending_sync_operations` | `entityType`, `operation`, `entityId`, `payload`, `retryCount`, `maxRetries`, `status` | Hàng đợi retry có giới hạn `maxRetries = 3` |

**Enum hỗ trợ:**

| Enum | Giá trị |
|------|---------|
| `SyncStatus` | `PENDING` \| `SYNCING` \| `SYNCED` \| `FAILED` |
| `SyncEntityType` | `QUIZ` \| `QUESTION` \| `CHOICE` \| `ATTEMPT` |
| `SyncOperation` | `CREATE` \| `UPDATE` \| `DELETE` |
| `PendingSyncStatus` | `PENDING` \| `IN_PROGRESS` \| `FAILED` \| `COMPLETED` |

Trường `isRemovedFromCloud` trên `QuizEntity` đánh dấu quiz bị admin xóa vĩnh viễn khỏi Firestore nhưng vẫn còn trong Room cục bộ; UI hiển thị cảnh báo cho người dùng.

---

### 4.1.3 DAOs (`data/local/dao/`)

| DAO | File | Phương thức chính | Kiểu trả về |
|-----|------|-------------------|-------------|
| `QuizDao` | `QuizDao.kt` | `getQuizzesByOwner`, `getAllQuizzes`, `getPublicQuizzes`, `searchQuizzes`, `getDeletedQuizzes`, `getQuizById`, `insertQuiz`, `updateQuiz`, `softDeleteQuiz`, `restoreQuiz`, `deleteQuizById`, `updateSyncStatus`, `incrementAttemptCount`, `markRemovedFromCloud`, các biến thể `*Limited` và `*Count` | `Flow<List<QuizEntity>>`, `suspend` |
| `QuestionDao` | `QuestionDao.kt` | `getByQuizId`, `getQuestionsByQuizIdOnce`, `insert`, `update`, `delete`, `updateSyncStatus` | `Flow<List<QuestionEntity>>` |
| `ChoiceDao` | `ChoiceDao.kt` | `getByQuestionId`, `insert`, `update`, `delete`, `deleteChoicesByQuestionId` | `List<ChoiceEntity>` |
| `AttemptDao` | `AttemptDao.kt` | `getByUserId`, `getByQuizId`, `getById`, `getLatestByQuizId`, `insert`, `update` | `Flow<List<AttemptEntity>>` |
| `UserDao` | `UserDao.kt` | `getById`, `insert`, `update`, `delete` | `UserEntity?` |
| `PendingSyncDao` | `PendingSyncDao.kt` | `getPendingOperations`, `getPendingCount`, `observePendingCount`, `insertOperation`, `updateStatus`, `incrementRetryCount`, `deleteCompletedOperations`, `resetFailedToPending` | `Flow<Int>` |

`PendingSyncDao.observePendingCount()` trả về `Flow<Int>` được UI dùng để hiển thị badge đồng bộ đang chờ xử lý.

---

### 4.1.4 `EntityMappers.kt`

| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Tách biệt hoàn toàn cấu trúc Room Entity khỏi domain model; domain không phụ thuộc vào Room |
| **Vị trí (WHERE)** | `data/local/EntityMappers.kt` |
| **Cách hoạt động (HOW)** | Extension functions `Entity.toDomain()` và `Domain.toEntity(syncStatus)`; dùng Gson để deserialize các trường JSON như `answers`, `permissions`, `questionOrder`; `tags` được join/split bằng dấu phẩy |
| **Khi nào dùng (WHEN)** | Gọi trong tất cả repository khi đọc từ Room (→ domain) hoặc ghi vào Room (→ entity) |

```app/src/main/java/com/example/androidapp/data/local/EntityMappers.kt#L23-31
fun QuizEntity.toDomain(): Quiz = Quiz(
    id = id,
    ownerId = ownerId,
    title = title,
    tags = if (tags.isBlank()) emptyList()
           else tags.split(",").map { it.trim() },
    isRemovedFromCloud = isRemovedFromCloud,
    // ...
)
```

---

### 4.1.5 `Converters.kt` (`data/local/converter/`)

| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Room không tự lưu được `List<String>`, `Map<String, List<String>>`, hay `Set<AdminPermission>` — cần TypeConverter |
| **Vị trí (WHERE)** | `data/local/converter/Converters.kt` |
| **Cách hoạt động (HOW)** | `@TypeConverters` dùng Gson: serialize object → JSON String khi ghi; deserialize JSON String → object khi đọc |
| **Khi nào dùng (WHEN)** | Đăng ký tự động với `AppDatabase` qua annotation `@TypeConverters(Converters::class)` |

---

### 4.1.6 `LocalQuizPurger.kt`

| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Tập trung logic xóa quiz cục bộ (quiz → questions → choices) tránh lặp code ở nhiều class |
| **Vị trí (WHERE)** | `data/local/LocalQuizPurger.kt` |
| **Cách hoạt động (HOW)** | `object` singleton với `purgeLocalQuiz(quizId, quizDao, questionDao, choiceDao)`: lấy questions → xóa choices từng question → xóa questions → xóa quiz. Attempts **không** bị xóa (bảo toàn lịch sử người dùng) |
| **Khi nào dùng (WHEN)** | Gọi bởi `QuizRepositoryImpl`, `QuizInvalidationManager`, và `SyncManager` khi phát hiện quiz không còn trên Firestore |

---

## 4.2 Dữ liệu từ xa — Firebase (`data/remote/`)

### 4.2.1 `FirestoreCollections.kt`

| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Nguồn sự thật duy nhất cho tên collection/field Firestore; tránh hardcode string rải rác |
| **Vị trí (WHERE)** | `data/remote/firebase/FirestoreCollections.kt` |
| **Cách hoạt động (HOW)** | `object` với `const val` cho collection names và nested `object Fields` cho field names |
| **Khi nào dùng (WHEN)** | Import trong mọi `*RemoteDataSource` và worker cần truy cập Firestore |

| Hằng số | Giá trị | Mô tả |
|---------|---------|-------|
| `USERS` | `"users"` | Collection người dùng |
| `QUIZZES` | `"quizzes"` | Collection quiz |
| `QUESTIONS` | `"questions"` | Subcollection câu hỏi (trong quiz) |
| `CHOICES` | `"choices"` | Subcollection đáp án (trong question) |
| `ATTEMPTS` | `"attempts"` | Collection lịch sử làm bài |
| `SHARE_CODES` | `"shareCodes"` | Collection mã chia sẻ |
| `QUESTION_POOL` | `"questionPool"` | Collection ngân hàng câu hỏi cộng đồng |
| `QUIZ_DELETIONS` | `"quizDeletions"` | Tombstone collection — ghi nhận xóa vĩnh viễn; dọn sau 90 ngày |
| `BATCH_LIMIT` | `500` | Giới hạn tối đa mỗi Firestore batch write |

`Fields`: `OWNER_ID`, `USER_ID`, `QUIZ_ID`, `IS_PUBLIC`, `IS_DRAFT`, `DELETED_AT`, `SHARE_CODE`, `ATTEMPT_COUNT`, `UPDATED_AT`, `IS_ACTIVE`, `CONTRIBUTOR_ID`, `ROLE`, `PERMISSIONS`.

---

### 4.2.2 DTOs (`data/remote/model/`)

| DTO | File | Tương ứng Domain | Trường đặc biệt |
|-----|------|-----------------|----------------|
| `QuizDto` | `QuizDtoModels.kt` | `Quiz` | Tất cả fields của Quiz theo định dạng Firestore |
| `QuestionDto` | `QuizDtoModels.kt` | `Question` | Nhúng `List<ChoiceDto>` để hỗ trợ batch write |
| `ChoiceDto` | `QuizDtoModels.kt` | `Choice` | — |
| `AttemptDto` | `AttemptDto.kt` | `Attempt` | `answers` là `Map<String, List<String>>` |
| `UserDto` | `UserDto.kt` | `User` | `role` lưu lowercase string; `permissions` là `List<String>` |
| `ShareCodeDto` | `ShareCodeDto.kt` | — | `quizId`, `code`, `createdAt` |
| `QuestionPoolItemDto` | `QuestionPoolItemDto.kt` | `QuestionPoolItem` | `contributorId`, `usageCount`, `isActive` |

---

### 4.2.3 `AppMappers.kt`

| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Tách biệt schema Firestore DTO khỏi domain model; DTO có thể thay đổi mà không ảnh hưởng domain |
| **Vị trí (WHERE)** | `data/remote/AppMappers.kt` |
| **Cách hoạt động (HOW)** | Extension functions `Dto.toDomain()` và `Domain.toDto()`; xử lý Firestore `Timestamp` ↔ `Long`, lowercase string ↔ enum |
| **Khi nào dùng (WHEN)** | Gọi trong `*RemoteDataSource` hoặc repository khi nhận data từ Firestore (→ domain) hoặc chuẩn bị ghi (→ DTO) |

---

### 4.2.4 Remote Data Sources (`data/remote/firebase/`)

| DataSource | Collection | Phương thức chính | Ghi chú |
|-----------|-----------|-------------------|---------|
| `QuizRemoteDataSource` | `quizzes` | `getPublicQuizzes`, `getQuizzesByOwner`, `getQuizById`, `saveQuiz`, `softDeleteQuiz`, `permanentlyDeleteQuiz`, `emptyTrash`, `incrementAttemptCount`, `getDeletionsSince`, `writeDeletionTombstone` | `callbackFlow` + `addSnapshotListener` cho real-time; batch write khi lưu questions/choices |
| `QuestionRemoteDataSource` | `questions` (subcollection) | `getQuestions`, `saveQuestions`, `deleteQuestions` | Batch write |
| `AttemptRemoteDataSource` | `attempts` | `getAttemptsByUser`, `getAttemptsByQuiz`, `saveAttempt`, `updateAttempt` | — |
| `UserRemoteDataSource` | `users` | `getUserById`, `saveUser`, `updateUser`, `updateRole`, `deleteUser` | — |
| `ShareCodeRemoteDataSource` | `shareCodes` | `lookupCode`, `generateUniqueCode`, `deleteCode`, `regenerateCode` | Retry tối đa 10 lần để đảm bảo uniqueness |
| `PoolRemoteDataSource` | `questionPool` | `contribute`, `getByTags`, `getByContributor`, `revoke`, `incrementUsage`, `getRandom` | — |
| `AdminRemoteDataSource` | nhiều collection | `getAllUsers`, `getAllQuizzes`, `getSystemStats`, paginated queries | Role-guarded; cursor-based pagination |
| `FirestoreCascadeHelper` | `quizzes` + subcollections | `cascadeDeleteQuiz`, `cascadeDeleteUserData`, `collectQuizSubcollectionRefs`, `buildTombstoneData` | Xóa quiz kèm questions/choices/shareCodes trong batch write |

---

## 4.3 Repository Implementations (`data/repository/`)

| Implementation | Interface | Chiến lược | Ghi chú đặc biệt |
|---------------|-----------|-----------|-----------------|
| `QuizRepositoryImpl` | `QuizRepository` | **Local-first**: ghi Room trước → Flow phát ngay → sync Firestore nền | Tính checksum sau upload; xử lý `isRemovedFromCloud`; hàm `*Limited`/`*Count` cho phân trang |
| `AttemptRepositoryImpl` | `AttemptRepository` | **Local-first**: ghi Room → sync Firestore nền | `linkGuestAttempts()` di chuyển attempt từ guest → tài khoản thật sau đăng nhập |
| `AuthRepositoryImpl` | `AuthRepository` | FirebaseAuth + UserDao + UserRemoteDataSource | Cache user vào Room sau login; `fetchRandomAvatarUrl()` gọi Wallhaven API; `fetchFullUserProfile()` so sánh và đồng bộ |
| `ShareCodeRepositoryImpl` | `ShareCodeRepository` | **Remote-only** (không dùng Room) | Retry 10 lần khi generate để đảm bảo uniqueness |
| `PoolRepositoryImpl` | `PoolRepository` | **Remote-only** (không dùng Room) | `contributeQuestions()` dùng batch write; nhận `FirebaseFirestore` trực tiếp |
| `AdminRepositoryImpl` | `AdminRepository` | **Remote-only**, cursor-based pagination | Role-guarded; không cache local |
| `SearchRepositoryImpl` | `SearchRepository` | **SharedPreferences-backed** recent searches | Không dùng Room hay Firebase; chỉ lưu lịch sử tìm kiếm gần đây |

---

## 4.4 Đồng bộ & Mạng (`data/sync/`, `data/network/`, `data/worker/`, `data/preferences/`)

### 4.4.1 `NetworkMonitor.kt`

| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Cần biết trạng thái mạng theo thời gian thực để bật/tắt sync và cảnh báo offline cho người dùng |
| **Vị trí (WHERE)** | `data/network/NetworkMonitor.kt` |
| **Cách hoạt động (HOW)** | Bọc `ConnectivityManager`; đăng ký `NetworkCallback` → cập nhật `MutableStateFlow`; phơi bày `isOnline: StateFlow<Boolean>` và `isWifi: StateFlow<Boolean>` |
| **Khi nào dùng (WHEN)** | `SyncManager`, `QuizInvalidationManager`, và các repository đọc `isOnline.value` trước khi thực hiện thao tác mạng |

---

### 4.4.2 `SyncManager.kt`

| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Điều phối tập trung toàn bộ quá trình đồng bộ hai chiều giữa Room và Firestore |
| **Vị trí (WHERE)** | `data/sync/SyncManager.kt` |
| **Cách hoạt động (HOW)** | Duy trì `syncState: StateFlow<SyncState>`; đọc hàng đợi `PendingSyncEntity` từ Room → dispatch theo `SyncEntityType` → cập nhật status → retry nếu thất bại |
| **Khi nào dùng (WHEN)** | Gọi từ `BackgroundSyncWorker` định kỳ hoặc repository khi trở lại online |

**`SyncState`:** `IDLE` | `SYNCING` | `PENDING` | `ERROR`

| Phương thức | Mô tả |
|-------------|-------|
| `enqueueSync()` | Đưa operation vào hàng đợi `PendingSyncEntity` trong Room |
| `processPendingOperations()` | Xử lý toàn bộ hàng đợi PENDING/FAILED chưa vượt `maxRetries` |
| `retryFailedOperations()` | Reset FAILED → PENDING rồi gọi lại `processPendingOperations()` |
| `executeOperation()` | Dispatch theo `SyncEntityType`: quiz / question / choice / attempt |
| `downloadQuizzes()` | Tải quiz của user từ Firestore → so sánh checksum → cập nhật Room; xóa stale quizzes |
| `performFullSync()` | Upload pending → `downloadQuizzes()` → `downloadAttempts()` |

---

### 4.4.3 `QuizInvalidationManager.kt`

| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Phát hiện quiz bị xóa vĩnh viễn trên Firestore bởi admin/user khác mà không cần tải lại toàn bộ collection (rất tốn kém) |
| **Vị trí (WHERE)** | `data/sync/QuizInvalidationManager.kt` |
| **Cách hoạt động (HOW)** | Ba tầng kiểm tra: **(1) Lazy** — `validateQuizExists()` kiểm tra 1 quiz on-demand (1 Firestore read); **(2) Tombstone sweep** — `checkForDeletedQuizzes()` query `quizDeletions` theo `deletedAt` từ lần check cuối, thường 0–5 document; **(3) Full cleanup** — safety net trong `SyncManager` so sánh toàn bộ ID set. Timestamp lần check cuối lưu trong SharedPreferences |
| **Khi nào dùng (WHEN)** | `checkForDeletedQuizzes()` gọi trong `BackgroundSyncWorker`; `validateQuizExists()` gọi khi user mở quiz không phải của mình |

---

### 4.4.4 `SettingsPreferences.kt`

| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Lưu trữ cài đặt ứng dụng bền vững không liên quan đến tài khoản Firebase |
| **Vị trí (WHERE)** | `data/preferences/SettingsPreferences.kt` |
| **Cách hoạt động (HOW)** | Dùng **Jetpack DataStore** (không phải SharedPreferences); phơi bày `Flow<T>` reactive cho mỗi setting và suspending setter |
| **Khi nào dùng (WHEN)** | `SettingsViewModel` đọc/ghi theme; `SyncManager.isSyncAllowed()` đọc `autoSyncEnabled` và `wifiOnlySync` |

| Setting | Key | Mặc định | Mô tả |
|---------|-----|-----------|-------|
| `autoSyncEnabled` | `auto_sync_enabled` | `true` | Bật/tắt tự động đồng bộ nền |
| `wifiOnlySync` | `wifi_only_sync` | `false` | Chỉ sync khi kết nối WiFi |
| `darkThemeMode` | `dark_theme_mode` | `0` | `0`=System, `1`=Light, `2`=Dark |

---

### 4.4.5 `BackgroundSyncWorker.kt`

| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Đảm bảo dữ liệu được đồng bộ định kỳ ngay cả khi app không ở foreground |
| **Vị trí (WHERE)** | `data/worker/BackgroundSyncWorker.kt` |
| **Cách hoạt động (HOW)** | `CoroutineWorker` WorkManager, chu kỳ 15 phút, ràng buộc network; kiểm tra `isSyncAllowed()` và user đã đăng nhập → `performFullSync()` → `checkForDeletedQuizzes()`; trả về `Result.retry()` khi thất bại (WorkManager áp dụng exponential backoff tự động) |
| **Khi nào dùng (WHEN)** | Schedule tại khởi động app trong `AppContainerImpl` |

---

### 4.4.6 `BackendMaintenanceWorker.kt`

| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Thực hiện bảo trì Firestore phía client (không có Cloud Functions) để tránh dữ liệu rác tích tụ |
| **Vị trí (WHERE)** | `data/worker/BackendMaintenanceWorker.kt` |
| **Cách hoạt động (HOW)** | `CoroutineWorker` chạy định kỳ; mỗi task chạy trong `try/catch` độc lập; lỗi một task không ngăn task khác; trả về `Result.retry()` nếu có bất kỳ lỗi nào |
| **Khi nào dùng (WHEN)** | Schedule tại khởi động app; chạy ít thường xuyên hơn `BackgroundSyncWorker` |

| Task | Mô tả |
|------|-------|
| `cleanupOldDeletedQuizzes()` | Xóa vĩnh viễn quiz trong thùng rác > 30 ngày (cascade: questions → choices → shareCodes); ghi tombstone trước khi xóa |
| `aggregateQuizStats()` | Đếm attempt thực tế và cập nhật `attemptCount` nếu sai lệch |
| `cleanupInactivePoolQuestions()` | Xóa entry `questionPool` có `isActive = false` |
| `cleanupDeletedUsers()` | Xóa tài khoản soft-deleted > 30 ngày kèm quizzes/attempts/pool contributions; ghi tombstone cho quizzes |
| `cleanupOldTombstones()` | Xóa tombstone trong `quizDeletions` cũ hơn 90 ngày để tránh collection phình to vô hạn |

---

> **Tóm tắt luồng dữ liệu:** UI → ViewModel → Repository Interface (domain) → `*RepositoryImpl` (data) → Room (phát `Flow` ngay lập tức) + Firestore (sync nền qua `SyncManager` / `*RemoteDataSource`). Chiều ngược lại: Firestore → `*RemoteDataSource` → `*RepositoryImpl` → Room → `Flow` → ViewModel → UI.

## 5. TẦNG DEPENDENCY INJECTION (`di/`)

### 5.1 Tổng quan

App dùng **Manual DI** — không Hilt/Dagger. Chuỗi phụ thuộc:
`AppContainer` (interface) → `AppContainerImpl` (lazy) → `QuizzezApplication.appContainer` → `LocalAppContainer` → Composables & ViewModels

### 5.2 Các file DI

**`di/AppModule.kt` — Giao diện AppContainer**

| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Định nghĩa hợp đồng (contract) cho tất cả singleton — tách rời interface khỏi implementation |
| **Vị trí (WHERE)** | `di/AppModule.kt` |
| **Cách hoạt động (HOW)** | Interface khai báo `val` properties: `firebaseAuth`, `firebaseFirestore`, `appDatabase`, 6 DAO, `networkMonitor`, `syncManager`, `quizInvalidationManager`, `quizRemoteDataSource`, 7 repository, `settingsPreferences` |
| **Khi nào dùng (WHEN)** | Mỗi lần thêm dependency mới: khai báo property tại đây trước tiên |

```AndroidApp/app/src/main/java/com/example/androidapp/di/AppModule.kt#L29-56
interface AppContainer {
    val context: Context
    val firebaseAuth: FirebaseAuth
    val firebaseFirestore: FirebaseFirestore
    val appDatabase: AppDatabase
    val quizDao: QuizDao
    // ... 5 DAO khác
    val networkMonitor: NetworkMonitor
    val syncManager: SyncManager
    val quizInvalidationManager: QuizInvalidationManager
    val quizRemoteDataSource: QuizRemoteDataSource
    val authRepository: AuthRepository
    // ... 6 repository khác
    val settingsPreferences: SettingsPreferences
}
```

---

**`di/FirebaseModule.kt` — AppContainerImpl**

| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Một implementation duy nhất — Firebase/Room chỉ khởi tạo khi thực sự được truy cập, tiết kiệm tài nguyên |
| **Vị trí (WHERE)** | `di/FirebaseModule.kt` |
| **Cách hoạt động (HOW)** | `class AppContainerImpl(val context: Context) : AppContainer`; mọi property dùng `by lazy {}`; emulator bật trong debug build qua `BuildConfig.USE_FIREBASE_EMULATOR` (host `10.0.2.2`) |
| **Khi nào dùng (WHEN)** | Khởi động app — tạo một lần duy nhất trong `QuizzezApplication` |

```AndroidApp/app/src/main/java/com/example/androidapp/di/FirebaseModule.kt#L53-60
override val firebaseAuth: FirebaseAuth by lazy {
    Firebase.auth.also { auth ->
        if (BuildConfig.USE_FIREBASE_EMULATOR) {
            auth.useEmulator(emulatorHost, 9099)
        }
    }
}
```

---

**`di/AppContainerExt.kt` — LocalAppContainer**

| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Composable cần truy cập container mà không prop-drilling qua từng cấp |
| **Vị trí (WHERE)** | `di/AppContainerExt.kt` |
| **Cách hoạt động (HOW)** | Extension property `@Composable get()` đọc `LocalContext.current.applicationContext as QuizzezApplication` rồi trả `.appContainer` |
| **Khi nào dùng (WHEN)** | Trong Composable cần tạo `ViewModelProvider.Factory` hoặc truy cập repository trực tiếp |

```AndroidApp/app/src/main/java/com/example/androidapp/di/AppContainerExt.kt#L9-12
val LocalAppContainer: AppContainer
    @Composable
    get() = (LocalContext.current.applicationContext as QuizzezApplication).appContainer
```

---

**`QuizzezApplication.kt` + `MainActivity.kt`**

| File | WHY | HOW |
|------|-----|-----|
| `QuizzezApplication` | Application class — singleton suốt lifecycle app | Tạo `AppContainerImpl(this)`; lên lịch `BackgroundSyncWorker` + `BackendMaintenanceWorker` qua WorkManager |
| `MainActivity` | Single-Activity — một Activity quản lý toàn bộ Compose UI | Render `QuizzezTheme { QuizzezNavHost() }`; `LocalAppContainer` qua extension property từ Application context |

### 5.3 Quy trình thêm dependency mới

| Bước | Hành động | File cần sửa |
|------|-----------|-------------|
| 1 | Khai báo property trong interface | `di/AppModule.kt` |
| 2 | Implement với `by lazy {}` | `di/FirebaseModule.kt` |
| 3 | Tạo ViewModel factory truyền dependency | Màn hình cần dependency đó |
| 4 | Truy cập qua `LocalAppContainer` | Composable tương ứng |

---

## 6. TẦNG GIAO DIỆN (UI LAYER)

### 6.1 Điều hướng — `ui/navigation/`

**`Routes.kt`**

| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Tập trung tất cả route string — tránh hardcode rải rác, dễ refactor và tránh lỗi typo |
| **Vị trí (WHERE)** | `ui/navigation/Routes.kt` |
| **Cách hoạt động (HOW)** | `object Routes` chứa `const val` cho mọi pattern; `sealed class NavigationDestination` cung cấp type-safe destinations và helper builders như `quizDetail(id)`, `quizPlay(id)`, `answerReview(quizId, attemptId)` |
| **Khi nào dùng (WHEN)** | Mọi lời gọi `navController.navigate()`; NavHost dùng các hằng số này để đăng ký destination |

Danh sách route:

| Nhóm | Pattern | Màn hình | Tham số |
|------|---------|----------|---------|
| Bottom Nav | `home` | HomeScreen | — |
| Bottom Nav | `search` | SearchScreen | `tag` (optional) |
| Bottom Nav | `profile` | ProfileScreen | — |
| Quiz | `quiz/{quizId}` | QuizDetailScreen | quizId |
| Quiz | `quiz/{quizId}/play` | TakeQuizScreen | quizId |
| Quiz | `quiz/{quizId}/result/{attemptId}` | QuizResultScreen | quizId, attemptId |
| Quiz | `quiz/{quizId}/edit` | EditQuizScreen | quizId |
| Quiz | `quiz/{quizId}/preview` | QuizPreviewScreen | quizId |
| Quiz | `quiz/{quizId}/review/{attemptId}` | AnswerReviewScreen | quizId, attemptId |
| Quiz | `quiz/create` | CreateQuizScreen | — |
| User | `history` | HistoryScreen | — |
| User | `trash` | TrashScreen | — |
| User | `settings` | SettingsScreen | — |
| User | `profile/edit` | EditProfileScreen | — |
| User | `question_pool` | QuestionPoolScreen | — |
| User | `csv_import` | CsvImportScreen | — |
| User | `attempt/{attemptId}` | AttemptDetailScreen | attemptId |
| Auth | `login` | LoginScreen | — |
| Auth | `register` | RegisterScreen | — |
| Admin | `admin/dashboard` | AdminDashboardScreen | — |
| Admin | `admin/users` | AdminUserManagementScreen | — |
| Admin | `admin/quizzes` | AdminQuizManagementScreen | — |
| Admin | `admin/reports` | AdminReportsScreen | — |

**`QuizzezNavHost.kt`**

| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Single NavHost là điểm vào duy nhất cho toàn bộ navigation graph |
| **Vị trí (WHERE)** | `ui/navigation/QuizzezNavHost.kt` |
| **Cách hoạt động (HOW)** | Đăng ký tất cả `composable()` destinations; admin routes được role-guard; `BottomNavBar` chỉ hiện trên `HOME`, `SEARCH`, `PROFILE` |
| **Khi nào dùng (WHEN)** | Được render bởi `MainActivity`; mọi điều hướng đi qua đây |

---

### 6.2 Xác thực — `ui/screens/auth/`

| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Tách biệt luồng auth khỏi phần còn lại; `AuthViewModel` dùng chung giữa Login và Register |
| **Vị trí (WHERE)** | `ui/screens/auth/` |
| **Cách hoạt động (HOW)** | `AuthFragment` (XML) bọc 2 tab Đăng nhập/Đăng ký + nút "Tiếp tục với tư cách khách"; `AuthViewModel` expose `uiState: StateFlow<AuthUiState>` và nhận `AuthEvent` |
| **Khi nào dùng (WHEN)** | Khi người dùng chưa đăng nhập hoặc phiên đăng nhập hết hạn |

`AuthUiState` sealed class:

| Trạng thái | Ý nghĩa |
|-----------|---------|
| `Idle` | Chờ thao tác, không có lỗi |
| `Loading` | Đang xử lý login / register |
| `Authenticated(user)` | Đăng nhập thành công |
| `Error(error: UiError)` | Thất bại với mã lỗi cụ thể |
| `SessionExpired` | Phiên hết hạn — cần đăng nhập lại |

`AuthEvent`: `Login(email, password)`, `Register(email, password, username)`, `Logout`, `ClearError`, `DismissSessionExpired`.

---

### 6.3 Trang chủ — `ui/screens/home/`

| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Điểm xuất phát chính — hiển thị quiz gần đây, quiz của tôi, trending và ô nhập mã tham gia |
| **Vị trí (WHERE)** | `ui/screens/home/HomeScreen.kt`, `HomeViewModel.kt` |
| **Cách hoạt động (HOW)** | `HomeViewModel` observe `authRepository.currentUser`; khi login gọi `observeHomeData(userId)` — long-lived Room Flow collector; pull-to-refresh chạy coroutine riêng với timeout 8 giây, xóa spinner trong `finally` |
| **Khi nào dùng (WHEN)** | Route `home` — Bottom Nav tab đầu tiên |

`HomeUiState` (data class):

| Trường | Kiểu | Mô tả |
|--------|------|-------|
| `recentQuizzes` | `List<Quiz>` | Quiz đã làm gần đây |
| `myQuizzes` | `List<Quiz>` | Quiz của người dùng hiện tại |
| `trendingQuizzes` | `List<Quiz>` | Quiz trending |
| `joinCode` | `String` | Mã nhập trong ô tham gia quiz |
| `isLoading` | `Boolean` | Đang tải lần đầu |
| `isRefreshing` | `Boolean` | Đang pull-to-refresh |
| `adminRemovedQuizCount` | `Int` | Số quiz bị admin xóa trên cloud nhưng còn local |
| `error` | `String?` | Thông báo lỗi chung |

---

### 6.4 Tìm kiếm — `ui/screens/search/`

| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Cho phép khám phá quiz công khai; hỗ trợ lọc tag, sắp xếp, phân trang, lịch sử tìm kiếm |
| **Vị trí (WHERE)** | `ui/screens/search/SearchScreen.kt`, `SearchViewModel.kt` |
| **Cách hoạt động (HOW)** | State tách ra `SearchUiState.kt`; event tách ra `SearchEvent.kt`; `SearchFilterLogic.filter()` dùng OR-logic cho tags; lịch sử từ `SearchRepository` (SharedPreferences) |
| **Khi nào dùng (WHEN)** | Route `search` — Bottom Nav tab thứ hai; hoặc navigate kèm tag từ màn hình khác |

File phụ trong package:

| File | Vai trò |
|------|--------|
| `SearchUiState.kt` | Data class state + `SortOption` enum (`DATE` / `POPULARITY` / `RELEVANCE`) |
| `SearchEvent.kt` | Sealed class: `OnQueryChange`, `OnTagToggle`, `OnSortOptionSelected`, `LoadMoreSearchResults`… |
| `QuizCardDraft.kt` | Display model tổng hợp cho card kết quả tìm kiếm |
| `SearchControlsRow.kt` | Row: thanh tìm kiếm + dropdown sắp xếp |
| `TagFilterRow.kt` | Horizontal scrollable chip filter theo tag |
| `SearchResultsGrid.kt` | Layout lưới kết quả |
| `SearchResultsList.kt` | Layout danh sách kết quả |
| `DiscoverSection.kt` | Section "Khám phá" — hiển thị khi chưa tìm kiếm |

---

### 6.5 Hồ sơ — `ui/screens/profile/`

| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Hiển thị thông tin người dùng, thống kê và lối vào Admin Panel |
| **Vị trí (WHERE)** | `ui/screens/profile/ProfileScreen.kt`, `ProfileViewModel.kt`, `EditProfileScreen.kt`, `EditProfileViewModel.kt` |
| **Cách hoạt động (HOW)** | `ProfileViewModel` observe `authRepository.currentUser`; `EditProfileViewModel` chỉ phụ thuộc `AuthRepository` — không có `StorageRepository` |
| **Khi nào dùng (WHEN)** | Route `profile` (Bottom Nav) và `profile/edit` |

`EditProfileViewModel` — điểm đặc biệt: avatar không upload lên Firebase Storage; người dùng **paste URL tay** hoặc nhấn "Lấy ảnh ngẫu nhiên" → gọi Wallhaven API (`categories=010`, `ratios=1x1`, `sorting=random`) lấy thumbnail URL.

`EditProfileUiState`:

| Trường | Kiểu | Mô tả |
|--------|------|-------|
| `displayName` | `String` | Tên hiển thị đang nhập |
| `email` | `String` | Email (chỉ đọc) |
| `photoUrl` | `String?` | URL avatar hiện tại |
| `isLoading` | `Boolean` | Đang lưu profile |
| `isLoadingAvatar` | `Boolean` | Đang fetch Wallhaven API |
| `isSaved` | `Boolean` | Lưu thành công — trigger navigate back |
| `error` | `UiError?` | Mã lỗi nếu có |

`EditProfileEvent`: `DisplayNameChanged(name)`, `AvatarUrlChanged(url)`, `FetchRandomAvatar`, `SaveProfile`, `ClearError`.

---

### 6.6 Luồng Quiz — `ui/screens/quiz/`

| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Ba màn hình liên tiếp tạo thành luồng chính: xem chi tiết → làm bài → xem kết quả |
| **Vị trí (WHERE)** | `ui/screens/quiz/` |
| **Cách hoạt động (HOW)** | Mỗi màn hình có ViewModel riêng; `TakeQuizViewModel` dùng `sealed class` state vì có nhiều pha rõ ràng |
| **Khi nào dùng (WHEN)** | Người dùng nhấn vào một quiz bất kỳ từ Home / Search / History |

**`QuizDetailUiState`** sealed: `Loading`, `Success(quiz, questions, isOwner, isDeleting, isDeleted, deleteError…)`, `Error(error, errorDetail)`.

**`TakeQuizUiState`** sealed class:

| Trạng thái | Dữ liệu chính | Ý nghĩa |
|-----------|--------------|---------|
| `Loading` | — | Đang tải quiz từ repository |
| `Active` | `quizTitle`, `currentQuestion`, `currentIndex`, `totalQuestions`, `selectedAnswers`, `elapsedSeconds`, `isMultiSelect`, `allAnswers`, `showExitDialog` | Đang làm bài |
| `Finished` | `attemptId` | Nộp thành công — navigate sang `QuizResultScreen` |
| `Error` | `error: UiError`, `errorDetail` | Lỗi tải hoặc lưu kết quả |

`TakeQuizViewModel` — luồng xử lý:

| Bước | Hành động | Utility / Repository |
|------|-----------|---------------------|
| 1 | `loadQuiz(quizId)` lấy câu hỏi | `QuizRepository.getQuestionsForQuizOnce()` |
| 2 | Xáo trộn câu hỏi và đáp án | `QuestionShuffler.shuffle()` |
| 3 | `AnswerSelected(choiceId)` cập nhật map | `answers[questionId] = Set<choiceId>` (toggle nếu multi-select) |
| 4 | `SubmitQuiz()` tính điểm | `ScoreCalculator.calculatePointScore(questions, userAnswers)` |
| 5 | Lưu kết quả | `AttemptRepository.saveAttempt(attempt)` |
| 6 | Chuyển trang | emit `TakeQuizUiState.Finished(attemptId)` → navigate |

```AndroidApp/app/src/main/java/com/example/androidapp/ui/screens/quiz/TakeQuizViewModel.kt#L116-130
questions = QuestionShuffler.shuffle(
    questions = quizRepository.getQuestionsForQuizOnce(quizId),
    getChoices = { it.choices },
    copyWithNewChoices = { q, newChoices -> q.copy(choices = newChoices) }
)
// ...
val scoreResult = ScoreCalculator.calculatePointScore(questions, userAnswers)
val attempt = Attempt(
    id = attemptId,
    score = scoreResult.earnedScore,
    totalQuestions = scoreResult.maxScore,
    answers = answerMap,
    startTimeMillis = startTimeMillis,
    endTimeMillis = System.currentTimeMillis()
)
```

**`QuizResultViewModel`** — nhận `quizId` + `attemptId`; load `Attempt` từ `AttemptRepository`; dùng `ScoreUtil` tính số sao và phần trăm; hiển thị tóm tắt điểm số cho người dùng.

## 6.7 Tạo & Sửa Quiz (`ui/screens/create/`)

### CreateQuizScreen + CreateQuizViewModel

| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Cho phép người dùng tạo quiz mới với tiêu đề, mô tả, tag, danh sách câu hỏi và lựa chọn |
| **Vị trí (WHERE)** | `ui/screens/create/CreateQuizScreen.kt`, `ui/screens/create/CreateQuizViewModel.kt` |
| **Cách hoạt động (HOW)** | Screen stateless hiển thị `QuizFormContent`; ViewModel quản lý `CreateQuizUiState`, xử lý `QuizFormEvent`, gọi `QuizRepository.createQuiz()` khi lưu |
| **Khi nào dùng (WHEN)** | Khi người dùng nhấn `CreateQuizFAB` từ `HomeScreen` |

### EditQuizScreen + EditQuizViewModel

| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Cho phép chủ sở hữu chỉnh sửa quiz đã tồn tại (tiêu đề, mô tả, câu hỏi, trạng thái publish) |
| **Vị trí (WHERE)** | `ui/screens/create/EditQuizScreen.kt`, `ui/screens/create/EditQuizViewModel.kt` |
| **Cách hoạt động (HOW)** | Load quiz theo `quizId` từ `QuizRepository`, hiển thị `QuizFormContent` đã điền sẵn, lưu bằng `QuizRepository.updateQuiz()` |
| **Khi nào dùng (WHEN)** | Khi người dùng chọn "Chỉnh sửa" từ `QuizDetailScreen` |

### QuizFormContent + QuizFormEvent + QuizFormHelper

| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Tái sử dụng UI form và logic dùng chung giữa Create và Edit, tránh trùng lặp code |
| **Vị trí (WHERE)** | `ui/screens/create/QuizFormContent.kt`, `QuizFormEvent.kt`, `QuizFormHelper.kt` |
| **Cách hoạt động (HOW)** | `QuizFormContent` là composable stateless nhận state + callback; `QuizFormEvent` là sealed class sự kiện (TitleChanged, TagAdded, QuestionAdded, ChoiceEdited, CorrectAnswerToggled...); `QuizFormHelper` chứa hàm thuần (validate, reorder) dùng ở cả 2 ViewModel |
| **Khi nào dùng (WHEN)** | Được gọi bởi cả `CreateQuizScreen` lẫn `EditQuizScreen` |

### CsvImportScreen + CsvImportViewModel

| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Cho phép import hàng loạt câu hỏi từ file `.csv` thay vì nhập tay từng câu |
| **Vị trí (WHERE)** | `ui/screens/create/CsvImportScreen.kt`, `ui/screens/create/CsvImportViewModel.kt` |
| **Cách hoạt động (HOW)** | Dùng `CsvParser.parse()` để đọc file, `CsvValidator.validate()` kiểm tra cột + dữ liệu, hiển thị lỗi nếu có, sau đó chuyển sang `QuizPreviewScreen` |
| **Khi nào dùng (WHEN)** | Khi người dùng chọn "Import CSV" trong luồng tạo quiz |

### QuizPreviewScreen + QuizPreviewViewModel

| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Hiển thị bản xem trước câu hỏi đã parse từ CSV trước khi người dùng xác nhận lưu |
| **Vị trí (WHERE)** | `ui/screens/create/QuizPreviewScreen.kt`, `ui/screens/create/QuizPreviewViewModel.kt` |
| **Cách hoạt động (HOW)** | Nhận danh sách câu hỏi qua shared ViewModel; khi xác nhận gọi `CreateQuizViewModel.saveQuiz()` |
| **Khi nào dùng (WHEN)** | Bước cuối trong luồng CSV import |

### Draft Models (UI layer only)

`QuestionDraft` và `ChoiceDraft` là data class trạng thái form — định nghĩa trong `CreateQuizViewModel.kt`, **không** thuộc `domain/`:

```AndroidApp/app/src/main/java/com/example/androidapp/ui/screens/create/CreateQuizViewModel.kt#L1-5
data class ChoiceDraft(val id: String, val text: String, val isCorrect: Boolean)
data class QuestionDraft(
    val id: String, val text: String, val imageUrl: String?,
    val choices: List<ChoiceDraft>, val explanation: String?
)
```

### Vòng Đời Quiz

| Trạng thái | `isDraft` | `isPublic` | Quy tắc | Ai truy cập |
|------------|-----------|------------|---------|------------|
| Nháp (Draft) | `true` | `false` (bắt buộc) | `isPublic` không thể `true` khi `isDraft=true` | Chỉ chủ sở hữu |
| Riêng tư (Private) | `false` | `false` | Đã hoàn thiện, không công khai | Chủ sở hữu + người có mã chia sẻ |
| Công khai (Public) | `false` | `true` | `isPublic` toggle là lựa chọn riêng biệt của người dùng | Tất cả mọi người |

> **Lưu ý:** Publish draft → `isDraft=false` nhưng `isPublic` **không** bị ép thành `true`.

### Luồng Import CSV

| Bước | Hành động | File/Utility |
|------|-----------|-------------|
| 1 | Chọn file `.csv` | `CsvImportScreen` |
| 2 | Đọc nội dung file | `CsvImportViewModel` |
| 3 | Parse CSV | `CsvParser.parse()` |
| 4 | Validate cột + dữ liệu | `CsvValidator.validate()` |
| 5 | Preview câu hỏi | `QuizPreviewScreen` / `QuizPreviewViewModel` |
| 6 | Xác nhận → lưu quiz | `CreateQuizViewModel.saveQuiz()` |

---

## 6.8 Các Màn Hình Còn Lại

### HistoryScreen + HistoryViewModel (`ui/screens/history/`)

| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Hiển thị lịch sử các lần làm bài, sắp xếp theo thời gian gần nhất |
| **Vị trí (WHERE)** | `ui/screens/history/HistoryScreen.kt`, `ui/screens/history/HistoryViewModel.kt` |
| **Cách hoạt động (HOW)** | `AttemptWithQuiz` (data class UI layer — `Attempt` + `quizTitle`) định nghĩa trong `HistoryViewModel.kt` (không thuộc `domain/`); dùng `ScoreUtil` tính % + sao, `TimeFormatter` định dạng thời gian |
| **Khi nào dùng (WHEN)** | Người dùng truy cập "Lịch sử" từ `ProfileScreen` |

### AnswerReviewScreen + AnswerReviewViewModel (`ui/screens/review/`)

| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Cho phép xem lại từng câu hỏi: đáp án đúng/sai và giải thích sau khi làm bài |
| **Vị trí (WHERE)** | `ui/screens/review/AnswerReviewScreen.kt`, `ui/screens/review/AnswerReviewViewModel.kt` |
| **Cách hoạt động (HOW)** | `QuestionReview` (data class UI layer — `question` + `userAnswer: List<String>` + `isCorrect: Boolean`) định nghĩa trong `AnswerReviewViewModel.kt`; hiển thị `explanation` nếu có |
| **Khi nào dùng (WHEN)** | Sau `QuizResultScreen`, nhấn "Xem lại đáp án" |

### AttemptDetailScreen + AttemptDetailViewModel (`ui/screens/attempt/`)

| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Hiển thị thống kê chi tiết của một lần làm bài (điểm, thời gian, số câu đúng/sai) |
| **Vị trí (WHERE)** | `ui/screens/attempt/AttemptDetailScreen.kt`, `ui/screens/attempt/AttemptDetailViewModel.kt` |
| **Cách hoạt động (HOW)** | Load `Attempt` theo `attemptId` từ `AttemptRepository`; tính toán thống kê qua `ScoreUtil` |
| **Khi nào dùng (WHEN)** | Nhấn vào một mục trong `HistoryScreen` |

### TrashScreen + RecycleBinViewModel (`ui/screens/trash/`)

| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Quản lý quiz đã xóa mềm; cho phép khôi phục hoặc xóa vĩnh viễn |
| **Vị trí (WHERE)** | `ui/screens/trash/TrashScreen.kt`, `ui/screens/trash/RecycleBinViewModel.kt` |
| **Cách hoạt động (HOW)** | ViewModel tên **`RecycleBinViewModel`** (không phải `TrashViewModel`); load quiz có `deletedAt != null` từ `QuizRepository` |
| **Khi nào dùng (WHEN)** | Từ `ProfileScreen` → "Thùng rác" |

| Hành động | Repository method | Kết quả |
|-----------|-------------------|---------|
| Khôi phục | `QuizRepository.restoreQuiz()` | `deletedAt=null`, quay lại danh sách quiz |
| Xóa vĩnh viễn | `QuizRepository.permanentlyDeleteQuiz()` | Xóa khỏi Room + Firestore |
| Làm trống thùng rác | `QuizRepository.emptyTrash(userId)` | Xóa tất cả quiz đã soft-delete |

### SettingsScreen + SettingsViewModel (`ui/screens/settings/`)

| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Cho phép người dùng tuỳ chỉnh giao diện, thông báo và hành vi đồng bộ |
| **Vị trí (WHERE)** | `ui/screens/settings/SettingsScreen.kt`, `ui/screens/settings/SettingsViewModel.kt` |
| **Cách hoạt động (HOW)** | Đọc/ghi `SettingsPreferences` (SharedPreferences); thay đổi theme áp dụng ngay qua recomposition |
| **Khi nào dùng (WHEN)** | Từ `ProfileScreen` → "Cài đặt" |

| Cài đặt | Lưu ở | Mô tả |
|---------|-------|-------|
| Giao diện (theme) | `SettingsPreferences` | Sáng / tối / theo hệ thống |
| Thông báo | `SettingsPreferences` | Bật/tắt push notification |
| Chỉ sync qua Wi-Fi | `SettingsPreferences` | Tránh tốn dữ liệu di động |

### QuestionPoolScreen + QuestionPoolViewModel (`ui/screens/pool/`)

| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Xem và quản lý câu hỏi đã đóng góp vào pool cộng đồng; tạo quiz tự động từ pool |
| **Vị trí (WHERE)** | `ui/screens/pool/QuestionPoolScreen.kt`, `ui/screens/pool/QuestionPoolViewModel.kt` |
| **Cách hoạt động (HOW)** | Gọi `PoolRepository` để load câu hỏi; hỗ trợ lọc theo tag, thu hồi đóng góp, auto-generate quiz |
| **Khi nào dùng (WHEN)** | Từ `ProfileScreen` → "Ngân hàng câu hỏi" |

---

## 6.9 Màn Hình Quản Trị (`ui/screens/admin/`)

| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Cung cấp bảng điều khiển cho admin quản lý người dùng, quiz và xem báo cáo hệ thống |
| **Vị trí (WHERE)** | `ui/screens/admin/` với 4 sub-package: `dashboard/`, `users/`, `quizzes/`, `reports/` |
| **Cách hoạt động (HOW)** | `BaseAdminStatsViewModel` là abstract base class chứa logic chung; mỗi màn hình kế thừa và mở rộng; route được guard trong `QuizzezNavHost` theo `UserRole`; filter section dùng `AnimatedVisibility` (mặc định collapsed, toggle bằng nút filter) |
| **Khi nào dùng (WHEN)** | Chỉ tài khoản có `UserRole.ADMIN` mới truy cập; vào từ `ProfileScreen` |

| Màn hình | Route | ViewModel | Tính năng đặc biệt |
|---------|-------|-----------|------------------|
| `AdminDashboardScreen` | `admin/dashboard` | `AdminDashboardViewModel` | `SystemStats` charts, dùng `AdminChart` + `StatisticCard` + `AdminInsightCard` |
| `AdminUserManagementScreen` | `admin/users` | `AdminUserManagementViewModel` | Collapsible filter (`AnimatedVisibility`), tìm kiếm, phân trang, ban/unban/đổi role |
| `AdminQuizManagementScreen` | `admin/quizzes` | `AdminQuizManagementViewModel` | Collapsible filter (`AnimatedVisibility`), tìm kiếm, phân trang, force publish/unpublish |
| `AdminReportsScreen` | `admin/reports` | `AdminReportsViewModel` | Phân tích xu hướng, biểu đồ thống kê |

---

## 6.10 Thư Viện Component (`ui/components/`)

| Component | Package | Mô tả | Màn hình dùng |
|-----------|---------|-------|--------------|
| `ShareCodeSection` | `components/` | Hiển thị + copy mã chia sẻ 6 ký tự | `QuizDetailScreen` |
| `TagSuggestionDialog` | `components/` | Dialog gợi ý + thêm tag | `CreateQuiz`, `EditQuiz` |
| `AdminChart` | `components/admin/` | Biểu đồ bar/line cho admin | Dashboard, Reports |
| `AdminInsightCard` | `components/admin/` | Card insight số liệu | `AdminDashboard` |
| `AdminQuizCard` | `components/admin/` | Card quiz trong admin panel | `AdminQuizManagement` |
| `AdminUserCard` | `components/admin/` | Card user trong admin panel | `AdminUserManagement` |
| `RoleSelector` | `components/admin/` | Dropdown chọn vai trò user | `AdminUserManagement` |
| `StatisticCard` | `components/admin/` | Card số liệu tổng hệ thống | `AdminDashboard` |
| `AlertDialog` | `components/common/` | Dialog xác nhận (yes/no) | Xóa quiz, logout |
| `BottomSheet` | `components/common/` | Modal bottom sheet | Menu tùy chọn |
| `MediaDisplay` | `components/common/` | Hiển thị ảnh/video từ URL | `TakeQuiz`, `QuizDetail` |
| `TagChip` | `components/common/` | Chip hiển thị tag | Search, `QuizDetail` |
| `LoginPromptDialog` | `components/common/` | Nhắc đăng nhập khi guest thực hiện tác vụ cần xác thực | Mọi màn hình guest |
| `EmptyState` | `components/feedback/` | UI trống (icon + thông báo) | Mọi danh sách rỗng |
| `ErrorState` | `components/feedback/` | UI lỗi (icon + nút retry) | Mọi màn hình load data |
| `LoadingSpinner` | `components/feedback/` | `CircularProgressIndicator` bọc chuẩn | Mọi nơi đang tải |
| `ScoreCard` | `components/feedback/` | Hiển thị điểm % + xếp hạng sao | `QuizResult`, History |
| `SkeletonLoader` | `components/feedback/` | Skeleton shimmer — `shimmerEffect()` là Modifier extension | List đang tải |
| `CodeInputField` | `components/forms/` | Input field mã 6 ký tự với auto-focus | Nhập share code |
| `DropdownSelector` | `components/forms/` | Dropdown menu chọn giá trị | Các form select |
| `SwitchToggle` | `components/forms/` | Toggle bật/tắt có label | Settings, `isPublic` |
| `TextInputField` | `components/forms/` | Text input chuẩn với error state | Mọi form text |
| `QuizSearchBar` | `components/forms/` | Search bar chuyên dụng cho quiz | `SearchScreen` |
| `AppTopBar` | `components/navigation/` | Top app bar: back button, title, actions | Mọi màn hình con |
| `BottomNavBar` | `components/navigation/` | Bottom navigation 3 tab | HOME, SEARCH, PROFILE |
| `CreateQuizFAB` | `components/navigation/` | Floating Action Button tạo quiz mới | `HomeScreen` |
| `ChoiceButton` | `components/quiz/` | Nút chọn đáp án (selected / unselected / correct / wrong) | `TakeQuizScreen` |
| `DynamicChoiceList` | `components/quiz/` | Render danh sách 2–10 `ChoiceButton` | `TakeQuizScreen` |
| `QuizCard` | `components/quiz/` | Card quiz (thumbnail, title, tags, stats) | Home, Search, History |
| `QuizProgressIndicator` | `components/quiz/` | Thanh tiến trình câu hỏi N/Total | `TakeQuizScreen` |
| `QuizThumbnail` | `components/quiz/` | `AsyncImage` (Coil) cho thumbnail quiz | `QuizCard` |
| `TimerDisplay` | `components/quiz/` | Thời gian đã qua qua `TimeFormatter` | `TakeQuizScreen` |

> **Quy tắc bắt buộc:** Mọi composable nhận `modifier: Modifier = Modifier`; thêm cả `@Preview` light + dark.

---

## 6.11 Hệ Thống Theme (`ui/theme/`)

| Tiêu chí | Chi tiết |
|----------|---------|
| **Tại sao (WHY)** | Thống nhất giao diện toàn app; `design-tokens.json` là single source of truth cho mọi giá trị thiết kế (màu, chữ, spacing, radius, elevation) |
| **Vị trí (WHERE)** | `ui/theme/Color.kt`, `Type.kt`, `Shape.kt`, `Theme.kt`; token nguồn tại `design-tokens.json` (root repo) |
| **Cách hoạt động (HOW)** | `QuizzezTheme` bọc toàn bộ app trong `MainActivity`; dùng Material 3; component truy cập qua `MaterialTheme.colorScheme.*` và `MaterialTheme.typography.*` |
| **Khi nào dùng (WHEN)** | Luôn luôn — toàn bộ UI kế thừa theme; không được hardcode màu hay cỡ chữ |

| File | Nội dung |
|------|---------|
| `design-tokens.json` | Nguồn sự thật: màu sắc, typography, spacing, radius, elevation |
| `Color.kt` | Light + dark `ColorScheme` ánh xạ từ design tokens |
| `Type.kt` | `PlayfairDisplayFamily` (Serif — display/headline) + `InterFamily` (Sans-Serif — labels/body) qua Google Fonts |
| `Shape.kt` | `Shapes` chuẩn + `FullShape = RoundedCornerShape(50.dp)` cho nút pill/capsule |
| `Theme.kt` | `QuizzezTheme(darkTheme, dynamicColor = false, content)` |

**Hệ thống chữ hai font:** `PlayfairDisplay` cho tiêu đề lớn; `Inter` cho nội dung, nhãn, nút bấm.

**Quy tắc bắt buộc:**
- Không hardcode màu — dùng `MaterialTheme.colorScheme.*`
- `dynamicColor` disabled mặc định (Android 12+ có thể bật nhưng không dùng)
- Mọi chuỗi hiển thị lấy từ `stringResource(R.string.*)`, toàn bộ bằng **tiếng Việt**

---

## 6.12 Tiện Ích UI Khác

| File | Vị trí | Mô tả |
|------|--------|-------|
| `UiError.kt` | `ui/common/` | Sealed class lỗi UI chung (`NetworkError`, `UnknownError`, v.v.) dùng trong `UiState` các màn hình |
| `QrCodeUtil.kt` | `ui/util/` | Generate QR code bitmap từ chuỗi share code để hiển thị trong `ShareCodeSection` |# 7. SƠ ĐỒ USE CASE & MÔ TẢ CHI TIẾT

---

## 7.1 Các Actor trong hệ thống

| Actor | Ký hiệu | Mô tả | Ví dụ |
|-------|---------|-------|-------|
| Khách (Guest) | A1 | Người dùng chưa đăng nhập, truy cập ẩn danh | Anonymous UUID |
| Người dùng (User) | A2 | Đã đăng nhập, có toàn quyền người dùng thông thường | Email + password |
| Quản trị viên (Admin) | A3 | Có quyền quản trị được cấp bởi Superuser, quyền hạn có thể cấu hình | Configurable permissions |
| Siêu quản trị (Superuser) | A4 | Toàn quyền tuyệt đối trên toàn hệ thống | App owner |

**Quan hệ kế thừa:** A1 ⊂ A2 (User kế thừa tất cả UC của Guest), A2 ⊂ A3 ⊂ A4

---

## 7.2 Sơ đồ Use Case tổng quát

```AndroidApp/report_part5.md#L1-1
(see diagram below)
```

+----------------------------------------------------------------------+
|                        HE THONG QUIZZEZ                              |
|                                                                      |
|  +-[KHACH]-+       UC-01 Xem trang chu                               |
|  | (Guest) |-----> UC-02 Tim kiem quiz                               |
|  |   A1    |-----> UC-03 Xem chi tiet quiz                           |
|  |         |-----> UC-04 Lam quiz qua ma chia se                     |
|  |         |-----> UC-05 Xem ket qua thi                             |
|  |         |-----> UC-06 Dang nhap / Dang ky                         |
|  +---------+                                                         |
|                                                                      |
|  +--[NGUOI DUNG]--+  (Ke thua tat ca UC cua Khach)                   |
|  |    (User)      |--> UC-07 Tao quiz thu cong                       |
|  |      A2        |--> UC-08 Nhap quiz tu CSV                        |
|  |                |--> UC-09 Sua quiz                                |
|  |                |--> UC-10 Xoa quiz (soft delete)                  |
|  |                |--> UC-11 Khoi phuc quiz tu thung rac             |
|  |                |--> UC-12 Xoa vinh vien quiz                      |
|  |                |--> UC-13 Chia se quiz qua ma                     |
|  |                |--> UC-14 Tao/tai tao ma chia se                  |
|  |                |--> UC-15 Xem lich su thi                         |
|  |                |--> UC-16 Xem lai dap an chi tiet                 |
|  |                |--> UC-17 Dong gop cau hoi vao kho                |
|  |                |--> UC-18 Tu dong tao quiz tu kho                 |
|  |                |--> UC-19 Chinh sua ho so ca nhan                 |
|  |                |--> UC-20 Xem kho cau hoi ca nhan                 |
|  |                |--> UC-21 Cai dat ung dung                        |
|  +----------------+                                                  |
|                                                                      |
|  +--[QUAN TRI VIEN]--+  (Ke thua UC cua Nguoi dung)                  |
|  |    (Admin)         |--> UC-22 Xem dashboard thong ke              |
|  |      A3            |--> UC-23 Quan ly nguoi dung                  |
|  |                    |--> UC-24 Can / Bo can nguoi dung             |
|  |                    |--> UC-25 Quan ly quiz toan he thong          |
|  |                    |--> UC-26 Force publish/unpublish quiz        |
|  |                    |--> UC-27 Xem bao cao & phan tich             |
|  +--------------------+                                              |
|                                                                      |
|  +--[SIEU QUAN TRI]--+  (Ke thua tat ca Admin UC)                    |
|  |  (Superuser)       |--> UC-28 Phan quyen cho admin                |
|  |      A4            |--> UC-29 Xoa vinh vien tai khoan             |
|  |                    |--> UC-30 Quan ly toan bo khong gioi han      |
|  +--------------------+                                              |
+----------------------------------------------------------------------+

---

## 7.3 Mô tả Use Case Chi tiết

---

### UC-01: Xem trang chủ

| Trường | Nội dung |
|--------|---------|
| **Mã UC** | UC-01 |
| **Tên UC** | Xem trang chủ |
| **Actor chính** | Khách (A1), Người dùng (A2) |
| **Actor phụ** | Firebase Firestore (hệ thống) |
| **Mô tả tóm tắt** | Người dùng mở ứng dụng và được chuyển đến màn hình trang chủ, hiển thị các quiz theo ngữ cảnh đăng nhập |
| **Điều kiện tiên quyết** | Ứng dụng đã khởi động; kết nối mạng tùy chọn (có Room cache) |
| **Luồng chính** | 1. Ứng dụng khởi động, `MainActivity` render `QuizzezNavHost` / 2. `HomeViewModel` gọi `QuizRepository.getHomeQuizzes(userId)` / 3. Room trả về dữ liệu cache ngay lập tức / 4. Nếu online, SyncManager làm mới từ Firestore / 5. `HomeScreen` hiển thị: `recentAttemptQuizzes`, `myQuizzes`, `trendingQuizzes` |
| **Luồng thay thế** | A1. Khách chưa đăng nhập: ẩn mục "Quiz của tôi", chỉ hiển thị trending/public; không có recent attempts |
| **Luồng ngoại lệ** | E1. Không có mạng và cache rỗng → hiển thị `EmptyState` với thông báo kiểm tra kết nối |
| **Hậu điều kiện** | Danh sách quiz được hiển thị; dữ liệu Room được cập nhật nếu online |
| **Màn hình liên quan** | `HomeScreen`, `HomeViewModel` |
| **Business Rule** | Trending quiz là public và không bị xóa (`deletedAt == null`, `isPublic == true`, `isDraft == false`) |

---

### UC-04: Làm quiz qua mã chia sẻ

| Trường | Nội dung |
|--------|---------|
| **Mã UC** | UC-04 |
| **Tên UC** | Làm quiz qua mã chia sẻ |
| **Actor chính** | Khách (A1), Người dùng (A2) |
| **Actor phụ** | Firebase Firestore, `ShareCodeRepository`, `ScoreCalculator`, `QuestionShuffler` |
| **Mô tả tóm tắt** | Người dùng nhập mã 6 ký tự để truy cập và làm một quiz cụ thể, kể cả quiz không công khai |
| **Điều kiện tiên quyết** | Có kết nối mạng; có mã chia sẻ hợp lệ |
| **Luồng chính** | 1. Người dùng nhập mã 6 ký tự vào `CodeInputField` / 2. `ShareCodeRepository.validateShareCode(code)` tra cứu collection `shareCodes` trên Firestore / 3. Lấy `quizId` từ document mã chia sẻ / 4. `QuizRepository.getQuizById(quizId)` trả về quiz / 5. Điều hướng đến `QuizDetailScreen` / 6. Người dùng bắt đầu → `TakeQuizScreen` / 7. `QuestionShuffler.shuffle()` xáo trộn câu hỏi và đáp án / 8. Người dùng trả lời từng câu / 9. Nộp bài → `ScoreCalculator.calculatePointScore()` tính điểm / 10. `AttemptRepository.saveAttempt()` lưu kết quả / 11. Điều hướng đến `QuizResultScreen` |
| **Luồng thay thế** | A1. Quiz đang ở trạng thái Draft (`isDraft == true`) → hiển thị thông báo lỗi, không cho phép làm bài |
| **Luồng ngoại lệ** | E1. Mã không tồn tại trong Firestore → hiển thị `ErrorState` "Mã không hợp lệ" / E2. Mạng gián đoạn giữa chừng → lưu tiến trình tạm thời, hiển thị cảnh báo |
| **Hậu điều kiện** | Attempt được lưu vào Room (PENDING) và đồng bộ lên Firestore; điểm và xếp hạng sao hiển thị |
| **Màn hình liên quan** | `TakeQuizScreen`, `TakeQuizViewModel`, `QuizDetailScreen`, `QuizResultScreen` |
| **Business Rule** | Mã chia sẻ là 6 ký tự in hoa, chỉ gồm A–Z và 0–9; một mã ánh xạ duy nhất đến một `quizId` trong collection `shareCodes` |

---

### UC-06: Đăng nhập / Đăng ký

| Trường | Nội dung |
|--------|---------|
| **Mã UC** | UC-06 |
| **Tên UC** | Đăng nhập / Đăng ký |
| **Actor chính** | Khách (A1) |
| **Actor phụ** | Firebase Authentication, Firestore, Room |
| **Mô tả tóm tắt** | Khách thực hiện đăng nhập bằng tài khoản có sẵn hoặc đăng ký tài khoản mới để trở thành Người dùng |
| **Điều kiện tiên quyết** | Có kết nối mạng; ứng dụng đang ở màn hình xác thực |
| **Luồng chính (Đăng nhập)** | 1. Khách nhập email và mật khẩu vào `LoginScreen` / 2. `AuthViewModel` gọi `AuthRepository.login(email, password)` / 3. `FirebaseAuth.signInWithEmailAndPassword()` xác thực / 4. Thông tin `User` được cache vào Room / 5. Điều hướng về `HomeScreen` |
| **Luồng chính (Đăng ký)** | 1. Khách nhập email, mật khẩu, tên người dùng vào `RegisterScreen` / 2. `AuthRepository.register()` gọi `FirebaseAuth.createUserWithEmailAndPassword()` / 3. Tạo bản ghi `User` lưu vào Firestore và Room / 4. Điều hướng về `HomeScreen` |
| **Luồng thay thế** | A1. Khách đã có attempt khi chưa đăng nhập → sau đăng ký gọi `linkGuestAttempts(guestId, userId)` để liên kết lịch sử |
| **Luồng ngoại lệ** | E1. Sai mật khẩu → hiển thị lỗi "Sai email hoặc mật khẩu" / E2. Email đã tồn tại → hiển thị lỗi "Email đã được sử dụng" / E3. Mất kết nối mạng → hiển thị `ErrorState` |
| **Hậu điều kiện** | Người dùng được xác thực; `AuthRepository` phát `currentUser` flow; bottom nav hiển thị đầy đủ |
| **Màn hình liên quan** | `AuthFragment`, `LoginScreen`, `RegisterScreen`, `AuthViewModel` |
| **Business Rule** | Mật khẩu tối thiểu 6 ký tự (Firebase mặc định); email phải đúng định dạng; username không được rỗng |

---

### UC-07: Tạo quiz thủ công

| Trường | Nội dung |
|--------|---------|
| **Mã UC** | UC-07 |
| **Tên UC** | Tạo quiz thủ công |
| **Actor chính** | Người dùng (A2) |
| **Actor phụ** | Room, Firestore, `SyncManager`, `QuizValidator` |
| **Mô tả tóm tắt** | Người dùng tạo một quiz mới bằng cách điền thông tin và thêm câu hỏi thủ công qua giao diện |
| **Điều kiện tiên quyết** | Người dùng đã đăng nhập |
| **Luồng chính** | 1. Nhấn `CreateQuizFAB` → điều hướng đến `CreateQuizScreen` / 2. Điền tiêu đề, mô tả, tags, cài đặt `isPublic` / 3. Thêm câu hỏi qua `QuestionDraft`, mỗi câu thêm 2–10 `ChoiceDraft` / 4. Đánh dấu đáp án đúng (≥1 mỗi câu) / 5. `QuizValidator.validate()` kiểm tra hợp lệ / 6. `CreateQuizViewModel.saveQuiz()` lưu vào Room với `syncStatus = PENDING` / 7. `SyncManager.enqueueSync()` đồng bộ lên Firestore |
| **Luồng thay thế** | A1. Lưu nháp (`isDraft = true`): `isPublic` bị ép về `false`; quiz không hiển thị công khai |
| **Luồng ngoại lệ** | E1. Validator thất bại (thiếu câu hỏi, không có đáp án đúng, nội dung rỗng) → hiển thị lỗi inline tương ứng |
| **Hậu điều kiện** | Quiz được lưu vào Room; đồng bộ Firestore khi online; hiển thị trong "Quiz của tôi" |
| **Màn hình liên quan** | `CreateQuizScreen`, `CreateQuizViewModel`, `QuizFormContent` |
| **Business Rule** | Tối thiểu 1 câu hỏi; mỗi câu 2–10 lựa chọn; ít nhất 1 đáp án đúng mỗi câu; không có nội dung rỗng; Draft buộc `isPublic = false` |

---

### UC-08: Nhập quiz từ CSV

| Trường | Nội dung |
|--------|---------|
| **Mã UC** | UC-08 |
| **Tên UC** | Nhập quiz từ CSV |
| **Actor chính** | Người dùng (A2) |
| **Actor phụ** | `CsvParser`, `CsvValidator`, `QuizPreviewScreen` |
| **Mô tả tóm tắt** | Người dùng tải lên file CSV để tự động tạo quiz với nhiều câu hỏi từ dữ liệu có cấu trúc |
| **Điều kiện tiên quyết** | Người dùng đã đăng nhập; có file CSV đúng định dạng |
| **Luồng chính** | 1. Điều hướng đến `CsvImportScreen` / 2. Người dùng chọn file CSV từ thiết bị / 3. `CsvImportViewModel` đọc nội dung file / 4. `CsvParser.parse()` phân tích cú pháp từng dòng / 5. `CsvValidator.validate()` kiểm tra tính hợp lệ / 6. Điều hướng đến `QuizPreviewScreen` hiển thị preview quiz / 7. Người dùng xác nhận → gọi `CreateQuizViewModel.saveQuiz()` |
| **Luồng thay thế** | A1. File hợp lệ một phần → hiển thị cảnh báo các dòng lỗi, cho phép tiếp tục với các dòng hợp lệ |
| **Luồng ngoại lệ** | E1. Định dạng sai (thiếu cột bắt buộc, sai encoding) → hiển thị lỗi kèm số dòng vi phạm / E2. File rỗng → hiển thị `EmptyState` |
| **Hậu điều kiện** | Quiz được tạo tương đương UC-07; lưu vào Room và đồng bộ Firestore |
| **Màn hình liên quan** | `CsvImportScreen`, `CsvImportViewModel`, `QuizPreviewScreen`, `QuizPreviewViewModel` |
| **Business Rule** | CSV phải có các cột bắt buộc: nội dung câu hỏi, các lựa chọn, đánh dấu đáp án đúng; quy tắc `QuizValidator` áp dụng sau khi parse |

---

### UC-10: Xóa quiz (soft delete)

| Trường | Nội dung |
|--------|---------|
| **Mã UC** | UC-10 |
| **Tên UC** | Xóa quiz (soft delete) |
| **Actor chính** | Người dùng (A2) |
| **Actor phụ** | Room, Firestore, `SyncManager` |
| **Mô tả tóm tắt** | Người dùng xóa quiz của mình; quiz được chuyển vào thùng rác thay vì xóa vĩnh viễn |
| **Điều kiện tiên quyết** | Người dùng đã đăng nhập; là chủ sở hữu quiz |
| **Luồng chính** | 1. Mở `QuizDetailScreen` → nhấn nút xóa / 2. Hiển thị `AlertDialog` xác nhận / 3. Xác nhận → `QuizRepository.deleteQuiz(quizId)` / 4. Room cập nhật: `deletedAt = now()`, `syncStatus = PENDING` / 5. `SyncManager` đồng bộ thay đổi lên Firestore / 6. Quiz biến mất khỏi danh sách chính; xuất hiện trong `TrashScreen` |
| **Luồng thay thế** | A1. Người dùng hủy dialog → không có thay đổi |
| **Luồng ngoại lệ** | E1. Quiz đã bị xóa trước đó → hiển thị lỗi trạng thái không hợp lệ |
| **Hậu điều kiện** | `deletedAt` được gán timestamp; quiz không hiển thị trong feed chính; có thể khôi phục trong 30 ngày |
| **Màn hình liên quan** | `QuizDetailScreen`, `TrashScreen`, `RecycleBinViewModel` |
| **Business Rule** | Chỉ chủ sở hữu quiz mới được xóa; quiz được giữ trong thùng rác tối đa 30 ngày trước khi tự động xóa vĩnh viễn |

---

### UC-11: Khôi phục quiz từ thùng rác

| Trường | Nội dung |
|--------|---------|
| **Mã UC** | UC-11 |
| **Tên UC** | Khôi phục quiz từ thùng rác |
| **Actor chính** | Người dùng (A2) |
| **Actor phụ** | Room, Firestore, `SyncManager` |
| **Mô tả tóm tắt** | Người dùng khôi phục quiz đã xóa từ thùng rác về trạng thái hoạt động bình thường |
| **Điều kiện tiên quyết** | Người dùng đã đăng nhập; quiz nằm trong thùng rác; `deletedAt` chưa quá 30 ngày |
| **Luồng chính** | 1. Điều hướng đến `TrashScreen` / 2. `RecycleBinViewModel` tải danh sách quiz đã xóa mềm / 3. Người dùng chọn quiz → nhấn "Khôi phục" / 4. `QuizRepository.restoreQuiz(quizId)` / 5. Room cập nhật: `deletedAt = null`, `syncStatus = PENDING` / 6. `SyncManager` đồng bộ lên Firestore / 7. Quiz xuất hiện lại trong danh sách "Quiz của tôi" |
| **Luồng thay thế** | A1. Không có quiz nào trong thùng rác → hiển thị `EmptyState` |
| **Luồng ngoại lệ** | E1. Quiz có `deletedAt` quá 30 ngày → không thể khôi phục; nút khôi phục bị vô hiệu hóa |
| **Hậu điều kiện** | `deletedAt` được đặt về `null`; quiz trở lại trạng thái trước khi xóa; đồng bộ với Firestore |
| **Màn hình liên quan** | `TrashScreen`, `RecycleBinViewModel` |
| **Business Rule** | Chỉ quiz có `deletedAt ≤ 30 ngày` mới được phép khôi phục; quiz cũ hơn phải bị xóa vĩnh viễn |

---

### UC-13: Chia sẻ quiz qua mã

| Trường | Nội dung |
|--------|---------|
| **Mã UC** | UC-13 |
| **Tên UC** | Chia sẻ quiz qua mã |
| **Actor chính** | Người dùng (A2) |
| **Actor phụ** | Clipboard hệ thống, Intent chia sẻ Android |
| **Mô tả tóm tắt** | Người dùng chia sẻ mã truy cập quiz cho người khác qua clipboard hoặc ứng dụng bên thứ ba |
| **Điều kiện tiên quyết** | Người dùng đã đăng nhập; quiz đã có mã chia sẻ (đã thực hiện UC-14) |
| **Luồng chính** | 1. Mở `QuizDetailScreen` / 2. Component `ShareCodeSection` hiển thị mã chia sẻ hiện tại / 3. Người dùng nhấn "Sao chép" → mã được copy vào clipboard / 4. Hoặc nhấn "Chia sẻ" → Android Share Intent mở lên / 5. Người nhận dùng mã này thực hiện UC-04 |
| **Luồng thay thế** | A1. Quiz chưa có mã → `ShareCodeSection` hiển thị nút "Tạo mã", kích hoạt UC-14 trước |
| **Luồng ngoại lệ** | E1. Quiz ở trạng thái Draft → hiển thị cảnh báo "Cần publish quiz trước khi chia sẻ" |
| **Hậu điều kiện** | Mã được sao chép vào clipboard; người nhận có thể dùng mã để truy cập quiz |
| **Màn hình liên quan** | `QuizDetailScreen`, component `ShareCodeSection` |
| **Business Rule** | Phụ thuộc UC-14 để có mã; quiz ở trạng thái Draft không nên được chia sẻ để làm bài |

---

### UC-14: Tạo/tái tạo mã chia sẻ

| Trường | Nội dung |
|--------|---------|
| **Mã UC** | UC-14 |
| **Tên UC** | Tạo/tái tạo mã chia sẻ |
| **Actor chính** | Người dùng (A2) |
| **Actor phụ** | `ShareCodeUtil`, `ShareCodeRepository`, Firestore |
| **Mô tả tóm tắt** | Hệ thống tạo mã 6 ký tự duy nhất để ánh xạ đến một quiz, hoặc thay thế mã cũ bằng mã mới |
| **Điều kiện tiên quyết** | Người dùng đã đăng nhập; là chủ sở hữu quiz; có kết nối mạng |
| **Luồng chính** | 1. `QuizDetailScreen` → nhấn "Tạo mã" hoặc "Tái tạo mã" / 2. `ShareCodeUtil.generateCode()` tạo chuỗi 6 ký tự ngẫu nhiên / 3. `ShareCodeRepository.generateShareCode(quizId)` kiểm tra tính duy nhất trên Firestore (thử tối đa 10 lần) / 4. Lưu document mới vào collection `shareCodes` / 5. Cập nhật trường `shareCode` trong quiz trên Room và Firestore / 6. `ShareCodeSection` hiển thị mã mới |
| **Luồng thay thế** | A1. Tái tạo mã: mã cũ bị xóa khỏi `shareCodes` trước khi tạo mã mới |
| **Luồng ngoại lệ** | E1. Không thể tìm mã duy nhất sau 10 lần thử → hiển thị lỗi "Không thể tạo mã, thử lại sau" |
| **Hậu điều kiện** | Mã mới được lưu vào Firestore; mã cũ bị vô hiệu; `quiz.shareCode` được cập nhật |
| **Màn hình liên quan** | `QuizDetailScreen`, component `ShareCodeSection` |
| **Business Rule** | Mã gồm 6 ký tự in hoa A–Z và 0–9; duy nhất trên toàn hệ thống; mã cũ bị xóa khi tái tạo |

---

### UC-15: Xem lịch sử thi

| Trường | Nội dung |
|--------|---------|
| **Mã UC** | UC-15 |
| **Tên UC** | Xem lịch sử thi |
| **Actor chính** | Người dùng (A2) |
| **Actor phụ** | `AttemptRepository`, `QuizRepository`, `ScoreUtil`, `TimeFormatter` |
| **Mô tả tóm tắt** | Người dùng xem toàn bộ lịch sử các lần làm bài của mình, bao gồm điểm, xếp hạng sao và thời gian |
| **Điều kiện tiên quyết** | Người dùng đã đăng nhập |
| **Luồng chính** | 1. Từ `ProfileScreen` → điều hướng đến `HistoryScreen` / 2. `HistoryViewModel` gọi `AttemptRepository.getAttemptsByUser(userId)` / 3. Kết hợp với `QuizRepository.getQuizById()` để lấy tiêu đề quiz → tạo danh sách `AttemptWithQuiz` / 4. Sắp xếp theo thời gian gần nhất / 5. Hiển thị điểm phần trăm (`ScoreUtil`), xếp hạng sao, thời gian (`TimeFormatter.formatTimestamp()`) |
| **Luồng thay thế** | A1. Chưa có attempt nào → hiển thị `EmptyState` "Bạn chưa làm bài thi nào" |
| **Luồng ngoại lệ** | E1. Lỗi tải dữ liệu → hiển thị `ErrorState` kèm nút thử lại |
| **Hậu điều kiện** | Danh sách lịch sử được hiển thị; người dùng có thể chọn để xem chi tiết (UC-16) |
| **Màn hình liên quan** | `HistoryScreen`, `HistoryViewModel` |
| **Business Rule** | `AttemptWithQuiz` là UI-layer helper class trong `HistoryViewModel.kt`, không thuộc `domain/`; điểm sao tính theo `ScoreUtil.calculateStarRating()` |

---

### UC-16: Xem lại đáp án chi tiết

| Trường | Nội dung |
|--------|---------|
| **Mã UC** | UC-16 |
| **Tên UC** | Xem lại đáp án chi tiết |
| **Actor chính** | Người dùng (A2) |
| **Actor phụ** | `AttemptRepository`, `QuizRepository` |
| **Mô tả tóm tắt** | Người dùng xem lại từng câu hỏi trong lần thi đã hoàn thành, so sánh đáp án của mình với đáp án đúng |
| **Điều kiện tiên quyết** | Người dùng đã đăng nhập; đã có ít nhất một attempt đã hoàn thành |
| **Luồng chính** | 1. Từ `HistoryScreen` → nhấn vào một attempt / 2. Điều hướng đến `AnswerReviewScreen` / 3. `AnswerReviewViewModel` tải attempt và quiz tương ứng / 4. Với mỗi câu hỏi: tạo `QuestionReview` (câu hỏi + đáp án người dùng chọn + đáp án đúng + `isCorrect`) / 5. Hiển thị từng `QuestionReview` với màu sắc đúng/sai / 6. Hiển thị giải thích nếu có |
| **Luồng thay thế** | A1. Attempt không có dữ liệu chi tiết → hiển thị chỉ điểm tổng |
| **Luồng ngoại lệ** | E1. Quiz gốc đã bị xóa vĩnh viễn → hiển thị thông báo "Quiz không còn tồn tại" nhưng vẫn hiện đáp án đã lưu |
| **Hậu điều kiện** | Người dùng hiểu rõ câu nào đúng/sai; không thay đổi dữ liệu |
| **Màn hình liên quan** | `AnswerReviewScreen`, `AnswerReviewViewModel` |
| **Business Rule** | `QuestionReview` là UI-layer helper class trong `AnswerReviewViewModel.kt`; đánh giá đúng/sai dùng `ScoreCalculator` (exact set equality) |

---

### UC-17: Đóng góp câu hỏi vào kho

| Trường | Nội dung |
|--------|---------|
| **Mã UC** | UC-17 |
| **Tên UC** | Đóng góp câu hỏi vào kho |
| **Actor chính** | Người dùng (A2) |
| **Actor phụ** | `PoolRepository`, Firestore |
| **Mô tả tóm tắt** | Người dùng đóng góp câu hỏi từ quiz của mình vào kho câu hỏi chung để cộng đồng sử dụng |
| **Điều kiện tiên quyết** | Người dùng đã đăng nhập; có quiz với câu hỏi hợp lệ; có kết nối mạng |
| **Luồng chính** | 1. Điều hướng đến `QuestionPoolScreen` / 2. Chọn câu hỏi từ quiz hiện có / 3. Chọn tags phân loại / 4. Bật/tắt tùy chọn ẩn danh (`anonymize`) / 5. `QuestionPoolViewModel` gọi `PoolRepository.contributeQuestions(questions, contributorId, sourceQuizId, tags, anonymize)` / 6. Batch write lên collection `questionPool` trên Firestore |
| **Luồng thay thế** | A1. Không có quiz nào → hiển thị hướng dẫn tạo quiz trước |
| **Luồng ngoại lệ** | E1. Mạng ngắt kết nối → hiển thị lỗi, batch write thất bại toàn bộ (atomicity) |
| **Hậu điều kiện** | Câu hỏi mới trong pool: `isActive = true`, `usageCount = 0`; nếu ẩn danh thì `contributorId = null` |
| **Màn hình liên quan** | `QuestionPoolScreen`, `QuestionPoolViewModel` |
| **Business Rule** | Nếu `anonymize = true`, `contributorId` lưu là `null`; `usageCount` khởi tạo bằng 0; `PoolRepositoryImpl` dùng batch write của `FirebaseFirestore` |

---

### UC-18: Tự động tạo quiz từ kho

| Trường | Nội dung |
|--------|---------|
| **Mã UC** | UC-18 |
| **Tên UC** | Tự động tạo quiz từ kho câu hỏi |
| **Actor chính** | Người dùng (A2) |
| **Actor phụ** | `PoolRepository`, Firestore |
| **Mô tả tóm tắt** | Người dùng chọn tags và số lượng để hệ thống tự chọn câu hỏi từ kho và tạo quiz mới |
| **Điều kiện tiên quyết** | Người dùng đã đăng nhập; kho có câu hỏi active phù hợp tags; có kết nối mạng |
| **Luồng chính** | 1. `QuestionPoolScreen` → tab tự động tạo / 2. Chọn một hoặc nhiều tags / 3. Đặt số lượng câu hỏi mong muốn / 4. `PoolRepository.autoGenerateQuiz(tags, count)` truy vấn pool (chỉ `isActive = true`) / 5. Chọn ngẫu nhiên `count` câu hỏi / 6. `incrementUsageCount()` cho từng câu được chọn / 7. Điều hướng đến `CreateQuizScreen` với câu hỏi được điền sẵn |
| **Luồng thay thế** | A1. Kho có ít câu hơn `count` → trả về tất cả câu hỏi có sẵn, thông báo số lượng thực tế |
| **Luồng ngoại lệ** | E1. Không có câu hỏi nào phù hợp tag → hiển thị `EmptyState` gợi ý chọn tag khác |
| **Hậu điều kiện** | `CreateQuizScreen` được mở với các `QuestionDraft` được điền sẵn; `usageCount` trong pool được cập nhật |
| **Màn hình liên quan** | `QuestionPoolScreen`, `QuestionPoolViewModel`, `CreateQuizScreen` |
| **Business Rule** | Chỉ `isActive = true` mới được chọn; `usageCount` tăng cho mỗi câu được dùng; số lượng thực tế có thể nhỏ hơn yêu cầu |

---

### UC-22: Xem dashboard thống kê (Admin)

| Trường | Nội dung |
|--------|---------|
| **Mã UC** | UC-22 |
| **Tên UC** | Xem dashboard thống kê |
| **Actor chính** | Quản trị viên (A3), Siêu quản trị (A4) |
| **Actor phụ** | `AdminRepository`, Firestore |
| **Mô tả tóm tắt** | Admin xem tổng quan thống kê hệ thống gồm số người dùng, quiz, lượt thi và các chỉ số phân tích |
| **Điều kiện tiên quyết** | Đã đăng nhập; có role `ADMIN` hoặc `SUPERUSER`; có quyền `VIEW_REPORTS` (hoặc là Superuser) |
| **Luồng chính** | 1. Từ `ProfileScreen` → mục "Quản trị" / 2. Điều hướng đến route `admin/dashboard` / 3. `AdminDashboardViewModel` gọi `AdminRepository.getSystemStats()` / 4. Firestore trả về `SystemStats` (tổng người dùng, quiz, lượt thi, v.v.) / 5. Hiển thị qua `AdminChart`, `StatisticCard`, `AdminInsightCard` |
| **Luồng thay thế** | A1. Không có dữ liệu → hiển thị giá trị 0 cho tất cả thống kê |
| **Luồng ngoại lệ** | E1. Người dùng không có quyền → `NavHost` chặn điều hướng, hiển thị thông báo "Không có quyền truy cập" |
| **Hậu điều kiện** | Dashboard hiển thị dữ liệu thống kê mới nhất; không thay đổi dữ liệu hệ thống |
| **Màn hình liên quan** | `AdminDashboardScreen`, `AdminDashboardViewModel`, `AdminDashboardUiState` |
| **Business Rule** | Quyền truy cập được kiểm tra ở cả `NavHost` và Firestore Security Rules; `BaseAdminStatsViewModel` là base class chung cho admin ViewModels |

---

### UC-25: Quản lý quiz toàn hệ thống (Admin)

| Trường | Nội dung |
|--------|---------|
| **Mã UC** | UC-25 |
| **Tên UC** | Quản lý quiz toàn hệ thống |
| **Actor chính** | Quản trị viên (A3), Siêu quản trị (A4) |
| **Actor phụ** | `AdminRepository`, Firestore |
| **Mô tả tóm tắt** | Admin xem, tìm kiếm, lọc và thực hiện các hành động quản trị trên toàn bộ quiz trong hệ thống |
| **Điều kiện tiên quyết** | Đã đăng nhập; có quyền `MANAGE_QUIZZES` |
| **Luồng chính** | 1. Điều hướng đến route `admin/quizzes` / 2. `AdminQuizManagementViewModel` gọi `AdminRepository.getAllQuizzes(includeDeleted = true)` / 3. Trả về danh sách phân trang (`PaginatedResult`) / 4. Hiển thị danh sách với `AdminQuizCard` / 5. Mở rộng phần lọc/tìm kiếm (`AnimatedVisibility`, mặc định thu gọn) / 6. Áp dụng bộ lọc → tải lại kết quả / 7. Chọn hành động: forcePublish, unpublish, xóa vĩnh viễn |
| **Luồng thay thế** | A1. Không tìm thấy quiz khớp bộ lọc → hiển thị `EmptyState` |
| **Luồng ngoại lệ** | E1. Thiếu quyền `MANAGE_QUIZZES` → `NavHost` chặn; Firestore rules từ chối ghi |
| **Hậu điều kiện** | Thay đổi trạng thái quiz được lưu lên Firestore và phản ánh ngay trong danh sách |
| **Màn hình liên quan** | `AdminQuizManagementScreen`, `AdminQuizManagementViewModel`, `AdminQuizManagementUiState` |
| **Business Rule** | Phần lọc/tìm kiếm thu gọn mặc định, toggle qua `AnimatedVisibility`; phân trang dùng `PaginatedResult` từ domain |

---

### UC-28: Phân quyền cho Admin (Superuser)

| Trường | Nội dung |
|--------|---------|
| **Mã UC** | UC-28 |
| **Tên UC** | Phân quyền cho Admin |
| **Actor chính** | Siêu quản trị (A4) |
| **Actor phụ** | `AdminRepository`, Firestore |
| **Mô tả tóm tắt** | Superuser cấp, thay đổi hoặc thu hồi quyền quản trị cho người dùng thông qua giao diện quản lý |
| **Điều kiện tiên quyết** | Đã đăng nhập với role `SUPERUSER`; có kết nối mạng |
| **Luồng chính** | 1. Điều hướng đến route `admin/users` / 2. `AdminUserManagementViewModel` tải danh sách người dùng / 3. Chọn người dùng cần phân quyền / 4. Component `RoleSelector` hiển thị vai trò và danh sách `AdminPermission` / 5. Superuser điều chỉnh role và permissions / 6. `AdminRepository.updateAdminPermissions(userId, permissions)` / 7. Cập nhật trường `permissions` trong document người dùng trên Firestore |
| **Luồng thay thế** | A1. Superuser xem profile của admin → nút phân quyền khả dụng; admin bình thường không thấy nút này |
| **Luồng ngoại lệ** | E1. Admin cố gắng sửa quyền người dùng khác → Firestore Security Rules từ chối / E2. Mạng ngắt → hiển thị lỗi, không lưu thay đổi |
| **Hậu điều kiện** | Quyền mới được lưu vào Firestore; người dùng được cấp quyền có thể truy cập các chức năng admin tương ứng ngay khi refresh token |
| **Màn hình liên quan** | `AdminUserManagementScreen`, `AdminUserManagementViewModel`, component `RoleSelector` |
| **Business Rule** | CHỈ Superuser mới được gán/sửa quyền admin; Admin không được sửa quyền của chính mình hoặc admin khác; Superuser có tất cả quyền ngầm định qua `AdminPermission.all()` |

---

*Ghi chú: Các UC còn lại (UC-02, UC-03, UC-05, UC-09, UC-12, UC-19, UC-20, UC-21, UC-23, UC-24, UC-26, UC-27, UC-29, UC-30) có mô tả tương tự theo cùng cấu trúc bảng, áp dụng đúng actor, màn hình và business rule tương ứng đã nêu trong sơ đồ 7.2.*

## 8. BẢNG TỔNG HỢP TẤT CẢ FILE CODE

Bảng dưới đây liệt kê toàn bộ **188 file** mã nguồn trong dự án Quizzez, nhóm theo tầng kiến trúc và chức năng. Tiền tố đường dẫn chung cho tất cả file Kotlin: `app/src/main/java/com/example/androidapp/`.

### 8.1 Tầng Domain — Models

| STT | Tên File | Đường dẫn (tương đối) | Tầng | Mục đích | Phụ thuộc chính |
|-----|----------|-----------------------|------|----------|-----------------|
| 1 | Quiz.kt | domain/model/ | Domain | Domain model bài quiz | - |
| 2 | Question.kt | domain/model/ | Domain | Domain model câu hỏi | Choice |
| 3 | Choice.kt | domain/model/ | Domain | Domain model đáp án | - |
| 4 | Attempt.kt | domain/model/ | Domain | Domain model lần thi | - |
| 5 | User.kt | domain/model/ | Domain | Domain model người dùng | UserRole, AdminPermission |
| 6 | UserRole.kt | domain/model/ | Domain | Enum vai trò người dùng | - |
| 7 | AdminPermission.kt | domain/model/ | Domain | Enum quyền admin | - |
| 8 | QuestionPoolItem.kt | domain/model/ | Domain | Domain model kho câu hỏi | Question |
| 9 | SystemStats.kt | domain/model/ | Domain | Thống kê hệ thống admin | - |
| 10 | PaginatedResult.kt | domain/model/ | Domain | Wrapper kết quả phân trang | - |

### 8.2 Tầng Domain — Repository Interfaces

| STT | Tên File | Đường dẫn (tương đối) | Tầng | Mục đích | Phụ thuộc chính |
|-----|----------|-----------------------|------|----------|-----------------|
| 11 | AuthRepository.kt | domain/repository/ | Domain | Interface xác thực | User |
| 12 | QuizRepository.kt | domain/repository/ | Domain | Interface quản lý quiz | Quiz, Question, HomeQuizzes |
| 13 | AttemptRepository.kt | domain/repository/ | Domain | Interface lần thi | Attempt |
| 14 | ShareCodeRepository.kt | domain/repository/ | Domain | Interface mã chia sẻ | - |
| 15 | PoolRepository.kt | domain/repository/ | Domain | Interface kho câu hỏi | QuestionPoolItem |
| 16 | AdminRepository.kt | domain/repository/ | Domain | Interface quản trị | User, Quiz, SystemStats |
| 17 | SearchRepository.kt | domain/repository/ | Domain | Interface tìm kiếm/lịch sử | - |

### 8.3 Tầng Domain — Utilities

| STT | Tên File | Đường dẫn (tương đối) | Tầng | Mục đích | Phụ thuộc chính |
|-----|----------|-----------------------|------|----------|-----------------|
| 18 | ScoreCalculator.kt | domain/util/ | Domain | Tính điểm exact-set-equality | Question |
| 19 | ScoreUtil.kt | domain/util/ | Domain | Tính % điểm, đánh sao | - |
| 20 | QuizValidator.kt | domain/util/ | Domain | Validate cấu trúc quiz | - |
| 21 | ChecksumUtil.kt | domain/util/ | Domain | SHA-256 checksum quiz | Quiz, Question |
| 22 | QuestionShuffler.kt | domain/util/ | Domain | Xáo trộn câu hỏi/đáp án | - |
| 23 | CsvParser.kt | domain/util/ | Domain | Parse CSV string | - |
| 24 | CsvValidator.kt | domain/util/ | Domain | Validate CSV data | - |
| 25 | SearchFilterLogic.kt | domain/util/ | Domain | Lọc quiz theo tag/ngày/visibility | - |
| 26 | ShareCodeUtil.kt | domain/util/ | Domain | Tạo mã chia sẻ 6 ký tự | - |
| 27 | InputSanitizer.kt | domain/util/ | Domain | Làm sạch input người dùng | - |
| 28 | TagValidator.kt | domain/util/ | Domain | Validate tag | - |
| 29 | TimeFormatter.kt | domain/util/ | Domain | Định dạng thời gian HH:MM:SS | - |
| 30 | SafeCall.kt | domain/util/ | Domain | Wrapper try/catch → Result<T> | - |

### 8.4 Tầng Dữ liệu — Local Room

| STT | Tên File | Đường dẫn (tương đối) | Tầng | Mục đích | Phụ thuộc chính |
|-----|----------|-----------------------|------|----------|-----------------|
| 31 | AppDatabase.kt | data/local/ | Data | Room DB v5, fallbackToDestructiveMigration | Tất cả entities |
| 32 | EntityMappers.kt | data/local/ | Data | Entity ↔ Domain mappers (toDomain/toEntity) | Gson |
| 33 | LocalQuizPurger.kt | data/local/ | Data | Purge stale local quiz data | QuizDao, QuestionDao, ChoiceDao |
| 34 | Converters.kt | data/local/converter/ | Data | Room TypeConverters dùng Gson | Gson |
| 35 | QuizDao.kt | data/local/dao/ | Data | CRUD + Flow cho quiz | QuizEntity |
| 36 | QuestionDao.kt | data/local/dao/ | Data | CRUD cho câu hỏi | QuestionEntity |
| 37 | ChoiceDao.kt | data/local/dao/ | Data | CRUD cho đáp án | ChoiceEntity |
| 38 | AttemptDao.kt | data/local/dao/ | Data | CRUD + Flow cho lần thi | AttemptEntity |
| 39 | UserDao.kt | data/local/dao/ | Data | CRUD cho người dùng | UserEntity |
| 40 | PendingSyncDao.kt | data/local/dao/ | Data | Hàng đợi sync, observePendingCount() | PendingSyncEntity |
| 41 | QuizEntity.kt | data/local/entity/ | Data | Bảng quizzes trong Room | - |
| 42 | QuestionEntity.kt | data/local/entity/ | Data | Bảng questions trong Room | - |
| 43 | ChoiceEntity.kt | data/local/entity/ | Data | Bảng choices trong Room | - |
| 44 | AttemptEntity.kt | data/local/entity/ | Data | Bảng attempts trong Room | - |
| 45 | UserEntity.kt | data/local/entity/ | Data | Bảng users trong Room | - |
| 46 | PendingSyncEntity.kt | data/local/entity/ | Data | Bảng pending_sync_operations | SyncStatus enums |

### 8.5 Tầng Dữ liệu — Remote Firebase

| STT | Tên File | Đường dẫn (tương đối) | Tầng | Mục đích | Phụ thuộc chính |
|-----|----------|-----------------------|------|----------|-----------------|
| 47 | FirestoreCollections.kt | data/remote/firebase/ | Data | Hằng số collection/field name | - |
| 48 | QuizRemoteDataSource.kt | data/remote/firebase/ | Data | Firestore CRUD cho quiz | FirebaseFirestore |
| 49 | QuestionRemoteDataSource.kt | data/remote/firebase/ | Data | Firestore CRUD cho câu hỏi | FirebaseFirestore |
| 50 | AttemptRemoteDataSource.kt | data/remote/firebase/ | Data | Firestore CRUD cho lần thi | FirebaseFirestore |
| 51 | UserRemoteDataSource.kt | data/remote/firebase/ | Data | Firestore CRUD cho user | FirebaseFirestore |
| 52 | ShareCodeRemoteDataSource.kt | data/remote/firebase/ | Data | Tạo/tra cứu mã chia sẻ | FirebaseFirestore |
| 53 | PoolRemoteDataSource.kt | data/remote/firebase/ | Data | CRUD kho câu hỏi | FirebaseFirestore |
| 54 | AdminRemoteDataSource.kt | data/remote/firebase/ | Data | Elevated access cho admin | FirebaseFirestore |
| 55 | FirestoreCascadeHelper.kt | data/remote/firebase/ | Data | Xóa cascade quiz+questions+choices | FirebaseFirestore |
| 56 | AppMappers.kt | data/remote/ | Data | DTO ↔ Domain mappers (toDomain/toDto) | - |
| 57 | QuizDtoModels.kt | data/remote/model/ | Data | DTO: QuizDto, QuestionDto, ChoiceDto | - |
| 58 | AttemptDto.kt | data/remote/model/ | Data | DTO cho lần thi | - |
| 59 | UserDto.kt | data/remote/model/ | Data | DTO cho người dùng | - |
| 60 | ShareCodeDto.kt | data/remote/model/ | Data | DTO cho mã chia sẻ | - |
| 61 | QuestionPoolItemDto.kt | data/remote/model/ | Data | DTO cho kho câu hỏi | - |

### 8.6 Tầng Dữ liệu — Repository Implementations & Sync

| STT | Tên File | Đường dẫn (tương đối) | Tầng | Mục đích | Phụ thuộc chính |
|-----|----------|-----------------------|------|----------|-----------------|
| 62 | QuizRepositoryImpl.kt | data/repository/ | Data | Impl QuizRepository (local-first) | QuizDao, QuizRemoteDataSource, SyncManager |
| 63 | AttemptRepositoryImpl.kt | data/repository/ | Data | Impl AttemptRepository (local-first) | AttemptDao, AttemptRemoteDataSource |
| 64 | AuthRepositoryImpl.kt | data/repository/ | Data | Impl AuthRepository | FirebaseAuth, UserDao, UserRemoteDataSource |
| 65 | ShareCodeRepositoryImpl.kt | data/repository/ | Data | Impl ShareCodeRepository (remote-only) | ShareCodeRemoteDataSource |
| 66 | PoolRepositoryImpl.kt | data/repository/ | Data | Impl PoolRepository (remote-only) | PoolRemoteDataSource, FirebaseFirestore |
| 67 | AdminRepositoryImpl.kt | data/repository/ | Data | Impl AdminRepository (remote-only) | AdminRemoteDataSource |
| 68 | SearchRepositoryImpl.kt | data/repository/ | Data | Impl SearchRepository (SharedPrefs) | SharedPreferences |
| 69 | NetworkMonitor.kt | data/network/ | Data | isOnline: StateFlow<Boolean> | ConnectivityManager |
| 70 | SettingsPreferences.kt | data/preferences/ | Data | App settings local | SharedPreferences |
| 71 | SyncManager.kt | data/sync/ | Data | Điều phối đồng bộ nền; SyncState enum | PendingSyncDao, QuizRepository |
| 72 | QuizInvalidationManager.kt | data/sync/ | Data | Tombstone-based invalidation | QuizRemoteDataSource, QuizDao |
| 73 | BackgroundSyncWorker.kt | data/worker/ | Data | WorkManager sync định kỳ | SyncManager |
| 74 | BackendMaintenanceWorker.kt | data/worker/ | Data | WorkManager dọn tombstone 90 ngày | FirebaseFirestore |

### 8.7 Dependency Injection & Application

| STT | Tên File | Đường dẫn (tương đối) | Tầng | Mục đích | Phụ thuộc chính |
|-----|----------|-----------------------|------|----------|-----------------|
| 75 | AppModule.kt | di/ | DI | AppContainer interface | Tất cả repositories, DAOs |
| 76 | FirebaseModule.kt | di/ | DI | AppContainerImpl (lazy init) | Firebase, Room |
| 77 | AppContainerExt.kt | di/ | DI | LocalAppContainer CompositionLocal | AppContainer |
| 78 | QuizzezApplication.kt | root | App | Application class, khởi tạo DI + WorkManager | AppContainerImpl |
| 79 | MainActivity.kt | root | App | Single Activity, render QuizzezNavHost | QuizzezTheme |

### 8.8 Tầng UI — Navigation

| STT | Tên File | Đường dẫn (tương đối) | Tầng | Mục đích | Phụ thuộc chính |
|-----|----------|-----------------------|------|----------|-----------------|
| 80 | Routes.kt | ui/navigation/ | UI | Tất cả route strings + NavigationDestination sealed class | - |
| 81 | QuizzezNavHost.kt | ui/navigation/ | UI | Single NavHost, full navigation graph | Routes, tất cả screens |

### 8.9 Tầng UI — Screens

| STT | Tên File | Đường dẫn (tương đối) | Tầng | Mục đích | Phụ thuộc chính |
|-----|----------|-----------------------|------|----------|-----------------|
| 82 | AuthFragment.kt | ui/screens/auth/ | UI | XML Fragment: Login/Register tabs + Guest | AuthViewModel |
| 83 | LoginScreen.kt | ui/screens/auth/ | UI | Composable màn hình đăng nhập | AuthViewModel |
| 84 | RegisterScreen.kt | ui/screens/auth/ | UI | Composable màn hình đăng ký | AuthViewModel |
| 85 | AuthViewModel.kt | ui/screens/auth/ | UI | Shared ViewModel xác thực | AuthRepository |
| 86 | HomeScreen.kt | ui/screens/home/ | UI | Trang chủ: trending, my quizzes, recent | HomeViewModel |
| 87 | HomeViewModel.kt | ui/screens/home/ | UI | State trang chủ | QuizRepository |
| 88 | SearchScreen.kt | ui/screens/search/ | UI | Màn hình tìm kiếm | SearchViewModel |
| 89 | SearchViewModel.kt | ui/screens/search/ | UI | Search state + filtering | QuizRepository, SearchRepository |
| 90 | SearchUiState.kt | ui/screens/search/ | UI | Data class state + SortOption enum | - |
| 91 | SearchEvent.kt | ui/screens/search/ | UI | Sealed class events tìm kiếm | - |
| 92 | QuizCardDraft.kt | ui/screens/search/ | UI | Display model kết quả tìm kiếm | - |
| 93 | SearchControlsRow.kt | ui/screens/search/ | UI | Row tìm kiếm + sort dropdown | - |
| 94 | TagFilterRow.kt | ui/screens/search/ | UI | Tag filter chips nằm ngang | - |
| 95 | SearchResultsGrid.kt | ui/screens/search/ | UI | Grid layout kết quả | - |
| 96 | SearchResultsList.kt | ui/screens/search/ | UI | List layout kết quả | - |
| 97 | DiscoverSection.kt | ui/screens/search/ | UI | Section khám phá quiz công khai | - |
| 98 | ProfileScreen.kt | ui/screens/profile/ | UI | Màn hình hồ sơ | ProfileViewModel |
| 99 | ProfileViewModel.kt | ui/screens/profile/ | UI | State hồ sơ | AuthRepository |
| 100 | EditProfileScreen.kt | ui/screens/profile/ | UI | Sửa tên, ảnh đại diện URL | EditProfileViewModel |
| 101 | EditProfileViewModel.kt | ui/screens/profile/ | UI | Edit profile + Wallhaven avatar fetch | AuthRepository |
| 102 | QuizDetailScreen.kt | ui/screens/quiz/ | UI | Chi tiết quiz, share code, play | QuizDetailViewModel |
| 103 | QuizDetailViewModel.kt | ui/screens/quiz/ | UI | State chi tiết quiz | QuizRepository, ShareCodeRepository |
| 104 | TakeQuizScreen.kt | ui/screens/quiz/ | UI | Giao diện làm bài | TakeQuizViewModel |
| 105 | TakeQuizViewModel.kt | ui/screens/quiz/ | UI | Logic làm bài, shuffle, score | QuizRepository, AttemptRepository |
| 106 | QuizResultScreen.kt | ui/screens/quiz/ | UI | Hiển thị kết quả sau bài thi | QuizResultViewModel |
| 107 | QuizResultViewModel.kt | ui/screens/quiz/ | UI | State kết quả | AttemptRepository |
| 108 | CreateQuizScreen.kt | ui/screens/create/ | UI | Form tạo quiz mới | CreateQuizViewModel |
| 109 | CreateQuizViewModel.kt | ui/screens/create/ | UI | State tạo quiz; QuestionDraft/ChoiceDraft | QuizRepository, QuizValidator |
| 110 | EditQuizScreen.kt | ui/screens/create/ | UI | Form sửa quiz | EditQuizViewModel |
| 111 | EditQuizViewModel.kt | ui/screens/create/ | UI | State sửa quiz | QuizRepository |
| 112 | QuizFormContent.kt | ui/screens/create/ | UI | Shared form UI cho Create + Edit | - |
| 113 | QuizFormEvent.kt | ui/screens/create/ | UI | Sealed class form events | - |
| 114 | QuizFormHelper.kt | ui/screens/create/ | UI | Shared form logic helpers | QuizValidator |
| 115 | CsvImportScreen.kt | ui/screens/create/ | UI | Import CSV UI | CsvImportViewModel |
| 116 | CsvImportViewModel.kt | ui/screens/create/ | UI | CSV parse + validate flow | CsvParser, CsvValidator |
| 117 | QuizPreviewScreen.kt | ui/screens/create/ | UI | Preview quiz trước khi lưu | QuizPreviewViewModel |
| 118 | QuizPreviewViewModel.kt | ui/screens/create/ | UI | State preview | - |
| 119 | HistoryScreen.kt | ui/screens/history/ | UI | Danh sách lịch sử thi | HistoryViewModel |
| 120 | HistoryViewModel.kt | ui/screens/history/ | UI | AttemptWithQuiz list | AttemptRepository, QuizRepository |
| 121 | AnswerReviewScreen.kt | ui/screens/review/ | UI | Xem lại đáp án chi tiết | AnswerReviewViewModel |
| 122 | AnswerReviewViewModel.kt | ui/screens/review/ | UI | QuestionReview list | AttemptRepository, QuizRepository |
| 123 | AttemptDetailScreen.kt | ui/screens/attempt/ | UI | Chi tiết 1 lần thi | AttemptDetailViewModel |
| 124 | AttemptDetailViewModel.kt | ui/screens/attempt/ | UI | State chi tiết attempt | AttemptRepository |
| 125 | TrashScreen.kt | ui/screens/trash/ | UI | Thùng rác quizzes | RecycleBinViewModel |
| 126 | RecycleBinViewModel.kt | ui/screens/trash/ | UI | State thùng rác (tên KHÔNG phải TrashViewModel) | QuizRepository |
| 127 | SettingsScreen.kt | ui/screens/settings/ | UI | Cài đặt ứng dụng | SettingsViewModel |
| 128 | SettingsViewModel.kt | ui/screens/settings/ | UI | State cài đặt | SettingsPreferences |
| 129 | QuestionPoolScreen.kt | ui/screens/pool/ | UI | Màn hình kho câu hỏi | QuestionPoolViewModel |
| 130 | QuestionPoolViewModel.kt | ui/screens/pool/ | UI | Contributions + auto-generate | PoolRepository |
| 131 | BaseAdminStatsViewModel.kt | ui/screens/admin/ | UI | Base class cho tất cả admin ViewModels | AdminRepository |
| 132 | AdminDashboardScreen.kt | ui/screens/admin/dashboard/ | UI | Dashboard thống kê admin | AdminDashboardViewModel |
| 133 | AdminDashboardViewModel.kt | ui/screens/admin/dashboard/ | UI | System stats state | AdminRepository |
| 134 | AdminDashboardUiState.kt | ui/screens/admin/dashboard/ | UI | UI state data class | - |
| 135 | AdminUserManagementScreen.kt | ui/screens/admin/users/ | UI | Quản lý người dùng, collapsible filter | AdminUserManagementViewModel |
| 136 | AdminUserManagementViewModel.kt | ui/screens/admin/users/ | UI | User list + ban/role state | AdminRepository |
| 137 | AdminUserManagementUiState.kt | ui/screens/admin/users/ | UI | UI state data class | - |
| 138 | AdminQuizManagementScreen.kt | ui/screens/admin/quizzes/ | UI | Quản lý quiz toàn hệ thống | AdminQuizManagementViewModel |
| 139 | AdminQuizManagementViewModel.kt | ui/screens/admin/quizzes/ | UI | Quiz list + publish state | AdminRepository |
| 140 | AdminQuizManagementUiState.kt | ui/screens/admin/quizzes/ | UI | UI state data class | - |
| 141 | AdminReportsScreen.kt | ui/screens/admin/reports/ | UI | Báo cáo phân tích | AdminReportsViewModel |
| 142 | AdminReportsViewModel.kt | ui/screens/admin/reports/ | UI | Analytics state | AdminRepository |
| 143 | AdminReportsUiState.kt | ui/screens/admin/reports/ | UI | UI state data class | - |

### 8.10 Tầng UI — Components

| STT | Tên File | Đường dẫn (tương đối) | Tầng | Mục đích | Phụ thuộc chính |
|-----|----------|-----------------------|------|----------|-----------------|
| 144 | ShareCodeSection.kt | ui/components/ | UI | Hiển thị + copy mã chia sẻ | - |
| 145 | TagSuggestionDialog.kt | ui/components/ | UI | Dialog gợi ý tag | - |
| 146 | AdminChart.kt | ui/components/admin/ | UI | Biểu đồ thống kê admin | - |
| 147 | AdminInsightCard.kt | ui/components/admin/ | UI | Card thông tin phân tích | - |
| 148 | AdminQuizCard.kt | ui/components/admin/ | UI | Card quiz trong admin panel | - |
| 149 | AdminUserCard.kt | ui/components/admin/ | UI | Card người dùng trong admin panel | - |
| 150 | RoleSelector.kt | ui/components/admin/ | UI | Dropdown chọn vai trò người dùng | - |
| 151 | StatisticCard.kt | ui/components/admin/ | UI | Card hiển thị số liệu đơn | - |
| 152 | AlertDialog.kt | ui/components/common/ | UI | Dialog xác nhận hành động | - |
| 153 | BottomSheet.kt | ui/components/common/ | UI | Bottom sheet dùng chung | - |
| 154 | MediaDisplay.kt | ui/components/common/ | UI | Hiển thị ảnh/media qua Coil AsyncImage | Coil |
| 155 | TagChip.kt | ui/components/common/ | UI | Chip hiển thị tag | - |
| 156 | LoginPromptDialog.kt | ui/components/common/ | UI | Dialog nhắc đăng nhập | - |
| 157 | EmptyState.kt | ui/components/feedback/ | UI | Composable trạng thái rỗng | - |
| 158 | ErrorState.kt | ui/components/feedback/ | UI | Composable trạng thái lỗi | - |
| 159 | LoadingSpinner.kt | ui/components/feedback/ | UI | Vòng xoay tải dữ liệu | - |
| 160 | ScoreCard.kt | ui/components/feedback/ | UI | Thẻ hiển thị điểm + sao kết quả | ScoreUtil |
| 161 | SkeletonLoader.kt | ui/components/feedback/ | UI | Skeleton shimmer (shimmerEffect() Modifier) | - |
| 162 | CodeInputField.kt | ui/components/forms/ | UI | Ô nhập mã chia sẻ | - |
| 163 | DropdownSelector.kt | ui/components/forms/ | UI | Dropdown chọn giá trị | - |
| 164 | SwitchToggle.kt | ui/components/forms/ | UI | Toggle bật/tắt | - |
| 165 | TextInputField.kt | ui/components/forms/ | UI | Ô nhập text có validation | - |
| 166 | QuizSearchBar.kt | ui/components/forms/ | UI | Thanh tìm kiếm quiz | - |
| 167 | AppTopBar.kt | ui/components/navigation/ | UI | Top action bar ứng dụng | - |
| 168 | BottomNavBar.kt | ui/components/navigation/ | UI | Bottom navigation bar (HOME/SEARCH/PROFILE) | Routes |
| 169 | CreateQuizFAB.kt | ui/components/navigation/ | UI | FAB tạo quiz nhanh | - |
| 170 | ChoiceButton.kt | ui/components/quiz/ | UI | Nút chọn đáp án | - |
| 171 | DynamicChoiceList.kt | ui/components/quiz/ | UI | Danh sách đáp án động 2–10 items | ChoiceButton |
| 172 | QuizCard.kt | ui/components/quiz/ | UI | Card preview thông tin quiz | - |
| 173 | QuizProgressIndicator.kt | ui/components/quiz/ | UI | Thanh tiến trình làm bài | - |
| 174 | QuizThumbnail.kt | ui/components/quiz/ | UI | Thumbnail quiz qua Coil AsyncImage | Coil |
| 175 | TimerDisplay.kt | ui/components/quiz/ | UI | Hiển thị đồng hồ đếm ngược | - |

### 8.11 Tầng UI — Theme & Utility

| STT | Tên File | Đường dẫn (tương đối) | Tầng | Mục đích | Phụ thuộc chính |
|-----|----------|-----------------------|------|----------|-----------------|
| 176 | Color.kt | ui/theme/ | UI | Material color scheme light + dark | - |
| 177 | Type.kt | ui/theme/ | UI | Dual font: Playfair Display + Inter (Google Fonts) | Google Fonts |
| 178 | Shape.kt | ui/theme/ | UI | Shapes + FullShape (pill/capsule 50.dp) | - |
| 179 | Theme.kt | ui/theme/ | UI | QuizzezTheme; dynamicColor disabled by default | - |
| 180 | UiError.kt | ui/common/ | UI | Common error sealed class | - |
| 181 | QrCodeUtil.kt | ui/util/ | UI | Tạo mã QR từ share code | - |

### 8.12 Tests

| STT | Tên File | Đường dẫn (tương đối) | Tầng | Mục đích |
|-----|----------|-----------------------|------|----------|
| 182 | ExampleInstrumentedTest.kt | androidTest/ | Test | Instrumented test mẫu |
| 183 | FirebaseModelTest.kt | test/ | Test | Unit test Firebase models |
| 184 | FirestoreTest.kt | test/ | Test | Unit test Firestore |
| 185 | ChecksumUtilTest.kt | test/ | Test | Unit test ChecksumUtil |
| 186 | InputSanitizerTest.kt | test/ | Test | Unit test InputSanitizer |
| 187 | ScoreUtilTest.kt | test/ | Test | Unit test ScoreUtil |
| 188 | TagValidatorTest.kt | test/ | Test | Unit test TagValidator |

---

## 9. QUY TẮC NGHIỆP VỤ QUAN TRỌNG

### 9.1 Tổng hợp các Business Rule

Bảng sau tổng hợp 15 quy tắc nghiệp vụ cốt lõi chi phối toàn bộ hành vi của hệ thống Quizzez.

| Mã BR | Quy tắc | Mô tả chi tiết | File liên quan |
|-------|---------|----------------|----------------|
| BR-01 | Quiz lifecycle 3 trạng thái | Draft → Private → Public; `isDraft` và `isPublic` là 2 toggle hoàn toàn độc lập | Quiz.kt, CreateQuizViewModel.kt |
| BR-02 | Tính điểm exact-set-equality | Tập đáp án người dùng chọn phải khớp CHÍNH XÁC với tập đáp án đúng | ScoreCalculator.kt |
| BR-03 | Xếp hạng sao 5 mức | <20%=0★, <40%=1★, <60%=2★, <80%=3★, <90%=4★, ≥90%=5★ | ScoreUtil.kt |
| BR-04 | Mã chia sẻ 6 ký tự | Uppercase A–Z + 0–9; retry tối đa 10 lần để đảm bảo tính duy nhất | ShareCodeUtil.kt, ShareCodeRepositoryImpl.kt |
| BR-05 | SHA-256 checksum | Hash toàn bộ nội dung quiz sau mỗi lần sync để phát hiện lỗi dữ liệu | ChecksumUtil.kt, SyncManager.kt |
| BR-06 | Local-first sync | Ghi Room trước (syncStatus=PENDING), đồng bộ Firestore nền qua SyncManager | SyncManager.kt, QuizRepositoryImpl.kt |
| BR-07 | Thùng rác 30 ngày | Soft delete qua trường `deletedAt`; có thể khôi phục trong vòng 30 ngày | Quiz.kt, RecycleBinViewModel.kt |
| BR-08 | Chế độ khách | UUID tạm thời cho guest; link attempts với tài khoản sau khi đăng ký | AuthRepositoryImpl.kt, AttemptRepository.kt |
| BR-09 | Kho câu hỏi ẩn danh | Tùy chọn anonymize: `contributorId = null` khi đóng góp câu hỏi | PoolRepositoryImpl.kt, QuestionPoolItem.kt |
| BR-10 | Phân cấp quyền Admin | GUEST < USER < ADMIN (configurable permissions) < SUPERUSER (tất cả quyền) | UserRole.kt, AdminPermission.kt, User.kt |
| BR-11 | Số đáp án linh hoạt | 2–10 đáp án mỗi câu hỏi; bắt buộc tối thiểu 1 đáp án đúng | QuizValidator.kt, DynamicChoiceList.kt |
| BR-12 | Xáo trộn mỗi lần thi | Questions + choices được shuffle mỗi phiên làm bài; flag `isCorrect` được giữ nguyên | QuestionShuffler.kt |
| BR-13 | Tombstone deletion | Collection `quizDeletions` đánh dấu xóa; dọn tombstone cũ sau 90 ngày | QuizInvalidationManager.kt, BackendMaintenanceWorker.kt |
| BR-14 | Ảnh đại diện URL | Không upload lên Firebase Storage; chỉ lưu URL (user-pasted hoặc Wallhaven API) | EditProfileViewModel.kt |
| BR-15 | Tất cả text là tiếng Việt | Bắt buộc dùng `stringResource(R.string.*)`; tuyệt đối không hardcode string | Tất cả UI files |

---

## 10. KẾT LUẬN

### 10.1 Điểm mạnh kiến trúc

| Đặc điểm | Lợi ích |
|----------|---------|
| Clean Architecture 3 tầng nghiêm ngặt | Dễ test, maintain và mở rộng; không có cross-layer coupling |
| Local-first với cloud sync | App hoạt động offline tốt; đồng bộ minh bạch qua WorkManager |
| Manual DI (không Hilt/Dagger) | Không phụ thuộc framework DI; dễ đọc hiểu và debug |
| SHA-256 checksum integrity | Phát hiện lỗi dữ liệu sau sync mà không cần so sánh toàn bộ nội dung |
| Tombstone-based invalidation | Sync xóa hiệu quả; tránh re-fetch toàn bộ danh sách quiz |
| WorkManager background tasks | Hoạt động đáng tin cậy kể cả khi app đóng hoặc thiết bị khởi động lại |
| Generic domain utilities | QuizValidator, QuestionShuffler, ScoreCalculator hoàn toàn độc lập, tái sử dụng được |

### 10.2 Khả năng mở rộng

- **Thêm repository mới**: implement interface trong `domain/repository/` → thêm property vào `AppContainer` → implement `by lazy` trong `AppContainerImpl`
- **Thêm màn hình mới**: tạo `{Name}Screen.kt` + `{Name}ViewModel.kt` → thêm route vào `Routes.kt` → đăng ký composable trong `QuizzezNavHost`
- **Thêm Firebase service**: khai báo trong `AppContainer` → khởi tạo trong `FirebaseModule` → inject vào RemoteDataSource hoặc Repository tương ứng
- **Thêm loại câu hỏi**: mở rộng `Question` model → cập nhật `QuizValidator` → thêm UI component xử lý loại mới vào `ui/components/quiz/`

### 11 . TÀI LIỆU THAM KHẢO

1. Android Developers. (2026). *Guide to app architecture*. https://developer.android.com/jetpack/guide
2. Google. (2026). *WorkManager documentation*. https://developer.android.com/topics/workmanager
3. Firebase. (2026). *Firestore documentation*. https://firebase.google.com/docs/firestore
4. Google. (2026). *Material Design Components*. https://material.io/components
5. Google Fonts. (2026). *Playfair Display & Inter*. https://fonts.google.com/specimen/Playfair+Display, https://fonts.google.com/specimen/Inter
6. Android Developers. (2026). *Testing documentation*. https://developer.android.com/testing
7. Stack Overflow. (2026). *SHA-256 checksum in Kotlin*. https://stackoverflow.com/questions/7616471/generate-a-sha-256-checksum-in-java
8. Android Developers. (2026). *Room database documentation*. https://developer.android.com/kotlin/multiplatform/room
9. Google. (2026). *Firestore Security Rules*. https://firebase.google.com/docs/firestore/security/get-started
10. Google. (2026). *Dynamic color in Material You*. https://developer.android.com/guide/topics/ui/look-and-feel/dynamic-color
11. Google. (2026). *Coil image loading library*. https://coil-kt.github.io/coil/
12. Google. (2026). *Jetpack Compose documentation*. https://developer.android.com/jetpack/compose
13. Google. (2026). *Navigation component documentation*. https://developer.android.com/guide/navigation
14. Google. (2026). *SharedPreferences documentation*. https://developer.android.com/reference
15. Google. (2026). *Material Design theming*. https://material.io/design/color/the-color-system.html
16. Google. (2026). *WorkManager constraints*. https://developer.android.com/topic/libraries/architecture/workmanager#constraints
17. Google. (2026). *Firestore data modeling best practices*. https://firebase.google.com/docs/firestore/manage-data/structure-data
18. Google. (2026). *Room TypeConverters documentation*. https://developer.android.com/reference/androidx/room/TypeConverter
19. Google. (2026). *ViewModel documentation*. https://developer.android.com/topic/libraries/architecture/viewmodel
20. Google. (2026). *LiveData documentation*. https://developer.android.com/topic/libraries/architecture/livedata
21. Google. (2026). *StateFlow documentation*. https://developer.android.com/kotlin/flow/stateflow-and-sharedflow
22. Google. (2026). *WorkManager periodic tasks*. https://developer.android.com/topic/libraries/architecture/workmanager#periodic
23. Google. (2026). *Firestore pagination*. https://firebase.google.com/docs/firestore/query-data/query-cursors
24. Google. (2026). *Firestore transactions and batches*. https://firebase.google.com/docs/firestore/manage-data/transactions
25. Google. (2026). *Firestore offline persistence*. https://firebase.google.com/docs/firestore/manage-data/enable-offline
26. Google. (2026). *Firestore data validation*. https://firebase.google.com/docs/firestore/security/rules-structure
27. Google. (2026). *Material Design components for Compose*. https://developer.android.com/jetpack/compose/material
28. Google. (2026). *Coil documentation*. https://coil-kt.github.io/coil/
29. Google. (2026). *Jetpack Compose theming*. https://developer.android.com/jetpack/compose/themes
30. Google. (2026). *Android app architecture patterns*. https://developer.android.com/jetpack/guide/architecture
And many more...

# 12. Tính năng trong tương lai
- Hỗ trợ quiz dạng flashcard (câu hỏi + đáp án hiển thị riêng biệt)
- Thêm timer giới hạn thời gian làm bài
- Thêm chế độ thi đấu trực tiếp (real-time multiplayer)
- Hỗ trợ upvote, downvote và bình luận quiz công khai
- Thêm tính năng bookmark quiz yêu thích
- Tích hợp AI vào developer console và giao diện người dùng
- Chuyển từ Full text search sang Sematic search cho hiệu suất tìm kiếm tốt hơn
- And more...
