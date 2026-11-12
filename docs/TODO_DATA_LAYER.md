# Todo 数据层说明

本文档对应当前 Android 工程中的待办数据层实现。该层复用现有 Room 数据库、KSP、协程和手动依赖容器，不包含待办 UI。

## 1. 实体与表结构

`TodoEntity` 位于 `app/src/main/java/com/example/noteflow/data/local/TodoEntity.kt`。

| Kotlin 属性 | 列名 | 类型 | 说明 |
| --- | --- | --- | --- |
| `id` | `id` | INTEGER PK AUTOINCREMENT | 主键，新建时为 0 |
| `title` | `title` | TEXT NOT NULL | 标题，Repository 会去除首尾空白并拒绝空标题 |
| `content` | `content` | TEXT NOT NULL | 内容 |
| `createdAt` | `created_at` | INTEGER NOT NULL | 创建时间，Unix 毫秒 |
| `isCompleted` | `is_completed` | INTEGER NOT NULL | 完成状态，0/1 |
| `priority` | `priority` | INTEGER NOT NULL | `TodoPriority`，LOW=1 / MEDIUM=2 / HIGH=3 |

索引：

- `index_todos_created_at`
- `index_todos_is_completed`
- `index_todos_priority`

优先级通过 `TodoConverters` 持久化为整数，避免数据库保存枚举名字符串，同时保持 DAO 接口使用类型安全的 `TodoPriority`。

## 2. DAO 全量方法

`TodoDao` 位于 `app/src/main/java/com/example/noteflow/data/local/TodoDao.kt`。

### 写入

- `insert(todo)` / `insertAll(todos)`
- `update(todo)`
- `updateCompletion(id, completed)`
- `updatePriority(id, priority)`
- `delete(todo)`
- `deleteById(id)`
- `deleteCompleted()`
- `deleteAll()`

### 响应式查询（Room Flow）

- `observeAllNewestFirst()` / `observeAllOldestFirst()`：按创建时间排序
- `observeByCompletion(completed)`：按完成状态查询
- `observeByPriority(priority)`：按优先级查询
- `observeById(id)`：单条监听，可能为 `null`
- `searchByKeyword(keyword)`：标题/内容模糊搜索

### 一次性挂起查询

- `queryAllNewestFirst()` / `queryAllOldestFirst()`
- `queryByCompletion(completed)`
- `queryByPriority(priority)`
- `queryById(id)`
- `queryByKeyword(keyword)`
- `count()` / `countCompleted()`

所有 DAO 读写方法都是 `suspend` 或返回 `Flow`，SQLite 访问不会阻塞主线程。

## 3. Repository 与协程

`TodoRepository` 位于 `app/src/main/java/com/example/noteflow/repository/TodoRepository.kt`。

- 所有一次性数据库调用在注入的 `ioDispatcher`（默认 `Dispatchers.IO`）中执行。
- 所有 Flow 查询通过 `flowOn(ioDispatcher)` 切换执行上下文。
- 写操作先做输入校验，再调用 DAO。
- `CancellationException` 会继续向上抛出，不会被结果封装吞掉。
- 单条查询找不到时返回 `NOT_FOUND`；列表为空时返回成功的空列表。
- `observeById` 保留可空发射，因为删除/重新创建过程中单条 Flow 的 `null` 是正常状态；一次性 `getById` 则返回 `NOT_FOUND`。
- Repository 已接入 `AppContainer.todoRepository`，并由 `TodoListViewModel` / `TodoEditViewModel` 使用。

## 4. 统一结果封装

`DataResult` 位于 `app/src/main/java/com/example/noteflow/util/DataResult.kt`。

```kotlin
sealed interface DataResult<out T> {
    data class Success<out T>(val data: T) : DataResult<T>
    data class Failure(
        val type: DataError,
        val message: String,
        val cause: Throwable? = null,
    ) : DataResult<Nothing>
}
```

错误分类：

| `DataError` | 场景 |
| --- | --- |
| `VALIDATION` | 空标题、非法 id、新建数据携带 id |
| `NOT_FOUND` | 更新/删除/查询的目标不存在 |
| `DATABASE` | SQLite/Room 异常 |
| `UNKNOWN` | 预留扩展 |

便捷方法：`fold`、`getOrNull`、`getOrDefault`、`isSuccess`、`isFailure`。

## 5. 数据库升级

数据库版本从 **1 → 2**。

- 新增 `todos` 表及三个索引。
- 通过 `MIGRATION_1_2` 保留已有的 `notes` 数据。
- `AppDatabase` 已注册 `addMigrations(MIGRATION_1_2)`。
- 导出的 Room schema 位于 `app/schemas/`，应随项目一起提交。

## 6. 调用示例

```kotlin
val repository = appContainer.todoRepository

val insertResult = repository.insert(
    TodoEntity(
        title = "完成数据层作业",
        content = "实体、DAO、Room、Repository、DataResult",
        priority = TodoPriority.HIGH,
    ),
)

when (insertResult) {
    is DataResult.Success -> {
        val saved: TodoEntity = insertResult.data
    }
    is DataResult.Failure -> {
        // insertResult.type / message / cause
    }
}
```

响应式列表：

```kotlin
repository.observeByCompletion(completed = false)
    .collect { result ->
        when (result) {
            is DataResult.Success -> render(result.data)
            is DataResult.Failure -> renderError(result.message)
        }
    }
```
