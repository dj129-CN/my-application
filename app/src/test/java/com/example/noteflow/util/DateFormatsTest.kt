package com.example.noteflow.util

import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class DateFormatsTest {

    private val shanghai = ZoneId.of("Asia/Shanghai")

    @Test
    fun full_usesChineseDateAndTwentyFourHourClock() {
        assertEquals("1970年1月1日 08:00", DateFormats.full(0L, shanghai))
    }

    @Test
    fun widgetLabel_keepsMonthDayAndTime() {
        assertEquals("1月1日 08:00", DateFormats.widgetLabel(0L, shanghai))
    }
}
