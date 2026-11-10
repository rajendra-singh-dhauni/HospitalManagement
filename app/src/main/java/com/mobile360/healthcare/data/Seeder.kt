package com.mobile360.healthcare.data

import com.mobile360.healthcare.R
import com.mobile360.healthcare.data.local.AccountEntity
import com.mobile360.healthcare.data.local.AppointmentEntity
import com.mobile360.healthcare.data.local.AvailabilityEntity
import com.mobile360.healthcare.data.local.CareDao
import com.mobile360.healthcare.data.local.DoctorEntity
import com.mobile360.healthcare.data.local.MedicationEntity
import com.mobile360.healthcare.data.local.MessageEntity
import com.mobile360.healthcare.data.local.NoteEntity
import com.mobile360.healthcare.data.local.PrescriptionEntity
import com.mobile360.healthcare.data.local.SlotEntity
import com.mobile360.healthcare.data.local.TimelineEntity
import com.mobile360.healthcare.data.model.AppointmentStatus
import com.mobile360.healthcare.data.model.MessageStatus
import com.mobile360.healthcare.data.model.TimelineType
import com.mobile360.healthcare.util.AppConfig
import com.mobile360.healthcare.util.Times
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton

object Ids {
    const val MAYA = "p_maya"
    const val PRIYA = "p_priya"
    const val RAHUL = "p_rahul"
    const val ANANYA = "p_ananya"
    const val KARAN = "p_karan"
    const val MEERA_ACCOUNT = "u_meera"
    const val RAO = "d_rao"
    const val KHAN = "d_khan"
    const val LI_WEI = "d_liwei"
    const val MEERA = "d_meera"
    fun slot(doctorId: String, millis: Long) = "slot_${doctorId}_$millis"
}

@Singleton
class Seeder @Inject constructor(private val dao: CareDao) {
    private val mutex = Mutex()

    suspend fun ensureSeeded() = mutex.withLock {
        if (dao.doctorCount() == 0) seedAll()
        dao.deletePastFreeSlots(System.currentTimeMillis())
        dao.activeDoctors().forEach { generateSlots(it.id) }
    }

    suspend fun regenerateFreeSlots(doctorId: String) = mutex.withLock {
        dao.deleteFutureFreeSlots(doctorId, System.currentTimeMillis())
        generateSlots(doctorId)
    }

    private suspend fun generateSlots(doctorId: String) {
        val avail = dao.availabilityOnce(doctorId)
        val today = LocalDate.now()
        val slots = mutableListOf<SlotEntity>()
        for (day in 1..14) {
            val date = today.plusDays(day.toLong())
            val dow = date.dayOfWeek.value
            val windows: List<Pair<LocalTime, LocalTime>> = if (avail.isEmpty()) {
                if (dow <= 5) listOf(LocalTime.of(10, 30) to LocalTime.of(12, 30), LocalTime.of(14, 0) to LocalTime.of(15, 0))
                else emptyList()
            } else {
                avail.firstOrNull { it.dayOfWeek == dow && it.enabled }
                    ?.let { listOf(LocalTime.parse(it.startTime) to LocalTime.parse(it.endTime)) }
                    ?: emptyList()
            }
            for ((start, end) in windows) {
                var t = start
                while (t.isBefore(end)) {
                    val millis = Times.startOfDay(date) + (t.toSecondOfDay() * 1000L)
                    slots += SlotEntity(Ids.slot(doctorId, millis), doctorId, millis, false)
                    t = t.plusMinutes(30)
                }
            }
        }
        dao.insertSlots(slots)
    }

