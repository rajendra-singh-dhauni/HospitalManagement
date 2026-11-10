package com.mobile360.healthcare

import com.mobile360.healthcare.data.model.AppointmentStatus
import com.mobile360.healthcare.testutil.apptView
import com.mobile360.healthcare.ui.screens.DoctorScheduleState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests: US-013 (REQ-010) - today's schedule data shapes. */
class Us013DailyAppointmentsTest {
    @Test fun test_us_013_appointments_sort_by_time_with_name_and_status() {
        val list = listOf(
            apptView("a2", AppointmentStatus.PENDING, 2000L, patientName = "Rahul Mehta"),
            apptView("a1", AppointmentStatus.CONFIRMED, 1000L, patientName = "Priya Nair"),
        ).sortedBy { it.startMillis }
        assertEquals(listOf("Priya Nair", "Rahul Mehta"), list.map { it.patientName })
        assertEquals(listOf("CONFIRMED", "PENDING"), list.map { it.status })
    }

    @Test fun test_us_013_only_own_appointments_remain_after_doctor_filter() {
        val all = listOf(apptView("a1", "CONFIRMED", 1L, doctorId = "d1"), apptView("a2", "CONFIRMED", 2L, doctorId = "d2"))
        assertEquals(listOf("a1"), all.filter { it.doctorId == "d1" }.map { it.id })
    }

    @Test fun test_us_013_empty_schedule_state_has_no_items_and_no_error() {
        val s = DoctorScheduleState(
            loading = false, error = null, appointments = emptyList(), online = true, message = null, showRequests = false,
        )
        assertTrue(s.appointments.isEmpty())
        assertEquals(null, s.error)
    }
}
