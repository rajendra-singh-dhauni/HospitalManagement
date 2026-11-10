package com.mobile360.healthcare.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mobile360.healthcare.R
import com.mobile360.healthcare.util.AppConfig
import com.mobile360.healthcare.ui.theme.AppColors

@Composable
fun PhoneEntryScreen(onOpenOTPEntry: () -> Unit, onBack: () -> Unit, viewModel: PhoneEntryViewModel = hiltViewModel()) {
    val phone by viewModel.phone.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.issued.collect { onOpenOTPEntry() } }

    Column(Modifier.fillMaxSize().background(AppColors.background)) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back), tint = AppColors.onSurface)
            }
            Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(AppColors.surfaceContainer), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.LocalHospital, null, tint = AppColors.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = AppColors.onSurface)
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(AppColors.secondaryContainer).padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Shield, null, tint = AppColors.onSecondaryContainer, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("Doctor & Patient Portal", style = MaterialTheme.typography.bodySmall, color = AppColors.onSecondaryContainer, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(12.dp))
            Text("Enter your mobile number", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = AppColors.onSurface)
            Spacer(Modifier.height(8.dp))
            Text("We will send a one-time code. Doctors must be approved by admin.", style = MaterialTheme.typography.bodyMedium, color = AppColors.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            Text("Mobile phone number", style = MaterialTheme.typography.labelLarge, color = AppColors.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.height(56.dp).clip(RoundedCornerShape(12.dp)).background(AppColors.surfaceContainerLowest)
                        .border(BorderStroke(1.dp, AppColors.outline), RoundedCornerShape(12.dp)).padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(AppConfig.COUNTRY_CODE, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = AppColors.onSurface)
                }
                Spacer(Modifier.width(8.dp))
                TextField(
                    value = phone,
                    onValueChange = viewModel::onPhoneChange,
                    modifier = Modifier.weight(1f).semantics { contentDescription = "Mobile phone number input" },
                    placeholder = { Text("00000 00000", color = AppColors.outlineVariant) },
                    singleLine = true,
                    isError = state.error != null,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    trailingIcon = {
                        if (phone.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onPhoneChange("") }) {
                                Icon(Icons.Filled.Cancel, "Clear mobile number", tint = AppColors.outline)
                            }
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = AppColors.surfaceContainerLowest,
                        unfocusedContainerColor = AppColors.surfaceContainerLowest,
                        focusedIndicatorColor = AppColors.primary,
                        unfocusedIndicatorColor = AppColors.outline
                    )
                )
            }
            state.error?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, color = AppColors.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.demo_hint), style = MaterialTheme.typography.bodySmall, color = AppColors.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(AppColors.surfaceContainerLow)
                    .border(BorderStroke(1.dp, AppColors.outlineVariant.copy(alpha = 0.4f)), RoundedCornerShape(12.dp)).padding(14.dp)
            ) {
                Icon(Icons.Filled.VerifiedUser, null, tint = AppColors.secondary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("HIPAA & NABH Compliant", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium, color = AppColors.onSurface)
                    Text("Your credentials and patient records are encrypted end-to-end with high-grade security.", style = MaterialTheme.typography.bodySmall, color = AppColors.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(24.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(AppColors.surfaceContainer).padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AdminPanelSettings, null, tint = AppColors.tertiary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Medical Staff Verification", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = AppColors.tertiary)
                }
                Spacer(Modifier.height(4.dp))
                Text("Doctors and healthcare practitioners require hospital board authentication. Ensure this number matches hospital administrative registry.", style = MaterialTheme.typography.bodySmall, color = AppColors.onSurfaceVariant)
            }
        }
        Column(Modifier.fillMaxWidth().background(AppColors.surfaceContainerLowest).padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text("We will send a one-time code. Doctors must be approved by admin.", style = MaterialTheme.typography.bodySmall, color = AppColors.onSurfaceVariant, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = viewModel::send,
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.primaryContainer, contentColor = AppColors.onPrimary)
            ) {
                if (state.loading) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = AppColors.onPrimary)
                } else {
                    Text("Send code")
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
