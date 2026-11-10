package com.mobile360.healthcare

import androidx.lifecycle.SavedStateHandle
import com.mobile360.healthcare.testutil.BoltMainRule
import com.mobile360.healthcare.testutil.FakeAppointments
import com.mobile360.healthcare.testutil.FakeRecords
import com.mobile360.healthcare.testutil.apptView
import com.mobile360.healthcare.testutil.failOutcome
import com.mobile360.healthcare.testutil.okOutcome
import com.mobile360.healthcare.ui.screens.ConsultationViewModel
import com.mobile360.healthcare.ui.screens.ConsultUi.NotFound
import com.mobile360.healthcare.ui.screens.ConsultUi.Ready
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import com.mobile360.healthcare.testutil.runTestUnconfined as runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Tests: US-017 (REQ-014) - consultation notes linked to the patient record. */
@OptIn(ExperimentalCoroutinesApi::class)
class Us017ConsultationNotesTest {
    @get:Rule val main = BoltMainRule()

    private fun vm(records: FakeRecords, found: Boolean = true): ConsultationViewModel {
        val appts = FakeAppointments().also { if (found) it.found = apptView("a1", "CONFIRMED", 0L) }
        return ConsultationViewModel(SavedStateHandle(mapOf("appointmentId" to "a1")), appts, records)
    }

    @Test fun test_us_017_save_note_is_stored_against_the_appointment() = runTest {
        val records = FakeRecords()
        val v = vm(records)
        backgroundScope.launch { v.noteMsg.collect { } }
        v.setDraft("  Findings stable  ")
        v.saveNote()
        assertEquals(listOf("a1" to "Findings stable"), records.savedNotes)
        assertEquals(true, v.noteMsg.value?.ok)
    }

    @Test fun test_us_017_empty_note_is_a_validation_error() = runTest {
        val records = FakeRecords()
        val v = vm(records)
        backgroundScope.launch { v.noteMsg.collect { } }
        v.setDraft("   ")
        v.saveNote()
        assertTrue(records.savedNotes.isEmpty())
        assertEquals("Write a note before saving.", v.noteMsg.value?.text)
    }

    @Test fun test_us_017_failed_save_reports_error_then_retry_succeeds() = runTest {
        val records = FakeRecords().also { it.nextOutcome = failOutcome("Could not save.") }
        val v = vm(records)
        backgroundScope.launch { v.noteMsg.collect { } }
        v.setDraft("Note")
        v.saveNote()
        assertEquals(false, v.noteMsg.value?.ok)
        records.nextOutcome = okOutcome()
        v.saveNote()
        assertEquals(true, v.noteMsg.value?.ok)
        assertEquals(1, records.savedNotes.size)
    }

    @Test fun test_us_017_existing_note_is_readable_by_an_authorized_doctor() = runTest {
        val records = FakeRecords()
        records.saveNote("a1", "Earlier finding")
        val v = vm(records)
        backgroundScope.launch { v.savedNote.collect { } }
        assertEquals("Earlier finding", v.savedNote.value)
    }

    @Test fun test_us_017_consultation_of_a_foreign_patient_is_not_found() = runTest {
        val v = vm(FakeRecords(), found = false)
        assertTrue(v.ui.value is NotFound)
    }

    @Test fun test_us_017_own_patient_consultation_is_ready() = runTest {
        val v = vm(FakeRecords())
        assertTrue(v.ui.value is Ready)
    }
}
