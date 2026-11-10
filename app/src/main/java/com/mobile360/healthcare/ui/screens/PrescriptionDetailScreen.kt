@file:OptIn(ExperimentalMaterial3Api::class)

package com.mobile360.healthcare.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mobile360.healthcare.R
import com.mobile360.healthcare.data.local.MedicationEntity

@Composable
fun PrescriptionDetailScreen(
    onBack: () -> Unit,
    viewModel: PrescriptionDetailViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val dl by viewModel.download.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val found = ui as? RxDetailUi.Found

    Scaffold(contentWindowInsets = WindowInsets(0), containerColor = cs.background) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().height(56.dp).background(cs.surface).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Navigate back") }
                Text(
                    "Prescription Details", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                IconButton(enabled = found != null, onClick = {
                    found?.let {
                        val send = Intent(Intent.ACTION_SEND).setType("text/plain")
                            .putExtra(Intent.EXTRA_SUBJECT, "Prescription ${it.rx.rxNumber}")
                            .putExtra(Intent.EXTRA_TEXT, recordsRxText(it.rx, it.meds))
                        context.startActivity(Intent.createChooser(send, "Share prescription"))
                    }
                }) { Icon(Icons.Filled.Share, "Share prescription") }
            }
            when (val s = ui) {
                RxDetailUi.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                RxDetailUi.NotFound, RxDetailUi.Error -> Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (s == RxDetailUi.NotFound) "This prescription was not found or you do not have access to it."
                        else "Could not load this prescription.",
                        color = cs.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onBack) { Text("Go back") }
                }
                is RxDetailUi.Found -> Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val rx = s.rx
                    Card(colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerLowest), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(Modifier.weight(1f)) {
                                    Text("Official Record", style = MaterialTheme.typography.labelSmall, color = cs.secondary)
                                    Text("Prescription, ${recordsDate(rx.issuedMillis, "dd MMM")}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                }
                                Row(
                                    Modifier.background(cs.surfaceContainer, CircleShape).border(1.dp, cs.outlineVariant, CircleShape).padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.ReceiptLong, null, Modifier.size(16.dp), tint = cs.primary)
                                    Text(" Rx #${rx.rxNumber}", style = MaterialTheme.typography.labelMedium, color = cs.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                            HorizontalDivider(Modifier.padding(vertical = 8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painterResource(if (rx.doctorImage != 0) rx.doctorImage else R.drawable.a_distinguished_male_senior_cardiologist),
                                    contentDescription = rx.doctorName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(44.dp).clip(CircleShape)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("${rx.doctorName} \u2022 ${rx.doctorSpecialty}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Schedule, null, Modifier.size(14.dp), tint = cs.onSurfaceVariant)
                                        Text(" Last updated ${recordsDate(rx.updatedMillis, "h:mm a")}", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.LocalHospital, null, Modifier.size(18.dp), tint = cs.secondary)
                                Text(" ${rx.clinic} \u2022 ${rx.address}", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Medication, null, tint = cs.primary)
                            Text(" Prescribed Medications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                        val active = rx.status.equals("ACTIVE", true)
                        Text(
                            if (active) "${s.meds.size} Active" else rx.status.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall, color = cs.onSecondaryContainer,
                            modifier = Modifier.background(cs.secondaryContainer.copy(alpha = 0.4f), CircleShape).padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    s.meds.forEachIndexed { i, m -> MedicationCard(m, i) }
                    if (rx.advice.isNotBlank()) {
                        Card(colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerLow), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(16.dp)) {
                                Icon(Icons.Filled.Description, null, tint = cs.primary)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text("Doctor's Advice", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                                    Text(rx.advice, color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = viewModel::askForCode, enabled = !dl.working,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = cs.primaryContainer),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Filled.Download, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Download PDF")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Lock, null, Modifier.size(14.dp), tint = cs.secondary)
                            Text(" Requires authentication code for secure download", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                        }
                        if (dl.working) CircularProgressIndicator(Modifier.size(24.dp))
                        dl.saved?.let { f ->
                            Text("Saved to ${f.where}", color = cs.primary)
                            recordsViewIntent(f)?.let { intent ->
                                OutlinedButton(onClick = {
                                    try { context.startActivity(intent) } catch (e: ActivityNotFoundException) { }
                                }) { Text("Open PDF") }
                            }
                        }
                        if (dl.failed) {
                            Text("The download failed.", color = cs.error)
                            OutlinedButton(onClick = viewModel::retrySave) { Text("Retry") }
                        }
                    }
                }
            }
        }
    }

    if (dl.askCode) {
        var code by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = viewModel::dismissCode,
            title = { Text("Authentication code") },
            text = {
                Column {
                    Text(stringResource(R.string.demo_hint), style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = code, onValueChange = { code = it.filter(Char::isDigit).take(6) },
                        singleLine = true, label = { Text("6-digit code") },
                        isError = dl.codeError != null,
                        supportingText = { dl.codeError?.let { Text(it) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                }
            },
            confirmButton = { TextButton(enabled = !dl.working, onClick = { viewModel.confirmCode(code) }) { Text("Confirm") } },
            dismissButton = { TextButton(onClick = viewModel::dismissCode) { Text("Cancel") } }
        )
    }
}

@Composable
private fun MedicationCard(m: MedicationEntity, index: Int) {
    val cs = MaterialTheme.colorScheme
    Card(colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerLowest), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(m.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${m.dosage} \u2022 ${m.frequency}", color = cs.onSurfaceVariant, fontWeight = FontWeight.Medium)
                }
                Box(Modifier.size(40.dp).background(cs.surfaceContainerLow, CircleShape), Alignment.Center) {
                    Icon(if (index % 2 == 0) Icons.Filled.Medication else Icons.Filled.Vaccines, null, Modifier.size(20.dp), tint = cs.primary)
                }
            }
            HorizontalDivider()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.EventRepeat, null, Modifier.size(16.dp), tint = cs.secondary)
                    Text(" ${m.supplyDays} days supply", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Replay, null, Modifier.size(16.dp), tint = cs.secondary)
                    Text(
                        " ${m.refillsLeft} ${if (m.refillsLeft == 1) "refill" else "refills"} remaining",
                        style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium
                    )
                }
            }
            if (m.note.isNotBlank()) {
                Row(
                    Modifier.background(cs.secondaryContainer.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.WaterDrop, null, Modifier.size(15.dp), tint = cs.onSecondaryContainer)
                    Text(" ${m.note}", style = MaterialTheme.typography.labelSmall, color = cs.onSecondaryContainer)
                }
            }
        }
    }
}
