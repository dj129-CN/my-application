# 待办页面与业务闭环说明

本阶段把待办 UI 从内存演示切换为真实 Room 数据，通过 `TodoRepository` 完成增删改查。页面仍由单 Activity + Navigation Component 管理。

## 1. 页面与路由

| 页面 | Fragment | ViewModel | 路由 / 参数 |
| --- | --- | --- | --- |
| 待办列表 | `TodoListFragment` | `TodoListViewModel` | 启动页 `todoListFragment` |
| 新增/编辑 | `TodoEditFragment` | `TodoEditViewModel` | `action_todoList_to_todoEdit`，参数 `todoId: Long` |
| 设置 | `TodoSettingsFragment` | 无 | `action_todoList_to_todoSettings` |

- 新建待办传 `todoId = -1`。
- 点击列表项传真实 id，编辑页从仓库读取详情。
- 设置页可返回原有笔记列表。

## 2. 数据流

```text
TodoListFragment
  -> TodoListViewModel
  -> TodoRepository.observeTodosNewestFirst()
  -> TodoDao / Room Flow

TodoEditFragment
  -> TodoEditViewModel
  -> TodoRepository.getById / insert / update
  -> TodoDao / Room
```

- Room 是唯一数据源，列表不维护本地副本。
- 新增、编辑、完成状态、删除成功后，Room Flow 会自动发射新列表，列表页无需手工刷新。
- Repository 的 `DataResult` 把校验错误、未找到和数据库异常转换为显式结果。

## 3. 列表页行为

- 展示 `TodoEntity`：标题、备注、完成状态。
- 点击列表项进入编辑页，并传递 `todoId`。
- 长按列表项弹出 Material 删除确认框，确认后删除并显示 Toast。
- 点击复选框切换完成状态：
  - 先乐观更新界面，保证点击即时响应。
  - 写入失败时恢复原状态，并显示错误 Toast。
- 没有数据且加载完成时显示空态；初次加载显示顶部进度条。
- 使用 `ListAdapter` + `DiffUtil` + 稳定 id，删除/更新有列表项动画；`DefaultItemAnimator` 时长统一为 180ms。
- 长按触发系统触感反馈，卡片有 Material ripple。

## 4. 编辑页行为

- 新建或编辑由 `todoId` 区分。
- 编辑时通过 `TodoRepository.getById` 加载数据，加载完成后填充表单。
- 保存操作：
  - 新建：`TodoRepository.insert(TodoEntity(...))`。
  - 编辑：保留原优先级、完成状态和创建时间，仅更新标题/备注。
  - 成功：Toast「待办已保存」，返回列表，Room Flow 自动刷新。
  - 失败：Toast「操作失败，请稍后重试」，保留表单并重新启用按钮。
- 保存中禁用输入和按钮，顶部显示进度条，防止重复提交。
- 标题为空时在 `TextInputLayout` 显示错误。
- 取消或返回不写入数据。
- 编辑目标不存在或加载失败时提示并返回列表。

## 5. Toast 与异常

| 场景 | 反馈 |
| --- | --- |
| 保存成功 | `todo_saved` |
| 删除成功 | `todo_deleted` |
| 标记完成/恢复 | `todo_completed` / `todo_reopened` |
| 仓库操作失败 | `todo_operation_failed`，详细异常写入 Logcat |
| 目标不存在 | `todo_missing_item` |
| 初始加载 | 顶部 `LinearProgressIndicator` |

异常不会使应用崩溃；页面通过 `DataResult.Failure` 分支恢复 UI 状态。

## 6. 设置页

- 主题：跟随系统 / 浅色 / 深色，复用 `ThemePreferences` 和 `AppCompatDelegate`。
- 小组件说明：桌面添加步骤、刷新入口、主题同步说明。
- 待办数据目前保存在应用内 Room，不写入桌面微件。
- 保留进入原有笔记列表的入口。

## 7. 验证流程

1. 首次启动显示空态。
2. 新建待办并保存，列表出现新项。
3. 强制停止并重新启动，数据仍然存在。
4. 点击复选框，显示完成 Toast 和删除线。
5. 点击列表项进入编辑，修改后保存，列表自动更新。
6. 长按列表项，确认删除，列表项移除动画并回到空态。
7. 进入设置切换主题，再返回列表，数据和主题均保持。
