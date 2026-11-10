package com.mobile360.healthcare.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobile360.healthcare.data.AccountRepository
import com.mobile360.healthcare.data.AuthRepository
import com.mobile360.healthcare.data.model.Session
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SecurityMessage(val section: String, val text: String, val ok: Boolean)

data class SecurityUi(val busy: Boolean = false, val message: SecurityMessage? = null)

@HiltViewModel
class SecurityAndAccountViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val account: AccountRepository
) : ViewModel() {
    val session: StateFlow<Session?> = auth.session.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    private val _state = MutableStateFlow(SecurityUi())
    val state: StateFlow<SecurityUi> = _state

    private fun run(section: String, block: suspend () -> Pair<Boolean, String>, after: (Boolean) -> Unit = {}) {
        if (_state.value.busy) return
        _state.update { SecurityUi(busy = true) }
        viewModelScope.launch {
            val (ok, text) = try { block() } catch (e: Exception) { false to "Something went wrong. Try again." }
            _state.update { SecurityUi(busy = false, message = SecurityMessage(section, text, ok)) }
            after(ok)
        }
    }

    fun signOutOthers() = run("devices", { account.signOutOtherDevices().let { it.ok to it.message } })

    fun changeNumber(newPhone: String, code: String, onDone: () -> Unit) {
        val digits = newPhone.filter { it.isDigit() }
        if (digits.length != 10) {
            _state.update { SecurityUi(message = SecurityMessage("phone", "Enter a 10-digit mobile number.", false)) }
            return
        }
        run("phone", { account.changeNumber(digits, code.trim()).let { it.ok to it.message } }) { if (it) onDone() }
    }

    fun deleteAccount(code: String, onDone: () -> Unit) =
        run("delete", { account.deleteAccount(code.trim()).let { it.ok to it.message } }) { if (it) viewModelScope.launch { auth.signOut(); onDone() } }

    fun signOut(onDone: () -> Unit) {
        viewModelScope.launch { auth.signOut(); onDone() }
    }

    fun invalid(section: String, text: String) = _state.update { SecurityUi(message = SecurityMessage(section, text, false)) }
}
