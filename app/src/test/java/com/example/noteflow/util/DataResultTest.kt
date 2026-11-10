package com.example.noteflow.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DataResultTest {

    @Test
    fun success_exposesDataAndHelpers() {
        val result = DataResult.Success("todo")

        assertTrue(result.isSuccess())
        assertFalse(result.isFailure())
        assertEquals("todo", result.getOrNull())
        assertEquals("todo", result.getOrDefault("fallback"))
        assertEquals(
            "value:todo",
            result.fold(
                onSuccess = { "value:$it" },
                onFailure = { "error" },
            ),
        )
    }

    @Test
    fun failure_returnsFallbackAndPreservesCause() {
        val cause = IllegalStateException("database unavailable")
        val result: DataResult<String> = DataResult.Failure(
            type = DataError.DATABASE,
            message = "Database operation failed",
            cause = cause,
        )

        assertTrue(result.isFailure())
        assertFalse(result.isSuccess())
        assertNull(result.getOrNull())
        assertEquals("fallback", result.getOrDefault("fallback"))
        assertEquals(
            "Database operation failed: ${cause.message}",
            result.fold(
                onSuccess = { "unexpected" },
                onFailure = { "${it.message}: ${it.cause?.message}" },
            ),
        )
    }
}
