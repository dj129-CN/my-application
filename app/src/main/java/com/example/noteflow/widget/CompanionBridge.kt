package com.example.noteflow.widget

import android.content.Context
import android.content.Intent
import com.example.noteflow.BuildConfig

/**
 * Best-effort refresh signal for the independently installed companion app.
 * The receiver on the other side is protected by a signature-level permission,
 * so only apps signed with the same certificate can trigger a refresh.
 */
object CompanionBridge {

    private const val ACTION_REFRESH_WIDGET =
        "com.example.noteflow.companion.action.REFRESH_WIDGET"
    private const val DEBUG_COMPANION_PACKAGE = "com.example.noteflow.companion.debug"
    private const val RELEASE_COMPANION_PACKAGE = "com.example.noteflow.companion"

    fun notifyNotesChanged(context: Context) {
        val companionPackage = if (BuildConfig.DEBUG) {
            DEBUG_COMPANION_PACKAGE
        } else {
            RELEASE_COMPANION_PACKAGE
        }
        val intent = Intent(ACTION_REFRESH_WIDGET)
            .setPackage(companionPackage)
            .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
        context.sendBroadcast(intent)
    }
}
