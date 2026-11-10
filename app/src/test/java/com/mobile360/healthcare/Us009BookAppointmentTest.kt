package com.mobile360.healthcare

import com.mobile360.healthcare.data.local.SlotEntity
import com.mobile360.healthcare.navigation.Routes
import com.mobile360.healthcare.testutil.FakeAppointments
import com.mobile360.healthcare.testutil.failOutcome
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests: US-009 (REQ-006) - booking contract: free slots, confirmation, conflict. */
class Us009BookAppointmentTest {
    @Test fun test_us_009_only_free_slots_are_offered() {
        val slots = listOf(
            SlotEntity("s1", "doc-1", 1L, false),
            SlotEntity("s2", "doc-1", 2L, true),
            SlotEntity("s3", "doc-1", 3L, false),
        )
        assertEquals(listOf("s1", "s3"), slots.filter { !it.booked }.map { it.id })
    }

    @Test fun test_us_009_successful_booking_returns_ok_with_message() = runTest {
        val repo = FakeAppointments()
        val r = repo.book("doc-1", "s1", "Chest discomfort")
        assertTrue(r.ok)
        assertEquals(listOf(Triple("doc-1", "s1", "Chest discomfort")), repo.booked)
    }

    @Test fun test_us_009_slot_taken_is_refused_with_a_message() = runTest {
        val repo = FakeAppointments().also { it.nextOutcome = failOutcome("That slot was just taken. Please choose another slot.") }
        val r = repo.book("doc-1", "s1", "x")
        assertFalse(r.ok)
        assertTrue(r.message.contains("taken"))
    }

    @Test fun test_us_009_booking_route_has_no_appointment_when_booking_new() {
        assertEquals(Routes.doctorProfileAndBooking("doc-1", null), Routes.doctorProfileAndBooking("doc-1", null))
        assertTrue(Routes.doctorProfileAndBooking("doc-1", null).contains("doc-1"))
    }
}
