package com.example.noteflow.companion.data

import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.noteflow.companion.BuildConfig
import com.example.noteflow.companion.CompanionActivity

data class CompanionNote(
    val id: Long,
    val title: String,
    val content: String,
    val updatedAt: Long,
)

data class CompanionSnapshot(
    val connected: Boolean,
    val latestNote: CompanionNote?,
)

/**
 * Runtime bridge to the main app. There is intentionally no Gradle dependency
 * on :app -- the companion only relies on the public ContentProvider + deep
 * link contract and degrades gracefully when the main app is absent.
 */
object CompanionProviderClient {

    private const val TAG = "CompanionProviderClient"
    private const val DEBUG_MAIN_PACKAGE = "com.example.noteflow.debug"
    private const val RELEASE_MAIN_PACKAGE = "com.example.noteflow"
    private const val DEBUG_AUTHORITY = "com.example.noteflow.debug.provider"
    private const val RELEASE_AUTHORITY = "com.example.noteflow.provider"

    const val DEEP_LINK_NOTES = "noteflow://open/notes"
    const val DEEP_LINK_NEW_NOTE = "noteflow://open/editor?noteId=-1"

    private val mainPackageName: String
        get() = if (BuildConfig.DEBUG) DEBUG_MAIN_PACKAGE else RELEASE_MAIN_PACKAGE

    private val authority: String
        get() = if (BuildConfig.DEBUG) DEBUG_AUTHORITY else RELEASE_AUTHORITY

    private val notesUri: Uri
        get() = Uri.parse("content://$authority/notes")

    fun loadSnapshot(context: Context): CompanionSnapshot {
        if (!isMainAppInstalled(context)) {
            return CompanionSnapshot(connected = false, latestNote = null)
        }

        return try {
            CompanionSnapshot(
                connected = true,
                latestNote = queryNotes(context).firstOrNull(),
            )
        } catch (exception: Exception) {
            Log.w(TAG, "Main app provider is not accessible", exception)
            CompanionSnapshot(connected = false, latestNote = null)
        }
    }

    fun isMainAppInstalled(context: Context): Boolean {
        @Suppress("DEPRECATION")
        return try {
            context.packageManager.getPackageInfo(mainPackageName, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openMainApp(context: Context, uri: String = DEEP_LINK_NOTES): Boolean {
        if (!isMainAppInstalled(context)) return false
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
            setPackage(mainPackageName)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP,
            )
        }
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    fun mainAppPendingIntent(
        context: Context,
        uri: String,
        requestCode: Int,
    ): PendingIntent? {
        if (!isMainAppInstalled(context)) return null
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
            setPackage(mainPackageName)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP,
            )
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun companionPendingIntent(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, CompanionActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun deepLinkForNote(noteId: Long): String = "noteflow://open/note/$noteId"

    private fun queryNotes(context: Context): List<CompanionNote> {
        val projection = arrayOf("_id", "title", "content", "updated_at")
        return context.contentResolver.query(
            notesUri,
            projection,
            null,
            null,
            null,
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow("_id")
            val titleColumn = cursor.getColumnIndexOrThrow("title")
            val contentColumn = cursor.getColumnIndexOrThrow("content")
            val updatedColumn = cursor.getColumnIndexOrThrow("updated_at")
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        CompanionNote(
                            id = cursor.getLong(idColumn),
                            title = cursor.getString(titleColumn).orEmpty(),
                            content = cursor.getString(contentColumn).orEmpty(),
                            updatedAt = cursor.getLong(updatedColumn),
                        ),
                    )
                }
            }
        }.orEmpty()
    }
}
