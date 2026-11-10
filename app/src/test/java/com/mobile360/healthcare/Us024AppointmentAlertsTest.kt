package com.mobile360.healthcare

import com.mobile360.healthcare.data.model.AlertKind
import com.mobile360.healthcare.testutil.*
import com.mobile360.healthcare.ui.screens.SettingsViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import com.mobile360.healthcare.testutil.runTestUnconfined as runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// Tests: US-024 (REQ-021)
class Us024AppointmentAlertsTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun test_us_024_appointment_alerts_are_on_by_default() = runTest {
        val vm = SettingsViewModel(StubAuth(), StubAccount())
        vm.prefs.onEach { }.launchIn(backgroundScope)
        assertTrue(vm.prefs.value.appointments)
    }

    @Test fun test_us_024_disabled_preference_is_visible_so_no_alert_is_sent() = runTest {
        val vm = SettingsViewModel(StubAuth(), StubAccount())
        vm.prefs.onEach { }.launchIn(backgroundScope)
        vm.setAlert(AlertKind.APPOINTMENTS, false)
        assertFalse(vm.prefs.value.appointments)
    }
}
