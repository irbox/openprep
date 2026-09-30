package com.openprep.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.openprep.app.data.ProgressRepository
import com.openprep.app.model.CourseManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

sealed class AppState {
    object Setup : AppState()
    object Loading : AppState()
    data class Success(val manifest: CourseManifest) : AppState()
    data class Error(val message: String) : AppState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow<AppState>(AppState.Setup)
    val uiState: StateFlow<AppState> = _uiState.asStateFlow()

    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    private val cacheFile = File(application.filesDir, "cached_manifest.json")
    
    // We need the repo here to update the streak automatically
    private val progressRepo = ProgressRepository(application)

    var currentServerUrl: String = ""
        private set

    fun connectToServer(serverUrl: String) {
        currentServerUrl = serverUrl.trimEnd('/')

        // Update Gamification Streak!
        viewModelScope.launch { progressRepo.updateDailyStreak() }

        if (cacheFile.exists()) {
            try {
                val cachedManifest = json.decodeFromString<CourseManifest>(cacheFile.readText())
                _uiState.value = AppState.Success(cachedManifest)
            } catch (e: Exception) {
                _uiState.value = AppState.Loading
            }
        } else {
            _uiState.value = AppState.Loading
        }

        viewModelScope.launch(Dispatchers.IO) {
            val request = Request.Builder().url("$currentServerUrl/course_manifest.json").build()

            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        if (responseBody != null) {
                            cacheFile.writeText(responseBody)
                            val manifest = json.decodeFromString<CourseManifest>(responseBody)
                            _uiState.value = AppState.Success(manifest)
                        }
                    } else if (!cacheFile.exists()) {
                        _uiState.value = AppState.Error("Server returned code: ${response.code}")
                    }
                }
            } catch (e: Exception) {
                if (!cacheFile.exists()) {
                    _uiState.value = AppState.Error("Network error: Make sure the URL is correct.")
                }
            }
        }
    }

    fun resetSetup() {
        cacheFile.delete()
        _uiState.value = AppState.Setup
    }
}
