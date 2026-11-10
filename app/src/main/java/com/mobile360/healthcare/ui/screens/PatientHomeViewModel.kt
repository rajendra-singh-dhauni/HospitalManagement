package com.mobile360.healthcare.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobile360.healthcare.data.AppointmentRepository
import com.mobile360.healthcare.data.AuthRepository
import com.mobile360.healthcare.data.ChatRepository
import com.mobile360.healthcare.data.ConnectivityObserver
import com.mobile360.healthcare.data.RecordsRepository
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

data class PatientHomeState(
    val loading: Boolean = true,
    val error: String? = null,
    val firstName: String = "",
    val next: AppointmentView? = null,
    val activePrescriptions: Int = 0,
    val threads: Int = 0,
    val online: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PatientHomeViewModel @Inject constructor(
    auth: AuthRepository,
    appointments: AppointmentRepository,
    records: RecordsRepository,
    chat: ChatRepository,
    connectivity: ConnectivityObserver
) : ViewModel() {
    private val retry = MutableStateFlow(0)

    val state: StateFlow<PatientHomeState> = retry.flatMapLatest {
        combine(
            auth.session,
            appointments.next(),
            records.prescriptions(),
            chat.threads(),
            connectivity.online
        ) { s, next, rx, th, online ->
            PatientHomeState(
                loading = false,
                firstName = s?.name?.trim()?.substringBefore(' ').orEmpty(),
                next = next,
                activePrescriptions = rx.count { !it.status.equals("EXPIRED", true) && !it.status.equals("CANCELLED", true) },
                threads = th.size,
                online = online
            )
        }.catch { emit(PatientHomeState(loading = false, error = "We could not load your home screen.")) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PatientHomeState())

    fun retry() { retry.value += 1 }
}
