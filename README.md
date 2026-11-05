# NoteFlow · 拾光笔记

一个可直接编译运行的 Android 示例工程，按 **Kotlin + AndroidX + MVVM** 组织，完整演示本地数据库、导航、Material 3 主题、协程、WorkManager、跨进程 `ContentProvider` 和桌面小组件。

- 应用名：**拾光笔记**
- Kotlin 包名：`com.example.noteflow`
- 编译版本：`compileSdk = 34`
- 最低版本：`minSdk = 26`
- 目标版本：`targetSdk = 34`
- 调试包名：`com.example.noteflow.debug`（便于与正式包并存）

## 功能

- 笔记的本地新增、查看、编辑、删除
- Room 持久化 + `Flow` 实时刷新
- 标题/正文搜索（ViewModel 内过滤）
- Navigation 单 Activity 三页面导航
- Material 3 日夜主题、统一的色彩/排版/间距资源
- 手动“同步”任务（WorkManager 一次性任务，带进度反馈）
- 周期性小组件刷新（WorkManager 每 15 分钟）
- 桌面微件展示笔记总数与最近更新笔记，写入后即时刷新
- 带签名权限保护的 ContentProvider，可作为跨进程数据入口
- ViewModel：StateFlow 承载页面状态、LiveData 承载一次性事件

## 技术栈

| 组件 | 版本 | 用途 |
| --- | --- | --- |
| Gradle | 8.7 | 构建工具 |
| Android Gradle Plugin | 8.5.2 | Android 构建 |
| Kotlin | 1.9.24 | 开发语言 |
| KSP | 1.9.24-1.0.20 | Room 注解处理 |
| JDK | 17 | 编译工具链 |
| AndroidX Core KTX | 1.13.1 | Kotlin 扩展 |
| AppCompat | 1.7.0 | 基础 Activity 兼容 |
| Fragment KTX | 1.8.1 | Fragment / `viewModels` |
| Material Components | 1.12.0 | Material 3 组件与主题 |
| Navigation Fragment KTX | 2.7.7 | 页面导航 |
| Room | 2.6.1 | SQLite ORM（KSP） |
| Lifecycle | 2.8.3 | ViewModel / LiveData / 生命周期 |
| WorkManager | 2.9.0 | 后台任务 |
| Kotlin Coroutines | 1.8.1 | 异步与 Flow |

依赖版本统一集中在 `gradle/libs.versions.toml`，`compileSdk`、`minSdk`、`targetSdk`、Java 版本也由该文件提供。

## 快速开始

### 环境

- Android Studio Hedgehog（2023.1.1）或更新版本
- JDK 17。Android Studio 自带 JBR 即可；命令行构建需要设置 `JAVA_HOME`
- Android SDK：Platform 34 + Build-Tools 34.0.0
- 已安装的模拟器或 Android 8.0（API 26）及以上设备

### Android Studio

1. `File → Open` 选择本工程根目录。
2. 等待 Gradle Sync 完成；首次会下载依赖。
3. 选择 `app` 配置，点击 Run。

### 命令行（Windows PowerShell）

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"

# 构建 Debug APK
.\gradlew.bat :app:assembleDebug

