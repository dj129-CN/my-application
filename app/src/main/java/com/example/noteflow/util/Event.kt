package com.example.noteflow.util

/**
 * A one-shot event wrapper for LiveData. Prevents configuration changes from
 * replaying snackbars or navigation events.
 */
class Event<out T>(private val content: T) {

    private var hasBeenHandled = false

    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }

    fun peekContent(): T = content
}
