// Tests: US-023 (REQ-020)
package com.mobile360.healthcare

import com.mobile360.healthcare.data.model.Outcome
import com.mobile360.healthcare.data.AuthRepository
import com.mobile360.healthcare.data.AccountRepository
import com.mobile360.healthcare.data.RecordsRepository
import com.mobile360.healthcare.data.local.CareDao
import com.mobile360.healthcare.ui.screens.PrescriptionDetailViewModel
import com.mobile360.healthcare.ui.screens.RecordsViewModel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Us023SecureRecordStorageTest {
    @Test fun test_us_023_unauthorized_access_outcome_is_refused() {
        val denied = Outcome.denied()
        assertFalse(denied.ok)
        assertTrue(denied.message.isNotBlank())
    }

    @Test fun test_us_023_record_viewmodels_never_touch_the_dao_directly() {
        listOf(RecordsViewModel::class.java, PrescriptionDetailViewModel::class.java).forEach { vm ->
            val params = vm.constructors.flatMap { it.parameterTypes.toList() }
            assertFalse("${vm.simpleName} must go through repositories", params.any { it == CareDao::class.java })
            assertTrue(params.any { it == RecordsRepository::class.java || it == AccountRepository::class.java || it == AuthRepository::class.java })
        }
    }
}
