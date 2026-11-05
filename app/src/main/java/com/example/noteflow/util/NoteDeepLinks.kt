package com.example.noteflow.util

/**
 * Public deep-link contract used by the app widget, the companion app and
 * external test commands. Keep these URIs in sync with nav_graph.xml and the
 * MainActivity intent filter.
 */
object NoteDeepLinks {

    const val SCHEME = "noteflow"
    const val HOST = "open"

    const val OPEN_NOTES = "$SCHEME://$HOST/notes"
    const val NEW_NOTE = "$SCHEME://$HOST/editor?noteId=-1"

    fun noteDetail(noteId: Long): String = "$SCHEME://$HOST/note/$noteId"
}
