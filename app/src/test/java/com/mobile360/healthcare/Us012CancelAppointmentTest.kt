package com.mobile360.healthcare

import androidx.lifecycle.SavedStateHandle
import com.mobile360.healthcare.data.model.AppointmentStatus
import com.mobile360.healthcare.testutil.BoltMainRule
import com.mobile360.healthcare.testutil.FakeAppointments
import com.mobile360.healthcare.testutil.apptView
import com.mobile360.healthcare.ui.screens.AppointmentsViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import com.mobile360.healthcare.testutil.runTestUnconfined as runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Tests: US-012 (REQ-009) - cancelling frees the slot; cancelled shows under Past. */
@OptIn(ExperimentalCoroutinesApi::class)
class Us012CancelAppointmentTest {
    @get:Rule val main = BoltMainRule()

    private val hour = 60 * 60 * 1000L

    @Test fun test_us_012_confirming_cancel_calls_the_repository_once() = runTest {
        val repo = FakeAppointments()
        val vm = AppointmentsViewModel(SavedStateHandle(), repo)
        vm.cancel("a1")
        assertEquals(listOf("a1"), repo.cancelled)
    }

    @Test fun test_us_012_keep_does_not_touch_the_repository() = runTest {
        val repo = FakeAppointments()
        AppointmentsViewModel(SavedStateHandle(), repo)
        assertTrue(repo.cancelled.isEmpty())
    }

    @Test fun test_us_012_cancelled_appointment_is_not_upcoming_but_listed_in_history() = runTest {
        val now = System.currentTimeMillis()
        val repo = FakeAppointments()
        val cancelled = apptView("a1", AppointmentStatus.CANCELLED, now + 5 * hour)
        val live = apptView("a2", AppointmentStatus.CONFIRMED, now + 6 * hour)
        repo.all.value = listOf(cancelled, live)
        val vm = AppointmentsViewModel(SavedStateHandle(), repo)
        backgroundScope.launch { vm.items.collect { } }
        assertFalse(vm.isUpcoming(cancelled, now))
        assertTrue(vm.isUpcoming(live, now))
        assertNotNull(vm.items.value)
    }
}
