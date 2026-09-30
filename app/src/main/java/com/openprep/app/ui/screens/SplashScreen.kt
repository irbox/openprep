package com.openprep.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.openprep.app.R

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    // Load the animation_splash.json you uploaded to res/raw/
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.animation_splash))
    
    // Track the progress of the animation
    val progress by animateLottieCompositionAsState(composition)

    // When animation hits 100% (1f), move to the next screen
    LaunchedEffect(progress) {
        if (progress == 1f) {
            onSplashFinished()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress }
        )
    }
}
