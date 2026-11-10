package com.mobile360.healthcare.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mobile360.healthcare.data.local.DoctorListItem
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val slotFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, hh:mm a", Locale.getDefault())

@Composable
fun DoctorSearchScreen(
    onOpenDoctorProfileAndBooking: (doctorId: String) -> Unit,
    onBack: () -> Unit,
    viewModel: DoctorSearchViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().background(cs.background)) {
        Row(Modifier.fillMaxWidth().height(56.dp).background(cs.surface).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back")
            }
            Text("Find a Doctor", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        if (!state.online) {
            Text(
                "You are offline. Showing saved doctors.",
                Modifier.fillMaxWidth().background(cs.errorContainer).padding(12.dp),
                color = cs.onErrorContainer, style = MaterialTheme.typography.bodySmall
            )
        }
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(
                value = state.query, onValueChange = viewModel::setQuery,
                placeholder = { Text("Specialization or name") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = viewModel::clearQuery) {
                            Icon(Icons.Filled.Cancel, contentDescription = "Clear search input")
                        }
                    }
                },
                singleLine = true, shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.specialties.forEach { name ->
                    val selected = name == state.specialty
                    Surface(
                        onClick = { viewModel.toggleSpecialty(name) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (selected) cs.secondaryContainer else cs.surfaceContainerLowest,
                        border = if (selected) null else BorderStroke(1.dp, cs.outline.copy(alpha = 0.3f)),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (selected) Icon(Icons.Filled.Check, null, Modifier.size(18.dp), tint = cs.onSecondaryContainer)
                            Text(name, style = MaterialTheme.typography.labelLarge, color = if (selected) cs.onSecondaryContainer else cs.onSurfaceVariant)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                val n = state.doctors.size
                Text(
                    if (n == 1) "1 Specialist Available" else "$n Specialists Available",
                    style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.Tune, null, Modifier.size(16.dp), tint = cs.primary)
                    Text("Filter", style = MaterialTheme.typography.bodySmall, color = cs.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.error != null -> Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state.error.orEmpty(), color = cs.error)
                Spacer(Modifier.height(8.dp))
                Button(onClick = viewModel::retry) { Text("Retry") }
            }
            state.doctors.isEmpty() -> Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No doctors found", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Try another specialization or name.", style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant, textAlign = TextAlign.Center
                )
            }
            else -> LazyColumn(
                Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.doctors, key = { it.doctor.id }) { item -> DoctorCard(item, onOpenDoctorProfileAndBooking) }
            }
        }
    }
}

@Composable
private fun DoctorCard(item: DoctorListItem, onBook: (String) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val d = item.doctor
    Surface(
        shape = RoundedCornerShape(12.dp), color = cs.surfaceContainerLowest,
        border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.2f)), modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                Box {
                    Image(
                        painterResource(d.imageRes), contentDescription = d.name, contentScale = ContentScale.Crop,
                        modifier = Modifier.size(64.dp).clip(CircleShape).border(2.dp, cs.surfaceVariant, CircleShape)
                    )
                    if (item.nextSlotMillis != null) {
                        Box(
                            Modifier.align(Alignment.BottomEnd).size(14.dp).clip(CircleShape)
                                .background(androidx.compose.ui.graphics.Color(0xFF22C55E))
                                .border(2.dp, cs.surfaceContainerLowest, CircleShape)
                        )
                    }
                }
                Column(Modifier.weight(1f)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(d.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Icon(Icons.Filled.Star, null, Modifier.size(16.dp), tint = cs.tertiary)
                            Text(d.rating.toString(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                            Text("(${d.reviewCount})", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                        }
                    }
                    Text(
                        d.title.ifBlank { "${d.specialty} Specialist" },
                        style = MaterialTheme.typography.bodyMedium, color = cs.secondary, fontWeight = FontWeight.Medium
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.History, null, Modifier.size(14.dp), tint = cs.onSurfaceVariant)
                        Text("${d.experienceYears} yrs exp", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                        Text("•", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                        Icon(Icons.Filled.Language, null, Modifier.size(14.dp), tint = cs.onSurfaceVariant)
                        Text(d.languages, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.Verified, null, Modifier.size(14.dp), tint = cs.onSurfaceVariant)
                        Text(d.clinic, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(cs.outlineVariant.copy(alpha = 0.3f)))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Next available", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.Schedule, null, Modifier.size(16.dp), tint = cs.primary)
                        Text(
                            item.nextSlotMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(slotFormat) } ?: "No free slots",
                            style = MaterialTheme.typography.labelLarge, color = cs.primary, fontWeight = FontWeight.Medium
                        )
                    }
                }
                Button(
                    onClick = { onBook(d.id) }, shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Book visit", fontWeight = FontWeight.SemiBold)
                    Icon(Icons.Filled.ChevronRight, null, Modifier.size(18.dp))
                }
            }
        }
    }
}
