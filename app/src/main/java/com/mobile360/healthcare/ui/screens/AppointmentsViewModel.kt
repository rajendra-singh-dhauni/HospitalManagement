package com.mobile360.healthcare.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobile360.healthcare.data.AppointmentRepository
import com.mobile360.healthcare.data.local.AppointmentView
import com.mobile360.healthcare.data.model.AppointmentStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AppointmentsViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val appointments: AppointmentRepository
) : ViewModel() {
    val showPast: StateFlow<Boolean> = savedState.getStateFlow("showPast", false)

    // null while loading
    val items: StateFlow<List<AppointmentView>?> = appointments.mine()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val message = MutableStateFlow<String?>(null)

    fun setPast(past: Boolean) {
        savedState["showPast"] = past
        message.value = null
    }

    fun isUpcoming(a: AppointmentView, now: Long = System.currentTimeMillis()): Boolean =
        (a.status == AppointmentStatus.CONFIRMED || a.status == AppointmentStatus.PENDING) && a.startMillis >= now

    fun canReschedule(a: AppointmentView): Boolean = a.startMillis - System.currentTimeMillis() > TWO_HOURS

    fun blockedReschedule() {
        message.value = "Rescheduling is blocked: 2 hours or less remain before this appointment. You can still message the doctor."
    }

    fun cancel(id: String) {
        viewModelScope.launch {
            val outcome = appointments.cancel(id)
            message.value = outcome.message
        }
    }

    fun dismissMessage() {
        message.value = null
    }

    private companion object {
        const val TWO_HOURS = 2 * 60 * 60 * 1000L
    }
}
