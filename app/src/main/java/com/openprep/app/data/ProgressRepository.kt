package com.openprep.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Create the DataStore instance
private val Context.dataStore by preferencesDataStore(name = "user_progress")

class ProgressRepository(private val context: Context) {
    
    fun getModuleScore(moduleId: String): Flow<Int?> {
        val key = intPreferencesKey("score_$moduleId")
        return context.dataStore.data.map { prefs -> prefs[key] }
    }

    suspend fun saveModuleScore(moduleId: String, score: Int) {
        val key = intPreferencesKey("score_$moduleId")
        context.dataStore.edit { prefs ->
            val currentScore = prefs[key] ?: 0
            if (score > currentScore) { // Only update if it's a new high score
                prefs[key] = score
            }
        }
    }

    fun isModuleCompleted(moduleId: String): Flow<Boolean> {
        val key = booleanPreferencesKey("completed_$moduleId")
        return context.dataStore.data.map { prefs -> prefs[key] ?: false }
    }

    suspend fun markModuleCompleted(moduleId: String) {
        val key = booleanPreferencesKey("completed_$moduleId")
        context.dataStore.edit { prefs -> prefs[key] = true }
    }
}
