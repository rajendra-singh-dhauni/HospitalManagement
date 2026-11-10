package com.mobile360.healthcare

import com.mobile360.healthcare.data.model.Role
import com.mobile360.healthcare.testutil.BoltMainRule
import com.mobile360.healthcare.testutil.FakeAvailability
import com.mobile360.healthcare.testutil.FixedSessionAuth
import com.mobile360.healthcare.testutil.availabilityRow
import com.mobile360.healthcare.ui.screens.AvailabilityViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Tests: US-015 (REQ-012) - availability editor: save, conflict warning, validation, failure. */
class Us015ManageAvailabilityTest {
    @get:Rule val main = BoltMainRule()

    private fun week() = (1..7).map { availabilityRow(it, enabled = it <= 5) }
    private fun vm(repo: FakeAvailability) = AvailabilityViewModel(FixedSessionAuth(Role.DOCTOR), repo)

    @Test fun test_us_015_loads_the_doctors_weekly_schedule() {
        val v = vm(FakeAvailability(week()))
        assertEquals(7, v.state.value.rows.size)
        assertFalse(v.state.value.loading)
    }

    @Test fun test_us_015_toggle_wednesday_and_save_stores_new_schedule() {
        val repo = FakeAvailability(week())
        val v = vm(repo)
        v.edit(3) { it.copy(enabled = false) }
        v.save()
        assertEquals(1, repo.saves.size)
        assertFalse(repo.saves.single().first { it.dayOfWeek == 3 }.enabled)
        assertEquals("Schedule saved. Patients see the new slots now.", v.state.value.message)
    }

    @Test fun test_us_015_affected_confirmed_appointments_need_confirmation() {
        val repo = FakeAvailability(week()).also { it.conflicts = 2 }
        val v = vm(repo)
        v.edit(1) { it.copy(enabled = false) }
        v.save()
        assertEquals(2, v.state.value.conflict)
        assertTrue(v.state.value.awaitingConfirm)
        assertTrue(repo.saves.isEmpty())
        v.save()
        assertEquals(1, repo.saves.size)
    }

    @Test fun test_us_015_end_before_start_is_refused() {
        val repo = FakeAvailability(week())
        val v = vm(repo)
        v.edit(1) { it.copy(startTime = "14:00", endTime = "13:00") }
        v.save()
        assertTrue(repo.saves.isEmpty())
        assertTrue(v.state.value.error != null || v.state.value.message != null)
    }

    @Test fun test_us_015_save_failure_keeps_edits_and_reports() {
        val repo = FakeAvailability(week()).also { it.failSave = true }
        val v = vm(repo)
        v.edit(3) { it.copy(enabled = false) }
        v.save()
        assertFalse(v.state.value.saving)
        assertNotNull(v.state.value.error ?: v.state.value.message)
        assertFalse(v.state.value.rows.first { it.dayOfWeek == 3 }.enabled)
    }
}
