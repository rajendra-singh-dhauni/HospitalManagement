package com.mobile360.healthcare.ui.screens

import androidx.lifecycle.ViewModel
import com.mobile360.healthcare.BuildConfig
import com.mobile360.healthcare.util.AppConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WelcomeViewModel @Inject constructor() : ViewModel() {
    val updateRequired: Boolean = AppConfig.isUpdateRequired()
    val versionLabel: String = "v" + BuildConfig.VERSION_NAME
}
