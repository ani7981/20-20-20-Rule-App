package com.attentiontracker.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension property — one DataStore per app process
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "attention_prefs")

/**
 * Lightweight wrapper around DataStore for persisting user preferences.
 * All reads are exposed as [Flow]s; writes are suspend functions.
 */
class PreferenceManager(private val context: Context) {

    companion object {
        private val THRESHOLD_KEY = longPreferencesKey("threshold_seconds")
        private val USER_NAME_KEY = androidx.datastore.preferences.core.stringPreferencesKey("user_name")

        private val COMPLETED_BREAKS_KEY = androidx.datastore.preferences.core.intPreferencesKey("completed_breaks")
        private val LAST_BREAK_DATE_KEY = androidx.datastore.preferences.core.stringPreferencesKey("last_break_date")

        /** Default: 15 seconds (good for testing; raise to 1200s for real use) */
        const val DEFAULT_THRESHOLD_SECONDS = 15L
    }

    /** Emits the current threshold in seconds, defaulting to [DEFAULT_THRESHOLD_SECONDS]. */
    val thresholdSeconds: Flow<Long> = context.dataStore.data
        .map { prefs -> prefs[THRESHOLD_KEY] ?: DEFAULT_THRESHOLD_SECONDS }

    val userName: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[USER_NAME_KEY] ?: "" }

    val completedBreaks: Flow<Int> = context.dataStore.data.map { prefs ->
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        if (prefs[LAST_BREAK_DATE_KEY] == today) prefs[COMPLETED_BREAKS_KEY] ?: 0 else 0
    }

    /** Persist a new threshold value (seconds). */
    suspend fun setThreshold(seconds: Long) {
        context.dataStore.edit { prefs ->
            prefs[THRESHOLD_KEY] = seconds
        }
    }

    suspend fun setUserName(name: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_NAME_KEY] = name
        }
    }

    suspend fun incrementCompletedBreaks() {
        context.dataStore.edit { prefs ->
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            val lastDate = prefs[LAST_BREAK_DATE_KEY]
            val currentBreaks = if (lastDate == today) prefs[COMPLETED_BREAKS_KEY] ?: 0 else 0
            
            prefs[LAST_BREAK_DATE_KEY] = today
            prefs[COMPLETED_BREAKS_KEY] = currentBreaks + 1
        }
    }
}
