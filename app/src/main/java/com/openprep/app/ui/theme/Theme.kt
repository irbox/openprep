package com.openprep.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color // <-- THIS IS THE MISSING LINE!
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
    primary = PrimaryTeal,
    secondary = PrimaryTealDark,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = Color.White,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    error = ErrorRed
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryTeal,
    secondary = PrimaryTealDark,
    background = LightBackground,
    surface = LightSurface,
    onPrimary = Color.White,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    error = ErrorRed
)

@Composable
fun OpenPrepTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
