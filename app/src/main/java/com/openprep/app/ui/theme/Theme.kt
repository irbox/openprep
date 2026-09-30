package com.openprep.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.core.view.WindowCompat
import com.openprep.app.R

// 1. Define the Nunito Font Family (Make sure you uploaded these to res/font/)
val NunitoFontFamily = FontFamily(
    Font(R.font.nunito_sans_regular, FontWeight.Normal),
    Font(R.font.nunito_sans_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_sans_bold, FontWeight.Bold)
)

// 2. Apply it to Material Typography
val AppTypography = Typography().copy(
    displayLarge = Typography().displayLarge.copy(fontFamily = NunitoFontFamily),
    displayMedium = Typography().displayMedium.copy(fontFamily = NunitoFontFamily),
    displaySmall = Typography().displaySmall.copy(fontFamily = NunitoFontFamily),
    headlineLarge = Typography().headlineLarge.copy(fontFamily = NunitoFontFamily),
    headlineMedium = Typography().headlineMedium.copy(fontFamily = NunitoFontFamily),
    headlineSmall = Typography().headlineSmall.copy(fontFamily = NunitoFontFamily),
    titleLarge = Typography().titleLarge.copy(fontFamily = NunitoFontFamily),
    titleMedium = Typography().titleMedium.copy(fontFamily = NunitoFontFamily),
    titleSmall = Typography().titleSmall.copy(fontFamily = NunitoFontFamily),
    bodyLarge = Typography().bodyLarge.copy(fontFamily = NunitoFontFamily),
    bodyMedium = Typography().bodyMedium.copy(fontFamily = NunitoFontFamily),
    bodySmall = Typography().bodySmall.copy(fontFamily = NunitoFontFamily),
    labelLarge = Typography().labelLarge.copy(fontFamily = NunitoFontFamily),
    labelMedium = Typography().labelMedium.copy(fontFamily = NunitoFontFamily),
    labelSmall = Typography().labelSmall.copy(fontFamily = NunitoFontFamily)
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryColor,
    background = BackgroundDark,
    surface = SurfaceDark,
    onPrimary = BackgroundDark,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun OpenPrepTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = BackgroundDark.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AppTypography, // <-- Apply typography here
        content = content
    )
}
