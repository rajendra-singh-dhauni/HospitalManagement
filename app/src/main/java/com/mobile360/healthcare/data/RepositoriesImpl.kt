package com.mobile360.healthcare.data

import android.util.Log
import androidx.room.withTransaction
import com.mobile360.healthcare.data.local.AccountEntity
import com.mobile360.healthcare.data.local.AppDatabase
import com.mobile360.healthcare.data.local.AppointmentEntity
import com.mobile360.healthcare.data.local.AppointmentView
import com.mobile360.healthcare.data.local.AvailabilityEntity
import com.mobile360.healthcare.data.local.CareDao
import com.mobile360.healthcare.data.local.DoctorEntity
import com.mobile360.healthcare.data.local.DoctorListItem
import com.mobile360.healthcare.data.local.MedicationEntity
import com.mobile360.healthcare.data.local.MessageEntity
import com.mobile360.healthcare.data.local.NoteEntity
import com.mobile360.healthcare.data.local.PrescriptionEntity
import com.mobile360.healthcare.data.local.PrescriptionView
import com.mobile360.healthcare.data.local.SlotEntity
import com.mobile360.healthcare.data.local.ThreadView
import com.mobile360.healthcare.data.local.TimelineEntity
import com.mobile360.healthcare.data.model.AlertKind
import com.mobile360.healthcare.data.model.AlertPrefs
import com.mobile360.healthcare.data.model.AppointmentStatus
import com.mobile360.healthcare.data.model.CodeRequestResult
import com.mobile360.healthcare.data.model.MedicationDraft
import com.mobile360.healthcare.data.model.MessageStatus
import com.mobile360.healthcare.data.model.Outcome
import com.mobile360.healthcare.data.model.Role
import com.mobile360.healthcare.data.model.Session
import com.mobile360.healthcare.data.model.TimelineType
import com.mobile360.healthcare.data.model.VerifyResult
import com.mobile360.healthcare.util.AppConfig
import com.mobile360.healthcare.util.Times
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "CareConnect"

private fun refuse(action: String): Outcome {
    Log.w(TAG, "Refused unauthorized action: $action")
    return Outcome.denied()
}

