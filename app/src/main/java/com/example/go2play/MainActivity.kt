package com.example.go2play

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.example.go2play.data.remote.SupabaseClient
import com.example.go2play.ui.navigation.AppNavHost
import com.example.go2play.ui.theme.Go2PlayTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        SupabaseClient.initialize(applicationContext)

        setContent {
            Go2PlayTheme(
                dynamicColor = false,
            ) {

                SystemBarsBackgroundsAndAppearance()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    AppNavHost(navController)
                }
            }
        }
    }
}

@Composable
fun SystemBarsBackgroundsAndAppearance() {
    val view = LocalView.current
    val activity = view.context as? Activity ?: return
    val backgroundColor = MaterialTheme.colorScheme.background
    val isLightIcons = backgroundColor.luminance() <= 0.5f

    SideEffect {
        val controller = WindowCompat.getInsetsController(activity.window, view)
        controller.isAppearanceLightStatusBars = !isLightIcons // true = dark icons
        controller.isAppearanceLightNavigationBars = !isLightIcons
    }

    // Draw a box behind the status bar area with same color as background
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsTopHeight(WindowInsets.statusBars) // height = status bar inset
            .background(backgroundColor)
    )

    // Draw a box behind the navigation bar area with same color as background
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsBottomHeight(WindowInsets.navigationBars) // height = nav bar inset
            .background(backgroundColor)
    )
}

