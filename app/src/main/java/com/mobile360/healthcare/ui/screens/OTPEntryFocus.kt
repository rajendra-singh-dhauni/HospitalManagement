package com.mobile360.healthcare.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester

@Composable
internal fun rememberOtpFocusRequester(): FocusRequester = remember { FocusRequester() }
