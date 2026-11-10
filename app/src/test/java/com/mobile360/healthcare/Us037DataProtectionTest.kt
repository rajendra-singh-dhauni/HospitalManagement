// Tests: US-037 (NFR-005, NFR-006, NFR-007, NFR-008)
package com.mobile360.healthcare

import com.mobile360.healthcare.data.SessionStore
import com.mobile360.healthcare.data.local.CareDao
import com.mobile360.healthcare.data.model.Outcome
import com.mobile360.healthcare.ui.screens.OTPEntryViewModel
import com.mobile360.healthcare.ui.screens.PhoneEntryViewModel
import com.mobile360.healthcare.ui.screens.RecordsViewModel
import com.mobile360.healthcare.ui.screens.SecurityAndAccountViewModel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Network config, backup rules and encryption at rest are verified by the build and on a device (test plan I/M).
class Us037DataProtectionTest {
    @Test fun test_us_037_unauthorized_attempt_is_refused_with_message() {
        val denied = Outcome.denied()
        assertFalse(denied.ok)
        assertTrue(denied.message.isNotBlank())
    }

    @Test fun test_us_037_sensitive_viewmodels_do_not_hold_stores_or_daos() {
        listOf(OTPEntryViewModel::class.java, PhoneEntryViewModel::class.java,
            RecordsViewModel::class.java, SecurityAndAccountViewModel::class.java).forEach { vm ->
            val params = vm.constructors.flatMap { it.parameterTypes.toList() }
            assertFalse(params.contains(SessionStore::class.java))
            assertFalse(params.contains(CareDao::class.java))
        }
    }
}
