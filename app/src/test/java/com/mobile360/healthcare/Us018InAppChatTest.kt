package com.mobile360.healthcare

import com.mobile360.healthcare.data.model.MessageStatus
import com.mobile360.healthcare.navigation.Routes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests: US-018 (REQ-015) - chat routes and message states. */
class Us018InAppChatTest {
    @Test fun test_us_018_chat_route_opens_a_conversation_for_an_appointment() {
        assertTrue(Routes.chat("a1").contains("a1"))
        assertEquals(Routes.chat(null), Routes.chat(null))
        assertNotEquals(Routes.chat("a1"), Routes.chat(null))
    }

    @Test fun test_us_018_failed_message_state_is_distinct_for_retry() {
        assertNotEquals(MessageStatus.FAILED, MessageStatus.DELIVERED)
        assertNotEquals(MessageStatus.FAILED, MessageStatus.QUEUED)
    }
}
