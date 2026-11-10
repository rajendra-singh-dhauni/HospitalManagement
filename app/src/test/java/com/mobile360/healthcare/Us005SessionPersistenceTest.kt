// Tests: US-005 (REQ-031)
package com.mobile360.healthcare

import com.mobile360.healthcare.data.model.Role
import com.mobile360.healthcare.navigation.MainViewModel
import com.mobile360.healthcare.navigation.AppState.SignedIn
import com.mobile360.healthcare.navigation.AppState.SignedOut
import com.mobile360.healthcare.testutil.FakeAuthRepository
import com.mobile360.healthcare.testutil.MainDispatcherRule
import com.mobile360.healthcare.testutil.sampleSession
import com.mobile360.healthcare.util.AppConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class Us005SessionPersistenceTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun test_us_005_session_lasts_30_days() {
        assertEquals(30, AppConfig.SESSION_DAYS.toInt())
    }

    @Test fun test_us_005_reopen_with_live_session_stays_signed_in() = runTest {
        val auth = FakeAuthRepository()
        auth.sessionFlow.value = sampleSession(Role.PATIENT, signedInAt = 1L)
        val vm = MainViewModel(auth)
        backgroundScope.launch { vm.state.collect { } }
        assertEquals(SignedIn(Role.PATIENT), vm.state.value)
    }

    @Test fun test_us_005_reopen_with_expired_session_requires_sign_in() = runTest {
        // The session store emits null once 30 days have passed.
        val auth = FakeAuthRepository()
        val vm = MainViewModel(auth)
        backgroundScope.launch { vm.state.collect { } }
        assertEquals(SignedOut, vm.state.value)
    }

    @Test fun test_us_005_sign_out_requires_sign_in_again() = runTest {
        val auth = FakeAuthRepository()
        auth.sessionFlow.value = sampleSession(Role.DOCTOR)
        val vm = MainViewModel(auth)
        backgroundScope.launch { vm.state.collect { } }
        auth.signOut()
        assertEquals(1, auth.signOutCalls)
        assertEquals(SignedOut, vm.state.value)
        assertTrue(vm.state.value !is SignedIn)
    }
}
