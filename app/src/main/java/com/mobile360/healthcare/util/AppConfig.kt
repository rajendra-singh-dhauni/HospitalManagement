package com.mobile360.healthcare.util

import com.mobile360.healthcare.BuildConfig

object AppConfig {
    // Local required version: equals the installed version, so no update notice shows.
    val REQUIRED_VERSION: String = BuildConfig.VERSION_NAME

    const val DEMO_CODE = "123456"
    const val DEMO_PATIENT_PHONE = "9876543210"
    const val DEMO_DOCTOR_PHONE = "9876500001"
    const val COUNTRY_CODE = "+91"

    const val CODE_VALID_MS = 5 * 60_000L
    const val MAX_ATTEMPTS = 3
    const val LOCK_MS = 15 * 60_000L
    const val SESSION_DAYS = 30L
    const val RESCHEDULE_CUTOFF_MS = 2 * 60 * 60_000L
    const val MOCK_LATENCY_MS = 300L

    const val SUPPORT_PHONE = "+911800120400"

    fun isUpdateRequired(): Boolean = compareVersions(BuildConfig.VERSION_NAME, REQUIRED_VERSION) < 0

    fun playStoreUri(packageName: String): String = "market://details?id=$packageName"

    private fun compareVersions(a: String, b: String): Int {
        val pa = a.split(".").map { it.toIntOrNull() ?: 0 }
        val pb = b.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(pa.size, pb.size)) {
            val c = pa.getOrElse(i) { 0 }.compareTo(pb.getOrElse(i) { 0 })
            if (c != 0) return c
        }
        return 0
    }
}
