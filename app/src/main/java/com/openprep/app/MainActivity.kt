package com.openprep.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.openprep.app.ui.screens.DashboardScreen
import com.openprep.app.ui.screens.ServerSetupScreen
import com.openprep.app.ui.theme.OpenPrepTheme
import com.openprep.app.viewmodel.AppState
import com.openprep.app.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OpenPrepTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OpenPrepApp()
                }
            }
        }
    }
}

@Composable
fun OpenPrepApp(viewModel: MainViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        is AppState.Setup, is AppState.Loading, is AppState.Error -> {
            ServerSetupScreen(
                uiState = state,
                onConnect = { url -> viewModel.connectToServer(url) }
            )
        }
        is AppState.Success -> {
            DashboardScreen(
                manifest = state.manifest,
                onDisconnect = { viewModel.resetSetup() },
                onModuleClick = { module ->
                    // We will implement the Video Player / PDF viewer here next
                    println("Clicked module: ${module.title} at ${module.url}")
                }
            )
        }
    }
}