# 安装到已连接设备
.\gradlew.bat :app:installDebug
```

macOS / Linux：

```bash
export JAVA_HOME=/path/to/jdk-17
./gradlew :app:assembleDebug
```

产物：`app/build/outputs/apk/debug/app-debug.apk`。

`local.properties` 是机器相关文件（已在 `.gitignore` 中），内容为：

```properties
sdk.dir=C:/Users/your-name/AppData/Local/Android/Sdk
```

Android Studio 会自动写入；命令行构建前也可以手动指向本机 SDK 目录。

### Gradle Wrapper 说明

`gradle-wrapper.properties` 默认使用腾讯镜像下载官方 Gradle 8.7 发行包，并通过 `distributionSha256Sum` 校验完整性。需要官方地址时改成：

```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.7-bin.zip
```

镜像文件与官方发行包的 SHA-256 相同，因此校验值不用变。

## 项目结构

```text
NoteFlow/
├── build.gradle.kts                       # 根构建脚本，插件统一声明
├── settings.gradle.kts                    # 仓库、模块、工程名
├── gradle.properties                      # AndroidX / 编译策略
├── gradle/
│   ├── libs.versions.toml                 # 版本目录：SDK、依赖、插件
│   └── wrapper/
├── gradlew / gradlew.bat
├── README.md
├── .editorconfig                          # 统一代码风格
└── app/
    ├── build.gradle.kts                   # 模块配置、依赖、KSP schema
    ├── proguard-rules.pro
    ├── schemas/                           # Room 导出的数据库 schema
    │   └── com.example.noteflow.data.local.AppDatabase/1.json
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── java/com/example/noteflow/
        │   │   ├── NoteFlowApp.kt          # Application，调度周期任务
        │   │   ├── AppContainer.kt         # 手动依赖容器
        │   │   ├── data/
        │   │   │   ├── local/
        │   │   │   │   ├── NoteEntity.kt
        │   │   │   │   ├── NoteDao.kt
        │   │   │   │   ├── AppDatabase.kt
        │   │   │   │   └── SyncPreferences.kt
        │   │   │   └── work/
        │   │   │       ├── NoteSyncScheduler.kt
        │   │   │       ├── NoteSyncWorker.kt
        │   │   │       └── WidgetRefreshWorker.kt
        │   │   ├── repository/
        │   │   │   └── NoteRepository.kt
        │   │   ├── provider/
        │   │   │   ├── NoteContract.kt
        │   │   │   └── NoteContentProvider.kt
        │   │   ├── widget/
        │   │   │   ├── NoteWidgetProvider.kt
        │   │   │   └── NoteWidgetUpdater.kt
        │   │   ├── util/
        │   │   │   ├── DateFormats.kt
        │   │   │   └── Event.kt
        │   │   └── ui/
        │   │       ├── MainActivity.kt
        │   │       ├── common/FragmentExt.kt
        │   │       ├── notes/
        │   │       │   ├── NoteListFragment.kt
        │   │       │   ├── NoteListViewModel.kt
        │   │       │   └── NoteAdapter.kt
        │   │       ├── detail/
        │   │       │   ├── NoteDetailFragment.kt
        │   │       │   └── NoteDetailViewModel.kt
        │   │       └── edit/
        │   │           ├── NoteEditFragment.kt
        │   │           └── NoteEditViewModel.kt
        │   └── res/
        │       ├── layout/                 # 页面与列表项
        │       ├── navigation/nav_graph.xml
        │       ├── menu/
        │       ├── values/                 # 主题、颜色、字符串、尺寸
        │       ├── values-night/           # 深色主题覆盖
        │       ├── values-v27/             # API 27+ 导航栏图标颜色
        │       ├── xml/                    # 备份规则、微件配置
        │       ├── drawable/               # 图标与背景
        │       └── mipmap-*/               # 启动图标（含自适应/单色）
        └── test/java/com/example/noteflow/util/DateFormatsTest.kt
```

### 分层职责

| 包 | 职责 |
| --- | --- |
| `ui` | Activity、Fragment、Adapter、ViewModel、页面状态；只处理展示和用户交互 |
| `data` | Room 实体/DAO/数据库、同步偏好、WorkManager Worker 与调度器 |
| `repository` | 单一数据访问边界；组合 Room，触发桌面微件与 ContentResolver 副作用 |
| `widget` | 桌面微件 Provider 与 RemoteViews 渲染 |
| `provider` | 跨进程 Binder 数据接口（ContentProvider + Contract） |
| `util` | 通用日期格式化、一次性 LiveData 事件包装等 |

`NoteFlowApp` 和 `AppContainer` 位于包根目录，作为应用级装配层；没有额外引入 Hilt，依赖关系保持清晰、可替换。

## 架构与数据流

```text
                       ┌─────────────────────────────┐
                       │       MainActivity          │
                       │  NavHostFragment + 三页面   │
                       └──────────────┬──────────────┘
                                      │ UI 事件
                                      ▼
   Fragment ─────────────────────► ViewModel
      ▲                              │  │
      │ StateFlow / LiveData         │  │ suspend
      └──────────────────────────────┘  ▼
                                  NoteRepository
                                      │
                                      ▼
                                    NoteDao
                                      │
                                      ▼
                              Room / SQLite (noteflow.db)

写入成功回调：
NoteRepository.onNotesChanged
   ├── ContentResolver.notifyChange → 通知 Provider 观察者
   └── NoteWidgetUpdater.requestUpdate → 刷新桌面微件

