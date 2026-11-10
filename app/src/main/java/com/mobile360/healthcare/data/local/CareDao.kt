package com.mobile360.healthcare.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

private const val APPT_SELECT =
    "SELECT a.id AS id, a.patientId AS patientId, a.doctorId AS doctorId, a.slotId AS slotId, " +
        "a.startMillis AS startMillis, a.status AS status, a.reason AS reason, a.room AS room, " +
        "a.instructions AS instructions, a.prepNote AS prepNote, a.fileNo AS fileNo, a.vitals AS vitals, a.referral AS referral, " +
        "a.mode AS mode, a.cancelNote AS cancelNote, p.name AS patientName, p.age AS patientAge, " +
        "p.sex AS patientSex, p.imageRes AS patientImage, d.name AS doctorName, d.specialty AS doctorSpecialty, " +
        "d.title AS doctorTitle, d.imageRes AS doctorImage, d.clinic AS doctorClinic, d.address AS doctorAddress, " +
        "d.phone AS doctorPhone FROM appointments a JOIN accounts p ON p.id = a.patientId " +
        "JOIN doctors d ON d.id = a.doctorId"

private const val RX_SELECT =
    "SELECT r.id AS id, r.rxNumber AS rxNumber, r.patientId AS patientId, r.doctorId AS doctorId, " +
        "r.appointmentId AS appointmentId, r.issuedMillis AS issuedMillis, r.status AS status, " +
        "r.advice AS advice, r.version AS version, r.updatedMillis AS updatedMillis, p.name AS patientName, " +
        "d.name AS doctorName, d.specialty AS doctorSpecialty, d.imageRes AS doctorImage, " +
        "d.clinic AS clinic, d.address AS address FROM prescriptions r " +
        "JOIN accounts p ON p.id = r.patientId JOIN doctors d ON d.id = r.doctorId"

