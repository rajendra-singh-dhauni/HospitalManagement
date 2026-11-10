package com.mobile360.healthcare

import android.app.Application
import com.mobile360.healthcare.data.Seeder
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class CareConnectApp : Application() {
    @Inject lateinit var seeder: Seeder

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { seeder.ensureSeeded() }
    }
}