后台：
WorkManager ──► NoteSyncWorker（一次性同步）──► SyncPreferences + Widget
            └─► WidgetRefreshWorker（每 15 分钟）──► NoteWidgetUpdater

跨进程：
其他进程/同签名应用 ──Binder──► NoteContentProvider ──► Room
```

### ViewModel 状态管理约定

- Room 的 `Flow<List<NoteEntity>>` → 搜索关键字 `MutableStateFlow` → `combine` → `NoteListUiState` → `stateIn(viewModelScope, WhileSubscribed(5s))`。
- Fragment 使用 `repeatOnLifecycle(STARTED)` 收集 StateFlow；离开 STARTED 后自动停止收集。
- Snackbar、导航等只应执行一次的事件使用 `LiveData<Event<T>>`，避免旋转屏幕后重放。
- 页面只保留 ViewBinding，不持有数据库或 ContentResolver 引用；写操作统一经 ViewModel → Repository。

### 跨进程 ContentProvider

`NoteContentProvider` 是一个运行在应用进程内、通过 Binder 暴露给其他进程的 `ContentProvider`。它使用签名级权限保护，不设置 `android:process` 单独进程。这样处理的原因：

- `ContentProvider` 的跨进程能力来自 Binder，不需要另起进程也能被其他进程访问。
- Room 的 InvalidationTracker 只在单进程内可靠；如果 Provider 被强制放在独立进程并直接写同一个数据库，主进程 UI 的 Room 观察可能收不到失效通知。
- 同进程 Provider 与 UI 共享一个 `AppDatabase` 单例，避免双连接写入和一致性风险。

Contract：

| 项目 | 值 |
| --- | --- |
| Authority（release） | `com.example.noteflow.provider` |
| Authority（debug） | `com.example.noteflow.debug.provider` |
| Uri | `content://<authority>/notes`；单条为 `/notes/{id}` |
| MIME（集合） | `vnd.android.cursor.dir/vnd.<authority>.note` |
| MIME（单条） | `vnd.android.cursor.item/vnd.<authority>.note` |
| 权限（release） | `com.example.noteflow.permission.ACCESS_NOTES` |
| 权限（debug） | `com.example.noteflow.debug.permission.ACCESS_NOTES` |
| 列 | `_id`、`title`、`content`、`created_at`、`updated_at` |

支持的操作：查询集合/单条、插入、更新、删除单条、删除全部。出于最小暴露面考虑，不支持 `selection` 和 `sortOrder` 参数，只接受 Uri 与列投影。

同签名客户端需要声明权限：

```xml
<uses-permission android:name="com.example.noteflow.permission.ACCESS_NOTES" />
```

然后通过系统 ContentResolver 跨进程访问：

```kotlin
val uri = Uri.parse("content://com.example.noteflow.provider/notes")
val cursor = contentResolver.query(
    uri,
    arrayOf("_id", "title", "updated_at"),
    null,
    null,
    null,
)
```

调试包使用带 `.debug` 的 authority 和权限名。由于权限级别为 `signature`，客户端必须使用与宿主相同的签名证书；`adb shell content` 运行在 shell UID 下，不具备该签名权限，不能直接用于查询。

### WorkManager

- **手动同步**：列表页工具栏“同步”调用 `NoteSyncScheduler.enqueueSync()`，以唯一任务名 `note_manual_sync` + `ExistingWorkPolicy.REPLACE` 入队。`NoteSyncWorker` 模拟远端同步，记录时间戳并刷新微件；Fragment 通过 `getWorkInfosForUniqueWorkLiveData()` 展示进度并在完成后提示。
- **周期刷新**：Application 启动时以唯一周期任务 `note_widget_refresh` 注册 `WidgetRefreshWorker`，周期 15 分钟，约束为“电量不低”，`ExistingPeriodicWorkPolicy.KEEP` 避免重复重置。每次笔记写入仍会通过 `NoteWidgetUpdater` 立即刷新微件。
- 模拟同步的 `delay(700)` 位于 `NoteSyncWorker`，接入真实 API 时替换为网络请求即可。

### 桌面微件

1. 安装并启动应用，至少创建一条笔记（也可以为空）。
2. 长按桌面空白处 → 小组件 → 找到“拾光笔记 · 微件”。
3. 拖到桌面。微件展示总数、最近更新笔记和更新时间；点击打开应用。
4. 新增/编辑/删除笔记或手动同步后，微件立即刷新；系统同时每 30 分钟调用一次 `onUpdate`。

