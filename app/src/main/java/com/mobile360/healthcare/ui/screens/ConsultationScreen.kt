@file:OptIn(ExperimentalMaterial3Api::class)

package com.mobile360.healthcare.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mobile360.healthcare.data.model.MedicationDraft

private fun countOf(n: Int, one: String, many: String): String = if (n == 1) "$n $one" else "$n $many"

@Composable
fun ConsultationScreen(
    onOpenChat: (appointmentId: String) -> Unit,
    onBack: () -> Unit,
    viewModel: ConsultationViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val advice by viewModel.advice.collectAsStateWithLifecycle()
    val meds by viewModel.meds.collectAsStateWithLifecycle()
    val noteMsg by viewModel.noteMsg.collectAsStateWithLifecycle()
    val rxMsg by viewModel.rxMsg.collectAsStateWithLifecycle()
    val saved by viewModel.savedNote.collectAsStateWithLifecycle()
    val issued by viewModel.issued.collectAsStateWithLifecycle()
    val cs = MaterialTheme.colorScheme
    var menu by remember { mutableStateOf(false) }
    var adding by rememberSaveable { mutableStateOf(false) }
    val ready = ui as? ConsultUi.Ready

    Scaffold(contentWindowInsets = WindowInsets(0), containerColor = cs.background) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().height(56.dp).background(cs.surface).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Go back") }
                Text("Consultation", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Box {
                    IconButton(enabled = ready != null, onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "More options") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Open chat") }, onClick = { menu = false; onOpenChat(viewModel.appointmentId) })
                    }
                }
            }
            when (val s = ui) {
                ConsultUi.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                ConsultUi.NotFound -> Column(
                    Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("This consultation was not found or is not one of your patients' appointments.", color = cs.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onBack) { Text("Go back") }
                }
                is ConsultUi.Ready -> Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val a = s.appt
                    Card(colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerLowest), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                                Box(Modifier.size(48.dp).background(cs.secondaryContainer, CircleShape), Alignment.Center) {
                                    Text(recordsInitials(a.patientName), fontWeight = FontWeight.SemiBold, color = cs.onSecondaryContainer)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(a.patientName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text("${a.patientAge} yrs \u2022 ${a.patientSex}", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.EventRepeat, null, Modifier.size(16.dp), tint = cs.secondary)
                                        Text(" " + a.reason.ifBlank { "Follow-up consultation" }, color = cs.secondary)
                                    }
                                }
                                if (a.room.isNotBlank()) Text(
                                    a.room, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant,
                                    modifier = Modifier.background(cs.surfaceContainerHigh, CircleShape).border(1.dp, cs.outlineVariant, CircleShape).padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            HorizontalDivider(Modifier.padding(vertical = 8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.HistoryEdu, null, Modifier.size(14.dp), tint = cs.primary)
                                    Text(" ID: ${a.fileNo.ifBlank { a.id }}", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Schedule, null, Modifier.size(14.dp), tint = cs.tertiary)
                                    Text(" " + startedText(a.startMillis), style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.EditNote, null, Modifier.size(18.dp), tint = cs.primary)
                                Text(" Consultation note", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
                            }
                            Text("Clinical Assessment", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                        }
                        OutlinedTextField(
                            value = draft, onValueChange = viewModel::setDraft, minLines = 4,
                            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Consultation note content" },
                            shape = RoundedCornerShape(12.dp),
                            supportingText = {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    if (saved != null && draft.trim() == saved && draft.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.VerifiedUser, null, Modifier.size(14.dp))
                                            Text(" Auto-saved locally")
                                        }
                                    } else Text(if (draft.isBlank()) "" else "Not saved yet")
                                    Text(countOf(draft.length, "character", "characters"))
                                }
                            }
                        )
                        noteMsg?.let { Text(it.text, color = if (it.ok) cs.primary else cs.error, style = MaterialTheme.typography.bodySmall) }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            FilledTonalButton(onClick = viewModel::saveNote, modifier = Modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(12.dp)) {
                                Icon(Icons.Filled.Save, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Save note")
                            }
                        }
                    }

                    Card(colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerLowest), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Medication, null, Modifier.size(20.dp), tint = cs.primary)
                                    Text(" Medications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                }
                                TextButton(onClick = { adding = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                                    Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                                    Text("Add Item")
                                }
                            }
                            if (meds.isEmpty()) Text("No medications added yet. Use Add Item to start a prescription.", color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                            meds.forEachIndexed { i, m ->
                                Row(
                                    Modifier.fillMaxWidth().background(cs.surfaceContainerLow, RoundedCornerShape(12.dp)).border(1.dp, cs.outlineVariant, RoundedCornerShape(12.dp)).padding(start = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(m.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
                                            if (m.category.isNotBlank()) Text(
                                                m.category, style = MaterialTheme.typography.labelSmall, color = cs.onSecondaryContainer,
                                                modifier = Modifier.padding(start = 4.dp).background(cs.secondaryContainer.copy(alpha = 0.6f), CircleShape).padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text("${m.dosage} ${m.frequency} \u2022 ${countOf(m.supplyDays, "day", "days")}", color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    IconButton(onClick = { viewModel.removeMedication(i) }) {
                                        Icon(Icons.Filled.Delete, "Remove ${m.name}", tint = cs.outline)
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = advice, onValueChange = viewModel::setAdvice,
                                label = { Text("Instructions") },
                                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Prescription instructions" },
                                shape = RoundedCornerShape(12.dp)
                            )
                            rxMsg?.let { Text(it.text, color = if (it.ok) cs.primary else cs.error, style = MaterialTheme.typography.bodySmall) }
                            Button(onClick = viewModel::issue, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                                Icon(Icons.Filled.AssignmentTurnedIn, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Issue prescription")
                            }
                            if (issued) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Lock, null, Modifier.size(14.dp), tint = cs.outline)
                                Text(" Locked; amend makes new version", style = MaterialTheme.typography.labelSmall, color = cs.outline)
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { onOpenChat(a.id) }, shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Filled.Chat, null, Modifier.size(20.dp), tint = cs.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Open chat with ${a.patientName}")
                    }
                }
            }
        }
    }

    if (adding) AddMedicationDialog(onDismiss = { adding = false }, onAdd = { viewModel.addMedication(it); adding = false })
}

private fun startedText(start: Long): String {
    val diff = (System.currentTimeMillis() - start) / 60_000L
    return when {
        diff < 0 -> "Starts ${recordsDate(start, "dd MMM, h:mm a")}"
        diff < 60 -> "Started ${diff}m ago"
        diff < 24 * 60 -> "Started ${diff / 60}h ago"
        else -> "Started ${recordsDate(start, "dd MMM, h:mm a")}"
    }
}

@Composable
private fun AddMedicationDialog(onDismiss: () -> Unit, onAdd: (MedicationDraft) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }
    var dosage by rememberSaveable { mutableStateOf("") }
    var frequency by rememberSaveable { mutableStateOf("") }
    var days by rememberSaveable { mutableStateOf("30") }
    var refills by rememberSaveable { mutableStateOf("0") }
    var note by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Item") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Medication name") }, singleLine = true)
                OutlinedTextField(category, { category = it }, label = { Text("Category") }, singleLine = true)
                OutlinedTextField(dosage, { dosage = it }, label = { Text("Dosage (e.g. 1 tab)") }, singleLine = true)
                OutlinedTextField(frequency, { frequency = it }, label = { Text("Frequency (e.g. daily at bedtime)") }, singleLine = true)
                OutlinedTextField(
                    days, { days = it.filter(Char::isDigit).take(3) }, label = { Text("Supply days") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    refills, { refills = it.filter(Char::isDigit).take(2) }, label = { Text("Refills") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(note, { note = it }, label = { Text("Note (optional)") }, singleLine = true)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val d = days.toIntOrNull() ?: 0
                when {
                    name.isBlank() -> error = "Enter the medication name."
                    dosage.isBlank() || frequency.isBlank() -> error = "Enter dosage and frequency."
                    d <= 0 -> error = "Supply days must be at least 1."
                    else -> onAdd(MedicationDraft(name.trim(), category.trim(), dosage.trim(), frequency.trim(), d, refills.toIntOrNull() ?: 0, note.trim()))
                }
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
