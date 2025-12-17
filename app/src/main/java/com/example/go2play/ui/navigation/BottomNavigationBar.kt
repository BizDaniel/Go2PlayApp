package com.example.go2play.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Explore : BottomNavItem("explore", "Explore", Icons.Default.Search)
    object Home : BottomNavItem("home", "Home", Icons.Default.Home)
    object Profile : BottomNavItem("profile", "Profile", Icons.Default.Person)
}

@Composable
fun BottomNavigationBar(
    selectedRoute: String,
    onNavigate: (String) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        NavigationBar(
            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars).height(64.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            val items = listOf(
                BottomNavItem.Explore,
                BottomNavItem.Home,
                BottomNavItem.Profile
            )

            items.forEach { item ->
                NavigationBarItem(
                    icon = { Icon(item.icon, contentDescription = item.title) },
                    label = { Text(item.title) },
                    selected = selectedRoute == item.route,
                    onClick = { onNavigate(item.route) }
                )
            }
        }
    }

}