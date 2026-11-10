package com.mobile360.healthcare.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobile360.healthcare.data.AccountRepository
import com.mobile360.healthcare.data.AuthRepository
import com.mobile360.healthcare.data.model.AlertKind
import com.mobile360.healthcare.data.model.AlertPrefs
import com.mobile360.healthcare.data.model.Session
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    auth: AuthRepository,
    private val account: AccountRepository
) : ViewModel() {
    val session: StateFlow<Session?> = auth.session.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val prefs: StateFlow<AlertPrefs> = account.alertPrefs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AlertPrefs())
    val error = MutableStateFlow<String?>(null)

    fun setAlert(kind: AlertKind, enabled: Boolean) {
        viewModelScope.launch {
            try {
                account.setAlert(kind, enabled)
                error.value = null
            } catch (e: Exception) {
                error.value = "Could not save this preference. Try again."
            }
        }
    }

    fun report(text: String) { error.value = text }

        // CR-002: dark theme preference; the switch follows the stored value, so a failed save leaves it unchanged
        val darkTheme: StateFlow<Boolean> =
            account.darkTheme.stateIn(viewModelScope, SharingStarted.Eagerly, false)

        fun setDarkTheme(enabled: Boolean) {
            viewModelScope.launch {
                try {
                    account.setDarkTheme(enabled)
                } catch (e: Exception) {
                    report("Could not save this preference. Try again.")
                }
            }
        }
}
