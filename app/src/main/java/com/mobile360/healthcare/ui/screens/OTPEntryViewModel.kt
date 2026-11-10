package com.mobile360.healthcare.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobile360.healthcare.data.AuthRepository
import com.mobile360.healthcare.data.model.CodeRequestResult
import com.mobile360.healthcare.data.model.Role
import com.mobile360.healthcare.data.model.VerifyResult
import com.mobile360.healthcare.util.AppConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class OTPEntryState(
    val maskedPhone: String = "",
    val expiresAtMillis: Long = 0L,
    val attemptsLeft: Int = AppConfig.MAX_ATTEMPTS,
    val loading: Boolean = false,
    val error: String? = null,
    val info: String? = null,
    val verifiedRole: Role? = null
)

@HiltViewModel
class OTPEntryViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val saved: SavedStateHandle
) : ViewModel() {
    val code: StateFlow<String> = saved.getStateFlow("code", "")
    private val _state = MutableStateFlow(OTPEntryState())
    val state: StateFlow<OTPEntryState> = _state.asStateFlow()
    private val _done = Channel<Role>(Channel.BUFFERED)
    val done = _done.receiveAsFlow()

    init {
        PendingCode.issued?.let {
            _state.value = _state.value.copy(maskedPhone = it.maskedPhone, expiresAtMillis = it.expiresAtMillis)
        }
    }

    fun onCodeChange(text: String) {
        saved["code"] = text.filter { it.isDigit() }.take(6)
        _state.value = _state.value.copy(error = null)
    }

    fun confirm() {
        if (_state.value.loading) return
        if (code.value.length != 6) {
            _state.value = _state.value.copy(error = "Enter the 6-digit code.", info = null)
            return
        }
        _state.value = _state.value.copy(loading = true, error = null, info = null)
        viewModelScope.launch {
            val r = auth.verifyCode(code.value)
            val s = _state.value.copy(loading = false)
            _state.value = when (r) {
                is VerifyResult.Success -> s.copy(verifiedRole = r.role)
                is VerifyResult.Wrong -> s.copy(attemptsLeft = r.attemptsLeft, error = "Wrong code. ${r.attemptsLeft} attempt(s) left.")
                VerifyResult.Expired -> s.copy(error = "This code expired. Request a new code.")
                is VerifyResult.Locked -> s.copy(attemptsLeft = 0, error = "Too many wrong codes. Sign-in is locked for ${r.minutesLeft} more minute(s).")
                VerifyResult.NoRequest -> s.copy(error = "No code was requested. Go back and send a code first.")
            }
            if (r is VerifyResult.Success) _done.send(r.role)
        }
    }

    fun resend() {
        val phone = PendingCode.phoneDigits
        if (phone == null) {
            _state.value = _state.value.copy(error = "No number to resend to. Go back and enter your number.", info = null)
            return
        }
        _state.value = _state.value.copy(loading = true, error = null, info = null)
        viewModelScope.launch {
            when (val r = auth.requestCode(phone)) {
                is CodeRequestResult.Issued -> {
                    PendingCode.record(phone, r)
                    saved["code"] = ""
                    _state.value = _state.value.copy(
                        loading = false, maskedPhone = r.maskedPhone, expiresAtMillis = r.expiresAtMillis,
                        attemptsLeft = AppConfig.MAX_ATTEMPTS, info = "A new demo code is active. Use 123456."
                    )
                }
                CodeRequestResult.InvalidNumber ->
                    _state.value = _state.value.copy(loading = false, error = "This number is not valid or not approved.")
                is CodeRequestResult.Locked ->
                    _state.value = _state.value.copy(loading = false, attemptsLeft = 0, error = "Too many wrong codes. Sign-in is locked for ${r.minutesLeft} more minute(s).")
            }
        }
    }
}
