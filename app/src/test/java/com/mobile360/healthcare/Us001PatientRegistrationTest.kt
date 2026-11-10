// Tests: US-001 (REQ-001)
package com.mobile360.healthcare

import androidx.lifecycle.SavedStateHandle
import com.mobile360.healthcare.data.model.CodeRequestResult
import com.mobile360.healthcare.data.model.Role
import com.mobile360.healthcare.data.model.VerifyResult
import com.mobile360.healthcare.testutil.FakeAuthRepository
import com.mobile360.healthcare.testutil.MainDispatcherRule
import com.mobile360.healthcare.ui.screens.OTPEntryViewModel
import com.mobile360.healthcare.ui.screens.PendingCode
import com.mobile360.healthcare.ui.screens.PhoneEntryViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class Us001PatientRegistrationTest {
    @get:Rule val main = MainDispatcherRule()

    private fun phoneVm(auth: FakeAuthRepository) = PhoneEntryViewModel(auth, SavedStateHandle())
    private fun otpVm(auth: FakeAuthRepository) = OTPEntryViewModel(auth, SavedStateHandle())

    @Test fun test_us_001_valid_number_requests_code_and_signals_code_screen() = runTest {
        val auth = FakeAuthRepository()
        val vm = phoneVm(auth)
        vm.onPhoneChange("98765 43210")
        vm.send()
        assertEquals(1, auth.requestedPhones.size)
        assertTrue(auth.requestedPhones.single().endsWith("9876543210"))
        vm.issued.first()
        assertNull(vm.state.value.error)
        assertFalse(vm.state.value.loading)
    }

    @Test fun test_us_001_too_short_number_shows_error_and_requests_nothing() = runTest {
        val auth = FakeAuthRepository()
        val vm = phoneVm(auth)
        vm.onPhoneChange("123")
        vm.send()
        assertTrue(auth.requestedPhones.isEmpty())
        assertNotNull(vm.state.value.error)
        assertTrue(vm.state.value.error!!.contains("10-digit"))
        assertFalse(vm.state.value.loading)
    }

    @Test fun test_us_001_empty_number_shows_error() = runTest {
        val auth = FakeAuthRepository()
        val vm = phoneVm(auth)
        vm.send()
        assertTrue(auth.requestedPhones.isEmpty())
        assertNotNull(vm.state.value.error)
    }

    @Test fun test_us_001_letters_are_not_accepted_as_number() = runTest {
        val auth = FakeAuthRepository()
        val vm = phoneVm(auth)
        vm.onPhoneChange("abcdefghij")
        vm.send()
        assertTrue(auth.requestedPhones.isEmpty())
        assertNotNull(vm.state.value.error)
    }

    @Test fun test_us_001_service_rejects_number_shows_error() = runTest {
        val auth = FakeAuthRepository().apply { requestResult = CodeRequestResult.InvalidNumber }
        val vm = phoneVm(auth)
        vm.onPhoneChange("9876543210")
        vm.send()
        assertNotNull(vm.state.value.error)
        assertFalse(vm.state.value.loading)
    }

    @Test fun test_us_001_correct_code_completes_registration_with_role() = runTest {
        val auth = FakeAuthRepository().apply { verifyResults += VerifyResult.Success(Role.PATIENT) }
        PendingCode.record("9876543210", CodeRequestResult.Issued("+91 ***** 43210", 300_000L))
        val vm = otpVm(auth)
        vm.onCodeChange("123456")
        vm.confirm()
        assertEquals(listOf("123456"), auth.verifiedCodes)
        assertEquals(Role.PATIENT, vm.state.value.verifiedRole)
        assertNull(vm.state.value.error)
    }

    @Test fun test_us_001_wrong_code_shows_error_and_does_not_complete() = runTest {
        val auth = FakeAuthRepository().apply { verifyResults += VerifyResult.Wrong(2) }
        PendingCode.record("9876543210", CodeRequestResult.Issued("+91 ***** 43210", 300_000L))
        val vm = otpVm(auth)
        vm.onCodeChange("000000")
        vm.confirm()
        assertNull(vm.state.value.verifiedRole)
        assertTrue(vm.state.value.error!!.contains("2 attempt"))
    }

    @Test fun test_us_001_incomplete_code_is_not_sent() = runTest {
        val auth = FakeAuthRepository()
        val vm = otpVm(auth)
        vm.onCodeChange("12")
        vm.confirm()
        assertTrue(auth.verifiedCodes.isEmpty())
        assertNotNull(vm.state.value.error)
    }
}
