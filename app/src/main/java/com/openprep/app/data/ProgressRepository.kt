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
    val history: String = "[]",
    val streak: Int = 0,
    val lastOpened: Long = 0L,
    val themeMode: Int = 0 // 0=System, 1=Light, 2=Dark
)

class ProgressRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    // --- APP THEME (Dark/Light Mode) ---
    private val THEME_MODE = intPreferencesKey("theme_mode")
    fun getThemeMode(): Flow<Int> = context.dataStore.data.map { it[THEME_MODE] ?: 0 }
    suspend fun setThemeMode(mode: Int) { context.dataStore.edit { it[THEME_MODE] = mode } }

    // --- IMPORT / EXPORT ---
    suspend fun exportProgress(): String {
        val prefs = context.dataStore.data.first()
        val scoresMap = mutableMapOf<String, Int>()
        val completedList = mutableListOf<String>()

        prefs.asMap().forEach { (key, value) ->
            val keyName = key.name
            if (keyName.startsWith("score_") && value is Int) scoresMap[keyName.removePrefix("score_")] = value
            else if (keyName.startsWith("completed_") && value == true) completedList.add(keyName.removePrefix("completed_"))
        }

        val backup = ProgressBackup(
            scores = scoresMap, completed = completedList,
            bookmarks = prefs[BOOKMARKS_KEY]?.toList() ?: emptyList(),
            history = prefs[HISTORY_KEY] ?: "[]", streak = prefs[CURRENT_STREAK] ?: 0,
            lastOpened = prefs[LAST_OPENED_DAY] ?: 0L, themeMode = prefs[THEME_MODE] ?: 0
        )
        return json.encodeToString(backup)
    }

    suspend fun importProgress(jsonString: String): Boolean {
        return try {
            val backup = json.decodeFromString<ProgressBackup>(jsonString)
            context.dataStore.edit { prefs ->
                backup.scores.forEach { (id, score) -> prefs[intPreferencesKey("score_$id")] = score }
                backup.completed.forEach { id -> prefs[booleanPreferencesKey("completed_$id")] = true }
                prefs[BOOKMARKS_KEY] = backup.bookmarks.toSet()
                prefs[HISTORY_KEY] = backup.history
                prefs[CURRENT_STREAK] = backup.streak
                prefs[LAST_OPENED_DAY] = backup.lastOpened
                prefs[THEME_MODE] = backup.themeMode
            }
            true
        } catch (e: Exception) { false }
    }

    // --- HISTORY, STREAK, PROFILE, URL, SCORES, BOOKMARKS (Same as before) ---
    private val HISTORY_KEY = stringPreferencesKey("learning_history")
    fun getHistory(): Flow<List<HistoryItem>> = context.dataStore.data.map { prefs -> try { json.decodeFromString<List<HistoryItem>>(prefs[HISTORY_KEY] ?: "[]") } catch (e: Exception) { emptyList() } }
    suspend fun addHistoryItem(title: String, type: String) { context.dataStore.edit { prefs -> val cur = try { json.decodeFromString<List<HistoryItem>>(prefs[HISTORY_KEY] ?: "[]") } catch (e: Exception) { emptyList() }; prefs[HISTORY_KEY] = json.encodeToString((listOf(HistoryItem(title, type, System.currentTimeMillis())) + cur).take(30)) } }

    private val LAST_OPENED_DAY = longPreferencesKey("last_opened_day")
    private val CURRENT_STREAK = intPreferencesKey("current_streak")
    fun getCurrentStreak(): Flow<Int> = context.dataStore.data.map { it[CURRENT_STREAK] ?: 0 }
    suspend fun updateDailyStreak() {
        val currentDay = System.currentTimeMillis() / (1000 * 60 * 60 * 24)
        context.dataStore.edit { prefs ->
            val lastDay = prefs[LAST_OPENED_DAY] ?: 0L
            if (lastDay == 0L || currentDay > lastDay + 1) { prefs[CURRENT_STREAK] = 1; prefs[LAST_OPENED_DAY] = currentDay }
            else if (currentDay == lastDay + 1) { prefs[CURRENT_STREAK] = (prefs[CURRENT_STREAK] ?: 0) + 1; prefs[LAST_OPENED_DAY] = currentDay }
        }
    }

    private val HAS_SEEN_ONBOARDING = booleanPreferencesKey("has_seen_onboarding")
    private val USER_NAME = stringPreferencesKey("user_name")
    private val TARGET_EXAM = stringPreferencesKey("target_exam")
    fun hasSeenOnboarding(): Flow<Boolean> = context.dataStore.data.map { it[HAS_SEEN_ONBOARDING] ?: false }
    suspend fun setOnboardingSeen() { context.dataStore.edit { it[HAS_SEEN_ONBOARDING] = true } }
    fun getUserProfile(): Flow<Pair<String, String>> = context.dataStore.data.map { Pair(it[USER_NAME] ?: "Learner", it[TARGET_EXAM] ?: "General Prep") }
    suspend fun saveUserProfile(name: String, exam: String) { context.dataStore.edit { it[USER_NAME] = name; it[TARGET_EXAM] = exam } }

    private val SERVER_URL_KEY = stringPreferencesKey("server_url")
    fun getServerUrl(): Flow<String?> = context.dataStore.data.map { it[SERVER_URL_KEY] }
    suspend fun saveServerUrl(url: String) { context.dataStore.edit { it[SERVER_URL_KEY] = url } }
    suspend fun clearServerUrl() { context.dataStore.edit { it.remove(SERVER_URL_KEY) } }

    private val LAST_PLAYED_KEY = stringPreferencesKey("last_played_module")
    fun getLastPlayedModuleId(): Flow<String?> = context.dataStore.data.map { it[LAST_PLAYED_KEY] }
    suspend fun saveLastPlayedModule(moduleId: String) { context.dataStore.edit { it[LAST_PLAYED_KEY] = moduleId } }

    fun getModuleScore(moduleId: String): Flow<Int?> = context.dataStore.data.map { it[intPreferencesKey("score_$moduleId")] }
    suspend fun saveModuleScore(moduleId: String, score: Int) { context.dataStore.edit { prefs -> val cur = prefs[intPreferencesKey("score_$moduleId")] ?: 0; if (score > cur) prefs[intPreferencesKey("score_$moduleId")] = score } }
    fun isModuleCompleted(moduleId: String): Flow<Boolean> = context.dataStore.data.map { it[booleanPreferencesKey("completed_$moduleId")] ?: false }
    suspend fun markModuleCompleted(moduleId: String) { context.dataStore.edit { it[booleanPreferencesKey("completed_$moduleId")] = true } }

    private val BOOKMARKS_KEY = stringSetPreferencesKey("bookmarked_modules")
    fun getBookmarkedModules(): Flow<Set<String>> = context.dataStore.data.map { it[BOOKMARKS_KEY] ?: emptySet() }
    suspend fun toggleBookmark(moduleId: String) { context.dataStore.edit { prefs -> val cur = prefs[BOOKMARKS_KEY] ?: emptySet(); prefs[BOOKMARKS_KEY] = if (cur.contains(moduleId)) cur - moduleId else cur + moduleId } }

    fun getAllPreferences() = context.dataStore.data
    suspend fun clearAllProgress() { context.dataStore.edit { it.clear() } }
}
