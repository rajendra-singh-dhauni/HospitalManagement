package com.mobile360.healthcare.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobile360.healthcare.data.AuthRepository
import com.mobile360.healthcare.data.AvailabilityRepository
import com.mobile360.healthcare.data.local.AvailabilityEntity
import com.mobile360.healthcare.data.model.Role
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AvailabilityUi(
    val loading: Boolean = true,
    val isDoctor: Boolean = true,
    val rows: List<AvailabilityEntity> = emptyList(),
    val selectedDay: Int = 3,
    val conflict: Int = 0,
    val awaitingConfirm: Boolean = false,
    val saving: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class AvailabilityViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val repo: AvailabilityRepository
) : ViewModel() {
    private val _state = MutableStateFlow(AvailabilityUi())
    val state: StateFlow<AvailabilityUi> = _state
    private var doctorId: String = ""

    init { load() }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val s = auth.session.first()
                if (s == null || s.role != Role.DOCTOR) {
                    _state.update { it.copy(loading = false, isDoctor = false) }
                    return@launch
                }
                doctorId = s.doctorId.orEmpty()
                val existing = repo.observe().first()
                val rows = (1..7).map { d ->
                    existing.firstOrNull { it.dayOfWeek == d } ?: AvailabilityEntity(
                        id = "av_${doctorId}_$d", doctorId = doctorId, dayOfWeek = d,
                        enabled = d <= 5, startTime = "09:00", endTime = "13:00"
                    )
                }
                _state.update { it.copy(loading = false, rows = rows) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = "Could not load your schedule. Try again.") }
            }
        }
    }

    fun select(day: Int) = _state.update { it.copy(selectedDay = day) }

    fun edit(day: Int, change: (AvailabilityEntity) -> AvailabilityEntity) = _state.update { st ->
        st.copy(
            rows = st.rows.map { if (it.dayOfWeek == day) change(it) else it },
            awaitingConfirm = false, conflict = 0, message = null
        )
    }

    fun showMessage(text: String) = _state.update { it.copy(message = text) }

    fun save() {
        val st = _state.value
        if (st.saving) return
        if (st.rows.any { it.enabled && it.endTime <= it.startTime }) {
            _state.update { it.copy(message = "End time must be after start time on every active day.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(saving = true, message = null) }
            try {
                if (!st.awaitingConfirm) {
                    val c = repo.conflictCount(st.rows)
                    if (c > 0) {
                        _state.update { it.copy(saving = false, awaitingConfirm = true, conflict = c) }
                        return@launch
                    }
                }
                val out = repo.save(st.rows)
                _state.update {
                    it.copy(
                        saving = false, awaitingConfirm = false, conflict = 0,
                        message = if (out.ok) "Schedule saved. Patients see the new slots now." else out.message
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(saving = false, message = "Could not save the schedule. Try again.") }
            }
        }
    }
}
