# 待办 UI 空壳说明

本阶段在现有单 Activity 工程中新增待办三页，使用 Navigation Component 管理页面与参数传递。待办数据只保存在 Activity 级 ViewModel 的内存中，不读写 Room，也不调用 `TodoRepository`。

## 1. 页面与路由

`app/src/main/res/navigation/nav_graph.xml` 的启动页已改为待办列表。

| 页面 | Fragment | 路由 / 参数 |
| --- | --- | --- |
| 待办列表 | `TodoListFragment` | 启动页 `todoListFragment` |
| 新增/编辑 | `TodoEditFragment` | `action_todoList_to_todoEdit`，参数 `todoId: Long` |
| 设置 | `TodoSettingsFragment` | `action_todoList_to_todoSettings` |

- 新建待办传 `todoId = -1`。
- 点击列表项传真实的正数 id，编辑页据此预填标题和备注。
- 设置页可通过 `action_todoSettings_to_noteList` 回到原有笔记列表。

## 2. 待办列表页

布局：`fragment_todo_list.xml`

- `RecyclerView` 展示待办，`TodoAdapter` 使用 `ListAdapter + DiffUtil`。
- `ExtendedFloatingActionButton` 新建待办。
- 空列表时隐藏 RecyclerView，显示图标 + 标题 + 说明的空态布局。
- 点击卡片进入编辑页；复选框切换完成状态并显示删除线。
- Toolbar 副标题实时显示内存中的待办数量。

列表初始为空，保存第一条演示待办后即可看到 RecyclerView 列表。

## 3. 新增/编辑页

布局：`fragment_todo_edit.xml`

- `TextInputLayout` 标题必填，备注可选。
- 保存：校验标题，写入共享 ViewModel，返回列表。
- 取消：直接返回，不保存。
- 页面底部显示当前 `todoId`，用于确认参数传递；负数代表新建。
- 编辑已不存在的数据时提示并返回列表。

## 4. 设置页

布局：`fragment_todo_settings.xml`

- 外观主题：跟随系统 / 浅色 / 深色，复用 `ThemePreferences` 与 `AppCompatDelegate`。
- 小组件说明：桌面添加步骤、当前微件展示内容、主题同步说明。
- “刷新桌面组件”按钮复用 `NoteWidgetUpdater`。
- “打开笔记列表”保留上一阶段的笔记、微件和深链功能。

## 5. 数据与状态边界

- `TodoListViewModel` 由列表页和编辑页通过 `activityViewModels()` 共享。
- 数据只存在于内存；进程被杀或应用重启后清空，这是本阶段“空壳”的预期行为。
- 未连接 `TodoRepository`，未产生 Room 写入；下一阶段可将 ViewModel 的内部列表替换为 `TodoRepository.observe...` 的 `DataResult` 流。
- 主题设置会写入现有 SharedPreferences，并触发 Activity 重建；这是现有功能，不属于“待办数据”。

## 6. 验证路径

1. 启动应用，确认默认进入“待办清单”并显示空态。
2. 点击“新建待办”，输入标题和备注，保存。
3. 列表出现卡片；点击复选框切换完成态，点击卡片进入编辑并预填。
4. 修改标题保存，或点击取消返回。
5. 进入右上角设置，切换主题，确认界面和微件色板变化。
6. 在设置页打开笔记列表，确认原有笔记模块仍可访问。
