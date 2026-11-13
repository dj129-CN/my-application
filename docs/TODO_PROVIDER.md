# Todo ContentProvider 说明

主应用新增 `TodoContentProvider`，通过 Binder/ContentResolver 暴露待办 CRUD。Provider 与 UI 共享同一个 Room 单例，因此外部进程写入后，应用内 Room Flow 会立即刷新。

## 1. Authority 与 URI

Authority 由 applicationId 派生，Debug 包为 `com.example.noteflow.debug.todo`。

| 用途 | URI |
| --- | --- |
| 集合查询 / 新增 / 全量删除 | `content://<authority>/todos` |
| 按完成状态查询 | `content://<authority>/todos?completed=true`（或 `false`） |
| 单条查询 / 更新 / 删除 | `content://<authority>/todos/<id>` |

公开常量集中在 `app/src/main/java/com/example/noteflow/provider/TodoContract.kt`：

```kotlin
TodoContract.CONTENT_URI
TodoContract.contentUriFor(id)
TodoContract.contentUriForCompleted(true)
```

## 2. 列与类型

| 列 | Cursor 类型 | 说明 |
| --- | --- | --- |
| `_id` | Long | 主键 |
| `title` | String | 必填，插入/更新时不能为空 |
| `content` | String | 备注 |
| `created_at` | Long | Unix 毫秒；只在插入时可指定，更新保持原值 |
| `is_completed` | Int | 0/1 |
| `priority` | String | `LOW` / `MEDIUM` / `HIGH` |

插入时 `priority` 缺省为 `MEDIUM`，`is_completed` 缺省为 0，`created_at` 缺省为当前时间。

## 3. 操作语义

- `query`：
  - 集合支持 `completed=true|false` 过滤，固定按创建时间倒序。
  - 单条 URI 返回 0 或 1 行。
  - 支持 projection；未知列抛 `IllegalArgumentException`。
- `insert`：仅集合 URI，返回 `content://.../todos/<id>`。
- `update`：仅单条 URI，支持部分列更新：`title`、`content`、`is_completed`、`priority`；返回影响行数。
- `delete`：集合 URI 清空全部，单条 URI 删除指定项，返回影响行数。
- `selection` / `sortOrder` 不在公开契约中，Provider 使用固定的安全查询面。

## 4. 签名权限

Manifest 声明：

```xml
<permission
    android:name="${applicationId}.permission.ACCESS_TODOS"
    android:protectionLevel="signature" />

<provider
    android:name=".provider.TodoContentProvider"
    android:authorities="${applicationId}.todo"
    android:exported="true"
    android:readPermission="${applicationId}.permission.ACCESS_TODOS"
    android:writePermission="${applicationId}.permission.ACCESS_TODOS" />
```

同签名客户端需要在自己的 Manifest 声明：

```xml
<uses-permission android:name="com.example.noteflow.debug.permission.ACCESS_TODOS" />
```

未持有该签名权限的第三方进程无法读取或写入待办数据。

## 5. 数据变更通知

每次成功的插入、更新、删除都会调用：

```kotlin
contentResolver.notifyChange(TodoContract.CONTENT_URI, null)
contentResolver.notifyChange(itemUri, null)
```

因此外部客户端注册在集合 URI（`notifyForDescendants=true`）或具体 item URI 上的 `ContentObserver` 都会收到变更。查询返回的 Cursor 也设置了集合通知 URI。

## 6. 跨进程测试

测试位置：

```text
app/src/androidTest/java/com/example/noteflow/provider/TodoContentProviderTest.kt
```

测试辅助组件：

```text
app/src/debug/java/com/example/noteflow/provider/TodoProviderProbeService.kt
```

- Probe 服务在 `:todoProbe` 进程运行。
- `NoteFlowApp` 只在主进程初始化 WorkManager 和微件逻辑，避免测试进程继承 Application 时触发 WorkManager 初始化异常。
- 测试进程通过 Messenger 绑定 Probe，由 Probe 执行 ContentResolver 的 insert/query/update/delete。
- Probe 回传自己的 `Process.myPid()`；测试断言它与测试进程 PID 不同，证明数据读写跨进程。
- 测试覆盖：
  - 跨进程新增、查询、更新、删除
  - `completed=true` 过滤
  - `notifyChange` 被外部 ContentObserver 收到
  - Provider 导出、读写权限、`signature` protectionLevel

运行：

```bash
./gradlew :app:connectedDebugAndroidTest
```

或直接安装两个 APK 后执行：

```bash
adb shell am instrument -w -r \
  com.example.noteflow.debug.test/androidx.test.runner.AndroidJUnitRunner
```

当前验证结果：`2 tests, 0 failures`，测试进程与 `:todoProbe` 进程 PID 不同。