### 主题、命名与代码风格

**主题基础**

- 主题入口：`Theme.NoteFlow`，父主题 `Theme.Material3.DayNight.NoActionBar`。
- 颜色角色集中在 `values/colors.xml`，深色值在 `values-night/colors.xml`。
- 字体样式：`TextAppearance.NoteFlow.*`。
- 尺寸：`dimens.xml` 的间距/圆角/按钮高度统一命名。
- API 27+ 的导航栏图标模式放在 `values-v27/themes.xml`，API 26 使用深色导航栏避免白色图标对比度不足。
- 所有页面使用主题色角色 `?attr/colorOnSurface` 等，不在业务代码里硬编码颜色。

**命名规范**

| 类型 | 规范 | 示例 |
| --- | --- | --- |
| Kotlin 包 | 小写、按职责分层 | `ui.notes`、`data.local` |
| 类 / 文件 | UpperCamelCase，文件名与类名一致 | `NoteListViewModel.kt` |
| 函数 / 属性 | lowerCamelCase | `saveNote`、`isLoading` |
| 常量 | UPPER_SNAKE_CASE | `ARG_NOTE_ID`、`NEW_NOTE_ID` |
| 布局 | `fragment_*` / `item_*` / `widget_*` | `fragment_note_edit.xml` |
| View ID | lowerCamelCase，语义化 | `fabNewNote`、`titleInput` |
| 资源 | snake_case，前缀分组 | `action_save`、`note_primary` |
| Navigation 参数 | lowerCamelCase | `noteId` |

**代码风格**

- Kotlin 官方风格；4 空格缩进、最大行宽 120、尾随逗号。
- `.editorconfig` 已统一编码、换行和缩进。
- 页面中不写 SQL/网络代码；数据库访问使用挂起函数或 `Flow`，禁止在主线程同步查询。
- Fragment 的协程统一使用 `viewLifecycleOwner.lifecycleScope` + `repeatOnLifecycle`。
- 所有面向用户的文案放入 `strings.xml`；颜色、尺寸、文本样式引用资源。

## 关键配置

### 1. 统一 SDK 与版本目录

`gradle/libs.versions.toml`（节选）：

```toml
[versions]
agp = "8.5.2"
kotlin = "1.9.24"
ksp = "1.9.24-1.0.20"
javaVersion = "17"

compileSdk = "34"
minSdk = "26"
targetSdk = "34"

room = "2.6.1"
navigation = "2.7.7"
work = "2.9.0"
material = "1.12.0"

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

### 2. 应用模块

`app/build.gradle.kts`（节选）：

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

val javaVersion = libs.versions.javaVersion.get()

android {
    namespace = "com.example.noteflow"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.example.noteflow"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(javaVersion)
        targetCompatibility = JavaVersion.toVersion(javaVersion)
    }

    kotlinOptions {
        jvmTarget = javaVersion
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
```

### 3. Manifest：Provider、微件、主题

```xml
<permission
    android:name="${applicationId}.permission.ACCESS_NOTES"
    android:protectionLevel="signature" />

<application
    android:name=".NoteFlowApp"
    android:allowBackup="true"
    android:dataExtractionRules="@xml/data_extraction_rules"
    android:fullBackupContent="@xml/backup_rules"
    android:icon="@mipmap/ic_launcher"
    android:label="@string/app_name"
    android:roundIcon="@mipmap/ic_launcher_round"
    android:supportsRtl="true"
    android:theme="@style/Theme.NoteFlow">

    <activity
        android:name=".ui.MainActivity"
        android:exported="true"
        android:windowSoftInputMode="adjustResize">
        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent-filter>
    </activity>

    <provider
        android:name=".provider.NoteContentProvider"
        android:authorities="${applicationId}.provider"
        android:exported="true"
        android:permission="${applicationId}.permission.ACCESS_NOTES" />

    <receiver
        android:name=".widget.NoteWidgetProvider"
        android:exported="false"
        android:label="@string/widget_name">
        <intent-filter>
            <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
        </intent-filter>
        <meta-data
            android:name="android.appwidget.provider"
            android:resource="@xml/note_widget_info" />
    </receiver>
</application>
```

### 4. Room

```kotlin
@Entity(
    tableName = "notes",
    indices = [Index(value = ["updated_at"])],
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0L,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "content") val content: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Database(entities = [NoteEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        fun getInstance(context: Context): AppDatabase = /* 双检锁单例 */ ...
    }
}
```

