package com.mobile360.healthcare.navigation

import com.mobile360.healthcare.data.model.Role

object Routes {
    const val WELCOME = "welcome"
    const val PHONE_ENTRY = "phone_entry"
    const val O_T_P_ENTRY = "o_t_p_entry"
    const val PATIENT_HOME = "patient_home"
    const val DOCTOR_SCHEDULE = "doctor_schedule"
    const val DOCTOR_SEARCH = "doctor_search"
    const val DOCTOR_PROFILE_AND_BOOKING = "doctor_profile_and_booking"
    const val APPOINTMENTS = "appointments"
    const val CHAT = "chat"
    const val RECORDS = "records"
    const val PRESCRIPTION_DETAIL = "prescription_detail"
    const val CONSULTATION = "consultation"
    const val AVAILABILITY = "availability"
    const val SETTINGS = "settings"
    const val SECURITY_AND_ACCOUNT = "security_and_account"

    const val ARG_DOCTOR_ID = "doctorId"
    const val ARG_APPOINTMENT_ID = "appointmentId"
    const val ARG_PRESCRIPTION_ID = "prescriptionId"

    const val DOCTOR_PROFILE_AND_BOOKING_PATTERN =
        "$DOCTOR_PROFILE_AND_BOOKING/{$ARG_DOCTOR_ID}?$ARG_APPOINTMENT_ID={$ARG_APPOINTMENT_ID}"
    const val CHAT_PATTERN = "$CHAT?$ARG_APPOINTMENT_ID={$ARG_APPOINTMENT_ID}"
    const val PRESCRIPTION_DETAIL_PATTERN = "$PRESCRIPTION_DETAIL/{$ARG_PRESCRIPTION_ID}"
    const val CONSULTATION_PATTERN = "$CONSULTATION/{$ARG_APPOINTMENT_ID}"

    // appointmentId set means the booking screen reschedules that appointment.
    fun doctorProfileAndBooking(doctorId: String, appointmentId: String? = null): String =
        "$DOCTOR_PROFILE_AND_BOOKING/$doctorId" +
            (appointmentId?.takeIf { it.isNotBlank() }?.let { "?$ARG_APPOINTMENT_ID=$it" } ?: "")

    fun chat(appointmentId: String? = null): String =
        if (appointmentId.isNullOrBlank()) CHAT else "$CHAT?$ARG_APPOINTMENT_ID=$appointmentId"

    fun prescriptionDetail(prescriptionId: String): String = "$PRESCRIPTION_DETAIL/$prescriptionId"
    fun consultation(appointmentId: String): String = "$CONSULTATION/$appointmentId"

    fun homeFor(role: Role?): String = if (role == Role.DOCTOR) DOCTOR_SCHEDULE else PATIENT_HOME
    fun startFor(role: Role?): String = if (role == null) WELCOME else homeFor(role)
}
