package com.mobile360.healthcare.ui.screens

import com.mobile360.healthcare.data.model.CodeRequestResult

// Remembers the last issued demo code request so the OTP screen can show it and resend.
object PendingCode {
    @Volatile var phoneDigits: String? = null
    @Volatile var issued: CodeRequestResult.Issued? = null

    fun record(digits: String, result: CodeRequestResult.Issued) {
        phoneDigits = digits
        issued = result
    }
}
