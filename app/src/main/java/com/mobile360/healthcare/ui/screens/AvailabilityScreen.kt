package com.mobile360.healthcare.ui.screens

import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mobile360.healthcare.data.local.AvailabilityEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

private fun dayName(d: Int): String = when (d) {
    1 -> "Monday"
    2 -> "Tuesday"
    3 -> "Wednesday"
    4 -> "Thursday"
    5 -> "Friday"
    6 -> "Saturday"
    else -> "Sunday"
}

@Composable
fun AvailabilityScreen(onBack: () -> Unit, viewModel: AvailabilityViewModel = hiltViewModel()) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme

    fun pickTime(row: AvailabilityEntity) {
        fun parse(t: String) = runCatching { LocalTime.parse(t) }.getOrDefault(LocalTime.of(9, 0))
        fun fmt(h: Int, m: Int) = "%02d:%02d".format(h, m)
        val s = parse(row.startTime)
        TimePickerDialog(context, { _, h, m ->
            val start = fmt(h, m)
            val e = parse(row.endTime)
            TimePickerDialog(context, { _, eh, em ->
                viewModel.edit(row.dayOfWeek) { it.copy(startTime = start, endTime = fmt(eh, em)) }
            }, e.hour, e.minute, true).apply { setTitle("End time") }.show()
        }, s.hour, s.minute, true).apply { setTitle("Start time") }.show()
    }

    fun addToCalendar() {
        val next = ui.rows.filter { it.enabled }.minByOrNull {
            LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.of(it.dayOfWeek))).toEpochDay()
        }
        if (next == null) {
            viewModel.showMessage("Turn on at least one day to add your hours to the calendar.")
            return
        }
        val date = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.of(next.dayOfWeek)))
        val zone = ZoneId.systemDefault()
        val begin = date.atTime(LocalTime.parse(next.startTime)).atZone(zone).toInstant().toEpochMilli()
        val end = date.atTime(LocalTime.parse(next.endTime)).atZone(zone).toInstant().toEpochMilli()
        val intent = Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.Events.TITLE, "Consultation hours")
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, begin)
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end)
        try { context.startActivity(intent) } catch (e: ActivityNotFoundException) {
            viewModel.showMessage("No calendar app is installed on this phone.")
        }
    }

    Column(Modifier.fillMaxSize().background(scheme.background)) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Doctor Availability", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            IconButton(onClick = { addToCalendar() }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.EventNote, contentDescription = "Add my hours to calendar")
            }
        }
        when {
            ui.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            ui.error != null -> Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(ui.error.orEmpty(), color = scheme.error)
                Spacer(Modifier.height(12.dp))
                Button(onClick = viewModel::load) { Text("Try again") }
            }
            !ui.isDoctor -> Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("Availability is only for doctor accounts.")
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onBack) { Text("Go back") }
            }
            else -> {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Manage your weekly consultation slots and schedule changes.", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                    Card(colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLowest), border = BorderStroke(1.dp, scheme.outlineVariant), shape = MaterialTheme.shapes.large) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.CalendarViewWeek, null, tint = scheme.primary)
                                Text("Weekly schedule", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }
                            ui.rows.forEach { row ->
                                HorizontalDivider(Modifier.padding(vertical = 4.dp), color = scheme.outlineVariant.copy(alpha = 0.4f))
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(dayName(row.dayOfWeek), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                        Text(
                                            if (row.enabled) "${row.startTime} to ${row.endTime}" else "Off",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (row.enabled) scheme.onSurfaceVariant else scheme.outline
                                        )
                                    }
                                    Surface(
                                        shape = MaterialTheme.shapes.extraLarge,
                                        color = if (row.enabled) scheme.secondaryContainer else scheme.surfaceContainerHigh
                                    ) {
                                        Text(
                                            if (row.enabled) "Active slots" else "Inactive",
                                            Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                    IconButton(onClick = {
                                        viewModel.select(row.dayOfWeek)
                                        if (row.enabled) pickTime(row)
                                    }, modifier = Modifier.size(48.dp)) {
                                        Icon(
                                            if (row.enabled) Icons.Filled.Schedule else Icons.Filled.EditCalendar,
                                            contentDescription = "Edit ${dayName(row.dayOfWeek)} hours"
                                        )
                                    }
                                }
                            }
                        }
                    }
                    val sel = ui.rows.firstOrNull { it.dayOfWeek == ui.selectedDay }
                    if (sel != null) {
                        Card(colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLowest), border = BorderStroke(1.dp, scheme.outlineVariant), shape = MaterialTheme.shapes.large) {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                                    Text("Available on ${dayName(sel.dayOfWeek)}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                    Text("Enable consultation booking", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = sel.enabled,
                                    onCheckedChange = { on -> viewModel.edit(sel.dayOfWeek) { it.copy(enabled = on) } },
                                    modifier = Modifier.semantics { contentDescription = "Available on ${dayName(sel.dayOfWeek)}" }
                                )
                            }
                        }
                    }
                    if (ui.awaitingConfirm && ui.conflict > 0) {
                        Surface(color = scheme.tertiaryContainer.copy(alpha = 0.35f), shape = MaterialTheme.shapes.large) {
                            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Filled.Warning, null, tint = scheme.tertiary)
                                Column {
                                    Text("Schedule conflict", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = scheme.tertiary)
                                    Text(
                                        if (ui.conflict == 1) "1 confirmed appointment affected. Confirm to continue."
                                        else "${ui.conflict} confirmed appointments affected. Confirm to continue.",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                    ui.message?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = scheme.primary)
                    }
                }
                Button(
                    onClick = viewModel::save,
                    enabled = !ui.saving,
                    modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(min = 48.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    if (ui.saving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Filled.Save, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (ui.awaitingConfirm) "Confirm and save schedule" else "Save schedule", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
