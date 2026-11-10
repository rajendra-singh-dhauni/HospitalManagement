package com.mobile360.healthcare.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhonelinkLock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mobile360.healthcare.R
import com.mobile360.healthcare.data.model.Role
import com.mobile360.healthcare.ui.theme.AppColors
import com.mobile360.healthcare.util.AppConfig
import kotlinx.coroutines.delay

@Composable
fun OTPEntryScreen(
    onOpenPatientHome: () -> Unit,
    onOpenDoctorSchedule: () -> Unit,
    onBack: () -> Unit,
    viewModel: OTPEntryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val code by viewModel.code.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val focus = rememberOtpFocusRequester()

    LaunchedEffect(Unit) {
        viewModel.done.collect { role ->
            if (role == Role.DOCTOR) onOpenDoctorSchedule() else onOpenPatientHome()
        }
    }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    val now by produceState(System.currentTimeMillis()) {
        while (true) { value = System.currentTimeMillis(); delay(1000) }
    }
    val remaining = (state.expiresAtMillis - now).coerceAtLeast(0L) / 1000
    val countdown = "%02d:%02d".format(remaining / 60, remaining % 60)

    Column(Modifier.fillMaxSize().background(AppColors.background).padding(horizontal = 16.dp)) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back), tint = AppColors.onSurface)
                }
                Row(
                    Modifier.clip(CircleShape).background(AppColors.surfaceContainer).padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.VerifiedUser, null, tint = AppColors.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("CareConnect Shield", style = MaterialTheme.typography.bodySmall, color = AppColors.secondary, fontWeight = FontWeight.Medium)
                }
                IconButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + AppConfig.SUPPORT_PHONE)))
                }) {
                    Icon(Icons.AutoMirrored.Filled.HelpOutline, "Help and support", tint = AppColors.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(16.dp))
            Box(Modifier.size(64.dp).clip(CircleShape).background(AppColors.secondaryContainer), contentAlignment = Alignment.Center) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(AppColors.primary), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.PhonelinkLock, null, tint = AppColors.onPrimary, modifier = Modifier.size(24.dp))
                }
                Box(Modifier.align(Alignment.BottomEnd).size(20.dp).clip(CircleShape).background(AppColors.surface), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Lock, null, tint = AppColors.tertiary, modifier = Modifier.size(12.dp))
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("Enter 6-digit code", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, color = AppColors.onSurface)
            Spacer(Modifier.height(8.dp))
            Text(
                "We sent a verification code to your registered mobile number " + (if (state.maskedPhone.isNotEmpty()) "(${state.maskedPhone}) " else "") + "for secure access.",
                style = MaterialTheme.typography.bodyMedium, color = AppColors.onSurfaceVariant, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.demo_hint), style = MaterialTheme.typography.bodySmall, color = AppColors.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            BasicTextField(
                value = code,
                onValueChange = viewModel::onCodeChange,
                modifier = Modifier.focusRequester(focus),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                decorationBox = {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (i in 0 until 6) {
                            val filled = i < code.length
                            val active = i == code.length
                            Box(
                                Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(12.dp))
                                    .background(if (filled || active) AppColors.surfaceContainerLowest else AppColors.surfaceContainer)
                                    .border(if (filled || active) 2.dp else 1.dp, if (filled) AppColors.primary else if (active) AppColors.primaryContainer else AppColors.outlineVariant, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (filled) Text(code[i].toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = AppColors.primary)
                                else if (!active) Box(Modifier.size(8.dp).clip(CircleShape).background(AppColors.outlineVariant))
                            }
                        }
                    }
                }
            )
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = {
                val pasted = clipboard.getText()?.text.orEmpty().filter { it.isDigit() }.take(6)
                viewModel.onCodeChange(pasted)
            }) {
                Icon(Icons.Filled.ContentPaste, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Paste code from SMS")
            }
            state.error?.let {
                Text(it, color = AppColors.error, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
            }
            state.info?.let {
                Text(it, color = AppColors.secondary, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Schedule, null, tint = AppColors.tertiary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Code expires in", style = MaterialTheme.typography.bodySmall, color = AppColors.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                Text(countdown, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = AppColors.onSurface)
                Spacer(Modifier.width(10.dp))
                Text("Attempts left:", style = MaterialTheme.typography.bodySmall, color = AppColors.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                Text(state.attemptsLeft.toString(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = AppColors.tertiary)
            }
            TextButton(onClick = viewModel::resend, enabled = !state.loading) {
                Text("Resend code", color = AppColors.primary, fontWeight = FontWeight.Medium)
            }
        }
        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Button(
                onClick = viewModel::confirm,
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.primaryContainer, contentColor = AppColors.onPrimary)
            ) {
                if (state.loading) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = AppColors.onPrimary)
                } else {
                    Text("Confirm")
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(20.dp))
                }
            }
            if (state.verifiedRole == Role.DOCTOR) {
                TextButton(onClick = onOpenDoctorSchedule, modifier = Modifier.fillMaxWidth()) {
                    Text("Doctor account: open Today", color = AppColors.secondary, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