DAO 用 `Flow` 提供响应式查询，写入使用 `suspend`，避免主线程 I/O。导出的 schema 已提交到 `app/schemas/`，便于后续数据库迁移审查。

### 5. Navigation

```xml
<navigation
    android:id="@+id/nav_graph"
    app:startDestination="@id/noteListFragment">
    <fragment
        android:id="@+id/noteListFragment"
        android:name="com.example.noteflow.ui.notes.NoteListFragment">
        <action
            android:id="@+id/action_noteList_to_noteDetail"
            app:destination="@id/noteDetailFragment" />
        <action
            android:id="@+id/action_noteList_to_noteEdit"
            app:destination="@id/noteEditFragment" />
    </fragment>
    <fragment
        android:id="@+id/noteDetailFragment"
        android:name="com.example.noteflow.ui.detail.NoteDetailFragment">
        <argument android:name="noteId" app:argType="long" />
        <action
            android:id="@+id/action_noteDetail_to_noteEdit"
            app:destination="@id/noteEditFragment" />
    </fragment>
    <fragment
        android:id="@+id/noteEditFragment"
        android:name="com.example.noteflow.ui.edit.NoteEditFragment">
        <argument android:name="noteId" app:argType="long" />
    </fragment>
</navigation>
```

不引入 Safe Args 插件，页面跳转通过 `bundleOf(ARG_NOTE_ID to id)` 传递参数；新增/编辑共用一个页面，`noteId = -1L` 表示新建。

### 6. 主题基础

```xml
<style name="Theme.NoteFlow.Base" parent="Theme.Material3.DayNight.NoActionBar">
    <item name="colorPrimary">@color/note_primary</item>
    <item name="colorOnPrimary">@color/note_on_primary</item>
    <item name="colorSurface">@color/note_surface</item>
    <item name="colorOnSurface">@color/note_on_surface</item>
    <item name="android:windowBackground">@color/note_surface</item>
    <item name="android:windowLightStatusBar">@bool/light_system_bars</item>
</style>

<style name="Theme.NoteFlow" parent="Theme.NoteFlow.Base" />
```

深色模式通过 `values-night/colors.xml` 与 `values-night/bools.xml` 自动切换，无需在每个页面判断。

## 验证

本地使用 JDK 17、Android SDK 34 / Build-Tools 34.0.0 验证：

```text
> Task :app:assembleDebug
BUILD SUCCESSFUL

> Task :app:testDebugUnitTest
BUILD SUCCESSFUL

> Task :app:lintDebug
BUILD SUCCESSFUL
Lint: No issues found.
```

APK 元数据：

```text
package: name='com.example.noteflow.debug'
compileSdkVersion='34'
sdkVersion:'26'
targetSdkVersion='34'
```

Debug APK 已使用默认调试证书签名并通过 `apksigner verify`。Release 构建未配置发布签名，正式发布时请在 `signingConfigs` 中接入自己的 keystore。

## 常见问题

**Q：`local.properties` 里的 SDK 路径不存在？**
A：该文件是机器本地配置，已被版本控制忽略。用 Android Studio 打开工程或手动改成自己的 SDK 路径。

**Q：构建提示缺少 SDK 34 / Build-Tools 34.0.0？**
A：在 SDK Manager 中安装对应版本；工程已设置 `android.builder.sdkDownload=false`，不会在构建时自动改写 SDK。

**Q：是否必须 JDK 17？**
A：是。AGP 8.5 与当前 Kotlin 配置使用 Java 17 工具链。Android Studio 自带的 JBR 17+ 可以直接使用。

**Q：Debug 包和 Release 包的 Provider authority 为什么不同？**
A：Debug 配置了 `applicationIdSuffix = ".debug"`，因此 `${applicationId}` 会变。客户端需要按目标的 authority 与 permission 使用对应值。接入其他应用时请以最终签名的 applicationId 为准。

**Q：如何替换模拟同步？**
A：修改 `NoteSyncWorker.doWork()` 中的延时逻辑，发起网络请求并按结果返回 `Result.success()` / `Result.retry()` / `Result.failure()`；输入、输出数据通过 `WorkerParameters.inputData` 与 `setOutputData` 传递。

## License

本工程为示例脚手架，可自由用于学习与二次开发。
