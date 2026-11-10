// Tests: US-003 (REQ-003)
package com.mobile360.healthcare

import androidx.lifecycle.SavedStateHandle
import com.mobile360.healthcare.data.model.CodeRequestResult
import com.mobile360.healthcare.data.model.Role
import com.mobile360.healthcare.data.model.VerifyResult
import com.mobile360.healthcare.navigation.Routes
import com.mobile360.healthcare.testutil.FakeAuthRepository
import com.mobile360.healthcare.testutil.MainDispatcherRule
import com.mobile360.healthcare.ui.screens.OTPEntryViewModel
import com.mobile360.healthcare.ui.screens.PendingCode
import com.mobile360.healthcare.ui.screens.PhoneEntryViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class Us003DoctorSignInTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun test_us_003_approved_doctor_enters_doctor_mode() = runTest {
        val auth = FakeAuthRepository().apply { verifyResults += VerifyResult.Success(Role.DOCTOR) }
        PendingCode.record("9876500001", CodeRequestResult.Issued("+91 ***** 00001", 300_000L))
        val vm = OTPEntryViewModel(auth, SavedStateHandle())
        vm.onCodeChange("123456")
        vm.confirm()
        assertEquals(Role.DOCTOR, vm.state.value.verifiedRole)
        assertEquals(Routes.DOCTOR_SCHEDULE, Routes.homeFor(vm.state.value.verifiedRole))
    }

    @Test fun test_us_003_unapproved_number_is_refused_with_message() = runTest {
        val auth = FakeAuthRepository().apply { requestResult = CodeRequestResult.InvalidNumber }
        val vm = PhoneEntryViewModel(auth, SavedStateHandle())
        vm.onPhoneChange("9000000000")
        vm.send()
        assertNotNull(vm.state.value.error)
        assertTrue(vm.state.value.error!!.contains("approved"))
    }

    @Test fun test_us_003_doctor_gets_same_expiry_message() = runTest {
        val auth = FakeAuthRepository().apply { verifyResults += VerifyResult.Expired }
        PendingCode.record("9876500001", CodeRequestResult.Issued("+91 ***** 00001", 300_000L))
        val vm = OTPEntryViewModel(auth, SavedStateHandle())
        vm.onCodeChange("123456")
        vm.confirm()
        assertTrue(vm.state.value.error!!.contains("expired"))
    }

    @Test fun test_us_003_doctor_gets_same_lockout_message() = runTest {
        val auth = FakeAuthRepository().apply { verifyResults += VerifyResult.Locked(15) }
        PendingCode.record("9876500001", CodeRequestResult.Issued("+91 ***** 00001", 300_000L))
        val vm = OTPEntryViewModel(auth, SavedStateHandle())
        vm.onCodeChange("123456")
        vm.confirm()
        assertTrue(vm.state.value.error!!.contains("locked"))
    }
}
