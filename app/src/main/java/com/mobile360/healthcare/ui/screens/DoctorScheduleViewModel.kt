package com.mobile360.healthcare.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobile360.healthcare.data.AppointmentRepository
import com.mobile360.healthcare.data.ConnectivityObserver
import com.mobile360.healthcare.data.local.AppointmentView
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DoctorScheduleState(
    val loading: Boolean = true,
    val error: String? = null,
    val appointments: List<AppointmentView> = emptyList(),
    val online: Boolean = true,
    val message: String? = null,
    val showRequests: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DoctorScheduleViewModel @Inject constructor(
    private val repo: AppointmentRepository,
    connectivity: ConnectivityObserver
) : ViewModel() {
    private val retry = MutableStateFlow(0)
    private val message = MutableStateFlow<String?>(null)
    private val tab = MutableStateFlow(false)

    private val data = retry.flatMapLatest {
        repo.todayForDoctor()
            .map { DoctorScheduleState(loading = false, appointments = it.sortedBy { a -> a.startMillis }) }
            .catch { emit(DoctorScheduleState(loading = false, error = "We could not load today's schedule.")) }
    }

    val state: StateFlow<DoctorScheduleState> = combine(data, connectivity.online, message, tab) { d, online, m, t ->
        d.copy(online = online, message = m, showRequests = t)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DoctorScheduleState())

    fun retry() { retry.value += 1 }
    fun showRequests(value: Boolean) { tab.value = value }
    fun dismissMessage() { message.value = null }

    fun accept(id: String) {
        viewModelScope.launch { message.value = repo.accept(id).message }
    }

    fun reject(id: String, reason: String) {
        viewModelScope.launch { message.value = repo.reject(id, reason).message }
    }
}
