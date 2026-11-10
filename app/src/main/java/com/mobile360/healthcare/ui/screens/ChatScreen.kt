package com.mobile360.healthcare.ui.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mobile360.healthcare.R
import com.mobile360.healthcare.data.local.MessageEntity
import com.mobile360.healthcare.data.local.ThreadView
import com.mobile360.healthcare.data.model.MessageStatus
import com.mobile360.healthcare.data.model.Role
import com.mobile360.healthcare.ui.components.BadgeTone
import com.mobile360.healthcare.ui.components.BrandTopBar
import com.mobile360.healthcare.ui.components.EmptyState
import com.mobile360.healthcare.ui.components.InfoBanner
import com.mobile360.healthcare.ui.components.LoadingPlaceholder
import com.mobile360.healthcare.ui.components.PersonAvatar
import com.mobile360.healthcare.ui.theme.AppColors
import com.mobile360.healthcare.util.Formats
import com.mobile360.healthcare.util.Times
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private sealed interface ChatItem {
    data class Header(val label: String) : ChatItem
    data class Msg(val message: MessageEntity) : ChatItem
}

private fun buildItems(messages: List<MessageEntity>): List<ChatItem> {
    val out = mutableListOf<ChatItem>()
    var last: LocalDate? = null
    val today = LocalDate.now()
    messages.forEach { m ->
        val d = Times.toDate(m.timeMillis)
        if (d != last) {
            last = d
            val pattern = DateTimeFormatter.ofPattern("MMMM d", Locale.getDefault())
            out += ChatItem.Header(if (d == today) "Today, ${d.format(pattern)}" else d.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())))
        }
        out += ChatItem.Msg(m)
    }
    return out
}

