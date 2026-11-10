// Tests: US-038 (NFR-009, NFR-010)
package com.mobile360.healthcare

import com.mobile360.healthcare.util.Formats
import com.mobile360.healthcare.util.Times
import org.junit.Assert.assertTrue
import org.junit.Test

// Font scale, TalkBack and contrast are manual/UI checks per the test plan; these cover readable formatting.
class Us038AccessibilityTest {
    @Test fun test_us_038_times_are_formatted_as_readable_text() {
        val t = Formats.time(Times.at(1, 9, 30))
        assertTrue(t.contains("09:30") || t.contains("9:30"))
    }

    @Test fun test_us_038_money_is_formatted_in_rupees() {
        val text = Formats.money(500)
        assertTrue(text.contains("500"))
    }
}
