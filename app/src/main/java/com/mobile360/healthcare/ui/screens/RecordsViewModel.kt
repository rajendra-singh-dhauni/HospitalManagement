package com.mobile360.healthcare.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobile360.healthcare.data.RecordsRepository
import com.mobile360.healthcare.data.local.PrescriptionView
import com.mobile360.healthcare.data.local.TimelineEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class RxRow(val rx: PrescriptionView, val medCount: Int)

data class RecordsUi(
    val loading: Boolean = true,
    val error: String? = null,
    val prescriptions: List<RxRow> = emptyList(),
    val timeline: List<TimelineEntity> = emptyList(),
    val anyPrescriptions: Boolean = false,
    val anyTimeline: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecordsViewModel @Inject constructor(
    private val repo: RecordsRepository,
    private val handle: SavedStateHandle
) : ViewModel() {
    val query: StateFlow<String> = handle.getStateFlow("query", "")
    val tab: StateFlow<Int> = handle.getStateFlow("tab", 0)
    val preset: StateFlow<String> = handle.getStateFlow("preset", "3m")
    private val customFrom: StateFlow<Long> = handle.getStateFlow("cfrom", 0L)
    private val customTo: StateFlow<Long> = handle.getStateFlow("cto", 0L)
    private val reload = MutableStateFlow(0)

    fun setQuery(q: String) { handle["query"] = q }
    fun setTab(t: Int) { handle["tab"] = t }
    fun setPreset(p: String) { handle["preset"] = p }
    fun setCustom(from: Long, to: Long) {
        handle["cfrom"] = from
        handle["cto"] = to
        handle["preset"] = "custom"
    }
    fun retry() { reload.value = reload.value + 1 }

    val rangeLabel: StateFlow<String> = combine(preset, customFrom, customTo) { p, f, t ->
        when (p) {
            "1m" -> "Last month"
            "3m" -> "Last 3 months"
            "6m" -> "Last 6 months"
            "all" -> "All time"
            else -> recordsDate(f) + " - " + recordsDate(t)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Last 3 months")

    private fun months(n: Long): Long =
        LocalDate.now().minusMonths(n).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private val range = combine(preset, customFrom, customTo) { p, f, t ->
        when (p) {
            "1m" -> months(1) to null
            "3m" -> months(3) to null
            "6m" -> months(6) to null
            "all" -> null to null
            else -> f to (t + 86_399_999L)
        }
    }

    private val rx = reload.flatMapLatest {
        repo.prescriptions().flatMapLatest { list ->
            if (list.isEmpty()) flowOf(emptyList())
            else combine(list.map { r -> repo.medications(r.id).map { RxRow(r, it.size) } }) { it.toList() }
        }
    }

    private val tl = combine(reload, range) { _, r -> r }.flatMapLatest { (f, t) -> repo.timeline(f, t) }

    val ui: StateFlow<RecordsUi> = combine(rx, tl, query) { rows, items, q ->
        val needle = q.trim()
        val rf = rows.sortedByDescending { it.rx.issuedMillis }.filter {
            needle.isEmpty() || it.rx.doctorName.contains(needle, true) ||
                it.rx.doctorSpecialty.contains(needle, true) || it.rx.rxNumber.contains(needle, true)
        }
        val tf = items.sortedByDescending { it.dateMillis }.filter {
            needle.isEmpty() || it.title.contains(needle, true) || it.detail.contains(needle, true)
        }
        RecordsUi(false, null, rf, tf, rows.isNotEmpty(), items.isNotEmpty())
    }.catch { emit(RecordsUi(loading = false, error = "Could not load your records. Please try again.")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RecordsUi())
}
