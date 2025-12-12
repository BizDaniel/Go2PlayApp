package com.example.go2play.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = FieldGreen80,
    secondary = LineWhite80,
    tertiary = GoalNet80,
    background = Color(0xFF1A1C1A),
    surface = Color(0xFF1F221F),
    surfaceVariant = Color(0xFF2A3D2A),
    surfaceContainer = Color(0xFF252D25),
    surfaceContainerHigh = Color(0xFF2D352D),
    surfaceContainerHighest = Color(0xFF353F35),
    primaryContainer = Color(0xFF3D5A3D),
    secondaryContainer = Color(0xFF4A6B4A),
    tertiaryContainer = Color(0xFF5A7A5A),
    onPrimary = Color(0xFF0D3D0D),
    onSecondary = Color(0xFF2D3B2D),
    onTertiary = Color(0xFF1A3D1A),
    onBackground = Color(0xFFE1E3E1),
    onSurface = Color(0xFFE1E3E1),
    onSurfaceVariant = Color(0xFFC0CCC0),
    onPrimaryContainer = Color(0xFFB8E6B8),
    onSecondaryContainer = Color(0xFFC8F0C8),
    onTertiaryContainer = Color(0xFFD0F5D0),
    outline = Color(0xFF8A9A8A),
    outlineVariant = Color(0xFF424F42)
)

private val LightColorScheme = lightColorScheme(
    primary = FieldGreen40,
    secondary = LineWhite40,
    tertiary = GoalNet40,
    background = Color(0xFFF8FBF8),
    surface = Color(0xFFFAFDFA),
    surfaceVariant = Color(0xFFE8F5E9),
    surfaceContainer = Color(0xFFF1F8F1),
    surfaceContainerHigh = Color(0xFFECF4EC),
    surfaceContainerHighest = Color(0xFFE6F1E6),
    primaryContainer = Color(0xFFC8E6C9),
    secondaryContainer = Color(0xFFDCEDC8),
    tertiaryContainer = Color(0xFFE8F5E9),
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFFFFFFFF),
    onTertiary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1A1C1A),
    onSurface = Color(0xFF1A1C1A),
    onSurfaceVariant = Color(0xFF3E4B3E),
    onPrimaryContainer = Color(0xFF1B5E20),
    onSecondaryContainer = Color(0xFF33691E),
    onTertiaryContainer = Color(0xFF2E7D32),
    outline = Color(0xFF6D7D6D),
    outlineVariant = Color(0xFFC0D0C0)
)

@Composable
fun Go2PlayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}