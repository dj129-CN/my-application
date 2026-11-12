# 拾光笔记 · 桌面灵感生态（NoteFlow Widget Ecosystem）

一个 **Kotlin + AndroidX + MVVM** 的桌面小组件生态示例，对应课程 **C6「桌面小组件生态」+ P1「桌面小组件伴生模块」**。

主应用「拾光笔记」负责本地笔记创作与主屏微件；独立伴生应用「拾光贴士」拥有自己的启动入口和桌面微件。两者通过 **ContentProvider、URI 深链、签名广播** 协作，同时保持各自可独立安装、独立运行、独立卸载。

> 选题审核完整材料：[docs/TOPIC_REVIEW.md](docs/TOPIC_REVIEW.md)

## 工程组成

| 模块 | 应用名 | Debug 包名 | 职责 |
| --- | --- | --- | --- |
| `:app` | 拾光笔记 | `com.example.noteflow.debug` | 笔记 CRUD、Room、主微件、深链、动态主题 |
| `:companion` | 拾光贴士 | `com.example.noteflow.companion.debug` | 独立伴生入口、独立微件、主应用状态感知、联动刷新 |

统一版本契约：

- `compileSdk = 34`
- `minSdk = 26`
- `targetSdk = 34`
- JDK 17
- Gradle 8.7 / AGP 8.5.2 / Kotlin 1.9.24

## 功能概览

### 主应用（`:app`）

- 笔记新增、查看、编辑、删除、标题/正文搜索
- Room 持久化 + Flow 响应式列表
- MVVM：StateFlow 页面状态 + LiveData 一次性事件
- Navigation 单 Activity 多 Fragment 导航（待办 3 页 + 原有笔记 3 页）
- Material 3 日夜主题
- **必做：主屏微件动态刷新**（写入即更新 + WorkManager 周期兜底 + 系统周期更新）
- **必做：微件深度跳转联动**（列表、最近笔记详情、新建笔记、查看全部使用不同深链）
- **自选：动态主题切换**（跟随系统 / 浅色 / 深色，主应用与微件同步换色）
- 签名级 ContentProvider
- WorkManager 手动同步模拟任务

### 伴生模块（`:companion`）

- 独立 Launcher 与独立桌面微件
- 每日灵感贴：主应用未安装时仍可独立使用
- 连接主应用后展示最近笔记摘要
- 微件“快速记录”深链到主应用新建笔记页
- 微件“查看笔记”深链到主应用列表
- 主应用写入笔记后，经签名广播触发伴生微件刷新
- 主应用缺失、签名不一致或 Provider 不可用时自动降级，不崩溃
- 独立安装、独立构建、独立卸载；不依赖 `:app` 编译产物

### 待办数据层（`:app`）

- `TodoEntity`：id、标题、内容、创建时间、完成状态、优先级
- `TodoDao`：新增/批量新增、更新、状态更新、优先级更新、删除、按状态/优先级/时间/关键字查询、计数
- Room 数据库升级到 v2：新增 `todos` 表、三个索引，并提供 `MIGRATION_1_2` 保留原笔记数据
- `TodoRepository`：所有数据库访问在 `Dispatchers.IO` 中执行；一次性操作为 `suspend`，监听查询为 `Flow`
- `DataResult<T>`：统一封装成功/失败，包含校验失败、未找到、数据库异常，并处理空值
- 详细说明：[docs/TODO_DATA_LAYER.md](docs/TODO_DATA_LAYER.md)

### 待办业务闭环（`:app`，本阶段新增）

- 单 Activity + Navigation：列表、新增/编辑、设置三个核心页面
- 列表页展示 Room 中的真实待办，支持点击编辑、长按删除确认、复选框标记完成
- 新增/编辑通过 `TodoRepository` 写入 Room，保存后 Room Flow 自动刷新列表
- 空态、加载进度、操作成功 Toast、目标不存在与数据库异常提示
- 列表使用 `ListAdapter + DiffUtil + 稳定 id`，统一 180ms 增删改动画；完成状态采用乐观更新
- 保存中禁用表单防止重复提交；标题为空显示输入错误
- 设置页保留主题切换、小组件说明、微件刷新和原有笔记入口
- 详细说明：[docs/TODO_UI.md](docs/TODO_UI.md)

## 深度链接

