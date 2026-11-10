package com.mobile360.healthcare.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobile360.healthcare.data.AuthRepository
import com.mobile360.healthcare.data.model.CodeRequestResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class PhoneEntryState(val loading: Boolean = false, val error: String? = null)

@HiltViewModel
class PhoneEntryViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val saved: SavedStateHandle
) : ViewModel() {
    val phone: StateFlow<String> = saved.getStateFlow("phone", "")
    private val _state = MutableStateFlow(PhoneEntryState())
    val state: StateFlow<PhoneEntryState> = _state.asStateFlow()
    private val _issued = Channel<Unit>(Channel.BUFFERED)
    val issued = _issued.receiveAsFlow()

    fun onPhoneChange(text: String) {
        val digits = text.filter { it.isDigit() }.take(10)
        saved["phone"] = if (digits.length > 5) digits.substring(0, 5) + " " + digits.substring(5) else digits
        _state.value = _state.value.copy(error = null)
    }

    fun send() {
        if (_state.value.loading) return
        val digits = phone.value.filter { it.isDigit() }
        if (digits.length != 10) {
            _state.value = PhoneEntryState(error = "Enter a valid 10-digit mobile number.")
            return
        }
        _state.value = PhoneEntryState(loading = true)
        viewModelScope.launch {
            when (val r = auth.requestCode(digits)) {
                is CodeRequestResult.Issued -> {
                    PendingCode.record(digits, r)
                    _state.value = PhoneEntryState()
                    _issued.send(Unit)
                }
                CodeRequestResult.InvalidNumber ->
                    _state.value = PhoneEntryState(error = "This number is not valid or, for doctors, not approved by admin.")
                is CodeRequestResult.Locked ->
                    _state.value = PhoneEntryState(error = "Too many wrong codes. Sign-in is locked for ${r.minutesLeft} more minute(s).")
            }
        }
    }
}
