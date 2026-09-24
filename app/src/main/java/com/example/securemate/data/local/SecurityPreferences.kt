package com.example.securemate.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "securemate_preferences")

class SecurityPreferences(private val context: Context) {

    companion object {
        val KEY_BACKGROUND_MONITORING = booleanPreferencesKey("background_monitoring")
        val KEY_SCAN_FREQUENCY_HOURS = intPreferencesKey("scan_frequency_hours")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode") // "SYSTEM", "DARK", "LIGHT"
        val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val KEY_LAST_SCAN_TIMESTAMP = stringPreferencesKey("last_scan_timestamp")
    }

    val backgroundMonitoringEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_BACKGROUND_MONITORING] ?: true
    }

    val scanFrequencyHours: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_SCAN_FREQUENCY_HOURS] ?: 24
    }

    val themeMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_THEME_MODE] ?: "SYSTEM"
    }

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_NOTIFICATIONS_ENABLED] ?: true
    }

    suspend fun setBackgroundMonitoringEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_BACKGROUND_MONITORING] = enabled
        }
    }

    suspend fun setScanFrequencyHours(hours: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SCAN_FREQUENCY_HOURS] = hours
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = mode
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_NOTIFICATIONS_ENABLED] = enabled
        }
    }
}