主应用 Manifest 注册 `noteflow://open`，`MainActivity` 使用 `singleTop` 接收冷启动/热启动 Intent，再交给 `NavController.handleDeepLink()`。

| URI | 页面 |
| --- | --- |
| `noteflow://open/notes` | 笔记列表 |
| `noteflow://open/note/{noteId}` | 指定笔记详情 |
| `noteflow://open/editor?noteId=-1` | 新建笔记 |

外部验证（Debug 包）：

```powershell
adb shell am start -a android.intent.action.VIEW `
  -d "noteflow://open/editor?noteId=-1" `
  com.example.noteflow.debug
```

## 桌面微件

### 主微件

- 显示笔记总数、最近更新笔记、更新时间
- 点击根区域/查看全部 → 笔记列表
- 点击最近笔记 → 指定详情
- 点击快速记录 → 新建笔记
- 切换 App 主题后微件使用相应浅色/深色色板
- 系统 `updatePeriodMillis` 30 分钟；WorkManager 15 分钟兜底；每次写入即时刷新

添加方式：长按桌面 → 小组件 → 拾光笔记 · 微件。

### 伴生微件

- 显示每日灵感或主应用最近笔记
- 未连接主应用：独立模式
- 已连接主应用：数据自动联动，快速记录直达主应用编辑器
- 主应用写入后通过受签名权限保护的广播触发刷新

添加方式：安装伴生 APK 后，长按桌面 → 小组件 → 拾光贴士 · 灵感微件。

> 跨应用联动要求两个 APK 使用同一签名证书。Debug 构建均自动使用调试证书；不同签名时伴生模块会正确进入独立模式。

## 快速开始

### Android Studio

1. `File → Open` 打开项目根目录。
2. 等待 Gradle Sync，安装 SDK Platform 34 与 Build-Tools 34.0.0。
3. 先运行 `app`，再运行 `companion`。两个应用可各自独立安装。

### 命令行

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"

# 构建两个 APK
.\gradlew.bat :app:assembleDebug :companion:assembleDebug

# 安装主应用
.\gradlew.bat :app:installDebug

# 安装伴生模块
.\gradlew.bat :companion:installDebug
```

产物：

- `app/build/outputs/apk/debug/app-debug.apk`
- `companion/build/outputs/apk/debug/companion-debug.apk`

`local.properties` 是机器本地配置（已 gitignore）；Android Studio 会自动生成，命令行构建也可手动指定 `sdk.dir`。

Gradle Wrapper 默认使用腾讯镜像下载官方 Gradle 8.7，并用官方 SHA-256 校验。切换官方地址时只改 `distributionUrl`。

## 架构

```text
┌────────────────────────── :app ──────────────────────────┐
│ Fragment ──> ViewModel ──> NoteRepository ──> Room DAO  │
│    ▲              │                              │       │
│    └─ StateFlow ──┘                              │       │
│                                                   ▼       │
│                                        NoteWidgetUpdater │
│                                         ├─ 主屏微件       │
│                                         ├─ 深链 PendingIntent
│                                         └─ CompanionBridge │
│                                                           │
│ ContentProvider（signature 权限）◄────── 伴生模块查询       │
└───────────────────────────────────────────────────────────┘
                                  │
                     签名刷新广播  │  ContentResolver
                                  ▼
┌──────────────────────── :companion ───────────────────────┐
│ CompanionActivity                                        │
│ CompanionWidgetProvider / CompanionWidgetUpdater         │
│ CompanionProviderClient ──> 主应用 Provider               │
│ 启动页 + 独立微件 + 本地每日灵感 + 失败降级                │
└──────────────────────────────────────────────────────────┘
```

关键设计：

- 主应用数据写入统一走 `NoteRepository`，写入回调刷新主微件、通知 ContentResolver、向伴生模块发送刷新广播。
- `NoteWidgetUpdater` 在后台构建 RemoteViews，按用户主题选择浅/深色板，绑定 4 个独立点击区域。
- `MainActivity` 统一处理 `noteflow://` 深链，`nav_graph.xml` 将 URI 映射到列表/详情/编辑页。
- 伴生模块没有 `:app` 编译依赖，仅在运行时通过公开契约协作；独立模式是一等路径，而不是异常兜底。
- Provider 与刷新广播都使用签名级权限，避免跨应用暴露笔记数据。

