// Tests: US-004 (REQ-004)
package com.mobile360.healthcare

import com.mobile360.healthcare.data.model.Outcome
import com.mobile360.healthcare.data.model.Role
import com.mobile360.healthcare.navigation.AppState.Loading
import com.mobile360.healthcare.navigation.MainViewModel
import com.mobile360.healthcare.navigation.Routes
import com.mobile360.healthcare.navigation.AppState.SignedIn
import com.mobile360.healthcare.navigation.AppState.SignedOut
import com.mobile360.healthcare.testutil.FakeAuthRepository
import com.mobile360.healthcare.testutil.MainDispatcherRule
import com.mobile360.healthcare.testutil.sampleSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class Us004RoleAccessTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun test_us_004_patient_and_doctor_start_on_different_home_routes() {
        assertEquals(Routes.PATIENT_HOME, Routes.homeFor(Role.PATIENT))
        assertEquals(Routes.DOCTOR_SCHEDULE, Routes.homeFor(Role.DOCTOR))
        assertNotEquals(Routes.homeFor(Role.PATIENT), Routes.homeFor(Role.DOCTOR))
    }

    @Test fun test_us_004_signed_out_user_starts_on_welcome() {
        assertEquals(Routes.WELCOME, Routes.startFor(null))
    }

    @Test fun test_us_004_denied_outcome_is_refusal_with_message() {
        val denied = Outcome.denied()
        assertFalse(denied.ok)
        assertTrue(denied.message.isNotBlank())
    }

    @Test fun test_us_004_app_state_carries_role_of_session() = runTest {
        val auth = FakeAuthRepository()
        val vm = MainViewModel(auth)
        backgroundScope.launch { vm.state.collect { } }
        auth.sessionFlow.value = sampleSession(Role.DOCTOR)
        assertEquals(SignedIn(Role.DOCTOR), vm.state.value)
        auth.sessionFlow.value = sampleSession(Role.PATIENT)
        assertEquals(SignedIn(Role.PATIENT), vm.state.value)
    }

    @Test fun test_us_004_sign_out_returns_to_signed_out_state() = runTest {
        val auth = FakeAuthRepository()
        auth.sessionFlow.value = sampleSession(Role.PATIENT)
        val vm = MainViewModel(auth)
        backgroundScope.launch { vm.state.collect { } }
        assertEquals(SignedIn(Role.PATIENT), vm.state.value)
        auth.signOut()
        assertEquals(SignedOut, vm.state.value)
        assertNotEquals(Loading, vm.state.value)
    }
}
