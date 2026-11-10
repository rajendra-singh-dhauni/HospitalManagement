package com.mobile360.healthcare

import androidx.lifecycle.SavedStateHandle
import com.mobile360.healthcare.testutil.*
import com.mobile360.healthcare.ui.screens.PrescriptionDetailViewModel
import com.mobile360.healthcare.ui.screens.SecurityAndAccountViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// Tests: US-006 (REQ-032)
class Us006ReAuthenticationTest {
    @get:Rule val main = MainDispatcherRule()

    private fun detailVm(account: StubAccount, records: StubRecords) = PrescriptionDetailViewModel(
        SavedStateHandle(mapOf("prescriptionId" to "r1")), records, account, android.content.ContextWrapper(null)
    )

    @Test fun test_us_006_download_asks_for_a_fresh_code_first() = runTest {
        val vm = detailVm(StubAccount(), StubRecords(rx = listOf(rxView("r1", 1L))))
        assertFalse(vm.download.value.askCode)
        vm.askForCode()
        assertTrue(vm.download.value.askCode)
    }

    @Test fun test_us_006_wrong_code_does_not_perform_download() = runTest {
        val account = StubAccount()
        val vm = detailVm(account, StubRecords(rx = listOf(rxView("r1", 1L))))
        vm.askForCode()
        vm.confirmCode("000000")
        assertEquals(1, account.reAuthCalls)
        assertNotNull(vm.download.value.codeError)
        assertNull(vm.download.value.saved)
    }

    @Test fun test_us_006_blank_code_is_rejected_without_calling_service() = runTest {
        val account = StubAccount()
        val vm = detailVm(account, StubRecords())
        vm.askForCode()
        vm.confirmCode("  ")
        assertEquals(0, account.reAuthCalls)
        assertEquals("Enter your authentication code.", vm.download.value.codeError)
    }

    @Test fun test_us_006_wrong_code_does_not_change_number() = runTest {
        val account = StubAccount()
        val vm = SecurityAndAccountViewModel(StubAuth(), account)
        vm.changeNumber("9876500002", "000000") { }
        assertNull(account.changedTo)
    }
}
