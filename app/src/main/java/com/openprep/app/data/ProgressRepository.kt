package com.openprep.app.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.openprep.app.model.HistoryItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "user_progress")

class ProgressRepository(private val context: Context) {
    
    private val json = Json { ignoreUnknownKeys = true }

    // --- LEARNING TIMELINE (HISTORY) ---
    private val HISTORY_KEY = stringPreferencesKey("learning_history")

    fun getHistory(): Flow<List<HistoryItem>> = context.dataStore.data.map { prefs ->
        val jsonString = prefs[HISTORY_KEY] ?: "[]"
        try {
            json.decodeFromString<List<HistoryItem>>(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addHistoryItem(title: String, type: String) {
        context.dataStore.edit { prefs ->
            val jsonString = prefs[HISTORY_KEY] ?: "[]"
            val currentList = try { json.decodeFromString<List<HistoryItem>>(jsonString) } catch (e: Exception) { emptyList() }
            
            // Add new item to the top, and keep only the last 30 activities to save storage
            val newList = listOf(HistoryItem(title, type, System.currentTimeMillis())) + currentList
            prefs[HISTORY_KEY] = json.encodeToString(newList.take(30))
        }
    }

    // --- STREAK TRACKING (Gamification) ---
    private val LAST_OPENED_DAY = longPreferencesKey("last_opened_day")
    private val CURRENT_STREAK = intPreferencesKey("current_streak")

    fun getCurrentStreak(): Flow<Int> = context.dataStore.data.map { it[CURRENT_STREAK] ?: 0 }

    suspend fun updateDailyStreak() {
        val currentDay = System.currentTimeMillis() / (1000 * 60 * 60 * 24)
        context.dataStore.edit { prefs ->
            val lastDay = prefs[LAST_OPENED_DAY] ?: 0L
            val currentStreak = prefs[CURRENT_STREAK] ?: 0

            if (lastDay == 0L || currentDay > lastDay + 1) {
                prefs[CURRENT_STREAK] = 1
                prefs[LAST_OPENED_DAY] = currentDay
            } else if (currentDay == lastDay + 1) {
                prefs[CURRENT_STREAK] = currentStreak + 1
                prefs[LAST_OPENED_DAY] = currentDay
            }
        }
    }

    // --- ONBOARDING & PROFILE ---
    private val HAS_SEEN_ONBOARDING = booleanPreferencesKey("has_seen_onboarding")
    private val USER_NAME = stringPreferencesKey("user_name")
    private val TARGET_EXAM = stringPreferencesKey("target_exam")

    fun hasSeenOnboarding(): Flow<Boolean> = context.dataStore.data.map { it[HAS_SEEN_ONBOARDING] ?: false }
    suspend fun setOnboardingSeen() { context.dataStore.edit { it[HAS_SEEN_ONBOARDING] = true } }

    fun getUserProfile(): Flow<Pair<String, String>> = context.dataStore.data.map { 
        Pair(it[USER_NAME] ?: "Learner", it[TARGET_EXAM] ?: "General Prep") 
    }
    suspend fun saveUserProfile(name: String, exam: String) {
        context.dataStore.edit { 
            it[USER_NAME] = name
            it[TARGET_EXAM] = exam
        }
    }

    // --- SERVER URL PERSISTENCE ---
    private val SERVER_URL_KEY = stringPreferencesKey("server_url")
    fun getServerUrl(): Flow<String?> = context.dataStore.data.map { it[SERVER_URL_KEY] }
    suspend fun saveServerUrl(url: String) { context.dataStore.edit { it[SERVER_URL_KEY] = url } }
    suspend fun clearServerUrl() { context.dataStore.edit { it.remove(SERVER_URL_KEY) } }

    // --- RESUME LEARNING STATE ---
    private val LAST_PLAYED_KEY = stringPreferencesKey("last_played_module")
    fun getLastPlayedModuleId(): Flow<String?> = context.dataStore.data.map { it[LAST_PLAYED_KEY] }
    suspend fun saveLastPlayedModule(moduleId: String) { context.dataStore.edit { it[LAST_PLAYED_KEY] = moduleId } }

    // --- MODULE PROGRESS ---
    fun getModuleScore(moduleId: String): Flow<Int?> = context.dataStore.data.map { it[intPreferencesKey("score_$moduleId")] }
    suspend fun saveModuleScore(moduleId: String, score: Int) {
        context.dataStore.edit { prefs -> 
            val current = prefs[intPreferencesKey("score_$moduleId")] ?: 0
            if (score > current) prefs[intPreferencesKey("score_$moduleId")] = score 
        }
    }
    fun isModuleCompleted(moduleId: String): Flow<Boolean> = context.dataStore.data.map { it[booleanPreferencesKey("completed_$moduleId")] ?: false }
    suspend fun markModuleCompleted(moduleId: String) { context.dataStore.edit { it[booleanPreferencesKey("completed_$moduleId")] = true } }

    // --- BOOKMARKING ---
    private val BOOKMARKS_KEY = stringSetPreferencesKey("bookmarked_modules")
    fun getBookmarkedModules(): Flow<Set<String>> = context.dataStore.data.map { it[BOOKMARKS_KEY] ?: emptySet() }
    suspend fun toggleBookmark(moduleId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[BOOKMARKS_KEY] ?: emptySet()
            prefs[BOOKMARKS_KEY] = if (current.contains(moduleId)) current - moduleId else current + moduleId
        }
    }

    // --- FOR SETTINGS & STATS ---
    fun getAllPreferences() = context.dataStore.data
    suspend fun clearAllProgress() { context.dataStore.edit { it.clear() } }
}