private fun newId(prefix: String) = prefix + UUID.randomUUID().toString().take(8)

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val dao: CareDao,
    private val store: SessionStore,
    private val seeder: Seeder
) : AuthRepository {
    override val session: Flow<Session?> = store.session

    private var pendingPhone: String? = null
    private var issuedAt = 0L
    private var failures = 0
    private var lockedUntil = 0L

    private fun minutesLeft(now: Long) = (((lockedUntil - now) + 59_999L) / 60_000L).toInt().coerceAtLeast(1)

    override suspend fun requestCode(phone: String): CodeRequestResult {
        val digits = phone.filter { it.isDigit() }
        if (digits.length != 10) return CodeRequestResult.InvalidNumber
        val now = System.currentTimeMillis()
        if (now < lockedUntil) return CodeRequestResult.Locked(minutesLeft(now))
        seeder.ensureSeeded()
        pendingPhone = digits
        issuedAt = now
        return CodeRequestResult.Issued("${AppConfig.COUNTRY_CODE} ••• ••• ${digits.takeLast(4)}", now + AppConfig.CODE_VALID_MS)
    }

    override suspend fun verifyCode(code: String): VerifyResult {
        val phone = pendingPhone ?: return VerifyResult.NoRequest
        val now = System.currentTimeMillis()
        if (now < lockedUntil) return VerifyResult.Locked(minutesLeft(now))
        if (now - issuedAt > AppConfig.CODE_VALID_MS) return VerifyResult.Expired
        if (code != AppConfig.DEMO_CODE) {
            failures++
            if (failures >= AppConfig.MAX_ATTEMPTS) {
                failures = 0
                pendingPhone = null
                lockedUntil = now + AppConfig.LOCK_MS
                return VerifyResult.Locked(minutesLeft(now))
            }
            return VerifyResult.Wrong(AppConfig.MAX_ATTEMPTS - failures)
        }
        // Role comes from the account of this number; an unknown number registers a new patient.
        val account = dao.accountByPhone(phone) ?: AccountEntity(
            id = "p_$phone", phone = phone, name = "New patient", role = "PATIENT",
            age = 0, sex = "", imageRes = 0, doctorId = null
        ).also { dao.insertAccount(it) }
        store.save(account)
        failures = 0
        pendingPhone = null
        return VerifyResult.Success(Role.valueOf(account.role))
    }

override suspend fun signOut() = store.clearSession()

        // CR-001: biometric opt-in and session restore
        override suspend fun isBiometricEnrolled(): Boolean = store.biometricAccountId() != null

        override suspend fun enableBiometric(): Boolean {
            val current = session.first() ?: return false
            store.saveBiometricAccountId(current.userId)
            return true
        }

        override suspend fun disableBiometric() = store.clearBiometricAccountId()

        override suspend fun signInWithBiometric(): com.mobile360.healthcare.data.model.BiometricSignInResult {
            val id = store.biometricAccountId()
                ?: return com.mobile360.healthcare.data.model.BiometricSignInResult.NOT_ENROLLED
            val account = dao.accountById(id)
            if (account == null) {
                store.clearBiometricAccountId()
                return com.mobile360.healthcare.data.model.BiometricSignInResult.ACCOUNT_MISSING
            }
            store.save(account)
            return com.mobile360.healthcare.data.model.BiometricSignInResult.SIGNED_IN
        }
}

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val db: AppDatabase,
    private val dao: CareDao,
    private val store: SessionStore
) : AccountRepository {
    override val alertPrefs: Flow<AlertPrefs> = store.alertPrefs

    override suspend fun setAlert(kind: AlertKind, enabled: Boolean) = store.setAlert(kind, enabled)

    override suspend fun reAuthenticate(code: String): Outcome =
        if (code == AppConfig.DEMO_CODE) Outcome.ok("Verified.") else Outcome.fail("That code is not correct. The action was not performed.")

    override suspend fun changeNumber(newPhone: String, code: String): Outcome {
        val digits = newPhone.filter { it.isDigit() }
        val s = store.session.first() ?: return refuse("changeNumber")
        if (digits.length != 10) return Outcome.fail("Enter a valid 10-digit mobile number.")
        if (code != AppConfig.DEMO_CODE) return Outcome.fail("That code is not correct. Your number was not changed.")
        val existing = dao.accountByPhone(digits)
        if (existing != null && existing.id != s.userId) return Outcome.fail("That number is already in use.")
        dao.updatePhone(s.userId, digits)
        store.updatePhone(digits)
        return Outcome.ok("Mobile number updated.")
    }

    override suspend fun signOutOtherDevices(): Outcome {
        delay(AppConfig.MOCK_LATENCY_MS)
        return Outcome.ok("Other devices were signed out (local demo service).")
    }

    override suspend fun deleteAccount(code: String): Outcome {
        val s = store.session.first() ?: return refuse("deleteAccount")
        if (code != AppConfig.DEMO_CODE) return Outcome.fail("That code is not correct. Nothing was deleted.")
        db.withTransaction {
            dao.deleteMessagesOfPatient(s.userId)
            dao.deleteNotesOfPatient(s.userId)
            dao.deleteMedicationsOfPatient(s.userId)
            dao.deletePrescriptionsOfPatient(s.userId)
            dao.deleteTimelineOfPatient(s.userId)
            dao.deleteAppointmentsOfPatient(s.userId)
            dao.deleteAccount(s.userId)
        }
        store.clearSession()
        return Outcome.ok("Deletion request submitted. Local data was cleared.").also { store.clearBiometricAccountId() /* CR-001 */ }
    }

        // CR-002
        override val darkTheme: Flow<Boolean> = store.darkTheme

        override suspend fun setDarkTheme(enabled: Boolean) = store.saveDarkTheme(enabled)
}

@Singleton
class DoctorRepositoryImpl @Inject constructor(private val dao: CareDao) : DoctorRepository {
    override fun specialties(): List<String> = listOf("Cardiology", "Dermatology", "Pediatrics", "Orthopedics")

    override fun search(query: String, specialty: String): Flow<List<DoctorListItem>> =
        dao.searchDoctors(query.trim(), specialty, System.currentTimeMillis())

    override fun doctor(id: String): Flow<DoctorEntity?> = dao.observeDoctor(id)

    override fun slots(doctorId: String, date: LocalDate): Flow<List<SlotEntity>> =
        dao.observeSlots(doctorId, Times.startOfDay(date), Times.startOfDay(date.plusDays(1)))
}

