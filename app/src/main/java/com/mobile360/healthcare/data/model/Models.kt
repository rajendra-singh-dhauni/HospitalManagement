package com.mobile360.healthcare.data.model

enum class Role { PATIENT, DOCTOR }

data class Session(
    val userId: String,
    val name: String,
    val phone: String,
    val role: Role,
    val doctorId: String?,
    val signedInAt: Long
) {
    // Sender id used in chat messages: the doctor directory id for doctors, the account id for patients.
    val chatId: String get() = doctorId ?: userId
}

enum class AlertKind { APPOINTMENTS, REMINDERS, REPORTS_AND_MESSAGES }

data class AlertPrefs(
    val appointments: Boolean = true,
    val reminders: Boolean = true,
    val reportsAndMessages: Boolean = true
)

data class Outcome(val ok: Boolean, val message: String, val id: String? = null) {
    companion object {
        fun ok(message: String, id: String? = null) = Outcome(true, message, id)
        fun fail(message: String) = Outcome(false, message)
        fun denied() = Outcome(false, "You do not have access to this.")
    }
}

sealed interface CodeRequestResult {
    data class Issued(val maskedPhone: String, val expiresAtMillis: Long) : CodeRequestResult
    data object InvalidNumber : CodeRequestResult
    data class Locked(val minutesLeft: Int) : CodeRequestResult
}

sealed interface VerifyResult {
    data class Success(val role: Role) : VerifyResult
    data class Wrong(val attemptsLeft: Int) : VerifyResult
    data object Expired : VerifyResult
    data class Locked(val minutesLeft: Int) : VerifyResult
    data object NoRequest : VerifyResult
}

data class MedicationDraft(
    val name: String,
    val category: String,
    val dosage: String,
    val frequency: String,
    val supplyDays: Int,
    val refills: Int,
    val note: String
)

object AppointmentStatus {
    const val CONFIRMED = "CONFIRMED"
    const val PENDING = "PENDING"
    const val CANCELLED = "CANCELLED"
    const val COMPLETED = "COMPLETED"
}

object MessageStatus {
    const val QUEUED = "QUEUED"
    const val FAILED = "FAILED"
    const val DELIVERED = "DELIVERED"
    const val READ = "READ"
}

object TimelineType {
    const val PRESCRIPTION = "PRESCRIPTION"
    const val CONSULTATION = "CONSULTATION"
    const val LABORATORY = "LABORATORY"
    const val APPOINTMENT = "APPOINTMENT"
}

// CR-001: outcome of restoring a session after a biometric check
enum class BiometricSignInResult { SIGNED_IN, NOT_ENROLLED, ACCOUNT_MISSING }
