package com.mobile360.healthcare.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobile360.healthcare.data.AuthRepository
import com.mobile360.healthcare.data.model.Role
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import com.mobile360.healthcare.data.AccountRepository
import kotlinx.coroutines.flow.flowOf

sealed interface AppState {
    data object Loading : AppState
    data object SignedOut : AppState
    data class SignedIn(val role: Role) : AppState
}

@HiltViewModel
class MainViewModel @Inject constructor(auth: AuthRepository,
    accountRepository: AccountRepository? = null) : ViewModel() {
    val state: StateFlow<AppState> = auth.session
        .map { s -> if (s == null) AppState.SignedOut else AppState.SignedIn(s.role) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppState.Loading)

        // CR-002: stored light/dark choice for the app-wide theme
        val darkTheme: StateFlow<Boolean> = (accountRepository?.darkTheme ?: flowOf(false))
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)
}
