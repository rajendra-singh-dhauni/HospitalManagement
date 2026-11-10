package com.mobile360.healthcare.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobile360.healthcare.data.ConnectivityObserver
import com.mobile360.healthcare.data.DoctorRepository
import com.mobile360.healthcare.data.local.DoctorListItem
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

data class DoctorSearchState(
    val loading: Boolean = true,
    val error: String? = null,
    val query: String = "",
    val specialty: String = "",
    val specialties: List<String> = emptyList(),
    val doctors: List<DoctorListItem> = emptyList(),
    val online: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DoctorSearchViewModel @Inject constructor(
    private val saved: SavedStateHandle,
    private val repo: DoctorRepository,
    connectivity: ConnectivityObserver
) : ViewModel() {
    private val query = saved.getStateFlow("query", "")
    private val specialty = saved.getStateFlow("specialty", "")
    private val retry = MutableStateFlow(0)

    private val results = combine(query, specialty, retry) { q, s, _ -> q.trim() to s }
        .flatMapLatest { (q, s) ->
            repo.search(q, s)
                .map<List<DoctorListItem>, Pair<List<DoctorListItem>, String?>> { it to null }
                .catch { emit(emptyList<DoctorListItem>() to "We could not load doctors.") }
        }

    val state: StateFlow<DoctorSearchState> = combine(results, query, specialty, connectivity.online) { r, q, s, online ->
        DoctorSearchState(
            loading = false, error = r.second, query = q, specialty = s,
            specialties = repo.specialties(), doctors = r.first, online = online
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DoctorSearchState(specialties = repo.specialties()))

    fun setQuery(value: String) { saved["query"] = value }
    fun toggleSpecialty(name: String) { saved["specialty"] = if (specialty.value == name) "" else name }
    fun clearQuery() { saved["query"] = "" }
    fun retry() { retry.value += 1 }
}
