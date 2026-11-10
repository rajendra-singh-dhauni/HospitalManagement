package com.mobile360.healthcare

import com.mobile360.healthcare.util.AppConfig
import org.junit.Assert.assertTrue
import org.junit.Test

// Tests: US-033 (REQ-038)
class Us033HelpSupportTest {
    @Test fun test_us_033_support_contact_number_is_configured() {
        assertTrue(AppConfig.SUPPORT_PHONE.toString().isNotBlank())
    }
}
