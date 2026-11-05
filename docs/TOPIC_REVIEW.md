# 选题审核材料｜拾光笔记 · 桌面灵感生态

> 对应课程：C6「桌面小组件生态」+ P1「桌面小组件伴生模块」  
> 主应用：**拾光笔记 NoteFlow**  
> 伴生模块：**拾光贴士 NoteFlow Companion**  
> 技术基线：Kotlin + AndroidX + MVVM + Room + Navigation + WorkManager  
> 审核版本：v1.1（2026-11-06）

## 1. 项目名称与一句话定位

**项目名称**

拾光笔记 · 桌面灵感生态（NoteFlow Widget Ecosystem）

**一句话定位**

以本地笔记主应用为数据源，把“看一眼最近的灵感”和“立刻记下一句话”延伸到桌面，并通过可独立安装的伴生微件应用形成一套轻量、可降级、可联动的桌面小组件生态。

**面向场景**

- 用户不想频繁打开 App，但希望桌面始终能看到最近的一条笔记；
- 灵感出现时，希望从桌面一步进入“新建笔记”而不是层层点击；
- 用户可能只安装伴生微件，用于接收每日灵感提示；安装主应用后自动升级为笔记联动模式；
- 强调本地优先与低打扰，不依赖账号体系或云端服务。

**视觉与内容差异化**

主应用采用鼠尾草绿与纸感浅色体系，伴生模块采用暖琥珀色；两个应用形成“写下来（绿）”与“提醒你写（琥珀）”的视觉分工。

## 2. 主 App 功能清单

主应用包名：`com.example.noteflow`（Debug：`com.example.noteflow.debug`）。

### 2.1 必做功能

| 功能 | 实现方式 | 用户可见效果 |
| --- | --- | --- |
| **桌面小组件动态刷新** | `NoteWidgetProvider` + `RemoteViews`；笔记写入后由 `NoteRepository` 统一回调 `NoteWidgetUpdater`，通过 `AppWidgetManager.updateAppWidget()` 刷新；系统 30 分钟周期更新，WorkManager 另有 15 分钟周期兜底 | 新增、编辑、删除或同步笔记后，桌面微件的数量、最近标题、正文预览和更新时间即时变化 |
| **小组件深度跳转联动** | `PendingIntent` 携带 `noteflow://` URI；`MainActivity` 以 `singleTop` 接收冷启动与热启动 Intent；`NavController.handleDeepLink()` 解析导航图深链 | 点击微件空白/标题区域直达最近笔记详情；点“快速记录”直达新建笔记编辑页；点“查看全部”回到笔记列表，而不是统一只打开首页 |
| **桌面微件快捷操作** | `RemoteViews.setOnClickPendingIntent` 为微件根布局、最新笔记区、快速记录和查看全部分别绑定不同的 `PendingIntent` | 微件既是信息展示卡，也是可操作的桌面入口 |
| **桌面微件主题联动** | 写入笔记时读取 `ThemePreferences` 的有效明暗模式，显式注入浅色/深色两套文字色和圆角背景 | 在 App 中切换主题后，主应用微件同步换装，不依赖桌面启动器的主题实现 |

**深链目标清单**

| 深链 | 目标页面 | 触发入口 |
| --- | --- | --- |
| `noteflow://open/notes` | 笔记列表 | 微件根区域、“查看全部” |
| `noteflow://open/note/{noteId}` | 指定笔记详情 | 微件“最近一条笔记”区域 |
| `noteflow://open/editor?noteId=-1` | 新建笔记编辑页 | 微件“快速记录” |

**深链处理链路**

```text
RemoteViews 点击
  -> PendingIntent(ACTION_VIEW, noteflow://...)
  -> MainActivity(singleTop / onNewIntent)
  -> NavController.handleDeepLink(intent)
  -> nav_graph.xml 中 <deepLink> 匹配
  -> 列表 / 详情 / 编辑页
```

### 2.2 自选功能：动态主题切换（已选）

| 项目 | 说明 |
| --- | --- |
| 可选模式 | 跟随系统、浅色、深色 |
| 实现方式 | `ThemePreferences` 持久化 + `AppCompatDelegate.setDefaultNightMode()`；Application 启动时恢复 |
| 桌面联动 | 主题切换后刷新微件；微件显式使用日间/夜间两套色板，保证与 App 选择一致 |
| 入口 | 笔记列表页工具栏“主题”按钮，单选对话框 |
| 价值 | 同时覆盖主应用页面、系统栏和桌面微件，避免“App 变色但微件不跟随”的割裂感 |

### 2.3 主应用完整功能基线

