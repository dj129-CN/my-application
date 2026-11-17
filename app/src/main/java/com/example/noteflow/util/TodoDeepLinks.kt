package com.example.noteflow.util

/**
 * Public deep-link contract for the todo feature. Used by the 4x1 widget,
 * external test commands, and any same-signature client that wants to open a
 * specific todo screen.
 *
 * Routes:
 * - todo://app/list          → todo list (start destination)
 * - todo://app/detail?id=X   → edit/view todo with id X
 * - todo://app/add           → new todo editor
 */
object TodoDeepLinks {

    const val SCHEME = "todo"
    const val HOST = "app"

    const val LIST = "$SCHEME://$HOST/list"
    const val ADD = "$SCHEME://$HOST/add"

    fun detail(todoId: Long): String = "$SCHEME://$HOST/detail?id=$todoId"
}