@Dao
interface CareDao {
    @Query("SELECT COUNT(*) FROM doctors") suspend fun doctorCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAccounts(list: List<AccountEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertDoctors(list: List<DoctorEntity>)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertSlots(list: List<SlotEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAppointments(list: List<AppointmentEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAppointment(item: AppointmentEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertPrescriptions(list: List<PrescriptionEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertPrescription(item: PrescriptionEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertMedications(list: List<MedicationEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertTimeline(list: List<TimelineEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertNote(note: NoteEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertMessages(list: List<MessageEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertMessage(item: MessageEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAvailability(list: List<AvailabilityEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAccount(item: AccountEntity)

    // Accounts
    @Query("SELECT * FROM accounts WHERE phone = :phone LIMIT 1") suspend fun accountByPhone(phone: String): AccountEntity?
    @Query("SELECT * FROM accounts WHERE id = :id") suspend fun accountById(id: String): AccountEntity?
    @Query("UPDATE accounts SET phone = :phone WHERE id = :id") suspend fun updatePhone(id: String, phone: String)
    @Query("DELETE FROM accounts WHERE id = :id") suspend fun deleteAccount(id: String)

    // Doctors and slots
    @Query("SELECT * FROM doctors WHERE active = 1") suspend fun activeDoctors(): List<DoctorEntity>
    @Query("SELECT * FROM doctors WHERE id = :id") fun observeDoctor(id: String): Flow<DoctorEntity?>

    @Query(
        "SELECT d.*, (SELECT MIN(s.startMillis) FROM slots s WHERE s.doctorId = d.id AND s.booked = 0 " +
            "AND s.startMillis > :now) AS nextSlotMillis FROM doctors d WHERE d.active = 1 " +
            "AND (:specialty = '' OR d.specialty = :specialty) " +
            "AND (d.name LIKE '%' || :query || '%' OR d.specialty LIKE '%' || :query || '%') " +
            "ORDER BY d.rating DESC"
    )
    fun searchDoctors(query: String, specialty: String, now: Long): Flow<List<DoctorListItem>>

    @Query("SELECT * FROM slots WHERE doctorId = :doctorId AND startMillis >= :from AND startMillis < :to ORDER BY startMillis")
    fun observeSlots(doctorId: String, from: Long, to: Long): Flow<List<SlotEntity>>

    @Query("SELECT * FROM slots WHERE id = :id") suspend fun slot(id: String): SlotEntity?
    @Query("UPDATE slots SET booked = :booked WHERE id = :id") suspend fun setSlotBooked(id: String, booked: Boolean)
    @Query("UPDATE slots SET booked = 1 WHERE id = :id AND booked = 0") suspend fun claimSlot(id: String): Int
    @Query("DELETE FROM slots WHERE booked = 0 AND startMillis < :now") suspend fun deletePastFreeSlots(now: Long)
    @Query("DELETE FROM slots WHERE booked = 0 AND doctorId = :doctorId AND startMillis >= :now")
    suspend fun deleteFutureFreeSlots(doctorId: String, now: Long)

    // Appointments
    @Query(APPT_SELECT + " WHERE a.patientId = :patientId ORDER BY a.startMillis")
    fun observeForPatient(patientId: String): Flow<List<AppointmentView>>

    @Query(APPT_SELECT + " WHERE a.doctorId = :doctorId ORDER BY a.startMillis")
    fun observeForDoctor(doctorId: String): Flow<List<AppointmentView>>

    @Query(APPT_SELECT + " WHERE a.doctorId = :doctorId AND a.startMillis >= :from AND a.startMillis < :to ORDER BY a.startMillis")
    fun observeForDoctorBetween(doctorId: String, from: Long, to: Long): Flow<List<AppointmentView>>

    @Query(APPT_SELECT + " WHERE a.id = :id") suspend fun appointmentView(id: String): AppointmentView?
    @Query(APPT_SELECT + " WHERE a.id = :id") fun observeAppointment(id: String): Flow<AppointmentView?>
    @Query("SELECT * FROM appointments WHERE id = :id") suspend fun appointment(id: String): AppointmentEntity?
    @Query("UPDATE appointments SET status = :status, cancelNote = :note WHERE id = :id")
    suspend fun updateAppointmentStatus(id: String, status: String, note: String)
    @Query("UPDATE appointments SET slotId = :slotId, startMillis = :start WHERE id = :id")
    suspend fun updateAppointmentSlot(id: String, slotId: String, start: Long)
    @Query("SELECT * FROM appointments WHERE doctorId = :doctorId AND status = 'CONFIRMED' AND startMillis > :now")
    suspend fun upcomingConfirmedForDoctor(doctorId: String, now: Long): List<AppointmentEntity>

    // Prescriptions
    @Query(RX_SELECT + " WHERE r.patientId = :patientId ORDER BY r.issuedMillis DESC")
    fun observePrescriptionsForPatient(patientId: String): Flow<List<PrescriptionView>>

    @Query(RX_SELECT + " WHERE r.doctorId = :doctorId ORDER BY r.issuedMillis DESC")
    fun observePrescriptionsForDoctor(doctorId: String): Flow<List<PrescriptionView>>

    @Query(RX_SELECT + " WHERE r.id = :id") fun observePrescription(id: String): Flow<PrescriptionView?>

    @Query(RX_SELECT + " WHERE r.appointmentId = :appointmentId ORDER BY r.version DESC LIMIT 1")
    fun observePrescriptionForAppointment(appointmentId: String): Flow<PrescriptionView?>

    @Query("SELECT * FROM medications WHERE prescriptionId = :prescriptionId ORDER BY id")
    fun observeMedications(prescriptionId: String): Flow<List<MedicationEntity>>

    @Query("SELECT MAX(version) FROM prescriptions WHERE appointmentId = :appointmentId")
    suspend fun maxPrescriptionVersion(appointmentId: String): Int?

    // Timeline and notes
    @Query("SELECT * FROM timeline WHERE patientId = :patientId AND dateMillis >= :from AND dateMillis <= :to ORDER BY dateMillis DESC")
    fun observeTimeline(patientId: String, from: Long, to: Long): Flow<List<TimelineEntity>>

    @Query("SELECT * FROM notes WHERE appointmentId = :appointmentId") fun observeNote(appointmentId: String): Flow<NoteEntity?>

    // Chat
    @Query("SELECT * FROM messages WHERE appointmentId = :appointmentId ORDER BY timeMillis")
    fun observeMessages(appointmentId: String): Flow<List<MessageEntity>>

    @Query("UPDATE messages SET status = :status WHERE id = :id") suspend fun updateMessageStatus(id: String, status: String)
    @Query("UPDATE messages SET status = 'DELIVERED' WHERE status = 'QUEUED'") suspend fun deliverQueued()

    @Query(
        "SELECT a.id AS appointmentId, a.doctorId AS doctorId, d.name AS doctorName, d.specialty AS doctorSpecialty, " +
            "d.imageRes AS doctorImage, d.phone AS doctorPhone, a.patientId AS patientId, p.name AS patientName, " +
            "p.imageRes AS patientImage, " +
            "(SELECT m.text FROM messages m WHERE m.appointmentId = a.id ORDER BY m.timeMillis DESC LIMIT 1) AS lastMessage, " +
            "(SELECT m.timeMillis FROM messages m WHERE m.appointmentId = a.id ORDER BY m.timeMillis DESC LIMIT 1) AS lastTime, " +
            "a.startMillis AS startMillis FROM appointments a JOIN accounts p ON p.id = a.patientId " +
            "JOIN doctors d ON d.id = a.doctorId WHERE (a.patientId = :userId OR a.doctorId = :doctorId) " +
            "AND a.status != 'CANCELLED' ORDER BY lastTime DESC"
    )
    fun observeThreads(userId: String, doctorId: String): Flow<List<ThreadView>>

    // Availability
    @Query("SELECT * FROM availability WHERE doctorId = :doctorId ORDER BY dayOfWeek")
    fun observeAvailability(doctorId: String): Flow<List<AvailabilityEntity>>

    @Query("SELECT * FROM availability WHERE doctorId = :doctorId ORDER BY dayOfWeek")
    suspend fun availabilityOnce(doctorId: String): List<AvailabilityEntity>

    // Account deletion
    @Query("DELETE FROM messages WHERE appointmentId IN (SELECT id FROM appointments WHERE patientId = :patientId)")
    suspend fun deleteMessagesOfPatient(patientId: String)
    @Query("DELETE FROM notes WHERE appointmentId IN (SELECT id FROM appointments WHERE patientId = :patientId)")
    suspend fun deleteNotesOfPatient(patientId: String)
    @Query("DELETE FROM appointments WHERE patientId = :patientId") suspend fun deleteAppointmentsOfPatient(patientId: String)
    @Query("DELETE FROM medications WHERE prescriptionId IN (SELECT id FROM prescriptions WHERE patientId = :patientId)")
    suspend fun deleteMedicationsOfPatient(patientId: String)
    @Query("DELETE FROM prescriptions WHERE patientId = :patientId") suspend fun deletePrescriptionsOfPatient(patientId: String)
    @Query("DELETE FROM timeline WHERE patientId = :patientId") suspend fun deleteTimelineOfPatient(patientId: String)
}
