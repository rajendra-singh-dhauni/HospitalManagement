// Tests: US-041 (REQ-037, NFR-012)
package com.mobile360.healthcare

import com.mobile360.healthcare.util.AppConfig
import org.junit.Assert.assertTrue
import org.junit.Test

// minSdk, AAB and store checklist are verified by the build (bundleRelease) and device matrix.
class Us041ReleaseReadinessTest {
    @Test fun test_us_041_required_version_is_a_dotted_version_string() {
        assertTrue(Regex("\\d+(\\.\\d+)*").matches(AppConfig.REQUIRED_VERSION))
    }

    @Test fun test_us_041_play_store_uri_is_built_for_the_release_package() {
        assertTrue(AppConfig.playStoreUri("com.mobile360.healthcare").contains("com.mobile360.healthcare"))
    }
}