@Singleton
class AppointmentRepositoryImpl @Inject constructor(
    private val db: AppDatabase,
    private val dao: CareDao,
    private val store: SessionStore
) : AppointmentRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun mine(): Flow<List<AppointmentView>> = store.session.flatMapLatest { s ->
        when {
            s == null -> flowOf(emptyList())
            s.role == Role.DOCTOR -> dao.observeForDoctor(s.doctorId.orEmpty())
            else -> dao.observeForPatient(s.userId)
        }
    }

    override fun next(): Flow<AppointmentView?> = mine().map { list ->
        val now = System.currentTimeMillis()
        list.filter { it.startMillis > now && (it.status == AppointmentStatus.CONFIRMED || it.status == AppointmentStatus.PENDING) }
            .minByOrNull { it.startMillis }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun todayForDoctor(): Flow<List<AppointmentView>> = store.session.flatMapLatest { s ->
        if (s?.role == Role.DOCTOR) dao.observeForDoctorBetween(s.doctorId.orEmpty(), Times.startOfDay(0), Times.startOfDay(1))
        else flowOf(emptyList())
    }

    override suspend fun get(id: String): AppointmentView? {
        val s = store.session.first() ?: return null
        val a = dao.appointmentView(id) ?: return null
        val allowed = if (s.role == Role.DOCTOR) a.doctorId == s.doctorId else a.patientId == s.userId
        if (!allowed) { refuse("readAppointment"); return null }
        return a
    }

    private suspend fun patient(): Session? = store.session.first()?.takeIf { it.role == Role.PATIENT }
    private suspend fun doctor(): Session? = store.session.first()?.takeIf { it.role == Role.DOCTOR }

    override suspend fun book(doctorId: String, slotId: String, reason: String): Outcome {
        val s = patient() ?: return refuse("book")
        delay(AppConfig.MOCK_LATENCY_MS)
        return db.withTransaction {
            val slot = dao.slot(slotId)
            when {
                slot == null || slot.doctorId != doctorId || slot.startMillis <= System.currentTimeMillis() ->
                    Outcome.fail("That slot is no longer available. Please choose another slot.")
                dao.claimSlot(slotId) == 0 ->
                    Outcome.fail("That slot was just taken. Please choose another slot.")
                else -> {
                    val id = newId("a_")
                    dao.insertAppointment(
                        AppointmentEntity(
                            id = id, patientId = s.userId, doctorId = doctorId, slotId = slotId,
                            startMillis = slot.startMillis, status = AppointmentStatus.PENDING,
                            reason = reason.ifBlank { "Consultation" }
                        )
                    )
                    Outcome.ok("Booking requested. The doctor will confirm shortly.", id)
                }
            }
        }
    }

    override suspend fun reschedule(appointmentId: String, newSlotId: String): Outcome {
        val s = patient() ?: return refuse("reschedule")
        delay(AppConfig.MOCK_LATENCY_MS)
        return db.withTransaction {
            val a = dao.appointment(appointmentId)
            val slot = dao.slot(newSlotId)
            when {
                a == null || a.patientId != s.userId -> refuse("reschedule")
                a.status != AppointmentStatus.CONFIRMED && a.status != AppointmentStatus.PENDING ->
                    Outcome.fail("Only upcoming appointments can be moved.")
                a.startMillis - System.currentTimeMillis() <= AppConfig.RESCHEDULE_CUTOFF_MS ->
                    Outcome.fail("Rescheduling is only available more than 2 hours before the visit.")
                slot == null || slot.doctorId != a.doctorId || slot.startMillis <= System.currentTimeMillis() ->
                    Outcome.fail("That slot is no longer available. Please choose another slot.")
                dao.claimSlot(newSlotId) == 0 -> Outcome.fail("That slot was just taken. Please choose another slot.")
                else -> {
                    a.slotId?.let { dao.setSlotBooked(it, false) }
                    dao.updateAppointmentSlot(a.id, newSlotId, slot.startMillis)
                    Outcome.ok("Appointment moved. The doctor was notified.", a.id)
                }
            }
        }
    }

    override suspend fun cancel(appointmentId: String): Outcome {
        val s = patient() ?: return refuse("cancel")
        delay(AppConfig.MOCK_LATENCY_MS)
        return db.withTransaction {
            val a = dao.appointment(appointmentId)
            when {
                a == null || a.patientId != s.userId -> refuse("cancel")
                a.status != AppointmentStatus.CONFIRMED && a.status != AppointmentStatus.PENDING ->
                    Outcome.fail("Only upcoming appointments can be cancelled.")
                else -> {
                    a.slotId?.let { dao.setSlotBooked(it, false) }
                    dao.updateAppointmentStatus(a.id, AppointmentStatus.CANCELLED, "Cancelled by patient")
                    Outcome.ok("Appointment cancelled. The doctor was notified.", a.id)
                }
            }
        }
    }

    override suspend fun accept(appointmentId: String): Outcome {
        val s = doctor() ?: return refuse("accept")
        val a = dao.appointment(appointmentId)
        if (a == null || a.doctorId != s.doctorId) return refuse("accept")
        dao.updateAppointmentStatus(a.id, AppointmentStatus.CONFIRMED, "")
        return Outcome.ok("Appointment accepted. The patient was notified.", a.id)
    }

    override suspend fun reject(appointmentId: String, reason: String): Outcome {
        val s = doctor() ?: return refuse("reject")
        return db.withTransaction {
            val a = dao.appointment(appointmentId)
            if (a == null || a.doctorId != s.doctorId) {
                refuse("reject")
            } else {
                a.slotId?.let { dao.setSlotBooked(it, false) }
                val note = if (reason.isBlank()) "Declined by doctor" else "Declined by doctor: ${reason.trim()}"
                dao.updateAppointmentStatus(a.id, AppointmentStatus.CANCELLED, note)
                Outcome.ok("Request rejected. The patient was notified.", a.id)
            }
        }
    }
}

