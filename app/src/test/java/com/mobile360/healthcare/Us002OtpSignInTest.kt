// Tests: US-002 (REQ-002)
package com.mobile360.healthcare

import androidx.lifecycle.SavedStateHandle
import com.mobile360.healthcare.data.model.CodeRequestResult
import com.mobile360.healthcare.data.model.Role
import com.mobile360.healthcare.data.model.VerifyResult
import com.mobile360.healthcare.testutil.FakeAuthRepository
import com.mobile360.healthcare.testutil.MainDispatcherRule
import com.mobile360.healthcare.ui.screens.OTPEntryViewModel
import com.mobile360.healthcare.ui.screens.PendingCode
import com.mobile360.healthcare.util.AppConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class Us002OtpSignInTest {
    @get:Rule val main = MainDispatcherRule()

    @Before fun pending() {
        PendingCode.record("9876543210", CodeRequestResult.Issued("+91 ***** 43210", 300_000L))
    }

    private fun vm(auth: FakeAuthRepository) = OTPEntryViewModel(auth, SavedStateHandle())

    @Test fun test_us_002_rules_are_5_minutes_3_attempts_15_minutes() {
        assertEquals(5 * 60_000L, AppConfig.CODE_VALID_MS.toLong())
        assertEquals(3, AppConfig.MAX_ATTEMPTS.toInt())
        assertEquals(15 * 60_000L, AppConfig.LOCK_MS.toLong())
    }

    @Test fun test_us_002_valid_code_signs_in() = runTest {
        val auth = FakeAuthRepository().apply { verifyResults += VerifyResult.Success(Role.PATIENT) }
        val v = vm(auth)
        v.onCodeChange("123456")
        v.confirm()
        assertEquals(Role.PATIENT, v.state.value.verifiedRole)
    }

    @Test fun test_us_002_expired_code_tells_user_and_resend_issues_new_code() = runTest {
        val auth = FakeAuthRepository().apply { verifyResults += VerifyResult.Expired }
        val v = vm(auth)
        v.onCodeChange("123456")
        v.confirm()
        assertTrue(v.state.value.error!!.contains("expired"))
        assertNull(v.state.value.verifiedRole)

        v.resend()
        assertEquals(1, auth.requestedPhones.size)
        assertNotNull(v.state.value.info)
    }

    @Test fun test_us_002_attempts_left_message_decrements() = runTest {
        val auth = FakeAuthRepository().apply {
            verifyResults += VerifyResult.Wrong(2)
            verifyResults += VerifyResult.Wrong(1)
        }
        val v = vm(auth)
        v.onCodeChange("111111"); v.confirm()
        assertTrue(v.state.value.error!!.contains("2 attempt"))
        v.onCodeChange("222222"); v.confirm()
        assertTrue(v.state.value.error!!.contains("1 attempt"))
    }

    @Test fun test_us_002_lockout_message_explains_reason_and_minutes() = runTest {
        val auth = FakeAuthRepository().apply { verifyResults += VerifyResult.Locked(15) }
        val v = vm(auth)
        v.onCodeChange("999999")
        v.confirm()
        val msg = v.state.value.error!!
        assertTrue(msg.contains("Too many wrong codes"))
        assertTrue(msg.contains("15"))
        assertNull(v.state.value.verifiedRole)
    }

    @Test fun test_us_002_resend_while_locked_shows_lock_message() = runTest {
        val auth = FakeAuthRepository().apply { requestResult = CodeRequestResult.Locked(14) }
        val v = vm(auth)
        v.resend()
        assertTrue(v.state.value.error!!.contains("locked"))
    }

    @Test fun test_us_002_code_input_keeps_only_six_digits() = runTest {
        val v = vm(FakeAuthRepository())
        v.onCodeChange("12ab3456789")
        assertTrue(v.code.value.length <= 6)
        assertTrue(v.code.value.all { it.isDigit() })
    }
}
