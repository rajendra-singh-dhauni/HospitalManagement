package com.mobile360.healthcare

import com.mobile360.healthcare.testutil.*
import com.mobile360.healthcare.ui.screens.SecurityAndAccountViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// Tests: US-007 (REQ-033)
class Us007DeviceProtectionTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun test_us_007_sign_out_other_devices_revokes_sessions_and_confirms() = runTest {
        val account = StubAccount()
        val auth = StubAuth()
        val vm = SecurityAndAccountViewModel(auth, account)
        vm.session.onEach { }.launchIn(backgroundScope)
        vm.signOutOthers()
        assertTrue(account.othersRevoked)
        val msg = vm.state.value.message
        assertNotNull(msg)
        assertTrue(msg!!.ok)
        assertEquals("devices", msg.section)
    }

    @Test fun test_us_007_current_session_stays_signed_in_after_revoking_others() = runTest {
        val auth = StubAuth()
        val vm = SecurityAndAccountViewModel(auth, StubAccount())
        vm.session.onEach { }.launchIn(backgroundScope)
        vm.signOutOthers()
        assertEquals(0, auth.signOuts)
        assertNotNull(auth.current.value)
    }
}