| 模块 | 功能 |
| --- | --- |
| 笔记 | 新增、查看、编辑、删除；标题/正文搜索；更新时间排序 |
| 数据 | Room（`noteflow.db`）、DAO、Repository；导出数据库 schema 便于迁移审查 |
| 架构 | 单 Activity + Navigation 三页面；MVVM；StateFlow 页面状态 + LiveData 一次性事件 |
| 后台 | WorkManager 手动同步模拟任务；周期刷新微件；同步状态进度提示 |
| 互操作 | 签名级 ContentProvider 提供跨进程笔记 CRUD；供伴生模块读取与外部同签名客户端使用 |
| 主题 | Material 3、浅/深色资源、统一间距/圆角/文本样式 |
| 同步 | 本地解析模拟同步，写入 `SyncPreferences` 并刷新微件；网络接口可替换 |

## 3. 伴生模块功能清单与独立性说明

伴生模块名称：**拾光贴士（NoteFlow Companion）**  
包名：`com.example.noteflow.companion`（Debug：`com.example.noteflow.companion.debug`）

### 3.1 功能清单

| 功能 | 说明 |
| --- | --- |
| 独立桌面微件 | 拥有自己的 `AppWidgetProvider`、`RemoteViews` 布局和 AppWidget 配置，可独立添加到桌面 |
| 每日灵感贴 | 主应用未连接时，按日期轮换本地灵感提示，保证微件本身仍有内容价值 |
| 主应用状态感知 | 启动页检测主应用安装状态、Provider 授权状态与最近笔记，展示“已连接/独立运行中” |
| 最近笔记摘要 | 连接主应用后读取最近一条笔记，在启动页和微件中展示标题与正文摘要 |
| 跨应用快速记录 | 微件“快速记录”通过深链直接打开主应用的新建笔记页；未连接时降级为打开伴生模块说明页 |
| 跨应用动态刷新 | 主应用笔记写入后，向伴生模块发送签名级刷新广播；伴生模块重新查询 Provider 并刷新自己的微件 |
| 独立启动入口 | 独立 Launcher Activity，可单独安装、启动、卸载，不依赖主应用先启动 |
| 降级与容错 | 主应用缺失、签名不一致、Provider 查询失败、跨应用跳转失败时均不崩溃；显示独立模式界面 |
| 手动刷新入口 | 启动页“刷新桌面组件”按钮，可立即重绘伴生微件 |

### 3.2 伴生模块独立性说明

**独立的部分**

- **独立 APK / 独立工程模块**：`companion` 是独立 Android Application 模块，拥有自己的 `applicationId`、Manifest、主题、图标、资源、Launcher 与 AppWidget。
- **无编译依赖**：`companion/build.gradle.kts` 不依赖 `:app`，不引用主应用类和 Room 实体。单独执行 `:companion:assembleDebug` 即可产出 APK。
- **独立运行生命周期**：即使从未安装主应用，伴生模块也能启动、添加桌面微件、显示每日灵感、打开自身页面。
- **独立卸载**：卸载任一应用不影响另一个应用启动；双方对“对方不存在”做了显式容错。
- **独立数据源**：没有主应用时使用本地日期轮换提示；不假设远端服务或账号存在。

**协作的部分（仅在主应用存在时启用）**

| 协作能力 | 约定 | 方向 |
| --- | --- | --- |
| 读取最近笔记 | 主应用导出 ContentProvider，authority 为 `<applicationId>.provider`；伴生模块按 Debug/Release 选择对应 authority | 伴生 -> 主应用 |
| 快速记录 | `noteflow://open/editor?noteId=-1` 深链 | 伴生 -> 主应用 |
| 查看笔记 | `noteflow://open/notes` / `noteflow://open/note/{id}` | 伴生 -> 主应用 |
| 数据变更刷新 | 主应用发送 `com.example.noteflow.companion.action.REFRESH_WIDGET`；伴生 RefreshReceiver 受同一签名权限保护 | 主应用 -> 伴生 |
| 安全前提 | 两个 APK 使用同一签名证书；Provider 权限为 `signature`；刷新广播接收器也受 `signature` 权限保护 | 构建/安装约束 |

**为什么不是“把主应用拆出一个库”**

课程要求的是“伴生模块”的独立性与桌面生态，而不是单纯的代码分层。本项目让伴生模块拥有独立安装包、独立桌面入口和独立降级路径，同时只通过公开的运行期契约与主应用协作，因此它既能作为主应用生态的一部分，也能单独立项演示。

**安装组合行为**

| 安装组合 | 主应用 | 伴生模块 |
| --- | --- | --- |
| 只有主应用 | 完整笔记功能 + 主应用微件 + 深链 | 不适用 |
| 只有伴生模块 | 不适用 | 独立启动 + 本地灵感微件；联动按钮给出安装提示 |
| 两者都安装且同签名 | 全部功能 + 跨应用刷新/深链 | 连接主应用；读取最近笔记、快速记录、自动刷新 |
| 两者都安装但签名不同 | 主应用正常 | 伴生模块识别为未连接，自动降级 |