    private suspend fun seedAll() {
        val tuesday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.TUESDAY))
        val tueOffset = (tuesday.toEpochDay() - LocalDate.now().toEpochDay()).toInt()

        dao.insertDoctors(doctors())
        dao.insertAccounts(accounts())
        dao.insertAvailability(meeraAvailability())
        dao.activeDoctors().forEach { generateSlots(it.id) }

        val raoTue1030 = Times.at(tueOffset, 10, 30)
        dao.setSlotBooked(Ids.slot(Ids.RAO, raoTue1030), true)
        dao.setSlotBooked(Ids.slot(Ids.RAO, Times.at(tueOffset, 11, 0)), true)

        dao.insertAppointments(
            listOf(
                AppointmentEntity(
                    id = "a_rao_next", patientId = Ids.MAYA, doctorId = Ids.RAO, slotId = Ids.slot(Ids.RAO, raoTue1030),
                    startMillis = raoTue1030, status = AppointmentStatus.CONFIRMED, reason = "Cardiology follow-up",
                    room = "Building B, Suite 402", instructions = "Arrive 15 mins early", prepNote = "Bring fasting blood panel results"
                ),
                AppointmentEntity(
                    id = "a_rao_past", patientId = Ids.MAYA, doctorId = Ids.RAO, slotId = null,
                    startMillis = Times.at(-5, 10, 30), status = AppointmentStatus.COMPLETED, reason = "Blood pressure review",
                    room = "Building B, Suite 402"
                ),
                AppointmentEntity(
                    id = "a_khan_past", patientId = Ids.MAYA, doctorId = Ids.KHAN, slotId = null,
                    startMillis = Times.at(-18, 11, 0), status = AppointmentStatus.COMPLETED, reason = "Stomach discomfort",
                    room = "Building A, Suite 210"
                ),
                AppointmentEntity(
                    id = "a_liwei_cancelled", patientId = Ids.MAYA, doctorId = Ids.LI_WEI, slotId = null,
                    startMillis = Times.at(-12, 9, 0), status = AppointmentStatus.CANCELLED, reason = "Breathing check",
                    room = "Building C, Suite 118", instructions = "Follow-up recommended", cancelNote = "Cancelled by patient"
                ),
                AppointmentEntity(
                    id = "a_priya", patientId = Ids.PRIYA, doctorId = Ids.MEERA, slotId = null,
                    startMillis = Times.at(0, 9, 0), status = AppointmentStatus.CONFIRMED, reason = "General Cardiology Follow-up",
                    room = "Room 302", fileNo = "CR-4412", vitals = "BP 120/78 mmHg"
                ),
                AppointmentEntity(
                    id = "a_rahul", patientId = Ids.RAHUL, doctorId = Ids.MEERA, slotId = null,
                    startMillis = Times.at(0, 9, 30), status = AppointmentStatus.PENDING, reason = "ECG Review & Chest Tightness",
                    room = "Room 302", fileNo = "CR-4420", referral = "Referred by Dr. S. Kulkarni • Urgent review requested"
                ),
                AppointmentEntity(
                    id = "a_ananya", patientId = Ids.ANANYA, doctorId = Ids.MEERA, slotId = null,
                    startMillis = Times.at(0, 11, 15), status = AppointmentStatus.CONFIRMED, reason = "Post-Angioplasty Check",
                    room = "Room 302", fileNo = "CR-4398"
                ),
                AppointmentEntity(
                    id = "a_karan", patientId = Ids.KARAN, doctorId = Ids.MEERA, slotId = null,
                    startMillis = Times.at(0, 12, 0), status = AppointmentStatus.CONFIRMED, reason = "Hypertension Follow-up",
                    room = "Room 302", fileNo = "CR-4405"
                )
            )
        )

        val rxDesc = "Maintain low-sodium diet and hydrate well. Schedule repeat lipid profile in 4 weeks."
        dao.insertPrescriptions(
            listOf(
                PrescriptionEntity("rx_rao", "CR-8821", Ids.MAYA, Ids.RAO, "a_rao_past", Times.at(-5, 11, 0), "ACTIVE", rxDesc, 1, Times.at(-5, 11, 5)),
                PrescriptionEntity("rx_khan", "CR-7342", Ids.MAYA, Ids.KHAN, "a_khan_past", Times.at(-18, 11, 0), "COMPLETED", "Take after meals and finish the course.", 1, Times.at(-18, 11, 10)),
                PrescriptionEntity("rx_priya", "CR-7710", Ids.PRIYA, Ids.MEERA, "a_priya", Times.at(-30, 9, 0), "ACTIVE", rxDesc, 1, Times.at(-30, 9, 10))
            )
        )
        dao.insertMedications(
            listOf(
                MedicationEntity("med_rao_1", "rx_rao", "Atorvastatin 10 mg", "Lipid", "1 tablet", "Once daily (Evening after dinner)", 30, 2, "With water • Bedtime"),
                MedicationEntity("med_rao_2", "rx_rao", "Aspirin 75 mg", "Antiplatelet", "1 tablet", "After lunch", 30, 1, "Take after food"),
                MedicationEntity("med_khan_1", "rx_khan", "Pantoprazole 40 mg", "Antacid", "1 tablet", "Once daily before breakfast", 14, 0, "Take 30 minutes before food"),
                MedicationEntity("med_priya_1", "rx_priya", "Atorvastatin 10 mg", "Lipid", "1 tablet", "Daily at bedtime", 30, 2, "With water • Bedtime"),
                MedicationEntity("med_priya_2", "rx_priya", "Aspirin 75 mg", "Antiplatelet", "1 tablet", "Daily after meals", 30, 1, "Take after food")
            )
        )
        dao.insertTimeline(
            listOf(
                TimelineEntity("tl_rx_rao", Ids.MAYA, TimelineType.PRESCRIPTION, "Prescription issued by Dr. Arun Rao", "Atorvastatin 10 mg • Aspirin 75 mg", "", Times.at(-5, 11, 5), "rx_rao"),
                TimelineEntity("tl_note_rao", Ids.MAYA, TimelineType.CONSULTATION, "Clinical Consultation Note by Dr. Arun Rao", "Patient reports steady cardiovascular stability. BP monitored at 122/78 mmHg. Follow-up blood test ordered.", "", Times.at(-5, 10, 30), "a_rao_past"),
                TimelineEntity("tl_lab", Ids.MAYA, TimelineType.LABORATORY, "Lipid Panel & CBC", "Verified by Central Labs", "New report", Times.at(-8, 9, 0), null),
                TimelineEntity("tl_rx_khan", Ids.MAYA, TimelineType.PRESCRIPTION, "Prescription issued by Dr. Sara Khan", "Pantoprazole 40 mg", "", Times.at(-18, 11, 10), "rx_khan")
            )
        )
        dao.upsertNote(NoteEntity("a_priya", "Patient reports improved stamina with minor morning fatigue. Blood pressure normalized at 120/78 mmHg. Recommend continuing current cardiac regimen.", Times.at(0, 8, 55)))

        val now = System.currentTimeMillis()
        val min = 60_000L
        dao.insertMessages(
            listOf(
                MessageEntity("m1", "a_rao_next", Ids.RAO, "Good morning Maya. How are you feeling after adjusting the beta blocker dosage?", now - 180 * min, MessageStatus.READ),
                MessageEntity("m2", "a_rao_next", Ids.MAYA, "Much better, thanks! The morning dizziness has completely stopped.", now - 150 * min, MessageStatus.READ),
                MessageEntity("m3", "a_rao_next", Ids.RAO, "Glad to hear that. Please continue recording your BP twice daily until Tuesday.", now - 146 * min, MessageStatus.READ),
                MessageEntity("m4", "a_rao_next", Ids.MAYA, "Will do, logged 120/78 this morning.", now - 144 * min, MessageStatus.READ),
                MessageEntity("m5", "a_rao_next", Ids.MAYA, "Also, should I take the potassium supplement with breakfast or before sleep?", now - 140 * min, MessageStatus.DELIVERED),
                MessageEntity("m6", "a_khan_past", Ids.MAYA, "Thank you doctor, the discomfort is gone.", now - 3 * 1440 * min - 30 * min, MessageStatus.READ),
                MessageEntity("m7", "a_khan_past", Ids.KHAN, "Take after meals and finish the course.", now - 3 * 1440 * min, MessageStatus.READ),
                MessageEntity("m8", "a_priya", Ids.PRIYA, "Good morning doctor, my morning fatigue is a little better.", now - 200 * min, MessageStatus.READ),
                MessageEntity("m9", "a_priya", Ids.MEERA, "Good to hear, Priya. We will review it at your visit.", now - 190 * min, MessageStatus.DELIVERED)
            )
        )
    }

    private fun doctors() = listOf(
        DoctorEntity(Ids.RAO, "Dr. Arun Rao", "Cardiology", "Cardiology Specialist", 20, 4.8, 124, "English, Hindi", "CareConnect Lead", "CareConnect Hospital", "Building B, Suite 402 • Main Campus", "+911800120411", R.drawable.a_headshot_portrait_of_dr_arun_rao_an_ex, true, 1200, "Consults Monday to Friday."),
        DoctorEntity(Ids.KHAN, "Dr. Sara Khan", "Internal Medicine", "Internal Medicine Specialist", 14, 4.6, 98, "English, Urdu", "Care team physician", "CareConnect Hospital", "Building A, Suite 210 • Main Campus", "+911800120412", R.drawable.professional_portrait_of_dr_sara_khan_an, true, 900, "Consults Monday to Friday."),
        DoctorEntity(Ids.LI_WEI, "Dr. Li Wei", "Pulmonology", "Pulmonology Clinic", 18, 4.7, 110, "English, Mandarin", "Pulmonology lead", "CareConnect Hospital", "Building C, Suite 118 • Main Campus", "+911800120413", R.drawable.a_high_quality_professional_portrait_of, true, 1000, "Consults Monday to Friday."),
        DoctorEntity(Ids.MEERA, "Dr. Meera Iyer", "Cardiology", "Cardiology Specialist", 16, 4.9, 87, "English, Tamil", "Heart Center", "CareConnect Hospital", "Building B, Suite 305 • Main Campus", "+911800120414", R.drawable.close_up_portrait_photograph_of_a_welcom, true, 1100, "Consults Monday, Tuesday, Thursday and Friday."),
        DoctorEntity("d_verma", "Dr. Neha Verma", "Dermatology", "Dermatology Specialist", 11, 4.5, 76, "English, Hindi", "Skin clinic", "CareConnect Hospital", "Building A, Suite 105 • Main Campus", "+911800120415", R.drawable.a_confident_female_cardiologist_of_middl, true, 800, "Consults Monday to Friday."),
        DoctorEntity("d_patel", "Dr. Rohan Patel", "Pediatrics", "Pediatrics Specialist", 9, 4.7, 102, "English, Gujarati", "Children's care", "CareConnect Hospital", "Building D, Suite 020 • Main Campus", "+911800120416", R.drawable.a_clean_professional_close_up_portrait_o, true, 700, "Consults Monday to Friday."),
        DoctorEntity("d_qureshi", "Dr. Imran Qureshi", "Orthopedics", "Orthopedics Specialist", 15, 4.4, 64, "English, Urdu", "Bone and joint", "CareConnect Hospital", "Building D, Suite 140 • Main Campus", "+911800120417", R.drawable.a_distinguished_south_asian_male_doctor, true, 1000, "Consults Monday to Friday."),
        DoctorEntity("d_chen", "Dr. Daniel Chen", "Orthopedics", "Orthopedics Specialist", 12, 4.5, 71, "English, Mandarin", "Sports injuries", "CareConnect Hospital", "Building D, Suite 142 • Main Campus", "+911800120419", R.drawable.an_east_asian_male_physician_in_his_40s, true, 950, "Consults Monday to Friday."),
        DoctorEntity("d_sethi", "Dr. Vikram Sethi", "Cardiology", "Cardiology Specialist", 22, 4.6, 58, "English, Hindi", "On leave", "CareConnect Hospital", "Building B, Suite 410 • Main Campus", "+911800120418", R.drawable.professional_clinical_headshot_of_an_exp, false, 1200, "Currently on leave.")
    )

    private fun accounts() = listOf(
        AccountEntity(Ids.MAYA, AppConfig.DEMO_PATIENT_PHONE, "Maya", "PATIENT", 31, "Female", R.drawable.a_warm_professional_portrait_photograph, null),
        AccountEntity(Ids.PRIYA, "9876500002", "Priya Nair", "PATIENT", 34, "Female", R.drawable.portrait_photograph_of_priya_nair_an_ind, null),
        AccountEntity(Ids.RAHUL, "9876500003", "Rahul Mehta", "PATIENT", 48, "Male", R.drawable.portrait_photography_of_rahul_mehta_an_a, null),
        AccountEntity(Ids.ANANYA, "9876500004", "Ananya Sen", "PATIENT", 41, "Female", 0, null),
        AccountEntity(Ids.KARAN, "9876500005", "Karan Shah", "PATIENT", 52, "Male", R.drawable.small_thumbnail_avatar_of_indian_male_ca, null),
        AccountEntity(Ids.MEERA_ACCOUNT, AppConfig.DEMO_DOCTOR_PHONE, "Dr. Meera Iyer", "DOCTOR", 45, "Female", R.drawable.close_up_portrait_photograph_of_a_welcom, Ids.MEERA)
    )

    private fun meeraAvailability(): List<AvailabilityEntity> {
        fun row(dow: Int, enabled: Boolean, start: String, end: String) =
            AvailabilityEntity("${Ids.MEERA}_$dow", Ids.MEERA, dow, enabled, start, end)
        return listOf(
            row(1, true, "09:00", "13:00"),
            row(2, true, "14:00", "18:00"),
            row(3, false, "09:00", "13:00"),
            row(4, true, "09:00", "13:00"),
            row(5, true, "09:00", "13:00"),
            row(6, false, "09:00", "13:00"),
            row(7, false, "09:00", "13:00")
        )
    }
}
