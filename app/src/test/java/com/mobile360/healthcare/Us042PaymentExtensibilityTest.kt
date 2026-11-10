// Tests: US-042 (REQ-028)
package com.mobile360.healthcare

import com.mobile360.healthcare.data.AppointmentRepository
import com.mobile360.healthcare.data.model.Outcome
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Us042PaymentExtensibilityTest {
    private interface PaymentStep { suspend fun charge(doctorId: String): Outcome }

    /** A payment step decorates the booking repository; callers and view models are untouched. */
    private class PaidBooking(
        private val inner: AppointmentRepository,
        private val payment: PaymentStep
    ) : AppointmentRepository by inner {
        override suspend fun book(doctorId: String, slotId: String, reason: String): Outcome {
            val paid = payment.charge(doctorId)
            return if (paid.ok) inner.book(doctorId, slotId, reason) else paid
        }
    }

    private fun innerRepo(calls: MutableList<String>): AppointmentRepository = java.lang.reflect.Proxy.newProxyInstance(
        AppointmentRepository::class.java.classLoader, arrayOf(AppointmentRepository::class.java)
    ) { _, method, args ->
        if (method.name == "book") {
            calls += "book"
            @Suppress("UNCHECKED_CAST")
            val cont = args.last() as kotlin.coroutines.Continuation<Any?>
            Outcome.ok("Booked", "a1")
        } else null
    } as AppointmentRepository

    @Test fun test_us_042_payment_step_can_wrap_booking_without_changing_the_interface() = runTest {
        val calls = mutableListOf<String>()
        val repo: AppointmentRepository = PaidBooking(innerRepo(calls), object : PaymentStep {
            override suspend fun charge(doctorId: String) = Outcome.ok("Paid", null)
        })
        assertTrue(repo.book("doc-1", "s1", "checkup").ok)
        assertEquals(listOf("book"), calls)
    }

    @Test fun test_us_042_failed_payment_stops_booking() = runTest {
        val calls = mutableListOf<String>()
        val repo: AppointmentRepository = PaidBooking(innerRepo(calls), object : PaymentStep {
            override suspend fun charge(doctorId: String) = Outcome.fail("Payment failed")
        })
        assertFalse(repo.book("doc-1", "s1", "checkup").ok)
        assertTrue(calls.isEmpty())
    }
}
