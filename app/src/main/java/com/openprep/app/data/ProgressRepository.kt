package com.openprep.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_progress")

class ProgressRepository(private val context: Context) {
    
    // Existing functions...
    fun getModuleScore(moduleId: String): Flow<Int?> = context.dataStore.data.map { it[intPreferencesKey("score_$moduleId")] }
    suspend fun saveModuleScore(moduleId: String, score: Int) {
        context.dataStore.edit { prefs -> 
            val current = prefs[intPreferencesKey("score_$moduleId")] ?: 0
            if (score > current) prefs[intPreferencesKey("score_$moduleId")] = score 
        }
    }
    fun isModuleCompleted(moduleId: String): Flow<Boolean> = context.dataStore.data.map { it[booleanPreferencesKey("completed_$moduleId")] ?: false }
    suspend fun markModuleCompleted(moduleId: String) { context.dataStore.edit { it[booleanPreferencesKey("completed_$moduleId")] = true } }
    // --- NEW: FOR STATS ---
    fun getAllPreferences() = context.dataStore.data
    // --- NEW: BOOKMARKING ---
    private val BOOKMARKS_KEY = stringSetPreferencesKey("bookmarked_modules")

    fun getBookmarkedModules(): Flow<Set<String>> {
        return context.dataStore.data.map { prefs -> prefs[BOOKMARKS_KEY] ?: emptySet() }
    }

    suspend fun toggleBookmark(moduleId: String) {
        context.dataStore.edit { prefs ->
            val currentBookmarks = prefs[BOOKMARKS_KEY] ?: emptySet()
            if (currentBookmarks.contains(moduleId)) {
                prefs[BOOKMARKS_KEY] = currentBookmarks - moduleId
            } else {
                prefs[BOOKMARKS_KEY] = currentBookmarks + moduleId
            }
        }
    }
}
