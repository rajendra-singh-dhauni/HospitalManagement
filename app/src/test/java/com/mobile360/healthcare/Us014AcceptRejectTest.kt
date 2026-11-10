package com.mobile360.healthcare

import com.mobile360.healthcare.testutil.FakeAppointments
import com.mobile360.healthcare.testutil.failOutcome
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests: US-014 (REQ-011) - accept or reject contract used by the schedule screen. */
class Us014AcceptRejectTest {
    @Test fun test_us_014_accept_calls_repository_with_the_request_id() = runTest {
        val repo = FakeAppointments()
        assertTrue(repo.accept("a1").ok)
        assertEquals(listOf("a1"), repo.accepted)
    }

    @Test fun test_us_014_reject_passes_optional_reason() = runTest {
        val repo = FakeAppointments()
        repo.reject("a1", "Fully booked")
        repo.reject("a2", "")
        assertEquals(listOf("a1" to "Fully booked", "a2" to ""), repo.rejected)
    }

    @Test fun test_us_014_request_already_cancelled_is_reported_as_failure() = runTest {
        val repo = FakeAppointments().also { it.nextOutcome = failOutcome("Already cancelled") }
        assertFalse(repo.accept("a1").ok)
    }
}
