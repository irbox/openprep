package com.openprep.app.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.openprep.app.model.HistoryItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "user_progress")

@Serializable
data class ProgressBackup(
    val scores: Map<String, Int> = emptyMap(),
    val completed: List<String> = emptyList(),
    val bookmarks: List<String> = emptyList(),
    val cardMastery: Map<String, String> = emptyMap(), // Card ID -> "again", "hard", "good"
    val history: String = "[]",
    val themeMode: Int = 0
)

class ProgressRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    // --- APP THEME (Dark/Light Mode) ---
    private val THEME_MODE = intPreferencesKey("theme_mode")
    fun getThemeMode(): Flow<Int> = context.dataStore.data.map { it[THEME_MODE] ?: 0 }
    suspend fun setThemeMode(mode: Int) { context.dataStore.edit { it[THEME_MODE] = mode } }

    // --- IMPORT / EXPORT (BYOS Data Ownership) ---
    suspend fun exportProgress(): String {
        val prefs = context.dataStore.data.first()
        val scoresMap = mutableMapOf<String, Int>()
        val completedList = mutableListOf<String>()
        val cardMasteryMap = mutableMapOf<String, String>()

        prefs.asMap().forEach { (key, value) ->
            val keyName = key.name
            if (keyName.startsWith("score_") && value is Int) {
                scoresMap[keyName.removePrefix("score_")] = value
            } else if (keyName.startsWith("completed_") && value == true) {
                completedList.add(keyName.removePrefix("completed_"))
            } else if (keyName.startsWith("card_mastery_") && value is String) {
                cardMasteryMap[keyName.removePrefix("card_mastery_")] = value
            }
        }

        val backup = ProgressBackup(
            scores = scoresMap,
            completed = completedList,
            bookmarks = prefs[BOOKMARKS_KEY]?.toList() ?: emptyList(),
            cardMastery = cardMasteryMap,
            history = prefs[HISTORY_KEY] ?: "[]",
            themeMode = prefs[THEME_MODE] ?: 0
        )
        return json.encodeToString(backup)
    }

    suspend fun importProgress(jsonString: String): Boolean {
        return try {
            val backup = json.decodeFromString<ProgressBackup>(jsonString)
            context.dataStore.edit { prefs ->
                backup.scores.forEach { (id, score) -> prefs[intPreferencesKey("score_$id")] = score }
                backup.completed.forEach { id -> prefs[booleanPreferencesKey("completed_$id")] = true }
                backup.cardMastery.forEach { (id, level) -> prefs[stringPreferencesKey("card_mastery_$id")] = level }
                prefs[BOOKMARKS_KEY] = backup.bookmarks.toSet()
                prefs[HISTORY_KEY] = backup.history
                prefs[THEME_MODE] = backup.themeMode
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    // --- FLASHCARD RETENTION & SPACED REPETITION ---
    fun getCardMastery(cardId: String): Flow<String?> {
        val key = stringPreferencesKey("card_mastery_$cardId")
        return context.dataStore.data.map { it[key] }
    }

    suspend fun recordCardReview(cardId: String, masteryLevel: String) {
        val key = stringPreferencesKey("card_mastery_$cardId")
        context.dataStore.edit { it[key] = masteryLevel } // "again", "hard", "good"
    }

    // --- LEARNING TIMELINE (HISTORY) ---
    private val HISTORY_KEY = stringPreferencesKey("learning_history")

    fun getHistory(): Flow<List<HistoryItem>> = context.dataStore.data.map { prefs ->
        val jsonString = prefs[HISTORY_KEY] ?: "[]"
        try { json.decodeFromString<List<HistoryItem>>(jsonString) } catch (e: Exception) { emptyList() }
    }

    suspend fun addHistoryItem(title: String, type: String) {
        context.dataStore.edit { prefs ->
            val jsonString = prefs[HISTORY_KEY] ?: "[]"
            val currentList = try { json.decodeFromString<List<HistoryItem>>(jsonString) } catch (e: Exception) { emptyList() }
            val newList = listOf(HistoryItem(title, type, System.currentTimeMillis())) + currentList
            prefs[HISTORY_KEY] = json.encodeToString(newList.take(30))
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

    fun getCompletedModules(): Flow<Set<String>> = context.dataStore.data.map { prefs ->
        prefs.asMap().filter { it.key.name.startsWith("completed_") && it.value == true }
            .keys.map { it.name.removePrefix("completed_") }.toSet()
    }

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

    fun getAllPreferences() = context.dataStore.data
    suspend fun clearAllProgress() { context.dataStore.edit { it.clear() } }
}
