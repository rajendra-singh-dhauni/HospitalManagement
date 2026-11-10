package com.mobile360.healthcare

import com.mobile360.healthcare.data.model.AlertKind
import com.mobile360.healthcare.data.model.TimelineType
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

// Tests: US-025 (REQ-022)
// Lock-screen privacy and tap-to-open are manual/instrumented checks (T-025-1, T-025-2).
class Us025ReportAlertsTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun test_us_025_reports_have_their_own_laboratory_timeline_type() {
        assertEquals("LABORATORY", TimelineType.LABORATORY)
    }

    @Test fun test_us_025_report_alerts_can_be_switched_off() = runTest {
        val account = StubAccount()
        val vm = SettingsViewModel(StubAuth(), account)
        vm.prefs.onEach { }.launchIn(backgroundScope)
        assertTrue(vm.prefs.value.reportsAndMessages)
        vm.setAlert(AlertKind.REPORTS_AND_MESSAGES, false)
        assertFalse(vm.prefs.value.reportsAndMessages)
    }
}
