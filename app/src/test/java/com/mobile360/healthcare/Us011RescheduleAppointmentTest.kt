package com.mobile360.healthcare

import androidx.lifecycle.SavedStateHandle
import com.mobile360.healthcare.data.model.AppointmentStatus
import com.mobile360.healthcare.navigation.Routes
import com.mobile360.healthcare.testutil.BoltMainRule
import com.mobile360.healthcare.testutil.FakeAppointments
import com.mobile360.healthcare.testutil.apptView
import com.mobile360.healthcare.ui.screens.AppointmentsViewModel
import com.mobile360.healthcare.util.AppConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Tests: US-011 (REQ-008) - the 2-hour reschedule rule and the reschedule route. */
class Us011RescheduleAppointmentTest {
    @get:Rule val main = BoltMainRule()

    private val hour = 60 * 60 * 1000L
    private fun vm() = AppointmentsViewModel(SavedStateHandle(), FakeAppointments())

    @Test fun test_us_011_cutoff_is_two_hours() {
        assertEquals(2 * hour, AppConfig.RESCHEDULE_CUTOFF_MS.toLong())
    }

    @Test fun test_us_011_more_than_two_hours_remaining_allows_reschedule() {
        val a = apptView("a1", AppointmentStatus.CONFIRMED, System.currentTimeMillis() + 5 * hour)
        assertTrue(vm().canReschedule(a))
    }

    @Test fun test_us_011_one_hour_remaining_blocks_reschedule() {
        val a = apptView("a1", AppointmentStatus.CONFIRMED, System.currentTimeMillis() + hour)
        assertFalse(vm().canReschedule(a))
    }

    @Test fun test_us_011_reschedule_route_carries_the_appointment_id() {
        val route = Routes.doctorProfileAndBooking("doc-1", "a1")
        assertTrue(route.contains("doc-1"))
        assertTrue(route.contains("a1"))
        assertFalse(Routes.doctorProfileAndBooking("doc-1", null).contains("a1"))
    }
}
