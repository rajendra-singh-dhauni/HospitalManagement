package com.mobile360.healthcare.ui.screens

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mobile360.healthcare.BuildConfig
import com.mobile360.healthcare.R
import com.mobile360.healthcare.data.model.AlertKind
import com.mobile360.healthcare.data.model.Role

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.filled.DarkMode
private const val SUPPORT_NUMBER = "1800123456"

@Composable
fun SettingsScreen(
    onOpenSecurityAndAccount: () -> Unit,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    var notificationsOn by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    var faqOpen by rememberSaveable { mutableStateOf(false) }
    var showRationale by rememberSaveable { mutableStateOf(false) }

    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) notificationsOn = NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    fun openAppSettings() {
        val i = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        try { context.startActivity(i) } catch (e: ActivityNotFoundException) {
            viewModel.report("Could not open notification settings on this phone.")
        }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsOn = granted || NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (!granted) openAppSettings()
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text("Allow notifications?") },
            text = { Text("CareConnect uses alerts for appointment changes, reminders and new messages. You can change this any time.") },
            confirmButton = {
                TextButton(onClick = {
                    showRationale = false
                    if (Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS) else openAppSettings()
                }) { Text("Continue") }
            },
            dismissButton = { TextButton(onClick = { showRationale = false }) { Text("Not now") } }
        )
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Navigate back")
            }
            Text("Profile & settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            session?.let { s ->
                Card(colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow), shape = MaterialTheme.shapes.large) {
                    Column(Modifier.padding(16.dp)) {
                        Text(s.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(s.phone, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                        Text(if (s.role == Role.DOCTOR) "Doctor" else "Patient", style = MaterialTheme.typography.bodySmall, color = scheme.secondary)
                    }
                }
            }
            if (!notificationsOn) {
                Card(colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow), border = BorderStroke(1.dp, scheme.outlineVariant), shape = MaterialTheme.shapes.large) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Filled.NotificationsOff, null, tint = scheme.tertiary)
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Notifications are off", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Text("Action Needed", style = MaterialTheme.typography.labelSmall, color = scheme.tertiary)
                                }
                                Text("Enable notifications to get timely updates on your doctor consultations and prescription ready notices.", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                            }
                        }
                        Button(onClick = { showRationale = true }, modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp)) {
                            Icon(Icons.Filled.NotificationsActive, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Allow")
                        }
                    }
                }
            }
            error?.let { Text(it, color = scheme.error, style = MaterialTheme.typography.bodyMedium) }

            SectionLabel("Alert Preferences")
            Card(colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLowest), shape = MaterialTheme.shapes.large) {
                ToggleRow(Icons.Filled.CalendarMonth, "Appointment alerts", "Live consultation schedule & status changes", prefs.appointments) { viewModel.setAlert(AlertKind.APPOINTMENTS, it) }
                HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.3f))
                ToggleRow(Icons.Filled.Alarm, "Reminders", "Timely check-ins 24h & 1h prior", prefs.reminders) { viewModel.setAlert(AlertKind.REMINDERS, it) }
                HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.3f))
                ToggleRow(Icons.Filled.UploadFile, "Reports and messages", "Lab reports & care team chats", prefs.reportsAndMessages) { viewModel.setAlert(AlertKind.REPORTS_AND_MESSAGES, it) }
            }

            // CR-002: appearance section
            SectionLabel("Appearance")
            ToggleRow(
                Icons.Filled.DarkMode,
                "Dark mode",
                "Use the dark colour scheme",
                viewModel.darkTheme.collectAsStateWithLifecycle().value
            ) { viewModel.setDarkTheme(it) }
            SectionLabel("Account Privacy")
            Card(colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLowest), shape = MaterialTheme.shapes.large) {
                LinkRow(Icons.Filled.Shield, "Security and account", "Device safety, mobile number, account security", onOpenSecurityAndAccount)
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SectionLabel("Help & Support")
                Text("CareConnect Hospital", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
            Card(colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLowest), shape = MaterialTheme.shapes.large) {
                LinkRow(Icons.Filled.Quiz, "FAQ: How do I reschedule?", "Instructions for modifying specialist visits") { faqOpen = !faqOpen }
                if (faqOpen) {
                    Text(
                        "Open Appointments, choose the visit and tap Reschedule. Pick a new free slot and confirm; your doctor sees the change at once.",
                        Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.3f))
                LinkRow(Icons.Filled.SupportAgent, "Contact support", "Care team available 24/7") {
                    try { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$SUPPORT_NUMBER"))) } catch (e: ActivityNotFoundException) {
                        viewModel.report("No phone app is available to call support.")
                    }
                }
            }
            Text(
                stringResource(R.string.app_name) + " v" + BuildConfig.VERSION_NAME,
                Modifier.fillMaxWidth().padding(bottom = 16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.outline,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun IconBubble(icon: ImageVector) {
    Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), modifier = Modifier.size(40.dp)) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = MaterialTheme.colorScheme.secondary) }
    }
}

@Composable
private fun ToggleRow(icon: ImageVector, title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        IconBubble(icon)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange, modifier = Modifier.semantics { contentDescription = title })
    }
}

@Composable
private fun LinkRow(icon: ImageVector, title: String, sub: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 56.dp).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        IconBubble(icon)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.outline)
    }
}
