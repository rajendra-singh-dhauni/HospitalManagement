package com.mobile360.healthcare.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mobile360.healthcare.data.local.AppointmentView
import com.mobile360.healthcare.data.model.AppointmentStatus
import com.mobile360.healthcare.ui.components.BadgeTone
import com.mobile360.healthcare.ui.components.BrandTopBar
import com.mobile360.healthcare.ui.components.ButtonKind
import com.mobile360.healthcare.ui.components.CareButton
import com.mobile360.healthcare.ui.components.CareCard
import com.mobile360.healthcare.ui.components.EmptyState
import com.mobile360.healthcare.ui.components.InfoBanner
import com.mobile360.healthcare.ui.components.LoadingPlaceholder
import com.mobile360.healthcare.ui.components.PersonAvatar
import com.mobile360.healthcare.ui.components.StatusBadge
import com.mobile360.healthcare.ui.theme.AppColors
import com.mobile360.healthcare.util.Formats

@Composable
fun AppointmentsScreen(
    onOpenDoctorProfileAndBooking: (doctorId: String, appointmentId: String) -> Unit,
    onOpenChat: (appointmentId: String) -> Unit,
    onOpenRecords: () -> Unit,
    viewModel: AppointmentsViewModel = hiltViewModel()
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val showPast by viewModel.showPast.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showBell by rememberSaveable { mutableStateOf(false) }
    var cancelTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var mapNote by remember { mutableStateOf<String?>(null) }

    val now = System.currentTimeMillis()
    val list = items
    val upcoming = list.orEmpty().filter { viewModel.isUpcoming(it, now) }.sortedBy { it.startMillis }
    val past = list.orEmpty().filter { !viewModel.isUpcoming(it, now) }.sortedByDescending { it.startMillis }

    Column(Modifier.fillMaxSize().background(AppColors.background)) {
        BrandTopBar(actions = {
            IconButton(onClick = { showBell = true }) {
                Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
            }
        })
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Appointments", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "Manage your consultations and clinical visits",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.onSurfaceVariant
                )
            }
            Row(
                Modifier.fillMaxWidth().clip(CircleShape).background(AppColors.surfaceContainerHigh).padding(4.dp)
            ) {
                TabPill("Upcoming", !showPast, Modifier.weight(1f)) { viewModel.setPast(false) }
                TabPill("Past", showPast, Modifier.weight(1f)) { viewModel.setPast(true) }
            }
            (message ?: mapNote)?.let {
                InfoBanner(it, icon = Icons.Filled.Event, tone = BadgeTone.Warning, actionLabel = "OK", onAction = {
                    viewModel.dismissMessage(); mapNote = null
                })
            }
        }
        val shown = if (showPast) past else upcoming
        when {
            list == null -> LoadingPlaceholder()
            shown.isEmpty() -> EmptyState(
                title = if (showPast) "No past appointments" else "No upcoming appointments",
                message = if (showPast) "Completed and cancelled visits will appear here." else "Book a doctor to see your appointments here.",
                icon = Icons.Filled.CalendarToday
            )
            else -> LazyColumn(
                Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(shown, key = { it.id }) { a ->
                    if (showPast) {
                        PastCard(a, onBookAgain = { onOpenDoctorProfileAndBooking(a.doctorId, "") }, onRecords = onOpenRecords)
                    } else {
                        UpcomingCard(
                            a,
                            onChat = { onOpenChat(a.id) },
                            onCancel = { cancelTarget = a.id },
                            onReschedule = {
                                if (viewModel.canReschedule(a)) onOpenDoctorProfileAndBooking(a.doctorId, a.id)
                                else viewModel.blockedReschedule()
                            },
                            onMap = {
                                val query = listOf(a.doctorClinic, a.doctorAddress).filter { it.isNotBlank() }.joinToString(", ")
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(query))))
                                } catch (_: ActivityNotFoundException) {
                                    mapNote = "No maps app is installed on this phone."
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    cancelTarget?.let { id ->
        AlertDialog(
            onDismissRequest = { cancelTarget = null },
            title = { Text("Cancel appointment") },
            text = { Text("Cancel this appointment? The slot is released and the doctor is notified.") },
            confirmButton = { TextButton(onClick = { viewModel.cancel(id); cancelTarget = null }) { Text("Cancel appointment") } },
            dismissButton = { TextButton(onClick = { cancelTarget = null }) { Text("Keep it") } }
        )
    }

    if (showBell) {
        val next = upcoming.firstOrNull()
        AlertDialog(
            onDismissRequest = { showBell = false },
            title = { Text("Notifications") },
            text = {
                Text(
                    if (next == null) "No new notifications."
                    else "Next: ${next.doctorName}, ${Formats.dayAndTime(next.startMillis)} (${next.status.lowercase().replaceFirstChar { it.uppercase() }})"
                )
            },
            confirmButton = { TextButton(onClick = { showBell = false }) { Text("OK") } }
        )
    }
}

@Composable
private fun TabPill(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        color = if (selected) AppColors.surfaceContainerLowest else Color0,
        shadowElevation = if (selected) 1.dp else 0.dp
    ) {
        Row(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.Center) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) AppColors.primary else AppColors.onSurfaceVariant
            )
        }
    }
}

