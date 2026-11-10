package com.mobile360.healthcare

import com.mobile360.healthcare.data.model.AlertKind
import com.mobile360.healthcare.testutil.*
import com.mobile360.healthcare.ui.screens.SettingsViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import com.mobile360.healthcare.testutil.runTestUnconfined as runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// Tests: US-028 (REQ-030)
class Us028AlertPreferencesTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun test_us_028_each_type_toggles_independently() = runTest {
        val account = StubAccount()
        val vm = SettingsViewModel(StubAuth(), account)
        vm.prefs.onEach { }.launchIn(backgroundScope)
        vm.setAlert(AlertKind.REMINDERS, false)
        val p = vm.prefs.value
        assertFalse(p.reminders)
        assertTrue(p.appointments && p.reportsAndMessages)
    }

    @Test fun test_us_028_choice_persists_for_a_new_view_model() = runTest {
        val account = StubAccount()
        val first = SettingsViewModel(StubAuth(), account)
        first.prefs.onEach { }.launchIn(backgroundScope)
        first.setAlert(AlertKind.APPOINTMENTS, false)
        val second = SettingsViewModel(StubAuth(), account)
        second.prefs.onEach { }.launchIn(backgroundScope)
        assertEquals(false, second.prefs.value.appointments)
    }
}
