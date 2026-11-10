package com.mobile360.healthcare.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mobile360.healthcare.data.local.AccountEntity
import com.mobile360.healthcare.data.model.AlertKind
import com.mobile360.healthcare.data.model.AlertPrefs
import com.mobile360.healthcare.data.model.Role
import com.mobile360.healthcare.data.model.Session
import com.mobile360.healthcare.util.AppConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.careDataStore by preferencesDataStore(name = "careconnect_session")

@Singleton
class SessionStore @Inject constructor(@ApplicationContext context: Context) {
    private val ds = context.careDataStore

    private object Keys {
        val USER_ID = stringPreferencesKey("user_id")
        val NAME = stringPreferencesKey("name")
        val PHONE = stringPreferencesKey("phone")
        val ROLE = stringPreferencesKey("role")
        val DOCTOR_ID = stringPreferencesKey("doctor_id")
        val SIGNED_IN_AT = longPreferencesKey("signed_in_at")
        val ALERT_APPT = booleanPreferencesKey("alert_appointments")
        val ALERT_REMINDERS = booleanPreferencesKey("alert_reminders")
        val ALERT_REPORTS = booleanPreferencesKey("alert_reports")
    }

    // Null when signed out or when the 30-day session has run out.
    val session: Flow<Session?> = ds.data.map { p ->
        val id = p[Keys.USER_ID] ?: return@map null
        val at = p[Keys.SIGNED_IN_AT] ?: 0L
        if (System.currentTimeMillis() - at > AppConfig.SESSION_DAYS * 86_400_000L) return@map null
        Session(
            userId = id,
            name = p[Keys.NAME].orEmpty(),
            phone = p[Keys.PHONE].orEmpty(),
            role = runCatching { Role.valueOf(p[Keys.ROLE].orEmpty()) }.getOrDefault(Role.PATIENT),
            doctorId = p[Keys.DOCTOR_ID],
            signedInAt = at
        )
    }

    val alertPrefs: Flow<AlertPrefs> = ds.data.map { p ->
        AlertPrefs(
            appointments = p[Keys.ALERT_APPT] ?: true,
            reminders = p[Keys.ALERT_REMINDERS] ?: true,
            reportsAndMessages = p[Keys.ALERT_REPORTS] ?: true
        )
    }

    suspend fun save(account: AccountEntity) {
        ds.edit {
            it[Keys.USER_ID] = account.id
            it[Keys.NAME] = account.name
            it[Keys.PHONE] = account.phone
            it[Keys.ROLE] = account.role
            val doctorId = account.doctorId
            if (doctorId != null) it[Keys.DOCTOR_ID] = doctorId else it.remove(Keys.DOCTOR_ID)
            it[Keys.SIGNED_IN_AT] = System.currentTimeMillis()
        }
    }

    suspend fun updatePhone(phone: String) {
        ds.edit { it[Keys.PHONE] = phone }
    }

    suspend fun setAlert(kind: AlertKind, enabled: Boolean) {
        ds.edit {
            when (kind) {
                AlertKind.APPOINTMENTS -> it[Keys.ALERT_APPT] = enabled
                AlertKind.REMINDERS -> it[Keys.ALERT_REMINDERS] = enabled
                AlertKind.REPORTS_AND_MESSAGES -> it[Keys.ALERT_REPORTS] = enabled
            }
        }
    }

    suspend fun clearSession() {
        ds.edit {
            it.remove(Keys.USER_ID)
            it.remove(Keys.NAME)
            it.remove(Keys.PHONE)
            it.remove(Keys.ROLE)
            it.remove(Keys.DOCTOR_ID)
            it.remove(Keys.SIGNED_IN_AT)
        }
    }
}
