// Tests: US-036 (REQ-036)
package com.mobile360.healthcare

import com.mobile360.healthcare.util.AppConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Us036UpdatePromptTest {
    @Test fun test_us_036_no_prompt_when_required_version_is_not_newer() {
        assertFalse(AppConfig.isUpdateRequired())
    }

    @Test fun test_us_036_play_store_link_targets_the_app_package() {
        val uri = AppConfig.playStoreUri("com.mobile360.healthcare")
        assertTrue(uri.contains("com.mobile360.healthcare"))
        assertTrue(uri.startsWith("market://") || uri.startsWith("https://play.google.com"))
    }
}
