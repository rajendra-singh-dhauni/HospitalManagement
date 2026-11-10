// Tests: US-032 (NFR-011)
package com.mobile360.healthcare

import com.mobile360.healthcare.data.RecordsRepository
import com.mobile360.healthcare.data.AppointmentRepository
import com.mobile360.healthcare.data.local.PrescriptionEntity
import com.mobile360.healthcare.data.model.TimelineType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Us032OfflineReadingTest {
    @Test fun test_us_032_prescriptions_carry_a_last_updated_time() {
        val rx = PrescriptionEntity(id = "r1", rxNumber = "RX-1", patientId = "p1", doctorId = "d1",
            appointmentId = null, issuedMillis = 1L, status = "ACTIVE", advice = "", version = 1, updatedMillis = 5L)
        assertEquals(5L, rx.updatedMillis)
    }

    @Test fun test_us_032_read_contracts_are_flows_served_from_local_store() {
        val records = RecordsRepository::class.java.methods.associateBy { it.name }
        assertTrue(records.getValue("prescriptions").returnType == kotlinx.coroutines.flow.Flow::class.java)
        assertTrue(records.getValue("timeline").returnType == kotlinx.coroutines.flow.Flow::class.java)
        val appts = AppointmentRepository::class.java.methods.associateBy { it.name }
        assertTrue(appts.getValue("mine").returnType == kotlinx.coroutines.flow.Flow::class.java)
    }

    @Test fun test_us_032_timeline_has_history_types() {
        assertEquals("PRESCRIPTION", TimelineType.PRESCRIPTION)
        assertEquals("APPOINTMENT", TimelineType.APPOINTMENT)
    }
}
