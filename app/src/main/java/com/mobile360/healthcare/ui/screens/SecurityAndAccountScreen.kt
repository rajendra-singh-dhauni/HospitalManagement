package com.mobile360.healthcare.ui.screens

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DevicesOther
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SendToMobile
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mobile360.healthcare.BuildConfig
import com.mobile360.healthcare.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

import androidx.compose.runtime.saveable.rememberSaveable
private enum class Pending { NONE, DEVICES, PHONE, DELETE }

@Composable
fun SecurityAndAccountScreen(
    onOpenWelcome: () -> Unit,
    onBack: () -> Unit,
    viewModel: SecurityAndAccountViewModel = hiltViewModel()
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val scheme = MaterialTheme.colorScheme
    var phone by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var pending by rememberSaveable { mutableStateOf(Pending.NONE) }

    val lastSignIn = session?.signedInAt?.takeIf { it > 0 }?.let {
        DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(it))
    } ?: "Not recorded"

    @Composable
    fun Msg(section: String) {
        ui.message?.takeIf { it.section == section }?.let {
            Text(it.text, color = if (it.ok) scheme.primary else scheme.error, style = MaterialTheme.typography.bodyMedium)
        }
    }

    if (pending != Pending.NONE) {
        val title = when (pending) {
            Pending.DEVICES -> "Sign out other devices?"
            Pending.PHONE -> "Confirm new number"
            else -> "Delete account?"
        }
        AlertDialog(
            onDismissRequest = { pending = Pending.NONE; code = "" },
            title = { Text(title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (pending) {
                        Pending.DEVICES -> Text("Other sessions of this account will be revoked in the demo service.")
                        Pending.PHONE -> Text("Enter the code to verify the new number.")
                        else -> Text("This permanently removes your records from this phone. Enter a fresh code to confirm.")
                    }
                    if (pending != Pending.DEVICES) {
                        OutlinedTextField(
                            value = code, onValueChange = { code = it.filter(Char::isDigit).take(6) },
                            label = { Text("Code") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                        )
                        Text(stringResource(R.string.demo_hint), style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val p = pending
                    pending = Pending.NONE
                    when (p) {
                        Pending.DEVICES -> viewModel.signOutOthers()
                        Pending.PHONE -> viewModel.changeNumber(phone, code) { phone = "" }
                        else -> viewModel.deleteAccount(code, onOpenWelcome)
                    }
                    code = ""
                }, enabled = pending == Pending.DEVICES || code.length == 6) { Text("Confirm") }
            },
            dismissButton = { TextButton(onClick = { pending = Pending.NONE; code = "" }) { Text("Cancel") } }
        )
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back")
            }
            Text("Security and account", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Icon(Icons.Filled.Shield, contentDescription = "Security", tint = scheme.onSurfaceVariant, modifier = Modifier.padding(12.dp))
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Card(colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow), shape = MaterialTheme.shapes.large) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Filled.LocalHospital, null, tint = scheme.secondary)
                    Column {
                        Text("CareConnect Hospital Account", style = MaterialTheme.typography.labelLarge)
                        Text("Manage session identity, clinical phone number, and verified patient credentials for emergency records.", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                        session?.let { Text(it.name + " · " + it.phone, style = MaterialTheme.typography.bodySmall, color = scheme.onSurface) }
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Device Safety", style = MaterialTheme.typography.titleLarge)
                Surface(shape = MaterialTheme.shapes.extraLarge, color = scheme.surfaceContainer) {
                    Text("Active Security", Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = scheme.secondary)
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLowest), border = BorderStroke(1.dp, scheme.outlineVariant), shape = MaterialTheme.shapes.large) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Filled.Smartphone, null, tint = scheme.primary, modifier = Modifier.size(32.dp))
                        Column {
                            Text(Build.MODEL + " (Current)", style = MaterialTheme.typography.labelLarge)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Verified, null, Modifier.size(14.dp), tint = scheme.secondary)
                                Text(" Verified", style = MaterialTheme.typography.labelSmall, color = scheme.secondary)
                            }
                            Text(stringResource(R.string.app_name) + " v" + BuildConfig.VERSION_NAME, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                        }
                    }
                    Surface(color = scheme.surfaceContainerLow, shape = MaterialTheme.shapes.medium) {
                        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Filled.Schedule, null, Modifier.size(18.dp), tint = scheme.onSurfaceVariant)
                            Text("Last sign-in: $lastSignIn", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                        }
                    }
                    FilledTonalButton(onClick = { pending = Pending.DEVICES }, enabled = !ui.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Icon(Icons.Filled.DevicesOther, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Sign out other devices")
                    }
                    Msg("devices")
                }
            }

            Column {
                Text("Phone Number Update", style = MaterialTheme.typography.titleLarge)
                Text("Used for appointment SMS reminders and prescription OTPs.", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
            }
            Card(colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLowest), border = BorderStroke(1.dp, scheme.outlineVariant), shape = MaterialTheme.shapes.large) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = phone, onValueChange = { phone = it.filter(Char::isDigit).take(10) },
                        label = { Text("New mobile number") },
                        prefix = { Text("+91 ") },
                        placeholder = { Text("98765 43210") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Verified by code (sends one-time SMS verification)", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                    Text(stringResource(R.string.demo_hint), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                    Button(onClick = {
                        if (phone.length != 10) viewModel.invalid("phone", "Enter a 10-digit mobile number.") else pending = Pending.PHONE
                    }, enabled = !ui.busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Icon(Icons.Filled.SendToMobile, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Change number")
                    }
                    Msg("phone")
                }
            }

            Column {
                Text("Account Management", style = MaterialTheme.typography.titleLarge)
                Text("Manage clinical account status and active access credentials.", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
            }
            Card(colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLowest), border = BorderStroke(1.dp, scheme.outlineVariant), shape = MaterialTheme.shapes.large) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Surface(shape = MaterialTheme.shapes.large, border = BorderStroke(1.dp, scheme.error.copy(alpha = 0.3f)), color = scheme.errorContainer.copy(alpha = 0.1f)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Filled.Warning, null, tint = scheme.error)
                                Column {
                                    Text("Permanent Account Deletion", style = MaterialTheme.typography.labelLarge, color = scheme.error)
                                    Text("Revokes electronic access to digital discharge summaries and lab records. Fresh code required.", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                                }
                            }
                            OutlinedButton(
                                onClick = { pending = Pending.DELETE }, enabled = !ui.busy,
                                border = BorderStroke(1.dp, scheme.error),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.error),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Filled.DeleteForever, null, Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Delete account")
                            }
                            Msg("delete")
                        }
                    }
                    HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.3f))
                    FilledTonalButton(onClick = { viewModel.signOut(onOpenWelcome) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.Logout, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Sign out", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            if (ui.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
        }
    }
}
