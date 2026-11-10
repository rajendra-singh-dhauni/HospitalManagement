package com.mobile360.healthcare.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobile360.healthcare.data.AuthRepository
import com.mobile360.healthcare.data.ChatRepository
import com.mobile360.healthcare.data.ConnectivityObserver
import com.mobile360.healthcare.data.local.MessageEntity
import com.mobile360.healthcare.data.local.ThreadView
import com.mobile360.healthcare.data.model.Session
import com.mobile360.healthcare.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ThreadSelection(
    val threads: List<ThreadView>,
    val current: ThreadView?,
    val missing: Boolean
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val chats: ChatRepository,
    auth: AuthRepository,
    private val connectivity: ConnectivityObserver
) : ViewModel() {
    private val requested: String = savedState.get<String>(Routes.ARG_APPOINTMENT_ID).orEmpty()
        .takeIf { !it.startsWith("{") }.orEmpty()

    val session: StateFlow<Session?> = auth.session.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val selectedId: StateFlow<String> = savedState.getStateFlow("selected", requested)
    val draft: StateFlow<String> = savedState.getStateFlow("draft", "")
    val query: StateFlow<String> = savedState.getStateFlow("query", "")
    val searching: StateFlow<Boolean> = savedState.getStateFlow("searching", false)

    // null while loading
    val selection: StateFlow<ThreadSelection?> = combine(chats.threads(), selectedId) { threads, id ->
        val found = threads.firstOrNull { it.appointmentId == id }
        when {
            id.isBlank() -> ThreadSelection(threads, threads.firstOrNull(), false)
            found != null -> ThreadSelection(threads, found, false)
            else -> ThreadSelection(threads, null, true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val messages: StateFlow<List<MessageEntity>?> = combine(
        selection.map { it?.current?.appointmentId }.flatMapLatest { id ->
            if (id == null) flowOf(null) else chats.messages(id)
        },
        query
    ) { list, q ->
        list?.sortedBy { it.timeMillis }?.filter { q.isBlank() || it.text.contains(q.trim(), ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val online: StateFlow<Boolean> = connectivity.online
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), connectivity.isOnlineNow())

    val error = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            connectivity.online.collect { if (it) chats.flushQueued() }
        }
    }

    fun select(id: String) {
        savedState["selected"] = id
        savedState["query"] = ""
        error.value = null
    }

    fun showAll() = select("")

    fun setDraft(text: String) { savedState["draft"] = text }
    fun setQuery(text: String) { savedState["query"] = text }
    fun setSearching(on: Boolean) {
        savedState["searching"] = on
        if (!on) savedState["query"] = ""
    }

    fun send(appointmentId: String, text: String = draft.value, clearDraft: Boolean = true) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            val outcome = chats.send(appointmentId, clean, connectivity.isOnlineNow())
            if (outcome.ok) {
                if (clearDraft) savedState["draft"] = ""
                error.value = null
            } else {
                error.value = outcome.message
            }
        }
    }

    fun retry(messageId: String) {
        viewModelScope.launch {
            val outcome = chats.retry(messageId, connectivity.isOnlineNow())
            error.value = if (outcome.ok) null else outcome.message
        }
    }

    fun report(message: String) { error.value = message }
}