@Composable
fun ChatScreen(
    onOpenSettings: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val selection by viewModel.selection.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val searching by viewModel.searching.collectAsStateWithLifecycle()
    val online by viewModel.online.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var voiceForSearch by rememberSaveable { mutableStateOf(false) }
    val isDoctor = session?.role == Role.DOCTOR
    val current = selection?.current

    val speech = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val text = r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (r.resultCode == Activity.RESULT_OK && !text.isNullOrBlank()) {
            if (voiceForSearch) viewModel.setQuery(text) else viewModel.setDraft((draft + " " + text).trim())
        }
    }
    fun startVoice(forSearch: Boolean) {
        voiceForSearch = forSearch
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        }
        try {
            speech.launch(intent)
        } catch (_: ActivityNotFoundException) {
            viewModel.report("Voice input is not installed on this phone.")
        }
    }
    val attach = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null && current != null) {
            val name = context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (i >= 0 && c.moveToFirst()) c.getString(i) else null
            } ?: uri.lastPathSegment ?: "document"
            viewModel.send(current.appointmentId, "Attachment: $name", clearDraft = false)
        }
    }

    Column(Modifier.fillMaxSize().background(AppColors.surface)) {
        BrandTopBar(actions = {
            IconButton(onClick = { viewModel.setSearching(!searching) }) {
                Icon(Icons.Filled.Search, contentDescription = "Search conversation")
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Info, contentDescription = "Conversation info")
            }
        })
        if (!online) InfoBanner(stringResource(R.string.offline_banner), modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), icon = Icons.Filled.WifiOff)
        val sel = selection
        when {
            sel == null -> LoadingPlaceholder()
            sel.missing -> EmptyState(
                title = "Conversation not available",
                message = "This conversation does not exist for your account.",
                icon = Icons.Filled.Chat,
                actionLabel = "View all conversations",
                onAction = viewModel::showAll
            )
            sel.threads.isEmpty() || current == null -> EmptyState(
                title = "No conversations yet",
                message = if (isDoctor) "Chats open when a patient has an appointment with you." else "Chats open when you have an appointment with a doctor.",
                icon = Icons.Filled.Chat
            )
            else -> {
                Column(Modifier.weight(1f)) {
                    ThreadStrip(sel.threads, current, isDoctor, viewModel::select)
                    ContactCard(current, isDoctor, onCall = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + current.doctorPhone)))
                        } catch (_: ActivityNotFoundException) {
                            viewModel.report("No phone app is available.")
                        }
                    })
                    if (searching) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = viewModel::setQuery,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            placeholder = { Text("Search conversation") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Filled.Search, null) },
                            trailingIcon = {
                                Row {
                                    if (query.isNotEmpty()) IconButton(onClick = { viewModel.setQuery("") }) {
                                        Icon(Icons.Filled.Cancel, contentDescription = "Clear search input")
                                    }
                                    IconButton(onClick = { startVoice(true) }) {
                                        Icon(Icons.Filled.Mic, contentDescription = "Voice search")
                                    }
                                }
                            },
                            shape = CircleShape
                        )
                    }
                    val list = messages
                    val items = buildItems(list.orEmpty())
                    val listState = rememberLazyListState()
                    LaunchedEffect(items.size, current.appointmentId) {
                        if (items.isNotEmpty()) listState.animateScrollToItem(items.lastIndex)
                    }
                    when {
                        list == null -> LoadingPlaceholder(Modifier.weight(1f), lines = 2)
                        list.isEmpty() -> Box(Modifier.weight(1f)) {
                            if (query.isNotBlank()) EmptyState("No matching messages", "Nothing in this conversation matches your search.", icon = Icons.Filled.Search)
                            else EmptyState("No messages yet", "Say hello to start the conversation.", icon = Icons.Filled.Chat)
                        }
                        else -> LazyColumn(
                            Modifier.weight(1f).fillMaxWidth(),
                            state = listState,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(items) { item ->
                                when (item) {
                                    is ChatItem.Header -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        Text(
                                            item.label,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = AppColors.outline,
                                            modifier = Modifier.clip(CircleShape).background(AppColors.surfaceContainerHigh).padding(horizontal = 12.dp, vertical = 2.dp)
                                        )
                                    }
                                    is ChatItem.Msg -> MessageRow(
                                        item.message,
                                        mine = item.message.senderId == session?.chatId,
                                        otherImage = if (isDoctor) current.patientImage else current.doctorImage,
                                        otherName = if (isDoctor) current.patientName else current.doctorName,
                                        onRetry = { viewModel.retry(item.message.id) }
                                    )
                                }
                            }
                            item {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                    Row(
                                        Modifier.clip(RoundedCornerShape(8.dp)).background(AppColors.surfaceContainer).padding(horizontal = 12.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Filled.Security, null, tint = AppColors.primary, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("End-to-end encrypted medical teleconsultation", style = MaterialTheme.typography.bodySmall, color = AppColors.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                    error?.let { InfoBanner(it, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), icon = Icons.Filled.ErrorOutline, tone = BadgeTone.Negative) }
                    Surface(color = AppColors.surface, shadowElevation = 4.dp) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { attach.launch("*/*") }) {
                                Icon(Icons.Filled.AttachFile, contentDescription = "Attach medical document or photo", tint = AppColors.onSurfaceVariant)
                            }
                            OutlinedTextField(
                                value = draft,
                                onValueChange = viewModel::setDraft,
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Type a message...") },
                                shape = CircleShape,
                                maxLines = 3,
                                trailingIcon = {
                                    IconButton(onClick = { startVoice(false) }) {
                                        Icon(Icons.Filled.Mic, contentDescription = "Voice input", tint = AppColors.outline)
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppColors.primary, unfocusedBorderColor = AppColors.outline.copy(alpha = 0.3f))
                            )
                            Spacer(Modifier.width(8.dp))
                            IconButton(
                                onClick = { viewModel.send(current.appointmentId) },
                                enabled = draft.isNotBlank(),
                                modifier = Modifier.size(48.dp).clip(CircleShape).background(if (draft.isNotBlank()) AppColors.primary else AppColors.surfaceContainerHigh)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send message", tint = if (draft.isNotBlank()) AppColors.onPrimary else AppColors.outline)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThreadStrip(threads: List<ThreadView>, current: ThreadView, isDoctor: Boolean, onSelect: (String) -> Unit) {
    Column(Modifier.padding(top = 8.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("CARE TEAM THREADS", style = MaterialTheme.typography.bodySmall, color = AppColors.onSurfaceVariant, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            val n = threads.size
            val noun = if (isDoctor) (if (n == 1) "Patient" else "Patients") else (if (n == 1) "Doctor" else "Doctors")
            Text("$n $noun Available", style = MaterialTheme.typography.bodySmall, color = AppColors.primary, fontWeight = FontWeight.Medium)
        }
        LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(threads, key = { it.appointmentId }) { t ->
                val selected = t.appointmentId == current.appointmentId
                val name = if (isDoctor) t.patientName else t.doctorName
                val image = if (isDoctor) t.patientImage else t.doctorImage
                val sub = (if (isDoctor) "Patient" else t.doctorSpecialty) + " • " + (t.lastMessage ?: "No messages yet")
                Row(
                    Modifier.clip(RoundedCornerShape(12.dp))
                        .background(if (selected) AppColors.surfaceContainerHigh else AppColors.surfaceContainerLowest)
                        .border(if (selected) BorderStroke(2.dp, AppColors.primary.copy(alpha = 0.4f)) else BorderStroke(1.dp, AppColors.outlineVariant.copy(alpha = 0.3f)), RoundedCornerShape(12.dp))
                        .clickable { onSelect(t.appointmentId) }.heightIn(min = 48.dp).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PersonAvatar(image, name, size = 40.dp)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(name, style = MaterialTheme.typography.labelLarge, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
                            if (selected) { Spacer(Modifier.width(4.dp)); Icon(Icons.Filled.Verified, null, tint = AppColors.primary, modifier = Modifier.size(14.dp)) }
                        }
                        Text(sub, style = MaterialTheme.typography.bodySmall, color = AppColors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 130.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactCard(t: ThreadView, isDoctor: Boolean, onCall: () -> Unit) {
    Surface(
        color = AppColors.surfaceContainerLow,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        border = BorderStroke(1.dp, AppColors.outlineVariant.copy(alpha = 0.2f))
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(AppColors.secondaryContainer), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Favorite, null, tint = AppColors.onSecondaryContainer, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(if (isDoctor) t.patientName else t.doctorName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    if (isDoctor) "Patient" else t.doctorSpecialty,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.onSecondaryContainer,
                    modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(AppColors.secondaryContainer.copy(alpha = 0.6f)).padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            if (!isDoctor) IconButton(onClick = onCall) {
                Icon(Icons.Filled.Call, contentDescription = "Call Doctor", tint = AppColors.primary)
            }
        }
    }
}

@Composable
private fun MessageRow(m: MessageEntity, mine: Boolean, otherImage: Int, otherName: String, onRetry: () -> Unit) {
    if (!mine) {
        Row(Modifier.fillMaxWidth(0.85f), verticalAlignment = Alignment.Bottom) {
            PersonAvatar(otherImage, otherName, size = 28.dp)
            Spacer(Modifier.width(8.dp))
            Column {
                Surface(
                    color = AppColors.surfaceContainerLowest,
                    shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
                    border = BorderStroke(1.dp, AppColors.outlineVariant.copy(alpha = 0.3f))
                ) { Text(m.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp)) }
                Text(Formats.time(m.timeMillis), style = MaterialTheme.typography.bodySmall, color = AppColors.outline, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
            }
        }
    } else {
        val pending = m.status == MessageStatus.QUEUED || m.status == MessageStatus.FAILED
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Column(Modifier.fillMaxWidth(0.85f), horizontalAlignment = Alignment.End) {
                Surface(
                    color = if (pending) AppColors.surfaceVariant else AppColors.secondaryContainer,
                    shape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp),
                    border = if (pending) BorderStroke(1.dp, AppColors.outline) else null
                ) { Text(m.text, style = MaterialTheme.typography.bodyMedium, color = AppColors.onSecondaryFixed, modifier = Modifier.padding(12.dp)) }
                Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (pending) {
                        Icon(Icons.Filled.Schedule, null, tint = AppColors.tertiary, modifier = Modifier.size(16.dp))
                        Text(
                            if (m.status == MessageStatus.FAILED) "Failed to send" else "Queued, will send when online",
                            style = MaterialTheme.typography.bodySmall, color = AppColors.tertiary, fontWeight = FontWeight.Medium
                        )
                        TextButton(onClick = onRetry) {
                            Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Retry")
                        }
                    } else {
                        Text(Formats.time(m.timeMillis), style = MaterialTheme.typography.bodySmall, color = AppColors.outline)
                        Text("•", style = MaterialTheme.typography.bodySmall, color = AppColors.outline)
                        Icon(Icons.Filled.DoneAll, null, tint = AppColors.secondary, modifier = Modifier.size(14.dp))
                        Text(
                            if (m.status == MessageStatus.READ) "Read" else "Delivered",
                            style = MaterialTheme.typography.bodySmall, color = AppColors.secondary, fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
