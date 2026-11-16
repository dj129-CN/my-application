# 4×1 待办桌面微件说明

本阶段新增一个 4×1 尺寸的 AppWidget，严格使用 RemoteViews 实现，展示今日待办总数、最近一条标题和刷新按钮。添加时通过配置 Activity 选择待办分类，配置立即生效。

## 1. 组件清单

| 文件 | 作用 |
| --- | --- |
| `widget/TodoWidgetProvider.kt` | AppWidgetProvider 入口，处理系统更新与刷新广播 |
| `widget/TodoWidgetUpdater.kt` | 共享渲染器：读 Room、按分类过滤、构建 RemoteViews |
| `widget/TodoWidgetPrefs.kt` | 每个微件实例的分类偏好（SharedPreferences） |
| `widget/TodoWidgetConfigActivity.kt` | 配置界面，选择分类后保存并触发首次更新 |
| `res/layout/todo_widget.xml` | 4×1 微件布局 |
| `res/layout/activity_todo_widget_config.xml` | 配置 Activity 布局 |
| `res/xml/todo_widget_info.xml` | 微件元数据，声明 configure、尺寸、预览图 |
| `res/drawable/ic_todo_widget_refresh_light.xml` | 浅色主题刷新图标 |
| `res/drawable/ic_todo_widget_refresh_dark.xml` | 深色主题刷新图标 |

Manifest 注册：

```xml
<receiver android:name=".widget.TodoWidgetProvider" ...>
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
    </intent-filter>
    <meta-data android:name="android.appwidget.provider"
               android:resource="@xml/todo_widget_info" />
</receiver>

<activity android:name=".widget.TodoWidgetConfigActivity" ...>
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_CONFIGURE" />
    </intent-filter>
</activity>
```

## 2. 尺寸与适配

- `minWidth="294dp"` / `minHeight="72dp"` 对应 4×1 单元格
- `targetCellWidth="4"` / `targetCellHeight="1"` 用于 API 31+ 启动器精确占位
- `resizeMode="horizontal"` 允许横向拉伸
- `previewImage` 使用待办空态图标作为预览
- 布局使用水平 LinearLayout：左侧计数区、中间标题区（weight=1）、右侧刷新按钮

## 3. 配置流程

1. 用户长按桌面 → 小组件 → 拖拽「拾光待办 · 微件」
2. 系统启动 `TodoWidgetConfigActivity`，传入 `EXTRA_APPWIDGET_ID`
3. 用户选择分类：全部 / 已完成 / 未完成
4. 点击保存：
   - 写入 SharedPreferences（key = `category_<id>`）
   - 调用 `TodoWidgetUpdater.updateWidget()` 立即渲染
   - `setResult(RESULT_OK, intent)` 并 finish
5. 若用户取消，`setResult(RESULT_CANCELED)`，微件不会被添加

## 4. 数据读取与渲染

- 从 `AppDatabase.todoDao().queryAllNewestFirst()` 获取全部待办
- 按今日时间范围过滤（`createdAt` 在今天 00:00–24:00 之间）
- 再按分类过滤：
  - `all`：不过滤
  - `completed`：仅 `isCompleted == true`
  - `incomplete`：仅 `isCompleted == false`
- 总数 = 过滤后列表大小
- 最新标题 = 过滤后第一条的 `title`（已按创建时间倒序）
- 无数据时显示「暂无待办」

RemoteViews 根据 `ThemePreferences.isDark()` 切换浅/深色背景和文字颜色，刷新图标也随主题切换资源。

## 5. 刷新按钮

- 点击发送广播 `com.example.noteflow.action.TODO_WIDGET_REFRESH`，携带 `EXTRA_APPWIDGET_ID`
- Provider 的 `onReceive` 捕获该 action，调用 `updateWidget(context, id)` 单实例更新
- PendingIntent 使用 `FLAG_IMMUTABLE`，requestCode 基于 widgetId 避免冲突

## 6. 生命周期清理

- `onDeleted` 时移除对应 widgetId 的 SharedPreferences 条目
- 配置 Activity 在 `appWidgetId == INVALID_APPWIDGET_ID` 时直接 finish

## 7. 验证方式

```bash
# 安装后检查 provider 注册
adb shell dumpsys appwidget | grep TodoWidgetProvider

# 检查 manifest 配置 activity
aapt dump xmltree app-debug.apk AndroidManifest.xml | grep APPWIDGET_CONFIGURE
```

手动验证：

1. 长按桌面空白处 → 小组件 → 找到「拾光待办 · 微件」
2. 拖到桌面，配置页弹出
3. 选择「未完成」→ 保存
4. 微件显示今日未完成待办数量与最新标题
5. 点击刷新按钮，数据重新加载
6. 切换应用主题为深色，微件颜色同步变化
