package com.openprep.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openprep.app.model.CourseManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.IOException

sealed class AppState {
    object Setup : AppState()
    object Loading : AppState()
    data class Success(val manifest: CourseManifest) : AppState()
    data class Error(val message: String) : AppState()
}

class MainViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<AppState>(AppState.Setup)
    val uiState: StateFlow<AppState> = _uiState.asStateFlow()

    private val client = OkHttpClient()
    
    // Ignore unknown keys makes it resilient to future JSON updates
    private val json = Json { ignoreUnknownKeys = true }

    var currentServerUrl: String = ""
        private set

    fun connectToServer(serverUrl: String) {
        currentServerUrl = serverUrl.trimEnd('/')
        _uiState.value = AppState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            val request = Request.Builder()
                .url("$currentServerUrl/course_manifest.json")
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        _uiState.value = AppState.Error("Server returned code: ${response.code}")
                        return@use
                    }

                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val manifest = json.decodeFromString<CourseManifest>(responseBody)
                        _uiState.value = AppState.Success(manifest)
                    } else {
                        _uiState.value = AppState.Error("Empty response from server.")
                    }
                }
            } catch (e: IOException) {
                _uiState.value = AppState.Error("Network error: Make sure the URL is correct and reachable.")
            } catch (e: Exception) {
                _uiState.value = AppState.Error("Failed to parse course data: ${e.localizedMessage}")
            }
        }
    }

    fun resetSetup() {
        _uiState.value = AppState.Setup
    }
}
