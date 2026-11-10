package com.mobile360.healthcare.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mobile360.healthcare.R
import com.mobile360.healthcare.ui.theme.AppColors

@Composable
fun WelcomeScreen(onOpenPhoneEntry: () -> Unit, viewModel: WelcomeViewModel = hiltViewModel()) {
    val context = LocalContext.current
    var legal by rememberSaveable { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().background(AppColors.background)) {
        if (viewModel.updateRequired) {
            UpdateNotice(viewModel.versionLabel) {
                val pkg = context.packageName
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")))
                } catch (e: Exception) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg")))
                }
            }
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(Modifier.size(260.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(180.dp).clip(RoundedCornerShape(24.dp)).background(AppColors.surfaceContainerLowest)) {
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Box(
                            Modifier.size(80.dp).clip(RoundedCornerShape(16.dp))
                                .background(Brush.linearGradient(listOf(AppColors.primary, AppColors.primaryContainer))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.LocalHospital, null, tint = AppColors.onPrimary, modifier = Modifier.size(42.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            Modifier.clip(CircleShape).background(AppColors.surfaceContainer).padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(AppColors.primary))
                            Spacer(Modifier.width(6.dp))
                            Text("Connected Care", style = MaterialTheme.typography.bodySmall, color = AppColors.secondary, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                Pill(Icons.Filled.VerifiedUser, "HIPAA Certified", AppColors.primary, Modifier.align(Alignment.TopStart))
                Pill(Icons.Filled.MonitorHeart, "Real-time Vitals", AppColors.tertiaryContainer, Modifier.align(Alignment.BottomEnd))
            }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = AppColors.onSurface)
            Spacer(Modifier.height(4.dp))
            Text("Your doctors and records, in one place", style = MaterialTheme.typography.bodyLarge, color = AppColors.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Filled.MedicalServices, null, tint = AppColors.outline, modifier = Modifier.size(16.dp))
                Text("CareConnect Hospital", style = MaterialTheme.typography.bodySmall, color = AppColors.onSurfaceVariant)
                Box(Modifier.size(4.dp).clip(CircleShape).background(AppColors.outlineVariant))
                Icon(Icons.Filled.Security, null, tint = AppColors.outline, modifier = Modifier.size(16.dp))
                Text("Encrypted", style = MaterialTheme.typography.bodySmall, color = AppColors.onSurfaceVariant)
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Button(
                onClick = onOpenPhoneEntry,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.primaryContainer, contentColor = AppColors.onPrimary)
            ) {
                Icon(Icons.Filled.PhoneAndroid, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Continue with mobile number", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("By continuing, you agree to CareConnect's", style = MaterialTheme.typography.bodySmall, color = AppColors.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { legal = "Terms of Service" }) { Text("Terms of Service", color = AppColors.primary) }
                Text("&", style = MaterialTheme.typography.bodySmall, color = AppColors.onSurfaceVariant)
                TextButton(onClick = { legal = "Privacy Policy" }) { Text("Privacy Policy", color = AppColors.primary) }
            }
        }
    }
    legal?.let { title ->
        AlertDialog(
            onDismissRequest = { legal = null },
            title = { Text(title) },
            text = {
                Text(
                    if (title == "Terms of Service") "This is a local demo build. Use it only with the demo accounts or your own test data; no real medical service is provided."
                    else "Your data stays on this device in this demo build. It is stored encrypted-at-rest by the system and is never sent to a server."
                )
            },
            confirmButton = { TextButton(onClick = { legal = null }) { Text("OK") } }
        )
    }
}

@Composable
private fun UpdateNotice(version: String, onUpdate: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp)).background(AppColors.surfaceContainerLowest)
            .padding(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).clip(CircleShape).background(AppColors.errorContainer), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.SystemUpdate, null, tint = AppColors.error, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Update required", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = AppColors.onSurface)
                Text("Install the latest version", style = MaterialTheme.typography.bodySmall, color = AppColors.onSurfaceVariant)
            }
            Text(version, style = MaterialTheme.typography.bodySmall, color = AppColors.onSurfaceVariant,
                modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(AppColors.surfaceContainerHigh).padding(horizontal = 6.dp, vertical = 2.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onUpdate, colors = ButtonDefaults.textButtonColors(contentColor = AppColors.primary)) {
                Icon(Icons.Filled.Shop, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Update on Play Store")
            }
        }
    }
}

@Composable
private fun Pill(icon: ImageVector, label: String, tint: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Row(
        modifier.clip(CircleShape).background(AppColors.surfaceContainerLowest).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = AppColors.onSurface)
    }
}
