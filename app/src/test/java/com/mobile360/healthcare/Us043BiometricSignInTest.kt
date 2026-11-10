package com.mobile360.healthcare

import androidx.lifecycle.SavedStateHandle
import com.mobile360.healthcare.data.AuthRepository
import com.mobile360.healthcare.data.model.BiometricSignInResult
import com.mobile360.healthcare.data.model.CodeRequestResult
import com.mobile360.healthcare.data.model.Session
import com.mobile360.healthcare.data.model.VerifyResult
import com.mobile360.healthcare.testutil.MainDispatcherRule
import com.mobile360.healthcare.ui.screens.BIOMETRIC_FAILED
import com.mobile360.healthcare.ui.screens.PhoneEntryViewModel
import com.mobile360.healthcare.ui.screens.SecurityAndAccountViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import com.mobile360.healthcare.testutil.StubAccount

/** Tests: CR-001 biometric sign-in on the login page. */
class Us043BiometricSignInTest {
    @get:Rule val main = MainDispatcherRule()

    private class BioAuth(
        var enrolled: Boolean,
        var result: BiometricSignInResult = BiometricSignInResult.SIGNED_IN,
    ) : AuthRepository {
        override val session: Flow<Session?> = MutableStateFlow(null)
        var signInCalls = 0
        override suspend fun requestCode(phone: String): CodeRequestResult = CodeRequestResult.InvalidNumber
        override suspend fun verifyCode(code: String): VerifyResult = VerifyResult.NoRequest
        override suspend fun signOut() {}
        override suspend fun isBiometricEnrolled() = enrolled
        override suspend fun enableBiometric(): Boolean { enrolled = true; return true }
        override suspend fun disableBiometric() { enrolled = false }
        override suspend fun signInWithBiometric(): BiometricSignInResult { signInCalls++; return result }
    }

    @Test
    fun test_cr_001_enrolled_user_sees_button_state_and_signs_in() {
        val auth = BioAuth(enrolled = true)
        val vm = PhoneEntryViewModel(auth, SavedStateHandle())
        var signedIn = false
        assertTrue(vm.biometricEnrolled.value)
        vm.onBiometricSuccess { signedIn = true }
        assertTrue(signedIn)
        assertEquals(1, auth.signInCalls)
    }

    @Test
    fun test_cr_001_not_enrolled_user_has_no_biometric_option() {
        val vm = PhoneEntryViewModel(BioAuth(enrolled = false), SavedStateHandle())
        assertFalse(vm.biometricEnrolled.value)
    }

    @Test
    fun test_cr_001_missing_account_shows_message_and_hides_option() {
        val auth = BioAuth(enrolled = true, result = BiometricSignInResult.ACCOUNT_MISSING)
        val vm = PhoneEntryViewModel(auth, SavedStateHandle())
        var signedIn = false
        vm.onBiometricSuccess { signedIn = true }
        assertFalse(signedIn)
        assertFalse(vm.biometricEnrolled.value)
        assertEquals(BIOMETRIC_FAILED, vm.state.value.error)
    }

    @Test
    fun test_cr_001_cancelled_prompt_shows_message_and_creates_no_session() {
        val auth = BioAuth(enrolled = true)
        val vm = PhoneEntryViewModel(auth, SavedStateHandle())
        vm.onBiometricFailed()
        assertEquals(BIOMETRIC_FAILED, vm.state.value.error)
        assertEquals(0, auth.signInCalls)
    }

    @Test
    fun test_cr_001_toggle_enables_and_disables_enrolment() {
        val auth = BioAuth(enrolled = false)
        val vm = SecurityAndAccountViewModel(auth, StubAccount())
        vm.enableBiometric()
        assertTrue(vm.biometricEnabled.value)
        vm.disableBiometric()
        assertFalse(vm.biometricEnabled.value)
        assertFalse(auth.enrolled)
    }
}
