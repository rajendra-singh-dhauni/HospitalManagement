package com.mobile360.healthcare.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mobile360.healthcare.data.local.DoctorEntity
import com.mobile360.healthcare.data.local.SlotEntity
import com.mobile360.healthcare.ui.components.BadgeTone
import com.mobile360.healthcare.ui.components.CareButton
import com.mobile360.healthcare.ui.components.CareCard
import com.mobile360.healthcare.ui.components.CareChip
import com.mobile360.healthcare.ui.components.CareTextField
import com.mobile360.healthcare.ui.components.CareTopBar
import com.mobile360.healthcare.ui.components.EmptyState
import com.mobile360.healthcare.ui.components.InfoBanner
import com.mobile360.healthcare.ui.components.PersonAvatar
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dayFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d", Locale.getDefault())
private val monthFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
private val slotFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())

private fun plural(n: Int, one: String, many: String): String = if (n == 1) "$n $one" else "$n $many"

private fun slotTime(s: SlotEntity) = Instant.ofEpochMilli(s.startMillis).atZone(ZoneId.systemDefault())

@Composable
fun DoctorProfileAndBookingScreen(
    onOpenAppointments: () -> Unit,
    onBack: () -> Unit,
    viewModel: DoctorProfileAndBookingViewModel = hiltViewModel()
) {
    val load by viewModel.doctor.collectAsStateWithLifecycle()
    val epochDay by viewModel.epochDay.collectAsStateWithLifecycle()
    val slotId by viewModel.slotId.collectAsStateWithLifecycle()
    val reason by viewModel.reason.collectAsStateWithLifecycle()
    val bookmarked by viewModel.bookmarked.collectAsStateWithLifecycle()
    val slots by viewModel.slots.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val confirmation by viewModel.confirmation.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val cs = MaterialTheme.colorScheme

    Column(Modifier.fillMaxSize().background(cs.background)) {
        CareTopBar(
            title = "Doctor Profile & Booking",
            onBack = onBack,
            actions = {
                IconButton(onClick = viewModel::toggleBookmark) {
                    Icon(
                        if (bookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = if (bookmarked) "Remove bookmark" else "Bookmark doctor"
                    )
                }
            }
        )
        when (val l = load) {
            DoctorLoad.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            DoctorLoad.Missing -> EmptyState(
                title = "Doctor not found",
                message = "This doctor is not available any more.",
                actionLabel = "Go back",
                onAction = onBack
            )
            is DoctorLoad.Found -> {
                val d = l.doctor
                val shortName = d.name.substringAfterLast(' ')
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    ProfileCard(d)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Date", style = MaterialTheme.typography.titleMedium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    LocalDate.ofEpochDay(epochDay).format(monthFormat),
                                    style = MaterialTheme.typography.bodySmall, color = cs.secondary
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Filled.CalendarMonth, null, Modifier.size(16.dp), tint = cs.secondary)
                            }
                        }
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val today = LocalDate.now()
                            (1..14).map { today.plusDays(it.toLong()) }.forEach { day ->
                                CareChip(day.format(dayFormat), day.toEpochDay() == epochDay, { viewModel.selectDate(day) })
                            }
                        }
                    }
                    val list = slots
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Free slots (live)", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.width(8.dp))
                                Box(Modifier.size(10.dp).background(cs.primary, RoundedCornerShape(50)))
                            }
                            val first = list?.firstOrNull()
                            if (first != null) {
                                Text(
                                    if (slotTime(first).hour < 12) "Morning session" else "Afternoon session",
                                    style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant
                                )
                            }
                        }
                        when {
                            list == null -> CircularProgressIndicator()
                            list.isEmpty() -> Text("No free slots on this day. Try another day.", color = cs.onSurfaceVariant)
                            else -> list.chunked(2).forEach { pair ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    pair.forEach { s ->
                                        SlotTile(s, s.id == slotId, Modifier.weight(1f)) { viewModel.selectSlot(s.id) }
                                    }
                                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (viewModel.isReschedule) {
                            Text("Rescheduling keeps your original reason for the visit.", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                        } else {
                            CareTextField(
                                value = reason, onValueChange = viewModel::setReason, label = "Reason for visit",
                                placeholder = "e.g. Routine checkup, fever, pain",
                                leadingIcon = { Icon(Icons.Filled.EditNote, null) }
                            )
                            Text(
                                "Provide brief symptoms to help Dr. $shortName prepare clinical context.",
                                style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                    Surface(color = cs.surfaceContainer, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.VerifiedUser, null, tint = cs.primary)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Consultation Fee", style = MaterialTheme.typography.labelLarge)
                                Text("Insurance eligible \u2022 In-person", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                            }
                            Text("\u20B9${d.fee}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    error?.let { InfoBanner(it, icon = Icons.Filled.EventAvailable, tone = BadgeTone.Negative) }
                    CareButton(
                        text = if (viewModel.isReschedule) "Confirm new time" else "Confirm booking",
                        onClick = viewModel::confirm,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    confirmation?.let { message ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Done") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = onOpenAppointments) { Text("View appointments") } }
        )
    }
}

@Composable
private fun SlotTile(slot: SlotEntity, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val label = slotTime(slot).format(slotFormat)
    val taken = slot.booked
    val container = when {
        selected -> cs.primaryContainer
        taken -> cs.surfaceDim.copy(alpha = 0.4f)
        else -> cs.surfaceContainerLowest
    }
    Surface(
        color = container,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) cs.primary else cs.outline.copy(alpha = 0.25f)),
        modifier = modifier.heightIn(min = 48.dp).then(if (taken) Modifier else Modifier.clickable(onClick = onClick))
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Schedule, null, Modifier.size(18.dp), tint = if (taken) cs.outline else cs.secondary)
            Spacer(Modifier.width(6.dp))
            Text(
                label, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge,
                color = if (taken) cs.outline else cs.onSurface,
                textDecoration = if (taken) TextDecoration.LineThrough else null
            )
            when {
                selected -> Icon(Icons.Filled.CheckCircle, "Selected", Modifier.size(20.dp), tint = cs.primary)
                taken -> Text("Taken", style = MaterialTheme.typography.labelSmall, color = cs.tertiary)
                else -> Text("Open", style = MaterialTheme.typography.labelSmall, color = cs.secondary)
            }
        }
    }
}

@Composable
private fun ProfileCard(d: DoctorEntity) {
    val cs = MaterialTheme.colorScheme
    CareCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            PersonAvatar(d.imageRes, d.name, size = 72.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(d.name, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Icon(Icons.Filled.Star, null, Modifier.size(16.dp), tint = cs.tertiary)
                    Text(" ${d.rating}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                }
                Text(d.specialty, style = MaterialTheme.typography.labelLarge, color = cs.onPrimaryContainer, modifier = Modifier.padding(top = 4.dp))
                Text(
                    plural(d.experienceYears, "year", "years") + " experience. " + d.bio,
                    style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)
                )
                Text(plural(d.reviewCount, "review", "reviews") + " \u2022 " + d.languages, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Top) {
            Icon(Icons.Filled.LocationOn, null, Modifier.size(18.dp), tint = cs.tertiary)
            Spacer(Modifier.width(8.dp))
            Text("${d.clinic}, ${d.address}", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        }
    }
}