@Singleton
class RecordsRepositoryImpl @Inject constructor(
    private val db: AppDatabase,
    private val dao: CareDao,
    private val store: SessionStore
) : RecordsRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun prescriptions(): Flow<List<PrescriptionView>> = store.session.flatMapLatest { s ->
        when {
            s == null -> flowOf(emptyList())
            s.role == Role.DOCTOR -> dao.observePrescriptionsForDoctor(s.doctorId.orEmpty())
            else -> dao.observePrescriptionsForPatient(s.userId)
        }
    }

    private fun allowed(s: Session?, rx: PrescriptionView?): PrescriptionView? {
        if (s == null || rx == null) return null
        val ok = if (s.role == Role.DOCTOR) rx.doctorId == s.doctorId else rx.patientId == s.userId
        if (!ok) Log.w(TAG, "Refused unauthorized prescription access")
        return rx.takeIf { ok }
    }

    override fun prescription(id: String): Flow<PrescriptionView?> =
        combine(store.session, dao.observePrescription(id)) { s, rx -> allowed(s, rx) }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun medications(prescriptionId: String): Flow<List<MedicationEntity>> =
        prescription(prescriptionId).flatMapLatest { rx ->
            if (rx == null) flowOf(emptyList()) else dao.observeMedications(rx.id)
        }

    override fun prescriptionForAppointment(appointmentId: String): Flow<PrescriptionView?> =
        combine(store.session, dao.observePrescriptionForAppointment(appointmentId)) { s, rx -> allowed(s, rx) }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun timeline(fromMillis: Long?, toMillis: Long?): Flow<List<TimelineEntity>> =
        store.session.flatMapLatest { s ->
            if (s == null || s.role != Role.PATIENT) flowOf(emptyList())
            else dao.observeTimeline(s.userId, fromMillis ?: 0L, toMillis ?: Long.MAX_VALUE)
        }

    override fun note(appointmentId: String): Flow<NoteEntity?> = dao.observeNote(appointmentId)

    private suspend fun ownedAppointment(appointmentId: String): AppointmentEntity? {
        val s = store.session.first()?.takeIf { it.role == Role.DOCTOR } ?: return null
        return dao.appointment(appointmentId)?.takeIf { it.doctorId == s.doctorId }
    }

    override suspend fun saveNote(appointmentId: String, text: String): Outcome {
        val a = ownedAppointment(appointmentId) ?: return refuse("saveNote")
        dao.upsertNote(NoteEntity(a.id, text.trim(), System.currentTimeMillis()))
        return Outcome.ok("Note saved to the patient record.")
    }

    override suspend fun issuePrescription(appointmentId: String, medications: List<MedicationDraft>, advice: String): Outcome {
        val a = ownedAppointment(appointmentId) ?: return refuse("issuePrescription")
        if (medications.isEmpty()) return Outcome.fail("Add at least one medication.")
        val now = System.currentTimeMillis()
        val id = newId("rx_")
        val version = (dao.maxPrescriptionVersion(a.id) ?: 0) + 1
        db.withTransaction {
            dao.insertPrescription(
                PrescriptionEntity(id, "CR-" + (now % 10_000).toString().padStart(4, '0'), a.patientId, a.doctorId, a.id, now, "ACTIVE", advice, version, now)
            )
            dao.insertMedications(medications.mapIndexed { i, m ->
                MedicationEntity("${id}_$i", id, m.name, m.category, m.dosage, m.frequency, m.supplyDays, m.refills, m.note)
            })
            dao.insertTimeline(
                listOf(TimelineEntity("tl_$id", a.patientId, TimelineType.PRESCRIPTION, "Prescription issued", medications.joinToString(" • ") { it.name }, "", now, id))
            )
        }
        return Outcome.ok(if (version == 1) "Prescription issued." else "Amendment recorded as version $version.", id)
    }
}

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val dao: CareDao,
    private val store: SessionStore
) : ChatRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun threads(): Flow<List<ThreadView>> = store.session.flatMapLatest { s ->
        if (s == null) flowOf(emptyList()) else dao.observeThreads(s.userId, s.doctorId.orEmpty())
    }

    private suspend fun canChat(s: Session?, appointmentId: String): Boolean {
        if (s == null) return false
        val a = dao.appointment(appointmentId) ?: return false
        val ok = a.status != AppointmentStatus.CANCELLED && (a.patientId == s.userId || a.doctorId == s.doctorId)
        if (!ok) Log.w(TAG, "Refused chat access without an appointment")
        return ok
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun messages(appointmentId: String): Flow<List<MessageEntity>> = store.session.flatMapLatest { s ->
        flowOf(Unit).map { canChat(s, appointmentId) }.flatMapLatest { ok ->
            if (ok) dao.observeMessages(appointmentId) else flowOf(emptyList())
        }
    }

    override suspend fun send(appointmentId: String, text: String, online: Boolean): Outcome {
        val s = store.session.first()
        if (!canChat(s, appointmentId) || s == null) return refuse("sendMessage")
        if (text.isBlank()) return Outcome.fail("Type a message first.")
        val status = if (online) MessageStatus.DELIVERED else MessageStatus.QUEUED
        val id = newId("m_")
        dao.insertMessage(MessageEntity(id, appointmentId, s.chatId, text.trim(), System.currentTimeMillis(), status))
        return if (online) Outcome.ok("Sent.", id) else Outcome.ok("Queued, will send when online.", id)
    }

    override suspend fun retry(messageId: String, online: Boolean): Outcome {
        if (!online) return Outcome.fail("Still offline. The message stays queued.")
        dao.updateMessageStatus(messageId, MessageStatus.DELIVERED)
        return Outcome.ok("Sent.", messageId)
    }

    override suspend fun flushQueued() = dao.deliverQueued()
}

