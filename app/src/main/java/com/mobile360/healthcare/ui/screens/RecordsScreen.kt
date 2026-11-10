@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.mobile360.healthcare.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mobile360.healthcare.data.local.TimelineEntity
import com.mobile360.healthcare.data.model.TimelineType
import kotlinx.coroutines.launch

@Composable
fun RecordsScreen(
    onOpenPrescriptionDetail: (prescriptionId: String) -> Unit,
    onBack: () -> Unit,
    viewModel: RecordsViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val tab by viewModel.tab.collectAsStateWithLifecycle()
    val label by viewModel.rangeLabel.collectAsStateWithLifecycle()
    var searching by rememberSaveable { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var picker by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun download(item: TimelineEntity) {
        scope.launch {
            val text = "${item.title}\n${recordsDate(item.dateMillis)}\n${item.detail}\n${item.extra}\n"
            val msg = runCatching {
                recordsSaveToDownloads(context, "report_${item.dateMillis}.txt", "text/plain", text.toByteArray())
            }.fold({ "Saved to ${it.where}" }, { "Download failed. Please try again." })
            snackbar.showSnackbar(msg)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().height(56.dp).background(MaterialTheme.colorScheme.surface).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Navigate back") }
                Column(Modifier.weight(1f)) {
                    Text("Records", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("CareConnect Hospital", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
                IconButton(onClick = {
                    searching = !searching
                    if (!searching) viewModel.setQuery("")
                }) { Icon(Icons.Filled.Search, "Search clinical records") }
                IconButton(onClick = { viewModel.setTab(1); menu = true }) {
                    Icon(Icons.Filled.FilterList, "Filter clinical records")
                }
            }
            if (searching) {
                OutlinedTextField(
                    value = query, onValueChange = viewModel::setQuery, singleLine = true,
                    placeholder = { Text("Search records") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) IconButton(onClick = { viewModel.setQuery("") }) {
                            Icon(Icons.Filled.Cancel, "Clear search input")
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            TabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.surface) {
                Tab(selected = tab == 0, onClick = { viewModel.setTab(0) }, text = { Text("Prescriptions") })
                Tab(selected = tab == 1, onClick = { viewModel.setTab(1) }, text = { Text("Timeline") })
            }
            when {
                ui.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                ui.error != null -> Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(ui.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = viewModel::retry) { Text("Retry") }
                }
                tab == 0 -> LazyColumn(
                    Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Medication, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(4.dp))
                                Text("Prescriptions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            }
                            Text("Newest first", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                    if (ui.prescriptions.isEmpty()) item {
                        RecordsEmpty(
                            if (ui.anyPrescriptions) "No prescriptions match your search."
                            else "No prescriptions yet. They appear here after a consultation."
                        )
                    }
                    items(ui.prescriptions, key = { it.rx.id }) { row ->
                        val active = row.rx.status.equals("ACTIVE", true)
                        Card(
                            onClick = { onOpenPrescriptionDetail(row.rx.id) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "View ${row.rx.doctorName} prescription details" }
                        ) {
                            Row(Modifier.height(IntrinsicSize.Min)) {
                                Box(Modifier.width(4.dp).fillMaxHeight().background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant))
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            Modifier.size(40.dp).background(if (active) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                                            Alignment.Center
                                        ) { Text(recordsInitials(row.rx.doctorName), fontWeight = FontWeight.Bold) }
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(row.rx.doctorName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
                                            Text(row.rx.doctorSpecialty, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                        }
                                        Text(
                                            row.rx.status.lowercase().replaceFirstChar { it.uppercase() },
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.background(
                                                if (active) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                                CircleShape
                                            ).padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                    HorizontalDivider()
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text("Issued: ${recordsDate(row.rx.issuedMillis)}", style = MaterialTheme.typography.labelSmall)
                                            val n = row.medCount
                                            Text(
                                                if (active) (if (n == 1) "1 active medication" else "$n active medications")
                                                else (if (n == 1) "1 medication" else "$n medications"),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.History, null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Clinical Timeline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                }
                                Box {
                                    Row(
                                        Modifier.heightIn(min = 48.dp).clickable { menu = true }
                                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
                                            .padding(horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Filled.CalendarMonth, null, Modifier.size(16.dp))
                                        Text(label, Modifier.padding(horizontal = 4.dp), style = MaterialTheme.typography.labelSmall)
                                        Icon(Icons.Filled.ArrowDropDown, null, Modifier.size(16.dp))
                                    }
                                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                        listOf("1m" to "Last month", "3m" to "Last 3 months", "6m" to "Last 6 months", "all" to "All time").forEach { (k, v) ->
                                            DropdownMenuItem(text = { Text(v) }, onClick = { viewModel.setPreset(k); menu = false })
                                        }
                                        DropdownMenuItem(text = { Text("Custom range") }, onClick = { menu = false; picker = true })
                                    }
                                }
                            }
                        }
                        if (ui.timeline.isEmpty()) item {
                            RecordsEmpty(
                                if (ui.anyTimeline) "Nothing matches your search."
                                else "No history in this period. Choose another date range or check back after a visit."
                            )
                        }
                        item {
                            val line = MaterialTheme.colorScheme.outlineVariant
                            Column(
                                Modifier.padding(start = 24.dp).drawBehind {
                                    drawLine(line, Offset(-13.dp.toPx(), 12.dp.toPx()), Offset(-13.dp.toPx(), size.height - 12.dp.toPx()), 2.dp.toPx())
                                },
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                ui.timeline.forEach { t -> TimelineRow(t, onDownload = { download(t) }) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (picker) {
        val state = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { picker = false },
            confirmButton = {
                TextButton(
                    enabled = state.selectedStartDateMillis != null && state.selectedEndDateMillis != null,
                    onClick = {
                        viewModel.setCustom(state.selectedStartDateMillis ?: 0L, state.selectedEndDateMillis ?: 0L)
                        picker = false
                    }
                ) { Text("Apply") }
            },
            dismissButton = { TextButton(onClick = { picker = false }) { Text("Cancel") } }
        ) { DateRangePicker(state, Modifier.height(480.dp)) }
    }
}

@Composable
private fun TimelineRow(t: TimelineEntity, onDownload: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val color = when (t.type) {
        TimelineType.PRESCRIPTION -> cs.primary
        TimelineType.CONSULTATION -> cs.secondary
        TimelineType.LABORATORY -> cs.tertiary
        else -> cs.outline
    }
    val lab = t.type == TimelineType.LABORATORY
    Box(Modifier.fillMaxWidth()) {
        Box(Modifier.offset(x = (-30).dp, y = 6.dp).size(12.dp).background(color, CircleShape))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(recordsDate(t.dateMillis), style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
                    Text(" \u2022 ", style = MaterialTheme.typography.labelSmall)
                    Text(t.type.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                }
                if (lab && t.extra.contains("new", true)) {
                    Text(
                        "New report", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                        color = cs.onErrorContainer,
                        modifier = Modifier.background(cs.errorContainer, CircleShape).padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = cs.surfaceContainerLowest), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(t.title, fontWeight = FontWeight.Medium)
                        if (lab) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Verified, null, Modifier.size(14.dp), tint = cs.primary)
                                Text(" Verified by Central Labs", style = MaterialTheme.typography.labelSmall, color = cs.secondary)
                            }
                        } else if (t.type == TimelineType.PRESCRIPTION && t.detail.isNotBlank()) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                t.detail.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach {
                                    Text(it, style = MaterialTheme.typography.labelSmall, color = cs.secondary,
                                        modifier = Modifier.background(cs.surfaceContainer, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 2.dp))
                                }
                            }
                        } else if (t.detail.isNotBlank()) {
                            Text(t.detail, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                        }
                    }
                    if (lab) IconButton(onClick = onDownload) {
                        Icon(Icons.Filled.Download, "Download laboratory report", tint = cs.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordsEmpty(text: String) {
    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Filled.FolderOpen, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(8.dp))
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
