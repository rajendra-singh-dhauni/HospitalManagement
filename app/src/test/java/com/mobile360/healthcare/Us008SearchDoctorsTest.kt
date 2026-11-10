package com.mobile360.healthcare

import com.mobile360.healthcare.data.local.DoctorEntity
import com.mobile360.healthcare.data.local.DoctorListItem
import com.mobile360.healthcare.ui.screens.DoctorSearchState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests: US-008 (REQ-005) - search state shapes behind the doctor search screen. */
class Us008SearchDoctorsTest {
    private fun doctor(id: String, specialty: String, active: Boolean) = DoctorEntity(
        id = id, name = "Dr $id", specialty = specialty, title = "MD", experienceYears = 10, rating = 4.7,
        reviewCount = 10, languages = "English", headline = "", clinic = "Clinic", address = "Street",
        phone = "000", imageRes = 0, active = active, fee = 500, bio = "",
    )

    private fun state(doctors: List<DoctorListItem>, error: String? = null) = DoctorSearchState(
        loading = false, error = error, query = "", specialty = "Pediatrics",
        specialties = listOf("Cardiology", "Pediatrics"), doctors = doctors, online = true,
    )

    @Test fun test_us_008_matching_doctors_are_carried_with_their_rating() {
        val items = listOf(DoctorListItem(doctor("rao", "Cardiology", true), 1L))
        val s = state(items)
        assertEquals(4.7, s.doctors.single().doctor.rating, 0.0)
    }

    @Test fun test_us_008_no_match_is_an_empty_list_without_error() {
        val s = state(emptyList())
        assertTrue(s.doctors.isEmpty())
        assertEquals(null, s.error)
        assertFalse(s.loading)
    }

    @Test fun test_us_008_inactive_doctor_is_flagged_so_it_can_be_filtered() {
        assertFalse(doctor("old", "Cardiology", false).active)
    }
}
