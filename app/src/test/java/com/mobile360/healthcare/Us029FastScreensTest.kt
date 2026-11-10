// Tests: US-029 (NFR-001, NFR-002)
package com.mobile360.healthcare

import com.mobile360.healthcare.util.AppConfig
import com.mobile360.healthcare.util.Formats
import com.mobile360.healthcare.util.Times
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Us029FastScreensTest {
    @Test fun test_us_029_mock_latency_leaves_room_in_2s_load_budget() {
        // a screen load may chain a few source calls
        assertTrue(AppConfig.MOCK_LATENCY_MS.toLong() * 4 < 2_000L)
    }

    @Test fun test_us_029_mock_latency_leaves_room_in_3s_booking_budget() {
        // booking: slot check + write + refresh
        assertTrue(AppConfig.MOCK_LATENCY_MS.toLong() * 3 < 3_000L)
    }

    @Test fun test_us_029_time_formatting_is_fast_for_many_items() {
        val start = System.nanoTime()
        repeat(1_000) { Formats.time(Times.at(1, 9, 30)) }
        val ms = (System.nanoTime() - start) / 1_000_000
        assertTrue("formatting took $ms ms", ms < 2_000)
    }

    @Test fun test_us_029_times_at_and_start_of_day_are_consistent() {
        val nine = Times.at(0, 9, 0)
        assertEquals(Times.toDate(nine), Times.toLocal(Times.startOfDay(0)).toLocalDate())
        assertTrue(nine >= Times.startOfDay(0))
    }
}
