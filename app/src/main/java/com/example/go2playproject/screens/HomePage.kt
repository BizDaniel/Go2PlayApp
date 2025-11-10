package com.example.go2playproject.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.go2playproject.AuthState
import com.example.go2playproject.AuthViewModel
import com.example.go2playproject.CalcettoViewModel
import com.example.go2playproject.UserViewModel
import com.example.go2playproject.ui.theme.Go2PlayColors.CardDark
import com.example.go2playproject.ui.theme.Go2PlayColors.Success

/*
* Definisco la funzione Composable HomePage
* */

@Composable
fun HomePage(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    authViewModel: AuthViewModel,
    calcettoViewModel: CalcettoViewModel,
    userViewModel: UserViewModel
) {
    val authState = authViewModel.authState.observeAsState()
    val context = LocalContext.current
    val currentUser = authViewModel.auth.currentUser
    val isDarkTheme = isSystemInDarkTheme()

    LaunchedEffect(authState.value) {
        when (authState.value) {
            is AuthState.Unauthenticated -> navController.navigate("login")
            is AuthState.Error -> Toast.makeText(
                context,
                (authState.value as AuthState.Error).message,
                Toast.LENGTH_SHORT
            ).show()
            else -> Unit
        }
    }

    Scaffold(
        containerColor = if (isDarkTheme) Color(0xFF0A0A0A) else Color(0xFFF8F9FA),
        bottomBar = {
            ModernBottomNavigationBar(
                navController = navController,
                currentRoute = "homepage"
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF2E7D32),
                                Color(0xFF4CAF50)
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 24.dp)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Welcome Back",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = currentUser?.displayName ?: "User",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Profile Avatar
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable { navController.navigate("profilepage") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            // Contenuto Principale
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ){
                item {
                    // Titolo delle mie azioni
                    Text(
                        text = "Your activities",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDarkTheme) Color.White else Color.Black.copy(alpha = 0.7f),
                    )
                }

                item {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.height(320.dp)
                    ) {
                        item {
                            ModernActionCard(
                                title = "Create Match",
                                icon = Icons.Default.Add,
                                backgroundColor = Color(0xFF2E7D32),
                                onClick = { navController.navigate("createamatchpage") }
                            )
                        }

                        item {
                            ModernActionCard(
                                title = "My Events",
                                icon = Icons.Default.Event,
                                backgroundColor = Color(0xFF00897B),
                                onClick = { navController.navigate("myevents") }
                            )
                        }

                        item {
                            ModernActionCard(
                                title = "Groups",
                                icon = Icons.Default.Groups,
                                backgroundColor = Color(0xFFE53935),
                                onClick = { navController.navigate("mygroups") }
                            )
                        }

                        item {
                            ModernActionCard(
                                title = "Find Match",
                                icon = Icons.Default.Search,
                                backgroundColor = Color(0xFFFF6F00),
                                onClick = { navController.navigate("available") }
                            )
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatItem(
                                label = "Matches",
                                value = "12",
                                color = Color(0xFF4CAF50)
                            )
                            Divider(
                                modifier = Modifier
                                    .height(40.dp)
                                    .width(1.dp),
                                color = Color.Gray.copy(alpha = 0.3f)
                            )
                            StatItem(
                                label = "Groups",
                                value = "3",
                                color = Color(0xFF2196F3)
                            )
                            Divider(
                                modifier = Modifier
                                    .height(40.dp)
                                    .width(1.dp),
                                color = Color.Gray.copy(alpha = 0.3f)
                            )
                            StatItem(
                                label = "Invites",
                                value = "2",
                                color = Color(0xFFFF9800)
                            )
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { /* TODO: Navigate to notifications */ },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = "Notifications",
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "2 new invitations",
                                        fontWeight = FontWeight.Medium,
                                        color = if (isDarkTheme) Color.White else Color.Black
                                    )
                                    Text(
                                        text = "Check your pending invites",
                                        fontSize = 12.sp,
                                        color = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50)
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

@Composable
fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .height(100.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Color.DarkGray
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = text,
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController, currentRoute: String = "homepage") {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .border(width = 1.dp, color = Color.LightGray)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        BottomNavItem(
            icon = Icons.Default.Explore,
            text = "Explore",
            selected = currentRoute == "explorepage",
            navController = navController,
            onClick = { navController.navigate("explorepage") }
        )

        BottomNavItem(
            icon = Icons.Default.Home,
            text = "General",
            selected = currentRoute == "homepage",
            navController = navController,
            onClick = { navController.navigate("homepage") }
        )

        BottomNavItem(
            icon = Icons.Default.Person,
            text = "Profile",
            selected = currentRoute == "profilepage",
            navController = navController,
            onClick = { navController.navigate("profilepage") }
        )
    }
}

@Composable
fun BottomNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    navController: NavHostController
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = if (selected) Color.Green else Color.Gray
        )
        Text(
            text = text,
            color = if (selected) Color.Green else Color.Gray,
            fontSize = 12.sp
        )
    }
}

