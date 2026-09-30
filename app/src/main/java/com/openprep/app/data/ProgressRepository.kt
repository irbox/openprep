package com.openprep.app.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_progress")

class ProgressRepository(private val context: Context) {
    
    // --- SERVER URL PERSISTENCE ---
    private val SERVER_URL_KEY = stringPreferencesKey("server_url")
    
    fun getServerUrl(): Flow<String?> = context.dataStore.data.map { it[SERVER_URL_KEY] }
    
    suspend fun saveServerUrl(url: String) {
        context.dataStore.edit { it[SERVER_URL_KEY] = url }
    }
    
    suspend fun clearServerUrl() {
        context.dataStore.edit { it.remove(SERVER_URL_KEY) }
    }

    // --- RESUME LEARNING STATE ---
    private val LAST_PLAYED_KEY = stringPreferencesKey("last_played_module")
    
    fun getLastPlayedModuleId(): Flow<String?> = context.dataStore.data.map { it[LAST_PLAYED_KEY] }
    
    suspend fun saveLastPlayedModule(moduleId: String) {
        context.dataStore.edit { it[LAST_PLAYED_KEY] = moduleId }
    }

    // --- MODULE PROGRESS ---
    fun getModuleScore(moduleId: String): Flow<Int?> = context.dataStore.data.map { it[intPreferencesKey("score_$moduleId")] }
    
    suspend fun saveModuleScore(moduleId: String, score: Int) {
        context.dataStore.edit { prefs -> 
            val current = prefs[intPreferencesKey("score_$moduleId")] ?: 0
            if (score > current) prefs[intPreferencesKey("score_$moduleId")] = score 
        }
    }
    
    fun isModuleCompleted(moduleId: String): Flow<Boolean> = context.dataStore.data.map { it[booleanPreferencesKey("completed_$moduleId")] ?: false }
    
    suspend fun markModuleCompleted(moduleId: String) { 
        context.dataStore.edit { it[booleanPreferencesKey("completed_$moduleId")] = true } 
    }

    // --- BOOKMARKING ---
    private val BOOKMARKS_KEY = stringSetPreferencesKey("bookmarked_modules")

    fun getBookmarkedModules(): Flow<Set<String>> = context.dataStore.data.map { it[BOOKMARKS_KEY] ?: emptySet() }

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

    // --- FOR SETTINGS & STATS ---
    fun getAllPreferences() = context.dataStore.data

    suspend fun clearAllProgress() {
        context.dataStore.edit { it.clear() }
    }
}
