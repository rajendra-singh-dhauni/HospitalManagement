package com.mobile360.healthcare

import com.mobile360.healthcare.testutil.*
import com.mobile360.healthcare.ui.screens.SecurityAndAccountViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// Tests: US-034 (REQ-039)
class Us034AccountDeletionTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun test_us_034_correct_code_submits_request_and_signs_out() = runTest {
        val account = StubAccount()
        val auth = StubAuth()
        val vm = SecurityAndAccountViewModel(auth, account)
        vm.session.onEach { }.launchIn(backgroundScope)
        var done = false
        vm.deleteAccount(GOOD_CODE) { done = true }
        assertTrue(account.deleted)
        assertEquals(1, auth.signOuts)
        assertTrue(done)
    }

    @Test fun test_us_034_wrong_code_submits_no_request() = runTest {
        val account = StubAccount()
        val auth = StubAuth()
        val vm = SecurityAndAccountViewModel(auth, account)
        vm.session.onEach { }.launchIn(backgroundScope)
        var done = false
        vm.deleteAccount("000000") { done = true }
        assertFalse(account.deleted)
        assertEquals(0, auth.signOuts)
        assertFalse(done)
    }
}
