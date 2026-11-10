@file:OptIn(ExperimentalLayoutApi::class)

package com.mobile360.healthcare.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mobile360.healthcare.R
import com.mobile360.healthcare.data.ImageCatalog
import com.mobile360.healthcare.data.model.AppointmentStatus
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val homeTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE hh:mm a", Locale.getDefault())

@Composable
fun PatientHomeScreen(
    onOpenAppointments: () -> Unit,
    onOpenDoctorSearch: () -> Unit,
    onOpenRecords: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: PatientHomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().background(cs.background)) {
        Row(
            Modifier.fillMaxWidth().height(64.dp).background(cs.surface).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(cs.secondaryContainer), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.LocalHospital, contentDescription = null, tint = cs.primary)
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (state.firstName.isBlank()) "Good morning" else "Good morning, ${state.firstName}",
                    style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold
                )
                Text(
                    "${stringResource(R.string.app_name)} Hospital",
                    style = MaterialTheme.typography.bodySmall, color = cs.secondary
                )
            }
            IconButton(onClick = onOpenSettings, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.Notifications, contentDescription = "Notifications", tint = cs.onSurfaceVariant)
            }
            IconButton(onClick = onOpenSettings, modifier = Modifier.size(48.dp)) {
                Image(
                    painterResource(R.drawable.a_warm_professional_portrait_photograph),
                    contentDescription = "Profile",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(36.dp).clip(CircleShape).border(1.dp, cs.outlineVariant, CircleShape)
                )
            }
        }
        if (!state.online) {
            Text(
                "You are offline. Showing saved information.",
                Modifier.fillMaxWidth().background(cs.errorContainer).padding(12.dp),
                color = cs.onErrorContainer, style = MaterialTheme.typography.bodySmall
            )
        }
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.error != null -> Column(
                Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(state.error.orEmpty(), color = cs.error)
                Spacer(Modifier.height(12.dp))
                Button(onClick = viewModel::retry) { Text("Retry") }
            }
            else -> Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                UpcomingCard(state, onOpenAppointments)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Clinical Hub", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Care Services", style = MaterialTheme.typography.bodySmall, color = cs.secondary)
                    }
                    HubCard(Icons.Filled.MedicalServices, "Find a doctor", "Book in-person or virtual clinical visits", Icons.Filled.ArrowForward, onOpenDoctorSearch) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
                            listOf("Cardiology", "Dermatology", "Pediatrics").forEach { name ->
                                Surface(
                                    onClick = onOpenDoctorSearch,
                                    shape = RoundedCornerShape(8.dp),
                                    color = cs.surfaceContainer,
                                    border = BorderStroke(1.dp, cs.outlineVariant),
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Box(Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                                        Text(name, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                    HubCard(Icons.Filled.Description, "My records", "Prescriptions and history", Icons.Filled.ChevronRight, onOpenRecords) {
                        InfoStrip(Modifier.padding(top = 8.dp)) {
                            Icon(Icons.Filled.Medication, null, Modifier.size(16.dp), tint = cs.tertiary)
                            Text(
                                if (state.activePrescriptions == 1) "1 active prescription" else "${state.activePrescriptions} active prescriptions",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text("•", color = cs.outline)
                            Text("Recent lab results", style = MaterialTheme.typography.bodySmall, color = cs.primary)
                        }
                    }
                    HubCard(Icons.Filled.Chat, "Chat", "Connect with Care Team", Icons.Filled.ChevronRight, onOpenChat, badge = "Live") {
                        InfoStrip(Modifier.padding(top = 8.dp)) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(cs.primary))
                            Text("Nurse triage active", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            Text("Typical reply < 5m", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                Surface(
                    onClick = onOpenSettings,
                    shape = RoundedCornerShape(12.dp),
                    color = cs.surfaceContainerLowest,
                    border = BorderStroke(1.dp, cs.outlineVariant),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(cs.surfaceContainer), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.ManageAccounts, null, tint = cs.secondary)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Profile and settings", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            Text("Insurance, emergency contacts & notifications", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                        }
                        Icon(Icons.Filled.ChevronRight, null, tint = cs.outline)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun UpcomingCard(state: PatientHomeState, onOpenAppointments: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val next = state.next
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("UPCOMING APPOINTMENT", style = MaterialTheme.typography.labelMedium, color = cs.secondary, fontWeight = FontWeight.SemiBold)
            if (next != null) {
                val label = if (next.status == AppointmentStatus.CONFIRMED) "Confirmed" else "Pending"
                Text(
                    label, style = MaterialTheme.typography.bodySmall, color = cs.primary, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(cs.surfaceContainer).padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
        Surface(
            shape = RoundedCornerShape(12.dp), color = cs.surfaceContainerLowest,
            border = BorderStroke(2.dp, cs.secondaryContainer), modifier = Modifier.fillMaxWidth()
        ) {
            if (next == null) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("No upcoming appointments", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Find a doctor below to book your first visit.", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                }
            } else {
                val whenText = Instant.ofEpochMilli(next.startMillis).atZone(ZoneId.systemDefault()).format(homeTimeFormat)
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Filled.EventAvailable, null, Modifier.size(20.dp), tint = cs.primary)
                                Text("Next Scheduled Visit", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                            }
                            Text(
                                "${next.doctorName}, ${next.doctorSpecialty}",
                                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable(onClick = onOpenAppointments)
                                    .semantics { contentDescription = "Next: ${next.doctorName}, ${next.doctorSpecialty}, $whenText" }
                            )
                            Text(whenText, style = MaterialTheme.typography.bodyMedium, color = cs.primary, fontWeight = FontWeight.SemiBold)
                        }
                        if (next.doctorImage != 0) {
                            Image(
                                painterResource(ImageCatalog.avatar(next.doctorImage)), contentDescription = next.doctorName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(48.dp).clip(CircleShape).border(2.dp, cs.secondaryContainer, CircleShape)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    if (next.status == AppointmentStatus.CONFIRMED) {
                        InfoStrip {
                            Icon(Icons.Filled.NotificationsActive, null, Modifier.size(16.dp), tint = cs.secondary)
                            Text("Reminders 24 h and 1 h before", style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.LocationOn, null, Modifier.size(18.dp), tint = cs.outline)
                        Text(
                            listOf(next.doctorAddress, next.doctorClinic).filter { it.isNotBlank() }.joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant,
                            maxLines = 2, overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        onClick = onOpenAppointments, shape = RoundedCornerShape(12.dp),
                        color = cs.secondaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    ) {
                        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CalendarToday, null, Modifier.size(20.dp), tint = cs.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("View appointment details", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                            Icon(Icons.Filled.ChevronRight, null, tint = cs.secondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoStrip(modifier: Modifier = Modifier, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        content = content
    )
}

@Composable
private fun HubCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    trailing: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    badge: String? = null,
    extra: @Composable () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(12.dp), color = cs.surfaceContainerLowest,
        border = BorderStroke(1.dp, cs.outlineVariant), modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(cs.surfaceContainer), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = cs.primary)
            }
            Column(Modifier.weight(1f)) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onClick),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            if (badge != null) {
                                Text(
                                    badge, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                                    color = cs.onTertiaryContainer,
                                    modifier = Modifier.clip(RoundedCornerShape(50)).background(cs.tertiaryContainer).padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = cs.secondary)
                    }
                    Icon(trailing, null, Modifier.size(20.dp), tint = cs.outline)
                }
                extra()
            }
        }
    }
}
