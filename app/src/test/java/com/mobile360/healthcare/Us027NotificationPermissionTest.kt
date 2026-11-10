package com.mobile360.healthcare

import com.mobile360.healthcare.data.model.AlertKind
import com.mobile360.healthcare.testutil.*
import com.mobile360.healthcare.ui.screens.SettingsViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// Tests: US-027 (REQ-029)
// Rationale dialog and denied banner are UI behaviours verified manually (T-027-1, T-027-2).
class Us027NotificationPermissionTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun test_us_027_app_keeps_working_with_alerts_enabled_or_not() = runTest {
        val vm = SettingsViewModel(StubAuth(), StubAccount())
        vm.prefs.onEach { }.launchIn(backgroundScope)
        vm.setAlert(AlertKind.APPOINTMENTS, false)
        vm.setAlert(AlertKind.APPOINTMENTS, true)
        assertTrue(vm.prefs.value.appointments)
    }
}
