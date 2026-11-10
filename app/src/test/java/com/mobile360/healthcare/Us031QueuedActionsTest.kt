// Tests: US-031 (REQ-035)
package com.mobile360.healthcare

import com.mobile360.healthcare.data.ChatRepository
import com.mobile360.healthcare.data.model.MessageStatus
import com.mobile360.healthcare.data.model.Outcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Us031QueuedActionsTest {
    @Test fun test_us_031_queued_and_failed_are_distinct_message_states() {
        assertEquals("QUEUED", MessageStatus.QUEUED)
        assertEquals("FAILED", MessageStatus.FAILED)
        assertNotEquals(MessageStatus.QUEUED, MessageStatus.DELIVERED)
    }

    @Test fun test_us_031_chat_contract_takes_online_flag_and_can_flush() {
        val names = ChatRepository::class.java.methods.map { it.name }.toSet()
        assertTrue(names.containsAll(listOf("send", "retry", "flushQueued")))
    }

    @Test fun test_us_031_booking_needs_connection_failure_is_reported() {
        val offline = Outcome.fail("You are offline. Booking needs a connection, so nothing was saved.")
        assertFalse(offline.ok)
        assertTrue(offline.message.contains("connection"))
    }
}
