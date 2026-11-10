package com.mobile360.healthcare.testutil

import com.mobile360.healthcare.data.AccountRepository
import com.mobile360.healthcare.data.AuthRepository
import com.mobile360.healthcare.data.RecordsRepository
import com.mobile360.healthcare.data.local.MedicationEntity
import com.mobile360.healthcare.data.local.NoteEntity
import com.mobile360.healthcare.data.local.PrescriptionView
import com.mobile360.healthcare.data.local.TimelineEntity
import com.mobile360.healthcare.data.model.AlertKind
import com.mobile360.healthcare.data.model.AlertPrefs
import com.mobile360.healthcare.data.model.CodeRequestResult
import com.mobile360.healthcare.data.model.MedicationDraft
import com.mobile360.healthcare.data.model.Outcome
import com.mobile360.healthcare.data.model.Role
import com.mobile360.healthcare.data.model.Session
import com.mobile360.healthcare.data.model.VerifyResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import com.mobile360.healthcare.data.model.CodeRequestResult.Issued
import com.mobile360.healthcare.data.model.VerifyResult.Success

const val GOOD_CODE = "123456"

/** Auth source with a signed-in patient; counts sign-outs. */
class StubAuth(initial: Session? = sampleSession(Role.PATIENT, 0L)) : AuthRepository {
    val current = MutableStateFlow(initial)
    var signOuts = 0
    override val session: Flow<Session?> = current
    override suspend fun requestCode(phone: String): CodeRequestResult = Issued("******3210", 0L)
    override suspend fun verifyCode(code: String): VerifyResult = Success(Role.PATIENT)
    override suspend fun signOut() {
        signOuts++
        current.value = null
    }
}

/** Account service that accepts only GOOD_CODE for sensitive actions. */
class StubAccount : AccountRepository {
    val prefs = MutableStateFlow(AlertPrefs(true, true, true))
    var reAuthCalls = 0
    var changedTo: String? = null
    var deleted = false
    var othersRevoked = false
    override val alertPrefs: Flow<AlertPrefs> = prefs

    override suspend fun setAlert(kind: AlertKind, enabled: Boolean) {
        val p = prefs.value
        prefs.value = when (kind) {
            AlertKind.APPOINTMENTS -> p.copy(appointments = enabled)
            AlertKind.REMINDERS -> p.copy(reminders = enabled)
            AlertKind.REPORTS_AND_MESSAGES -> p.copy(reportsAndMessages = enabled)
        }
    }

    override suspend fun reAuthenticate(code: String): Outcome {
        reAuthCalls++
        return if (code == GOOD_CODE) Outcome.ok("ok", null) else Outcome.fail("Wrong code")
    }

    override suspend fun changeNumber(newPhone: String, code: String): Outcome {
        if (code != GOOD_CODE) return Outcome.fail("Wrong code")
        changedTo = newPhone
        return Outcome.ok("Number updated", null)
    }

    override suspend fun signOutOtherDevices(): Outcome {
        othersRevoked = true
        return Outcome.ok("Other devices signed out", null)
    }

    override suspend fun deleteAccount(code: String): Outcome {
        if (code != GOOD_CODE) return Outcome.fail("Wrong code")
        deleted = true
        return Outcome.ok("Deletion requested", null)
    }
}

fun rxView(id: String, issued: Long, doctor: String = "Dr. Arun Rao") = PrescriptionView(
    id = id, rxNumber = "RX-$id", patientId = "p1", doctorId = "d1", appointmentId = null,
    issuedMillis = issued, status = "ACTIVE", advice = "Rest", version = 1, updatedMillis = issued,
    patientName = "Priya Nair", doctorName = doctor, doctorSpecialty = "Cardiology", doctorImage = 0,
    clinic = "Clinic", address = "Street 1"
)

fun medication(id: String, rxId: String) = MedicationEntity(
    id = id, prescriptionId = rxId, name = "Atorvastatin", category = "Statin", dosage = "10 mg",
    frequency = "once daily", supplyDays = 30, refillsLeft = 1, note = ""
)

fun timelineItem(id: String, type: String, at: Long) = TimelineEntity(
    id = id, patientId = "p1", type = type, title = "t-$id", detail = "", extra = "", dateMillis = at, refId = null
)

/** In-memory records source; the timeline filter is inclusive on both bounds. */
class StubRecords(
    var rx: List<PrescriptionView> = emptyList(),
    var items: List<TimelineEntity> = emptyList(),
    var failing: Boolean = false
) : RecordsRepository {
    override fun prescriptions(): Flow<List<PrescriptionView>> = flow {
        if (failing) throw IllegalStateException("offline")
        emit(rx)
    }

    override fun prescription(id: String): Flow<PrescriptionView?> = flow { emit(rx.firstOrNull { it.id == id }) }
    override fun medications(prescriptionId: String): Flow<List<MedicationEntity>> =
        flow { emit(listOf(medication("m-$prescriptionId", prescriptionId))) }

    override fun prescriptionForAppointment(appointmentId: String): Flow<PrescriptionView?> = flow { emit(null) }

    override fun timeline(fromMillis: Long?, toMillis: Long?): Flow<List<TimelineEntity>> = flow {
        if (failing) throw IllegalStateException("offline")
        emit(items.filter { (fromMillis == null || it.dateMillis >= fromMillis) && (toMillis == null || it.dateMillis <= toMillis) }
            .sortedByDescending { it.dateMillis })
    }

    override fun note(appointmentId: String): Flow<NoteEntity?> = flow { emit(null) }
    var issued = 0
    override suspend fun saveNote(appointmentId: String, text: String): Outcome = Outcome.ok("saved", null)
    override suspend fun issuePrescription(appointmentId: String, medications: List<MedicationDraft>, advice: String): Outcome {
        issued++
        return Outcome.ok("issued", "new-rx")
    }
}

