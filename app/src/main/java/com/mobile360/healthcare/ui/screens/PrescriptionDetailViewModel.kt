package com.mobile360.healthcare.ui.screens

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobile360.healthcare.data.AccountRepository
import com.mobile360.healthcare.data.RecordsRepository
import com.mobile360.healthcare.data.local.MedicationEntity
import com.mobile360.healthcare.data.local.PrescriptionView
import com.mobile360.healthcare.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed interface RxDetailUi {
    data object Loading : RxDetailUi
    data object NotFound : RxDetailUi
    data object Error : RxDetailUi
    data class Found(val rx: PrescriptionView, val meds: List<MedicationEntity>) : RxDetailUi
}

data class RxDownloadState(
    val askCode: Boolean = false,
    val working: Boolean = false,
    val codeError: String? = null,
    val saved: RecordsSavedFile? = null,
    val failed: Boolean = false
)

@HiltViewModel
class PrescriptionDetailViewModel @Inject constructor(
    handle: SavedStateHandle,
    records: RecordsRepository,
    private val accounts: AccountRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val id: String = handle.get<String>(Routes.ARG_PRESCRIPTION_ID).orEmpty()

    val ui: StateFlow<RxDetailUi> = combine(records.prescription(id), records.medications(id)) { rx, meds ->
        if (rx == null) RxDetailUi.NotFound else RxDetailUi.Found(rx, meds)
    }.catch { emit(RxDetailUi.Error) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RxDetailUi.Loading)

    private val _download = MutableStateFlow(RxDownloadState())
    val download: StateFlow<RxDownloadState> = _download

    fun askForCode() { _download.update { RxDownloadState(askCode = true) } }
    fun dismissCode() { _download.update { it.copy(askCode = false, codeError = null) } }

    fun confirmCode(code: String) {
        if (code.isBlank()) { _download.update { it.copy(codeError = "Enter your authentication code.") }; return }
        viewModelScope.launch {
            _download.update { it.copy(working = true, codeError = null) }
            val out = accounts.reAuthenticate(code.trim())
            if (!out.ok) {
                _download.update { it.copy(working = false, codeError = out.message) }
            } else {
                _download.update { it.copy(askCode = false) }
                save()
            }
        }
    }

    fun retrySave() { viewModelScope.launch { _download.update { it.copy(working = true) }; save() } }

    private suspend fun save() {
        val found = ui.value as? RxDetailUi.Found
        if (found == null) { _download.update { it.copy(working = false, failed = true) }; return }
        val result = withContext(Dispatchers.IO) { runCatching { rxDetailSavePdf(context, found.rx, found.meds) } }
        _download.update { it.copy(working = false, saved = result.getOrNull(), failed = result.isFailure) }
    }
}
