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
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = Color(0xFF3B5F65),        // variante scura dell'azzurro
    onPrimaryContainer = TrentinoSoft,

    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = Color(0xFF4E5F28),      // verde lime molto scuro
    onSecondaryContainer = TrentinoSoft,

    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = Color(0xFF0F445E),       // blu scurissimo
    onTertiaryContainer = TrentinoSoft,

    background = DarkBackground,
    onBackground = DarkOnBackground,

    surface = DarkBackground,
    onSurface = DarkOnSurface,
    surfaceVariant = Color(0xFF3E494A),          // grigio-azzurro scuro
    onSurfaceVariant = TrentinoSoft,

    surfaceContainer = Color(0xFF262B2C),
    surfaceContainerHigh = Color(0xFF303536),
    surfaceContainerHighest = Color(0xFF383E3F),

    outline = Color(0xFF7E8C8E),                 // outline azzurro/neutral
    outlineVariant = Color(0xFF414A4C),

    error = Color(0xFFCF6679),                   // default Material
    onError = Color.Black
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = Color(0xFFD0E8F4),        // blu chiarissimo
    onPrimaryContainer = TrentinoBlue,

    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = Color(0xFFE9F3D0),      // lime chiaro E9F3D0FF
    onSecondaryContainer = TrentinoLime,

    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = Color(0xFFCDE1E4),       // aqua chiarissimo
    onTertiaryContainer = TrentinoAqua,

    background = LightBackground,
    onBackground = LightOnBackground,

    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = TrentinoSoft,
    onSurfaceVariant = TrentinoGrey,

    surfaceContainer = Color(0xFFF1F7F7),
    surfaceContainerHigh = Color(0xFFC3F6D2),
    surfaceContainerHighest = Color(0xFFE4EDED),

    outline = Color(0xFF7E9094),
    outlineVariant = Color(0xFFC9D4D6),

    error = Color(0xFFBA1A1A),
    onError = Color.White
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