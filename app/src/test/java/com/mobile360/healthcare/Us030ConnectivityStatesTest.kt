// Tests: US-030 (REQ-034)
package com.mobile360.healthcare

import com.mobile360.healthcare.data.ConnectivityObserver
import com.mobile360.healthcare.ui.screens.DoctorScheduleState
import com.mobile360.healthcare.ui.screens.PatientHomeState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Us030ConnectivityStatesTest {
    @Test fun test_us_030_patient_home_state_models_offline_and_error() {
        val offline = PatientHomeState(loading = false, error = null, firstName = "Priya", next = null,
            activePrescriptions = 0, threads = 0, online = false)
        assertFalse(offline.online)
        val failed = offline.copy(online = true, error = "We could not load your home screen.")
        assertNotNull(failed.error)
    }

    @Test fun test_us_030_doctor_schedule_default_state_is_loading_without_error() {
        val s = DoctorScheduleState()
        assertTrue(s.loading)
        assertNull(s.error)
    }

    @Test fun test_us_030_connectivity_is_observable_as_a_flow() {
        val m = ConnectivityObserver::class.java.methods.map { it.name }
        assertTrue(m.contains("getOnline"))
        assertTrue(m.contains("isOnlineNow"))
    }
}
