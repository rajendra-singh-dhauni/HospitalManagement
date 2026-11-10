package com.mobile360.healthcare.testutil

import com.mobile360.healthcare.data.AuthRepository
import com.mobile360.healthcare.data.model.CodeRequestResult
import com.mobile360.healthcare.data.model.Role
import com.mobile360.healthcare.data.model.Session
import com.mobile360.healthcare.data.model.VerifyResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}

/** Scripted auth source: no network, no clock, completes at once. */
class FakeAuthRepository : AuthRepository {
    val sessionFlow = MutableStateFlow<Session?>(null)
    override val session: Flow<Session?> = sessionFlow

    val requestedPhones = mutableListOf<String>()
    val verifiedCodes = mutableListOf<String>()
    var signOutCalls = 0

    var requestResult: CodeRequestResult = CodeRequestResult.Issued("+91 ***** 43210", 5 * 60_000L)
    val verifyResults = ArrayDeque<VerifyResult>()

    override suspend fun requestCode(phone: String): CodeRequestResult {
        requestedPhones += phone
        return requestResult
    }

    override suspend fun verifyCode(code: String): VerifyResult {
        verifiedCodes += code
        return verifyResults.removeFirstOrNull() ?: VerifyResult.NoRequest
    }

    override suspend fun signOut() {
        signOutCalls++
        sessionFlow.value = null
    }
}

fun sampleSession(role: Role = Role.PATIENT, signedInAt: Long = 0L): Session =
    if (role == Role.DOCTOR) {
        Session(
            userId = "d1",
            name = "Meera Kapoor",
            phone = "+919876500001",
            role = role,
            doctorId = "doc-1",
            signedInAt = signedInAt,
        )
    } else {
        Session(
            userId = "p1",
            name = "Priya Nair",
            phone = "+919876543210",
            role = role,
            doctorId = null,
            signedInAt = signedInAt,
        )
    }

/** runTest whose body and launched children start eagerly, so state flows are observed without a manual clock. */
fun runTestUnconfined(block: suspend kotlinx.coroutines.test.TestScope.() -> Unit) =
    kotlinx.coroutines.test.runTest(UnconfinedTestDispatcher()) { block() }
