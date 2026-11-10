package com.mobile360.healthcare.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MedicalInformation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mobile360.healthcare.R
import com.mobile360.healthcare.data.local.AppointmentView
import com.mobile360.healthcare.data.model.AppointmentStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun clock(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault()))

@Composable
fun DoctorScheduleScreen(
    onOpenConsultation: (appointmentId: String) -> Unit,
    onOpenAvailability: () -> Unit,
    viewModel: DoctorScheduleViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val cs = MaterialTheme.colorScheme
    var rejecting by rememberSaveable { mutableStateOf<String?>(null) }
    var reason by rememberSaveable { mutableStateOf("") }

    val pending = state.appointments.filter { it.status == AppointmentStatus.PENDING }
    val scheduled = state.appointments.filter { it.status != AppointmentStatus.PENDING && it.status != AppointmentStatus.CANCELLED }
    val first = scheduled.firstOrNull()
    val later = scheduled.drop(1)
    val dateText = LocalDate.now().format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))
    val count = scheduled.size + pending.size

    Column(Modifier.fillMaxSize().background(cs.background)) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).background(cs.surface).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onOpenAvailability, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.LocalHospital, contentDescription = "Doctor", tint = cs.primary)
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium, color = cs.primary, fontWeight = FontWeight.Bold)
                Text("${stringResource(R.string.app_name)} Hospital", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            }
            IconButton(onClick = { viewModel.showRequests(true) }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.Notifications, contentDescription = "Notifications: show booking requests", tint = cs.onSurfaceVariant)
            }
            Image(
                painterResource(R.drawable.close_up_portrait_photograph_of_a_welcom),
                contentDescription = "Doctor profile photo", contentScale = ContentScale.Crop,
                modifier = Modifier.size(36.dp).clip(CircleShape).border(1.dp, cs.outlineVariant, CircleShape)
            )
        }
        if (!state.online) {
            Text(
                "You are offline. Showing saved information.",
                Modifier.fillMaxWidth().background(cs.errorContainer).padding(12.dp),
                color = cs.onErrorContainer, style = MaterialTheme.typography.bodySmall
            )
        }
        state.message?.let { msg ->
            Row(
                Modifier.fillMaxWidth().background(cs.secondaryContainer).padding(start = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(msg, Modifier.weight(1f), color = cs.onSecondaryContainer, style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = viewModel::dismissMessage) { Text("OK") }
            }
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column {
                Text("Doctor Schedule", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.CalendarToday, null, Modifier.size(14.dp), tint = cs.primary)
                    Text(
                        "Today, $dateText • " + if (count == 1) "1 appointment scheduled" else "$count appointments scheduled",
                        style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant
                    )
                }
            }
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(cs.surfaceContainerHigh).padding(4.dp)) {
                TabButton(Icons.Filled.Today, "Today", !state.showRequests, null, Modifier.weight(1f)) { viewModel.showRequests(false) }
                TabButton(Icons.Filled.PendingActions, "Requests", state.showRequests, pending.size.takeIf { it > 0 }, Modifier.weight(1f)) { viewModel.showRequests(true) }
            }
            when {
                state.loading -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                state.error != null -> Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.error.orEmpty(), color = cs.error)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = viewModel::retry) { Text("Retry") }
                }
                state.showRequests -> {
                    if (pending.isEmpty()) {
                        EmptyBlock("No booking requests", "New requests from patients will appear here.")
                    } else pending.forEach { PendingCard(it, viewModel::accept) { id -> reason = ""; rejecting = id } }
                }
                else -> {
                    if (scheduled.isEmpty() && pending.isEmpty()) {
                        EmptyBlock("No appointments today", "Your confirmed visits for today will appear here.")
                    }
                    first?.let { ConfirmedCard(it, onOpenConsultation) }
                    pending.forEach { PendingCard(it, viewModel::accept) { id -> reason = ""; rejecting = id } }
                    if (later.isNotEmpty()) {
                        Surface(shape = RoundedCornerShape(12.dp), color = cs.surfaceContainerLow, border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.3f))) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Filled.Event, null, Modifier.size(18.dp), tint = cs.outline)
                                        Text("Later Today", style = MaterialTheme.typography.labelLarge)
                                    }
                                    Text(if (later.size == 1) "1 appointment" else "${later.size} appointments", style = MaterialTheme.typography.bodySmall, color = cs.outline)
                                }
                                later.forEach { a ->
                                    Surface(
                                        onClick = { onOpenConsultation(a.id) }, shape = RoundedCornerShape(8.dp),
                                        color = cs.surfaceContainerLowest, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                    ) {
                                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Text(clock(a.startMillis), style = MaterialTheme.typography.labelLarge, color = cs.secondary, fontWeight = FontWeight.SemiBold)
                                            Box(Modifier.width(1.dp).height(12.dp).background(cs.outlineVariant))
                                            Column(Modifier.weight(1f)) {
                                                Text(a.patientName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                                Text(a.reason, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                                            }
                                            Icon(Icons.Filled.ChevronRight, null, tint = cs.outline)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Box(Modifier.fillMaxWidth().padding(bottom = 16.dp), contentAlignment = Alignment.Center) {
                TextButton(onClick = onOpenAvailability, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icon(Icons.Filled.EditCalendar, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Edit Doctor Availability", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    rejecting?.let { id ->
        AlertDialog(
            onDismissRequest = { rejecting = null },
            title = { Text("Reject request") },
            text = {
                OutlinedTextField(
                    value = reason, onValueChange = { reason = it },
                    label = { Text("Optional referral note") }, modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = { TextButton(onClick = { viewModel.reject(id, reason); rejecting = null }) { Text("Reject") } },
            dismissButton = { TextButton(onClick = { rejecting = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun TabButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, selected: Boolean, badge: Int?,
    modifier: Modifier, onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        onClick = onClick, shape = RoundedCornerShape(8.dp),
        color = if (selected) cs.surfaceContainerLowest else cs.surfaceContainerHigh,
        modifier = modifier.heightIn(min = 48.dp)
    ) {
        Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(18.dp), tint = if (selected) cs.primary else cs.onSurfaceVariant)
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) cs.primary else cs.onSurfaceVariant)
            if (badge != null) {
                Spacer(Modifier.width(6.dp))
                Text(
                    badge.toString(), style = MaterialTheme.typography.labelSmall, color = cs.onTertiary, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(CircleShape).background(cs.tertiary).padding(horizontal = 6.dp, vertical = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyBlock(title: String, body: String) {
    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
private fun TimeChip(millis: Long, color: androidx.compose.ui.graphics.Color) {
    Row(
        Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(Icons.Filled.Schedule, null, Modifier.size(16.dp), tint = color)
        Text(clock(millis), style = MaterialTheme.typography.labelLarge, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PatientRow(a: AppointmentView, subtitleColor: androidx.compose.ui.graphics.Color, extra: @Composable () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
        Image(
            painterResource(a.patientImage), contentDescription = a.patientName, contentScale = ContentScale.Crop,
            modifier = Modifier.size(48.dp).clip(CircleShape).border(1.dp, cs.outlineVariant, CircleShape)
        )
        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(a.patientName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text("${a.patientAge} yrs • ${a.patientSex.take(1)}", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            }
            Text(a.reason, style = MaterialTheme.typography.bodyMedium, color = subtitleColor)
            extra()
        }
    }
}

@Composable
private fun ConfirmedCard(a: AppointmentView, onOpen: (String) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        onClick = { onOpen(a.id) }, shape = RoundedCornerShape(12.dp), color = cs.surfaceContainerLowest,
        border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.3f)), modifier = Modifier.fillMaxWidth()
            .semantics { contentDescription = "${clock(a.startMillis)} ${a.patientName}, ${a.status.lowercase().replaceFirstChar { it.uppercase() }}" }
    ) {
        Row {
            Box(Modifier.width(6.dp).heightIn(min = 120.dp).background(cs.primary))
            Column(Modifier.padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    TimeChip(a.startMillis, cs.primary)
                    Row(
                        Modifier.clip(RoundedCornerShape(50)).background(cs.secondaryContainer).padding(horizontal = 10.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Filled.CheckCircle, null, Modifier.size(14.dp), tint = cs.onSecondaryContainer)
                        Text(
                            a.status.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodySmall, color = cs.onSecondaryContainer
                        )
                    }
                }
                PatientRow(a, cs.onSurfaceVariant) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        val room = listOf(a.room, a.mode).filter { it.isNotBlank() }.joinToString(" • ")
                        if (room.isNotBlank()) Text(room, style = MaterialTheme.typography.labelSmall, color = cs.primary)
                        if (a.fileNo.isNotBlank()) Text("File #${a.fileNo.removePrefix("#")}", style = MaterialTheme.typography.bodySmall, color = cs.outline)
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (a.vitals.isNotBlank()) {
                            Icon(Icons.Filled.MedicalInformation, null, Modifier.size(16.dp), tint = cs.onSurfaceVariant)
                            Text("Vitals logged: ${a.vitals}", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                        }
                    }
                    Row(Modifier.heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Consultation", color = cs.primary, style = MaterialTheme.typography.labelLarge)
                        Icon(Icons.Filled.ChevronRight, null, Modifier.size(18.dp), tint = cs.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingCard(a: AppointmentView, onAccept: (String) -> Unit, onReject: (String) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(12.dp), color = cs.surfaceContainerLowest,
        border = BorderStroke(1.dp, cs.tertiaryContainer.copy(alpha = 0.3f)), modifier = Modifier.fillMaxWidth()
    ) {
        Row {
            Box(Modifier.width(6.dp).heightIn(min = 160.dp).background(cs.tertiary))
            Column(Modifier.padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    TimeChip(a.startMillis, cs.tertiary)
                    Row(
                        Modifier.clip(RoundedCornerShape(50)).background(cs.tertiaryContainer.copy(alpha = 0.25f)).padding(horizontal = 10.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Filled.HourglassTop, null, Modifier.size(14.dp), tint = cs.tertiary)
                        Text("Pending Review", style = MaterialTheme.typography.bodySmall, color = cs.tertiary)
                    }
                }
                PatientRow(a, cs.tertiary) {
                    if (a.referral.isNotBlank()) Text(a.referral, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onReject(a.id) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Filled.Close, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Reject")
                    }
                    Button(onClick = { onAccept(a.id) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Filled.Check, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Accept")
                    }
                }
                Text(
                    "Reject allows sending an optional referral note", style = MaterialTheme.typography.bodySmall, color = cs.outline,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        }
    }
}