## 项目结构

```text
.
├── app/                                # 主应用模块
│   ├── build.gradle.kts
│   ├── schemas/                        # Room schema 导出（v1/v2）
│   └── src/main/
│       ├── AndroidManifest.xml         # 主应用、Provider、微件、深链、权限
│       ├── java/com/example/noteflow/
│       │   ├── NoteFlowApp.kt
│       │   ├── AppContainer.kt
│       │   ├── data/local/             # Room、Note/Todo DAO、实体、类型转换、同步偏好
│       │   ├── data/work/              # WorkManager 调度与 Worker
│       │   ├── data/model/             # TodoPriority 等领域模型
│       │   ├── repository/             # NoteRepository、TodoRepository
│       │   ├── provider/               # ContentProvider + Contract
│       │   ├── widget/                 # 主微件、主题化 RemoteViews、伴生广播桥
│       │   ├── util/                   # 日期、Event、深链、主题偏好、DataResult
│       │   └── ui/                     # 待办三页、笔记三页与 ViewModel
│       └── res/                        # 布局、导航、主题、微件、图标
├── companion/                          # 独立伴生应用模块
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml         # 独立 Launcher、微件、刷新 Receiver
│       ├── java/com/example/noteflow/companion/
│       │   ├── CompanionActivity.kt
│       │   ├── data/CompanionProviderClient.kt
│       │   └── widget/                 # 独立微件、刷新接收器
│       └── res/                        # 独立主题、布局、微件、图标
├── docs/
│   ├── TOPIC_REVIEW.md                 # C6 + P1 选题审核材料
│   ├── TODO_DATA_LAYER.md              # 待办数据层说明
│   └── TODO_UI.md                      # 待办页面与业务闭环说明
├── gradle/libs.versions.toml           # 统一版本目录
├── gradle/wrapper/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── .editorconfig
└── README.md
```

## 主题与代码风格

- 主题入口：主应用 `Theme.NoteFlow`，伴生模块 `Theme.NoteFlowCompanion`，父主题均为 `Theme.Material3.DayNight.NoActionBar`。
- 颜色：`values/colors.xml` + `values-night/colors.xml`；主微件额外使用显式浅/深色板，确保跟随 App 主题。
- 工具：`gradle/libs.versions.toml` 集中管理 SDK、依赖和插件版本；KSP 处理 Room。
- 格式：`.editorconfig` 统一 UTF-8、LF、4 空格、120 列。
- 分层：UI 不直接访问 SQLite；Room 写入使用协程；Fragment 使用 `viewLifecycleOwner` 收集状态。
- 命名：包名小写；Kotlin 类 UpperCamelCase；资源和导航参数 snake_case/lowerCamelCase；用户文案集中在 strings.xml。

## 构建验证

本项目已在 JDK 17、Android SDK 34 / Build-Tools 34.0.0 上验证：

```text
:app:assembleDebug            BUILD SUCCESSFUL
:companion:assembleDebug      BUILD SUCCESSFUL
:app:testDebugUnitTest        6 tests, 0 failures
:app:lintDebug                No issues found
:companion:lintDebug          No issues found
```

Room schema 当前为 v2，`todos` 表和 `notes` 表会随 `app/schemas/` 一起提交。

两个 Debug APK 使用同一调试证书签名，满足签名级 Provider 与广播联动的安装前提。

APK 元数据：

```text
app:       compileSdk=34, minSdk=26, targetSdk=34
companion: compileSdk=34, minSdk=26, targetSdk=34
```

## 常见问题

**两个 APK 装好后伴生模块显示“独立运行中”？**  
检查是否使用同一签名构建/安装，且 Debug 主应用包名为 `com.example.noteflow.debug`；跨应用数据受签名权限保护。

**点击伴生微件没有进入主应用？**  
先确认主应用已安装并与伴生模块同签名。主应用缺失时，伴生模块会降级打开自己的说明页。

**微件刷新频率为何不是秒级？**  
系统 `updatePeriodMillis` 最短有效间隔约 30 分钟；应用内数据变更后的刷新是即时的。WorkManager 仅作为 15 分钟周期兜底。

**Release 可以直接发布吗？**  
工程未配置生产签名。正式发布前需为两个模块配置同一 release keystore，否则跨应用整合会降级。
