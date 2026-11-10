package com.mobile360.healthcare.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobile360.healthcare.data.AppointmentRepository
import com.mobile360.healthcare.data.ConnectivityObserver
import com.mobile360.healthcare.data.DoctorRepository
import com.mobile360.healthcare.data.local.DoctorEntity
import com.mobile360.healthcare.data.local.SlotEntity
import com.mobile360.healthcare.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DoctorLoad {
    data object Loading : DoctorLoad
    data class Found(val doctor: DoctorEntity) : DoctorLoad
    data object Missing : DoctorLoad
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DoctorProfileAndBookingViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    doctors: DoctorRepository,
    private val appointments: AppointmentRepository,
    private val connectivity: ConnectivityObserver
) : ViewModel() {
    val doctorId: String = savedState.get<String>(Routes.ARG_DOCTOR_ID).orEmpty()
    val appointmentId: String? = savedState.get<String>(Routes.ARG_APPOINTMENT_ID)?.takeIf { it.isNotBlank() }
    val isReschedule: Boolean get() = appointmentId != null

    val epochDay: StateFlow<Long> = savedState.getStateFlow("epochDay", LocalDate.now().toEpochDay())
    val slotId: StateFlow<String> = savedState.getStateFlow("slotId", "")
    val reason: StateFlow<String> = savedState.getStateFlow("reason", "")
    val bookmarked: StateFlow<Boolean> = savedState.getStateFlow("bookmarked", false)

    val doctor: StateFlow<DoctorLoad> = doctors.doctor(doctorId)
        .map { if (it == null) DoctorLoad.Missing else DoctorLoad.Found(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DoctorLoad.Loading)

    // Live from Room: a slot taken or released elsewhere shows up without a refresh.
    val slots: StateFlow<List<SlotEntity>?> = epochDay.flatMapLatest { day ->
        doctors.slots(doctorId, LocalDate.ofEpochDay(day)).map { list ->
            val now = System.currentTimeMillis()
            list.filter { it.startMillis > now }.sortedBy { it.startMillis }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val error = MutableStateFlow<String?>(null)
    val confirmation = MutableStateFlow<String?>(null)
    val busy = MutableStateFlow(false)

    fun selectDate(day: LocalDate) {
        savedState["epochDay"] = day.toEpochDay()
        savedState["slotId"] = ""
        error.value = null
    }

    fun selectSlot(id: String) {
        savedState["slotId"] = id
        error.value = null
    }

    fun setReason(text: String) {
        savedState["reason"] = text
        error.value = null
    }

    fun toggleBookmark() {
        savedState["bookmarked"] = !bookmarked.value
    }

    fun confirm() {
        val slot = slotId.value
        val currentSlots = slots.value.orEmpty()
        if (slot.isBlank() || currentSlots.none { it.id == slot && !it.booked }) {
            error.value = "Choose a free slot first."
            savedState["slotId"] = ""
            return
        }
        if (!isReschedule && reason.value.isBlank()) {
            error.value = "Add a short reason for the visit."
            return
        }
        if (!connectivity.isOnlineNow()) {
            error.value = "You are offline. Booking needs a connection, so nothing was saved."
            return
        }
        if (busy.value) return
        viewModelScope.launch {
            busy.value = true
            val outcome = appointmentId?.let { appointments.reschedule(it, slot) }
                ?: appointments.book(doctorId, slot, reason.value.trim())
            busy.value = false
            if (outcome.ok) {
                confirmation.value = outcome.message
            } else {
                error.value = outcome.message
                savedState["slotId"] = ""
            }
        }
    }
}