@Composable
fun ModernActionCard(
    title: String,
    icon: ImageVector,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "More",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ModernBottomNavigationBar(
    navController: NavHostController,
    currentRoute: String
) {
    val isDarkTheme = isSystemInDarkTheme()

    NavigationBar(
        containerColor = if (isDarkTheme) CardDark else Color.White,
        contentColor = Success,
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            selected = currentRoute == "explorepage",
            onClick = { navController.navigate("explorepage") },
            icon = {
                Icon(
                    Icons.Default.Explore,
                    contentDescription = "Explore",
                    tint = if (currentRoute == "explorepage") Color(0xFF4CAF50) else Color.Gray
                )
            },
            label = {
                Text(
                    "Explore",
                    color = if (currentRoute == "explorepage") Color(0xFF4CAF50) else Color.Gray
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF4CAF50),
                unselectedIconColor = Color.Gray,
                selectedTextColor = Color(0xFF4CAF50),
                unselectedTextColor = Color.Gray,
                indicatorColor = Color.Transparent // Rimuove l'indicatore di sfondo
            )
        )

        NavigationBarItem(
            selected = currentRoute == "homepage",
            onClick = { navController.navigate("homepage") },
            icon = {
                Box(
                    modifier = if (currentRoute == "homepage") {
                        Modifier
                            .size(56.dp)
                            .background(Color(0xFF4CAF50), CircleShape)
                    } else Modifier,
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.SportsSoccer,
                        contentDescription = "Home",
                        tint = if (currentRoute == "homepage") Color.White else Color.Gray,
                        modifier = Modifier.size(if (currentRoute == "homepage") 28.dp else 24.dp)
                    )
                }
            },
            label = {
                Text(
                    "Home",
                    color = if (currentRoute == "homepage") Color(0xFF4CAF50) else Color.Gray
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF4CAF50),
                unselectedIconColor = Color.Gray,
                selectedTextColor = Color(0xFF4CAF50),
                unselectedTextColor = Color.Gray,
                indicatorColor = Color.Transparent // Rimuove l'indicatore di sfondo
            )
        )

        NavigationBarItem(
            selected = currentRoute == "profilepage",
            onClick = { navController.navigate("profilepage") },
            icon = {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = if (currentRoute == "profilepage") Color(0xFF4CAF50) else Color.Gray
                )
            },
            label = {
                Text(
                    "Profile",
                    color = if (currentRoute == "profilepage") Color(0xFF4CAF50) else Color.Gray
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF4CAF50),
                unselectedIconColor = Color.Gray,
                selectedTextColor = Color(0xFF4CAF50),
                unselectedTextColor = Color.Gray,
                indicatorColor = Color.Transparent // Rimuove l'indicatore di sfondo
            )
        )
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}





