package com.mobile360.healthcare.testutil

import com.mobile360.healthcare.data.AppointmentRepository
import com.mobile360.healthcare.data.AuthRepository
import com.mobile360.healthcare.data.AvailabilityRepository
import com.mobile360.healthcare.data.RecordsRepository
import com.mobile360.healthcare.data.local.AppointmentView
import com.mobile360.healthcare.data.local.AvailabilityEntity
import com.mobile360.healthcare.data.local.MedicationEntity
import com.mobile360.healthcare.data.local.NoteEntity
import com.mobile360.healthcare.data.local.PrescriptionView
import com.mobile360.healthcare.data.local.TimelineEntity
import com.mobile360.healthcare.data.model.CodeRequestResult
import com.mobile360.healthcare.data.model.MedicationDraft
import com.mobile360.healthcare.data.model.Outcome
import com.mobile360.healthcare.data.model.Session
import com.mobile360.healthcare.data.model.VerifyResult
import com.mobile360.healthcare.data.model.Role
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import com.mobile360.healthcare.data.model.CodeRequestResult.Issued
import com.mobile360.healthcare.data.model.VerifyResult.Success

/** Main dispatcher that runs coroutines at once, so view models settle without a clock. */
@OptIn(ExperimentalCoroutinesApi::class)
class BoltMainRule : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

fun okOutcome(message: String = "ok") = Outcome(true, message, null)
fun failOutcome(message: String) = Outcome(false, message, null)

fun apptView(
    id: String,
    status: String,
    startMillis: Long,
    patientId: String = "p1",
    doctorId: String = "d1",
    patientName: String = "Priya Nair",
    doctorName: String = "Dr. Arun Rao",
) = AppointmentView(
    id = id, patientId = patientId, doctorId = doctorId, slotId = "s_$id", startMillis = startMillis,
    status = status, reason = "Check-up", room = "R1", instructions = "", prepNote = "", fileNo = "F1",
    vitals = "", referral = "", mode = "In-Person", cancelNote = "", patientName = patientName,
    patientAge = 34, patientSex = "F", patientImage = 0, doctorName = doctorName,
    doctorSpecialty = "Cardiology", doctorTitle = "MD", doctorImage = 0, doctorClinic = "Clinic",
    doctorAddress = "Street 1", doctorPhone = "000",
)

fun availabilityRow(day: Int, enabled: Boolean = true, start: String = "09:00", end: String = "13:00") =
    AvailabilityEntity("av_doc-1_$day", "doc-1", day, enabled, start, end)

/** In-memory appointments: records every call, answers with scripted outcomes. */
class FakeAppointments : AppointmentRepository {
    val all = MutableStateFlow<List<AppointmentView>>(emptyList())
    val booked = mutableListOf<Triple<String, String, String>>()
    val rescheduled = mutableListOf<Pair<String, String>>()
    val cancelled = mutableListOf<String>()
    val accepted = mutableListOf<String>()
    val rejected = mutableListOf<Pair<String, String>>()
    var nextOutcome: Outcome = okOutcome()
    var found: AppointmentView? = null

    override fun mine(): Flow<List<AppointmentView>> = all
    override fun next(): Flow<AppointmentView?> = flowOf(null)
    override fun todayForDoctor(): Flow<List<AppointmentView>> = all
    override suspend fun get(id: String): AppointmentView? = found
    override suspend fun book(doctorId: String, slotId: String, reason: String): Outcome {
        booked += Triple(doctorId, slotId, reason)
        return nextOutcome
    }

    override suspend fun reschedule(appointmentId: String, newSlotId: String): Outcome {
        rescheduled += appointmentId to newSlotId
        return nextOutcome
    }

    override suspend fun cancel(appointmentId: String): Outcome {
        cancelled += appointmentId
        return nextOutcome
    }

    override suspend fun accept(appointmentId: String): Outcome {
        accepted += appointmentId
        return nextOutcome
    }

    override suspend fun reject(appointmentId: String, reason: String): Outcome {
        rejected += appointmentId to reason
        return nextOutcome
    }
}

/** In-memory records: notes and prescriptions issued through the view model land here. */
class FakeRecords : RecordsRepository {
    val noteFlow = MutableStateFlow<NoteEntity?>(null)
    val savedNotes = mutableListOf<Pair<String, String>>()
    val issued = mutableListOf<Triple<String, List<MedicationDraft>, String>>()
    var nextOutcome: Outcome = okOutcome()

    override fun prescriptions(): Flow<List<PrescriptionView>> = flowOf(emptyList())
    override fun prescription(id: String): Flow<PrescriptionView?> = flowOf(null)
    override fun medications(prescriptionId: String): Flow<List<MedicationEntity>> = flowOf(emptyList())
    override fun prescriptionForAppointment(appointmentId: String): Flow<PrescriptionView?> = flowOf(null)
    override fun timeline(fromMillis: Long?, toMillis: Long?): Flow<List<TimelineEntity>> = flowOf(emptyList())
    override fun note(appointmentId: String): Flow<NoteEntity?> = noteFlow
    override suspend fun saveNote(appointmentId: String, text: String): Outcome {
        if (nextOutcome.ok) {
            savedNotes += appointmentId to text
            noteFlow.value = NoteEntity(appointmentId, text, 1L)
        }
        return nextOutcome
    }

    override suspend fun issuePrescription(
        appointmentId: String,
        medications: List<MedicationDraft>,
        advice: String,
    ): Outcome {
        if (nextOutcome.ok) issued += Triple(appointmentId, medications, advice)
        return nextOutcome
    }
}

/** In-memory availability: reports a scripted number of affected confirmed appointments. */
class FakeAvailability(initial: List<AvailabilityEntity>) : AvailabilityRepository {
    val rows = MutableStateFlow(initial)
    val saves = mutableListOf<List<AvailabilityEntity>>()
    var conflicts = 0
    var failSave = false

    override fun observe(): Flow<List<AvailabilityEntity>> = rows
    override suspend fun conflictCount(rows: List<AvailabilityEntity>): Int = conflicts
    override suspend fun save(rows: List<AvailabilityEntity>): Outcome {
        if (failSave) throw IllegalStateException("save failed")
        saves += rows
        this.rows.value = rows
        return okOutcome()
    }
}

/** Auth source that only exposes a fixed session. */
class FixedSessionAuth(role: Role) : AuthRepository {
    private val current = sampleSession(role, 0L)
    override val session: Flow<Session?> = flowOf(current)
    override suspend fun requestCode(phone: String): CodeRequestResult = Issued("98*****10", 0L)
    override suspend fun verifyCode(code: String): VerifyResult = Success(role = current.role)
    override suspend fun signOut() = Unit
}