## 4. 拟用技术点与强制技术点匹配对照表

> 说明：本表按 C6「桌面小组件生态」与 P1「桌面小组件伴生模块」的核心审核口径整理；如果任课教师给出额外的强制技术点清单，可在本表末尾逐项追加，不影响现有实现。

| 编号 | 强制技术点 / 能力点 | 本方案技术实现 | 关键落点 | 可验收行为 |
| --- | --- | --- | --- | --- |
| C6-1 | 桌面小组件基础 | `AppWidgetProvider` + `RemoteViews` + `AppWidgetProviderInfo` | `NoteWidgetProvider.kt`、`widget_note.xml`、`note_widget_info.xml` | 主屏可添加“拾光笔记”微件，展示数量与最近笔记 |
| C6-2 | 小组件动态刷新 | 数据写入统一回调 `NoteWidgetUpdater`，使用 `AppWidgetManager.updateAppWidget()`；`WorkManager` 周期刷新兜底 | `NoteRepository.kt`、`NoteWidgetUpdater.kt`、`WidgetRefreshWorker.kt` | 编辑/删除笔记后不重开桌面即可看到微件变化 |
| C6-3 | 深度跳转联动 | `PendingIntent` + `ACTION_VIEW` + 自定义 URI Scheme；`singleTop` + `onNewIntent`；`NavController.handleDeepLink()` | `MainActivity.kt`、`nav_graph.xml`、`AndroidManifest.xml` | 点微件不同区域分别进入列表、详情、新建页 |
| C6-4 | 桌面可交互 | 4 个独立 `PendingIntent` 点击区域；微件快速记录/查看全部 | `NoteWidgetUpdater.kt`、`widget_note.xml` | 微件不是静态展示，可直接完成记录入口跳转 |
| P1-1 | 伴生模块独立性 | 独立 `:companion` Application 模块、独立包名、Launcher、主题、微件和资源；不依赖 `:app` | `companion/`、`companion/build.gradle.kts`、`companion/AndroidManifest.xml` | `:companion:assembleDebug` 单独产出可安装 APK；主应用缺失时仍可启动 |
| P1-2 | 跨应用数据协作 | 签名级 ContentProvider + 跨应用 `ContentResolver` 查询；Debug/Release authority 映射 | `NoteContentProvider.kt`、`CompanionProviderClient.kt` | 两者同签名安装后，伴生模块展示主应用最近笔记 |
| P1-3 | 跨应用联动刷新 | 主应用发送受 `signature` 权限保护的显式包广播；伴生 `BroadcastReceiver` 触发自身微件刷新 | `CompanionBridge.kt`、`CompanionRefreshReceiver.kt`、两个 Manifest | 主应用新增笔记后，伴生微件内容随下次可见刷新 |
| P1-4 | 安全与降级 | Provider 与刷新广播均使用签名级权限；Provider 查询失败/主应用未安装时展示独立模式 | 两个 Manifest、`CompanionProviderClient.kt` | 不同签名或未安装主应用时不崩溃、不越权读取 |
| S-1 | 自选：动态主题切换 | SharedPreferences 保存主题模式；`AppCompatDelegate` 应用明暗模式；微件使用显式双色板 | `ThemePreferences.kt`、`NoteListFragment.kt`、`NoteWidgetUpdater.kt` | 切换深色/浅色后页面与微件同步改变 |
| F-1 | AndroidX + MVVM | Activity/Fragment -> ViewModel -> Repository -> DAO；StateFlow + LiveData | `ui/`、`repository/`、`data/local/` | 旋转屏幕、返回栈和状态恢复时页面不重复加载、不重放事件 |
| F-2 | 本地持久化 | Room + KSP、DAO Flow、schema 导出 | `AppDatabase.kt`、`NoteDao.kt`、`app/schemas/` | 重启应用笔记仍存在；Flow 自动刷新列表 |
| F-3 | 后台任务 | 一次性同步任务 + 周期微件刷新任务，唯一任务名去重 | `NoteSyncScheduler.kt`、`NoteSyncWorker.kt`、`WidgetRefreshWorker.kt` | 点击同步出现进度与完成提示；周期任务不重复注册 |
| F-4 | 工程规范 | 版本目录统一 SDK/依赖；统一 Material 3 主题；`values-night`；`.editorconfig`；Lint 0 error | `gradle/libs.versions.toml`、`values*/`、`.editorconfig` | 两模块可编译、单测通过、Lint 无错误 |

**强制点覆盖结论**

