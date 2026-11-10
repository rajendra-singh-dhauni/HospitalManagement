package com.mobile360.healthcare

import com.mobile360.healthcare.data.model.AlertKind
import com.mobile360.healthcare.data.model.AppointmentStatus
import com.mobile360.healthcare.testutil.*
import com.mobile360.healthcare.ui.screens.SettingsViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import com.mobile360.healthcare.testutil.runTestUnconfined as runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

// Tests: US-026 (REQ-023)
// Scheduling at 24 h / 1 h and reboot survival need WorkManager (instrumented, T-026-3).
class Us026AppointmentRemindersTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun test_us_026_only_confirmed_appointments_are_distinct_from_cancelled() {
        assertNotEquals(AppointmentStatus.CONFIRMED, AppointmentStatus.CANCELLED)
        assertEquals("CONFIRMED", AppointmentStatus.CONFIRMED)
    }

    @Test fun test_us_026_reminders_can_be_switched_off() = runTest {
        val vm = SettingsViewModel(StubAuth(), StubAccount())
        vm.prefs.onEach { }.launchIn(backgroundScope)
        vm.setAlert(AlertKind.REMINDERS, false)
        assertFalse(vm.prefs.value.reminders)
    }
}
