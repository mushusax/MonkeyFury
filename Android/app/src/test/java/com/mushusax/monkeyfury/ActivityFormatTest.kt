package com.mushusax.monkeyfury

import com.mushusax.monkeyfury.data.Activity
import com.mushusax.monkeyfury.data.ActivityKind
import com.mushusax.monkeyfury.data.formatDuration
import com.mushusax.monkeyfury.data.formatNumber
import com.mushusax.monkeyfury.data.formatValue
import com.mushusax.monkeyfury.data.parseDuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActivityFormatTest {

    private fun activity(kind: ActivityKind, unit: String = "") =
        Activity(publicId = "id", name = "x", category = "", kind = kind, unit = unit)

    @Test
    fun parsesDurations() {
        assertEquals(90.0, parseDuration("1:30")!!, 0.0)
        assertEquals(65.0, parseDuration("1:5")!!, 0.0)
        assertEquals(45.0, parseDuration("45")!!, 0.0)
        assertEquals(15.0, parseDuration(":15")!!, 0.0)
        assertEquals(600.0, parseDuration(" 10:00 ")!!, 0.0)
    }

    @Test
    fun rejectsBadDurations() {
        assertNull(parseDuration(""))
        assertNull(parseDuration("1:60"))
        assertNull(parseDuration("1:30:00"))
        assertNull(parseDuration("abc"))
        assertNull(parseDuration("-5"))
        assertNull(parseDuration("1.5"))
    }

    @Test
    fun formatsDurations() {
        assertEquals("1:30", formatDuration(90.0))
        assertEquals("0:05", formatDuration(5.0))
        assertEquals("12:00", formatDuration(720.0))
    }

    @Test
    fun formatsNumbers() {
        assertEquals("3", formatNumber(3.0))
        assertEquals("3.1", formatNumber(3.1))
        assertEquals("5.3", formatNumber(3.1f.toDouble() + 2.2f.toDouble()))
        assertEquals("0", formatNumber(0.0))
    }

    @Test
    fun formatsValuesPerKind() {
        assertEquals("Yes", activity(ActivityKind.BOOLEAN).formatValue(1.0))
        assertEquals("No", activity(ActivityKind.BOOLEAN).formatValue(0.0))
        assertEquals("1:15", activity(ActivityKind.DURATION).formatValue(75.0))
        assertEquals("3.1 mi", activity(ActivityKind.NUMBER, "mi").formatValue(3.1))
        assertEquals("V5", activity(ActivityKind.NUMBER, "V").formatValue(5.0))
        assertEquals("2", activity(ActivityKind.NUMBER).formatValue(2.0))
    }
}
