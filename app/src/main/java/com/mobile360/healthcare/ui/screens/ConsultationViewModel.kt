package com.mobile360.healthcare.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobile360.healthcare.data.AppointmentRepository
import com.mobile360.healthcare.data.RecordsRepository
import com.mobile360.healthcare.data.local.AppointmentView
import com.mobile360.healthcare.data.model.MedicationDraft
import com.mobile360.healthcare.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ConsultUi {
    data object Loading : ConsultUi
    data object NotFound : ConsultUi
    data class Ready(val appt: AppointmentView) : ConsultUi
}

data class ConsultMsg(val ok: Boolean, val text: String)

@HiltViewModel
class ConsultationViewModel @Inject constructor(
    private val handle: SavedStateHandle,
    private val appointments: AppointmentRepository,
    private val records: RecordsRepository
) : ViewModel() {
    val appointmentId: String = handle.get<String>(Routes.ARG_APPOINTMENT_ID).orEmpty()

    private val _ui = MutableStateFlow<ConsultUi>(ConsultUi.Loading)
    val ui: StateFlow<ConsultUi> = _ui

    val draft: StateFlow<String> = handle.getStateFlow("draft", "")
    val advice: StateFlow<String> = handle.getStateFlow("advice", "")
    private val _meds = MutableStateFlow<List<MedicationDraft>>(emptyList())
    val meds: StateFlow<List<MedicationDraft>> = _meds
    private val _noteMsg = MutableStateFlow<ConsultMsg?>(null)
    val noteMsg: StateFlow<ConsultMsg?> = _noteMsg
    private val _rxMsg = MutableStateFlow<ConsultMsg?>(null)
    val rxMsg: StateFlow<ConsultMsg?> = _rxMsg

    val savedNote: StateFlow<String?> = records.note(appointmentId).map { it?.text.orEmpty() }
        .catch { emit("") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val issued: StateFlow<Boolean> = records.prescriptionForAppointment(appointmentId).map { it != null }
        .catch { emit(false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        viewModelScope.launch {
            val a = runCatching { appointments.get(appointmentId) }.getOrNull()
            _ui.value = if (a == null) ConsultUi.NotFound else ConsultUi.Ready(a)
            if (a != null && handle.get<String>("draft") == null) {
                handle["draft"] = records.note(appointmentId).first()?.text.orEmpty()
            }
        }
    }

    fun setDraft(t: String) { handle["draft"] = t; _noteMsg.value = null }
    fun setAdvice(t: String) { handle["advice"] = t }
    fun addMedication(m: MedicationDraft) { _meds.value = _meds.value + m; _rxMsg.value = null }
    fun removeMedication(i: Int) { _meds.value = _meds.value.filterIndexed { idx, _ -> idx != i } }

    fun saveNote() {
        val text = draft.value.trim()
        if (text.isEmpty()) { _noteMsg.value = ConsultMsg(false, "Write a note before saving."); return }
        viewModelScope.launch {
            val out = records.saveNote(appointmentId, text)
            _noteMsg.value = ConsultMsg(out.ok, out.message)
        }
    }

    fun issue() {
        if (_meds.value.isEmpty()) { _rxMsg.value = ConsultMsg(false, "Add at least one medication first."); return }
        viewModelScope.launch {
            val out = records.issuePrescription(appointmentId, _meds.value, advice.value.trim())
            _rxMsg.value = ConsultMsg(out.ok, out.message)
            if (out.ok) { _meds.value = emptyList(); handle["advice"] = "" }
        }
    }
}
