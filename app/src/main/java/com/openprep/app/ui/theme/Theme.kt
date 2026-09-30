package com.openprep.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.core.view.WindowCompat
import com.openprep.app.R

val NunitoFontFamily = FontFamily(
    Font(R.font.nunito_sans_regular, FontWeight.Normal),
    Font(R.font.nunito_sans_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_sans_bold, FontWeight.Bold)
)

val AppTypography = Typography().copy(
    displayLarge = Typography().displayLarge.copy(fontFamily = NunitoFontFamily),
    headlineMedium = Typography().headlineMedium.copy(fontFamily = NunitoFontFamily),
    titleLarge = Typography().titleLarge.copy(fontFamily = NunitoFontFamily),
    bodyLarge = Typography().bodyLarge.copy(fontFamily = NunitoFontFamily),
    bodyMedium = Typography().bodyMedium.copy(fontFamily = NunitoFontFamily),
    labelLarge = Typography().labelLarge.copy(fontFamily = NunitoFontFamily)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF00C6A0),
    secondary = Color(0xFF009688),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF2C2C2C),
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    error = Color(0xFFCF6679)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF00C6A0),
    secondary = Color(0xFF009688),
    background = Color(0xFFF4F7F9),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE0E0E0),
    onPrimary = Color.White,
    onBackground = Color(0xFF1D1D1D),
    onSurface = Color(0xFF1D1D1D),
    error = Color(0xFFB00020)
)

@Composable
fun OpenPrepTheme(
    themeMode: Int = 0, // 0=Sys, 1=Light, 2=Dark
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    
    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDark
        }
    }

    MaterialTheme(colorScheme = colorScheme, typography = AppTypography, content = content)
}