private val Color0 = androidx.compose.ui.graphics.Color.Transparent

private fun statusTone(status: String) = when (status) {
    AppointmentStatus.CONFIRMED -> BadgeTone.Positive
    AppointmentStatus.PENDING -> BadgeTone.Warning
    AppointmentStatus.CANCELLED -> BadgeTone.Negative
    else -> BadgeTone.Neutral
}

private fun statusLabel(status: String) = status.lowercase().replaceFirstChar { it.uppercase() }

@Composable
private fun DatePill(text: String, past: Boolean) {
    Row(
        Modifier.clip(RoundedCornerShape(8.dp)).background(AppColors.surfaceContainer).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(if (past) Icons.Filled.History else Icons.Filled.Event, null, tint = AppColors.secondary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = if (past) AppColors.onSurfaceVariant else AppColors.secondary, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun UpcomingCard(
    a: AppointmentView,
    onChat: () -> Unit,
    onCancel: () -> Unit,
    onReschedule: () -> Unit,
    onMap: () -> Unit
) {
    CareCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box0 { DatePill(Formats.dayAndTime(a.startMillis), false) }
                Spacer(Modifier.weight(1f))
                StatusBadge(statusLabel(a.status), statusTone(a.status))
            }
            Row(verticalAlignment = Alignment.Top) {
                PersonAvatar(a.doctorImage, a.doctorName, size = 56.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(a.doctorName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("${a.doctorSpecialty} Specialist", style = MaterialTheme.typography.bodyMedium, color = AppColors.primary, fontWeight = FontWeight.Medium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocationOn, null, tint = AppColors.outline, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            listOf(a.doctorAddress, a.doctorClinic).filter { it.isNotBlank() }.joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.onSurfaceVariant
                        )
                    }
                }
            }
            Surface(color = AppColors.surfaceContainerLow, shape = RoundedCornerShape(8.dp)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Schedule, null, tint = AppColors.primary)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            a.instructions.ifBlank { "Arrive 15 mins early" },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        if (a.prepNote.isNotBlank()) {
                            Text(a.prepNote, style = MaterialTheme.typography.bodySmall, color = AppColors.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = onMap) { Icon(Icons.Filled.Map, contentDescription = "View location map", tint = AppColors.primary) }
                }
            }
            CareButton("Message doctor", onChat, kind = ButtonKind.Outlined, leadingIcon = Icons.Filled.Chat, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CareButton("Cancel appointment", onCancel, kind = ButtonKind.Outlined, modifier = Modifier.weight(1f))
                CareButton("Reschedule", onReschedule, kind = ButtonKind.Tonal, modifier = Modifier.weight(1f))
            }
            Text(
                "Reschedule available up to 2h before",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.outline,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun Box0(content: @Composable () -> Unit) = content()

@Composable
private fun PastCard(a: AppointmentView, onBookAgain: () -> Unit, onRecords: () -> Unit) {
    CareCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                DatePill(Formats.dayAndTime(a.startMillis), true)
                Spacer(Modifier.weight(1f))
                StatusBadge(statusLabel(a.status), statusTone(a.status))
            }
            Row(verticalAlignment = Alignment.Top) {
                PersonAvatar(a.doctorImage, a.doctorName, size = 48.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(a.doctorName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(a.doctorSpecialty, style = MaterialTheme.typography.bodyMedium, color = AppColors.secondary)
                    if (a.status == AppointmentStatus.CANCELLED) {
                        Text(
                            a.cancelNote.ifBlank { "Cancelled by patient" } + " • " + Formats.dateShort(a.startMillis),
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.outline
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Follow-up recommended", style = MaterialTheme.typography.bodySmall, color = AppColors.onSurfaceVariant, modifier = Modifier.weight(1f))
                if (a.status == AppointmentStatus.COMPLETED) {
                    TextButton(onClick = onRecords) { Text("Visit records") }
                }
                CareButton("Book again", onBookAgain, kind = ButtonKind.Outlined, leadingIcon = Icons.Filled.Event)
            }
        }
    }
}
