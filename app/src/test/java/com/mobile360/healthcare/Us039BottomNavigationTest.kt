// Tests: US-039 (REQ-041)
package com.mobile360.healthcare

import com.mobile360.healthcare.data.model.Role
import com.mobile360.healthcare.navigation.Routes
import org.junit.Assert.assertEquals
import org.junit.Test

class Us039BottomNavigationTest {
    @Test fun test_us_039_each_role_lands_on_its_tab_host() {
        assertEquals(Routes.PATIENT_HOME, Routes.startFor(Role.PATIENT))
        assertEquals(Routes.DOCTOR_SCHEDULE, Routes.startFor(Role.DOCTOR))
    }

    @Test fun test_us_039_key_module_routes_are_distinct() {
        val tabs = listOf(Routes.PATIENT_HOME, Routes.APPOINTMENTS, Routes.RECORDS, Routes.CHAT, Routes.SETTINGS,
            Routes.DOCTOR_SCHEDULE, Routes.AVAILABILITY)
        assertEquals(tabs.size, tabs.toSet().size)
    }

    @Test fun test_us_039_chat_route_can_target_a_thread() {
        assertEquals(true, Routes.chat("a1").contains("a1"))
    }
}
