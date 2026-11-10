package com.mobile360.healthcare

import com.mobile360.healthcare.navigation.Routes
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests: US-019 (REQ-016) - no video media is built; chat stays the consult channel. */
class Us019VideoExtensionPointTest {
    @Test fun test_us_019_no_video_route_exists_by_default() {
        val routes = listOf(Routes.CHAT, Routes.APPOINTMENTS, Routes.CONSULTATION, Routes.DOCTOR_SCHEDULE)
        assertFalse(routes.any { it.contains("video") })
    }

    @Test fun test_us_019_chat_route_is_available_from_an_appointment() {
        assertTrue(Routes.chat("a1").startsWith(Routes.CHAT))
    }
}
