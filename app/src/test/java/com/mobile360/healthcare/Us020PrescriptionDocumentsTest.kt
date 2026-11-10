package com.mobile360.healthcare

import com.mobile360.healthcare.data.model.MedicationDraft
import com.mobile360.healthcare.testutil.*
import com.mobile360.healthcare.ui.screens.recordsRxLines
import com.mobile360.healthcare.ui.screens.recordsRxText
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Tests: US-020 (REQ-017)
class Us020PrescriptionDocumentsTest {
    private val rx = rxView("r1", 1_700_000_000_000L)
    private val meds = listOf(medication("m1", "r1"))

    @Test fun test_us_020_document_text_lists_doctor_and_medication() {
        val text = recordsRxText(rx, meds)
        assertTrue(text.contains("Dr. Arun Rao"))
        assertTrue(text.contains("Atorvastatin"))
        assertTrue(text.contains("RX-r1"))
    }

    @Test fun test_us_020_document_lines_are_never_empty_without_medications() {
        assertTrue(recordsRxLines(rx, emptyList()).isNotEmpty())
    }

    @Test fun test_us_020_doctor_issue_attaches_document_through_repository() = runTest {
        val repo = StubRecords()
        val out = repo.issuePrescription("a1", listOf(MedicationDraft("X", "Cat", "5 mg", "daily", 10, 0, "")), "Rest")
        assertTrue(out.ok)
        assertEquals(1, repo.issued)
    }
}