@Singleton
class AvailabilityRepositoryImpl @Inject constructor(
    private val db: AppDatabase,
    private val dao: CareDao,
    private val store: SessionStore,
    private val seeder: Seeder
) : AvailabilityRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observe(): Flow<List<AvailabilityEntity>> = store.session.flatMapLatest { s ->
        if (s?.role == Role.DOCTOR) dao.observeAvailability(s.doctorId.orEmpty()) else flowOf(emptyList())
    }

    override suspend fun conflictCount(rows: List<AvailabilityEntity>): Int {
        val s = store.session.first()?.takeIf { it.role == Role.DOCTOR } ?: return 0
        val byDay = rows.associateBy { it.dayOfWeek }
        return dao.upcomingConfirmedForDoctor(s.doctorId.orEmpty(), System.currentTimeMillis()).count { a ->
            val dt = Times.toLocal(a.startMillis)
            val r = byDay[dt.dayOfWeek.value]
            r == null || !r.enabled || dt.toLocalTime() < LocalTime.parse(r.startTime) || dt.toLocalTime() >= LocalTime.parse(r.endTime)
        }
    }

    override suspend fun save(rows: List<AvailabilityEntity>): Outcome {
        val s = store.session.first()?.takeIf { it.role == Role.DOCTOR } ?: return refuse("saveAvailability")
        val doctorId = s.doctorId.orEmpty()
        if (rows.any { it.doctorId != doctorId }) return refuse("saveAvailability")
        if (rows.any { it.enabled && LocalTime.parse(it.startTime) >= LocalTime.parse(it.endTime) }) {
            return Outcome.fail("End time must be after start time.")
        }
        db.withTransaction { dao.insertAvailability(rows) }
        seeder.regenerateFreeSlots(doctorId)
        return Outcome.ok("Schedule saved. Patients see your new slots now.")
    }
}
