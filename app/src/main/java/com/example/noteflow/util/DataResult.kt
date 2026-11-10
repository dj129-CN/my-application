package com.example.noteflow.util

enum class DataError {
    VALIDATION,
    NOT_FOUND,
    DATABASE,
    UNKNOWN,
}

/**
 * Unified result wrapper for data-layer calls. Database exceptions are
 * converted to [DataResult.Failure]; cancellation is always rethrown.
 */
sealed interface DataResult<out T> {

    data class Success<out T>(val data: T) : DataResult<T>

    data class Failure(
        val type: DataError,
        val message: String,
        val cause: Throwable? = null,
    ) : DataResult<Nothing>
}

inline fun <T, R> DataResult<T>.fold(
    onSuccess: (T) -> R,
    onFailure: (DataResult.Failure) -> R,
): R = when (this) {
    is DataResult.Success -> onSuccess(data)
    is DataResult.Failure -> onFailure(this)
}

fun <T> DataResult<T>.getOrNull(): T? =
    (this as? DataResult.Success)?.data

fun <T> DataResult<T>.getOrDefault(defaultValue: T): T = when (this) {
    is DataResult.Success -> data
    is DataResult.Failure -> defaultValue
}

fun <T> DataResult<T>.isSuccess(): Boolean = this is DataResult.Success

fun <T> DataResult<T>.isFailure(): Boolean = this is DataResult.Failure
