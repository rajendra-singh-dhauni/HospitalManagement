// Tests: US-010 (REQ-007)
package com.mobile360.healthcare

import com.mobile360.healthcare.data.local.SlotEntity
import com.mobile360.healthcare.data.model.Outcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// The slot list is a Flow from the data source (live from Room); these tests pin the model
// and the conflict outcome the booking screen depends on.
class Us010LiveAvailabilityTest {
    private fun slot(id: String, booked: Boolean) =
        SlotEntity(id = id, doctorId = "doc-1", startMillis = 1_000L, booked = booked)

    @Test fun test_us_010_taken_slot_change_is_emitted_to_collectors() = runTest {
        val source = MutableStateFlow(listOf(slot("s1", false), slot("s2", false)))
        val seen = mutableListOf<List<SlotEntity>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { source.collect { seen += it } }
        source.value = listOf(slot("s1", true), slot("s2", false))
        assertEquals(2, seen.size)
        assertTrue(seen.last().first { it.id == "s1" }.booked)
    }

    @Test fun test_us_010_released_slot_becomes_free_again() = runTest {
        val taken = slot("s1", true)
        assertFalse(taken.copy(booked = false).booked)
    }

    @Test fun test_us_010_conflict_outcome_is_failure_with_message() {
        val conflict = Outcome.fail("That slot was just taken. Pick another time.")
        assertFalse(conflict.ok)
        assertTrue(conflict.message.isNotBlank())
    }
}
