package com.mobile360.healthcare

import androidx.lifecycle.SavedStateHandle
import com.mobile360.healthcare.data.model.MedicationDraft
import com.mobile360.healthcare.testutil.BoltMainRule
import com.mobile360.healthcare.testutil.FakeAppointments
import com.mobile360.healthcare.testutil.FakeRecords
import com.mobile360.healthcare.testutil.apptView
import com.mobile360.healthcare.testutil.failOutcome
import com.mobile360.healthcare.ui.screens.ConsultationViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Tests: US-016 (REQ-013) - issue a prescription for a consultation. */
@OptIn(ExperimentalCoroutinesApi::class)
class Us016PrescriptionTest {
    @get:Rule val main = BoltMainRule()

    private val med = MedicationDraft("Atorvastatin", "Statin", "10 mg", "Daily", 30, 1, "")

    private fun vm(records: FakeRecords): ConsultationViewModel {
        val appts = FakeAppointments().also { it.found = apptView("a1", "CONFIRMED", 0L) }
        return ConsultationViewModel(SavedStateHandle(mapOf("appointmentId" to "a1")), appts, records)
    }

    @Test fun test_us_016_issue_links_prescription_to_the_consultation() = runTest {
        val records = FakeRecords()
        val v = vm(records)
        backgroundScope.launch { v.rxMsg.collect { } }
        v.addMedication(med)
        v.setAdvice("After meals")
        v.issue()
        val (appointmentId, meds, advice) = records.issued.single()
        assertEquals("a1", appointmentId)
        assertEquals(listOf(med), meds)
        assertEquals("After meals", advice)
        assertEquals(true, v.rxMsg.value?.ok)
    }

    @Test fun test_us_016_empty_medications_is_a_validation_error() = runTest {
        val records = FakeRecords()
        val v = vm(records)
        backgroundScope.launch { v.rxMsg.collect { } }
        v.issue()
        assertTrue(records.issued.isEmpty())
        assertEquals(false, v.rxMsg.value?.ok)
        assertEquals("Add at least one medication first.", v.rxMsg.value?.text)
    }

    @Test fun test_us_016_removing_the_only_medication_blocks_issue() = runTest {
        val records = FakeRecords()
        val v = vm(records)
        v.addMedication(med)
        v.removeMedication(0)
        v.issue()
        assertTrue(records.issued.isEmpty())
    }

    @Test fun test_us_016_repository_refusal_is_reported_not_swallowed() = runTest {
        val records = FakeRecords().also { it.nextOutcome = failOutcome("You do not have access to this.") }
        val v = vm(records)
        backgroundScope.launch { v.rxMsg.collect { } }
        v.addMedication(med)
        v.issue()
        assertTrue(records.issued.isEmpty())
        assertNotNull(v.rxMsg.value)
        assertFalse(v.rxMsg.value!!.ok)
    }
}
