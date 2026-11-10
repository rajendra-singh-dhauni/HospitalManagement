package com.mobile360.healthcare

import com.mobile360.healthcare.testutil.*
import com.mobile360.healthcare.ui.screens.SecurityAndAccountViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// Tests: US-035 (REQ-040)
class Us035ChangeNumberTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun test_us_035_correct_code_updates_number() = runTest {
        val account = StubAccount()
        val vm = SecurityAndAccountViewModel(StubAuth(), account)
        vm.session.onEach { }.launchIn(backgroundScope)
        var done = false
        vm.changeNumber("9876500002", GOOD_CODE) { done = true }
        assertEquals("9876500002", account.changedTo)
        assertTrue(done)
    }

    @Test fun test_us_035_wrong_code_keeps_number_and_shows_error() = runTest {
        val account = StubAccount()
        val vm = SecurityAndAccountViewModel(StubAuth(), account)
        vm.session.onEach { }.launchIn(backgroundScope)
        var done = false
        vm.changeNumber("9876500002", "000000") { done = true }
        assertNull(account.changedTo)
        assertFalse(done)
        assertFalse(vm.state.value.message!!.ok)
    }

    @Test fun test_us_035_invalid_number_is_rejected_before_calling_service() = runTest {
        val account = StubAccount()
        val vm = SecurityAndAccountViewModel(StubAuth(), account)
        vm.session.onEach { }.launchIn(backgroundScope)
        vm.changeNumber("12", GOOD_CODE) { }
        assertNull(account.changedTo)
        assertEquals("Enter a 10-digit mobile number.", vm.state.value.message?.text)
    }
}
