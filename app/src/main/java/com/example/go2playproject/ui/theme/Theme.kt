package com.example.go2playproject.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Go2PlayColors.PrimaryGreen,
    onPrimary = Color.White,
    primaryContainer = Go2PlayColors.AccentGreen,
    onPrimaryContainer = Go2PlayColors.PrimaryGreenDark,

    secondary = Go2PlayColors.SecondaryGreen,
    onSecondary = Color.White,
    secondaryContainer = Go2PlayColors.AccentGreen.copy(alpha = 0.3f),
    onSecondaryContainer = Go2PlayColors.PrimaryGreenDark,

    tertiary = Go2PlayColors.CategoryTeal,
    onTertiary = Color.White,

    background = Go2PlayColors.SurfaceLight,
    onBackground = Color.Black,

    surface = Go2PlayColors.CardLight,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = Color(0xFF424242),

    error = Go2PlayColors.Error,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Go2PlayColors.PrimaryGreen,
    onPrimary = Color.White,
    primaryContainer = Go2PlayColors.AccentGreen,
    onPrimaryContainer = Go2PlayColors.PrimaryGreenDark,

    secondary = Go2PlayColors.SecondaryGreen,
    onSecondary = Color.White,
    secondaryContainer = Go2PlayColors.AccentGreen.copy(alpha = 0.3f),
    onSecondaryContainer = Go2PlayColors.PrimaryGreenDark,

    tertiary = Go2PlayColors.CategoryTeal,
    onTertiary = Color.White,

    background = Go2PlayColors.SurfaceLight,
    onBackground = Color.Black,

    surface = Go2PlayColors.CardLight,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = Color(0xFF424242),

    error = Go2PlayColors.Error,
    onError = Color.White
)

@Composable
fun Go2PlayProjectTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}