- 桌面小组件不是静态展示：同时具备 **动态刷新、桌面点击、深度跳转**。
- 伴生模块不是主应用中的 `widget` 包改名：它是 **独立安装、独立启动、独立微件、可降级** 的第二个应用。
- 两个应用之间不是数据库文件直读：通过 **ContentProvider / URI Deep Link / 签名广播** 三件套协作，边界清晰、可安全演示。

## 5. 个性化差异说明

### 5.1 与同题同学的区分点

| 常见同题实现 | 本项目做法 | 差异化价值 |
| --- | --- | --- |
| 只做一个桌面微件，点击统一打开首页 | 微件根区域、最新笔记、快速记录、查看全部分别绑定点位，深链直达列表/详情/新建页 | 展示“深度跳转联动”，不是简单的 Launcher 快捷方式 |
| 主应用和小组件强耦合在一个 APK 中 | 拆出可独立安装的 `:companion` APK，主应用不依赖它 | 对应 P1“伴生模块”的独立性审核点 |
| 伴生模块只是空壳或必须依赖主应用 | 无主应用时提供每日灵感本地模式；有主应用时自动升级为数据联动 | 同时具备独立性与生态联动，而不是伪模块 |
| 主题切换只改 App，微件保持原样 | 主题模式显式传递到 RemoteViews，微件切换浅/深两套色板 | 动态主题落到桌面端，视觉一致 |
| 直接开放 Provider 或使用明文广播 | Provider 与刷新广播均为 `signature` 权限；外部未同签名无法访问 | 演示跨进程协作时兼顾安全边界 |
| 通用待办/时钟/天气微件 | 定位为“灵感记录”的场景化微件：最近笔记 + 快速记录 + 每日提示 | 内容主题与交互路径更贴合“拾光”品牌与目标用户 |
| 只演示一次刷新 | 主应用写入 -> 立即刷新；WorkManager 15 分钟兜底；系统 30 分钟更新；伴生模块收到广播再刷新 | 形成主应用/伴生模块/系统三路刷新机制 |

### 5.2 本项目的记忆点

1. **一个生态，两个 App，三个通道**：主应用微件、伴生模块微件、ContentProvider 数据通道 + URI 深链 + 签名刷新广播。
2. **“看一眼”与“写一句”**：桌面微件既展示最近笔记，也把新建笔记缩到一次点击。
3. **有边界的联动**：主应用不需要知道伴生模块内部实现；伴生模块在缺少主应用时完整降级。
4. **主题是一等公民**：App、系统栏、桌面微件使用同一套明暗模式选择。
5. **可审核的工程结果**：两模块均可单独构建，Lint 通过，Room schema 导出，硬编码文案集中到资源。

### 5.3 范围边界与风险说明

- **不包含云同步/账号**：所有笔记保存在本机 Room；同步页的“同步”为可替换的 WorkManager 演示任务，不制造“已上云”的误导。
- **跨应用联动依赖同一签名**：这是 Android `signature` 权限的安全要求。Debug 构建自动使用同一调试证书；若手动安装不同签名的 APK，伴生模块会显示独立模式，这是预期行为。
- **微件宿主差异**：不同桌面启动器对微件尺寸、圆角和更新频率有差异；系统 `updatePeriodMillis` 的最短有效周期为 30 分钟，App 内写入后的刷新是即时触发。
- **无独立后端**：P1 考核重点为主/伴生协作与桌面生态，不引入服务端依赖，保证评审现场可离线演示。

## 附：评审演示路径

1. **构建两个 APK**

   ```powershell
   .\gradlew.bat :app:assembleDebug :companion:assembleDebug
   ```

2. **安装顺序**：先安装 `:app`，再安装 `:companion`；两者 Debug 包使用同一调试签名。
3. **主应用**：添加到主屏微件，写一条笔记，观察微件标题/数量/时间即时变化；切换“主题”为深色，观察 App 与微件同时切换色板。
4. **深度跳转**：分别点击微件的“快速记录”、最近笔记区域、“查看全部”，验证进入新建页、详情页、列表页。
5. **伴生模块**：打开“拾光贴士”，确认显示“已连接拾光笔记”；随后在主应用修改笔记，回到桌面观察伴生微件刷新。
6. **独立降级**：卸载主应用（或安装不同签名构建），重新打开“拾光贴士”，确认进入独立模式且微件仍显示每日灵感。
7. **外部深链验证（Debug）**

   ```powershell
   adb shell am start -a android.intent.action.VIEW `
     -d "noteflow://open/editor?noteId=-1" `
     com.example.noteflow.debug
   ```

> 本材料中的“强制技术点”按 C6/P1 的桌面生态与伴生模块能力整理；若课程另有编号化技术点清单，可直接把表中的 C6-*/P1-* 行替换为课程原始编号，实现与验收证据无需改动。
