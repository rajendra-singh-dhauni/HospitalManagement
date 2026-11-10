package com.mobile360.healthcare.data

import com.mobile360.healthcare.data.local.AppointmentView
import com.mobile360.healthcare.data.local.AvailabilityEntity
import com.mobile360.healthcare.data.local.DoctorEntity
import com.mobile360.healthcare.data.local.DoctorListItem
import com.mobile360.healthcare.data.local.MedicationEntity
import com.mobile360.healthcare.data.local.MessageEntity
import com.mobile360.healthcare.data.local.NoteEntity
import com.mobile360.healthcare.data.local.PrescriptionView
import com.mobile360.healthcare.data.local.SlotEntity
import com.mobile360.healthcare.data.local.ThreadView
import com.mobile360.healthcare.data.local.TimelineEntity
import com.mobile360.healthcare.data.model.AlertKind
import com.mobile360.healthcare.data.model.AlertPrefs
import com.mobile360.healthcare.data.model.CodeRequestResult
import com.mobile360.healthcare.data.model.MedicationDraft
import com.mobile360.healthcare.data.model.Outcome
import com.mobile360.healthcare.data.model.Session
import com.mobile360.healthcare.data.model.VerifyResult
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import com.mobile360.healthcare.data.model.BiometricSignInResult

// Every interface is backed by the local Room database seeded with sample data.
// A real API source can replace an implementation through DI (RepositoryModule) without UI changes.

interface AuthRepository {
    val session: Flow<Session?>
    suspend fun requestCode(phone: String): CodeRequestResult
    suspend fun verifyCode(code: String): VerifyResult
    suspend fun signOut()

    // CR-001: biometric opt-in; defaults keep existing implementations compiling
    suspend fun isBiometricEnrolled(): Boolean = false
    suspend fun enableBiometric(): Boolean = false
    suspend fun disableBiometric() {}
    suspend fun signInWithBiometric(): BiometricSignInResult = BiometricSignInResult.NOT_ENROLLED
}

interface AccountRepository {
    val alertPrefs: Flow<AlertPrefs>
    suspend fun setAlert(kind: AlertKind, enabled: Boolean)
    suspend fun reAuthenticate(code: String): Outcome
    suspend fun changeNumber(newPhone: String, code: String): Outcome
    suspend fun signOutOtherDevices(): Outcome
    suspend fun deleteAccount(code: String): Outcome
}

interface DoctorRepository {
    fun specialties(): List<String>
    fun search(query: String, specialty: String): Flow<List<DoctorListItem>>
    fun doctor(id: String): Flow<DoctorEntity?>
    fun slots(doctorId: String, date: LocalDate): Flow<List<SlotEntity>>
}

interface AppointmentRepository {
    fun mine(): Flow<List<AppointmentView>>
    fun next(): Flow<AppointmentView?>
    fun todayForDoctor(): Flow<List<AppointmentView>>
    suspend fun get(id: String): AppointmentView?
    suspend fun book(doctorId: String, slotId: String, reason: String): Outcome
    suspend fun reschedule(appointmentId: String, newSlotId: String): Outcome
    suspend fun cancel(appointmentId: String): Outcome
    suspend fun accept(appointmentId: String): Outcome
    suspend fun reject(appointmentId: String, reason: String): Outcome
}

interface RecordsRepository {
    fun prescriptions(): Flow<List<PrescriptionView>>
    fun prescription(id: String): Flow<PrescriptionView?>
    fun medications(prescriptionId: String): Flow<List<MedicationEntity>>
    fun prescriptionForAppointment(appointmentId: String): Flow<PrescriptionView?>
    fun timeline(fromMillis: Long?, toMillis: Long?): Flow<List<TimelineEntity>>
    fun note(appointmentId: String): Flow<NoteEntity?>
    suspend fun saveNote(appointmentId: String, text: String): Outcome
    suspend fun issuePrescription(appointmentId: String, medications: List<MedicationDraft>, advice: String): Outcome
}

interface ChatRepository {
    fun threads(): Flow<List<ThreadView>>
    fun messages(appointmentId: String): Flow<List<MessageEntity>>
    suspend fun send(appointmentId: String, text: String, online: Boolean): Outcome
    suspend fun retry(messageId: String, online: Boolean): Outcome
    suspend fun flushQueued()
}

interface AvailabilityRepository {
    fun observe(): Flow<List<AvailabilityEntity>>
    suspend fun conflictCount(rows: List<AvailabilityEntity>): Int
    suspend fun save(rows: List<AvailabilityEntity>): Outcome
}
