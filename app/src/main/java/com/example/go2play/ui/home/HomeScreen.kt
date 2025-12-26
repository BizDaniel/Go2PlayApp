package com.example.go2play.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.go2play.ui.finduser.UserDetailsDialog
import com.example.go2play.ui.notifications.NotificationViewModel
import com.example.go2play.ui.profile.ProfileViewModel
import com.example.go2play.ui.theme.TrentinoAqua
import com.example.go2play.ui.theme.TrentinoBlue
import com.example.go2play.ui.theme.TrentinoLime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class WeatherData(
    val temperature: Double,
    val weatherCode: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    profileViewModel: ProfileViewModel = viewModel(),
    notificationViewModel: NotificationViewModel = viewModel(),
    onNavigateToCreateGroup: () -> Unit = {},
    onNavigateToMyGroups: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToMyEvents: () -> Unit = {},
    onNavigateToFindMatch: () -> Unit = {},
    onNavigateToFindUsers: () -> Unit = {}
) {
    val profileState by profileViewModel.profileState.collectAsStateWithLifecycle()
    val notificationState by notificationViewModel.notificationState.collectAsStateWithLifecycle()
    val profile = profileState.profile

    var weatherData by remember { mutableStateOf<WeatherData?>(null) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var currentTime by remember { mutableStateOf(getCurrentDateTime()) }

    // Ricarica le notifiche quando la schermata diventa visibile
    LaunchedEffect(Unit) {
        notificationViewModel.loadNotifications()
        try {
            weatherData = fetchWeather()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Per aggiornare l'ora
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = getCurrentDateTime()
            kotlinx.coroutines.delay(60000) // 60 secondi
        }
    }

    if (showProfileDialog && profile != null) {
        UserDetailsDialog(
            user = profile,
            onDismiss = { showProfileDialog = false }
        )
    }

    val headerGradient = Brush.verticalGradient(
        colors = listOf(
            TrentinoBlue,
            TrentinoAqua,
            TrentinoLime
        )
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                windowInsets = WindowInsets(0),
                title = {
                    Text(
                        text = "${currentTime.dayOfWeek}, ${currentTime.time}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Trento",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    weatherData?.let { weather ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = getWeatherEmoji(weather.weatherCode),
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${weather.temperature.toInt()}°C",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Card/Header con Avatar e Nome utente
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                onClick = { showProfileDialog = true }
            ) {
                Box(
                    modifier = Modifier
                        .background(headerGradient)
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Welcome back,",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onTertiary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = profile?.username ?: "User",
                                style = MaterialTheme.typography.headlineMedium,
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiary
                            )
                        }

                        // Avatar dell'utente
                        if (profile?.avatarUrl != null) {
                            AsyncImage(
                                model = profile.avatarUrl,
                                contentDescription = "User Avatar",
                                modifier = Modifier
                                    .size(66.dp)
                                    .clip(CircleShape)
                                    .border(
                                        3.dp,
                                        MaterialTheme.colorScheme.primary,
                                        CircleShape
                                    ),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(
                                        3.dp,
                                        MaterialTheme.colorScheme.secondary,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Default avatar",
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Sezione Quick Actions
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Griglia 2x2 dei pulsanti principali
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    EnhancedActionButton(
                        icon = Icons.Default.Search,
                        label = "Find Match",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToFindMatch
                    )
                    EnhancedActionButton(
                        icon = Icons.Default.Event,
                        label = "My Events",
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToMyEvents
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    EnhancedActionButton(
                        icon = Icons.Default.Groups,
                        label = "My Groups",
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToMyGroups
                    )
                    EnhancedActionButton(
                        icon = Icons.Default.GroupAdd,
                        label = "Create Group",
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToCreateGroup
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                ModernCard(
                    icon = Icons.Default.People,
                    title = "Find Users",
                    subtitle = "Check the profile of the other users!",
                    iconColor = Color(0xFFFF9800),
                    onClick = onNavigateToFindUsers
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Sezione Updates
                Text(
                    text = "Updates",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Card Notifiche
                ModernCard(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    subtitle = if (notificationState.pendingCount > 0) {
                        "${notificationState.pendingCount} pending invitation${if (notificationState.pendingCount > 1) "s" else ""}"
                    } else {
                        "Check your latest updates"
                    },
                    iconColor = Color(0xFFFF9800),
                    badge = if (notificationState.pendingCount > 0) notificationState.pendingCount.toString() else null,
                    onClick = onNavigateToNotifications
                )

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

suspend fun fetchWeather(): WeatherData = withContext(Dispatchers.IO) {
    // Coordinate città di Trento
    val lat = 46.0664
    val lon = 11.1257

    // Uso l'API Open-Meteo per ricavare info sul meteo
    val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true"

    val response = URL(url).readText()
    val json = JSONObject(response)
    val currentWeather = json.getJSONObject("current_weather")

    WeatherData(
        temperature = currentWeather.getDouble("temperature"),
        weatherCode = currentWeather.getInt("weathercode")
    )
}

fun getWeatherEmoji(weatherCode: Int): String {
    return when(weatherCode) {
        0 -> "☀️"
        1, 2, 3 -> "⛅"
        45, 48 -> "🌫️"
        51, 53, 55 -> "🌦️"
        61, 63, 65 -> "🌧️"
        66, 67 -> "🌨️"
        71, 73, 75 -> "❄️"
        77 -> "🌨️"
        80, 81, 82 -> "🌧️"
        85, 86 -> "❄️"
        95 -> "⛈️"
        96, 99 -> "⛈️"
        else -> "🌤️"
    }
}

@Composable
fun EnhancedActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(100),
        label = "scale"
    )
    Card(
        modifier = modifier
            .height(110.dp)
            .scale(scale),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 8.dp
        ),
        onClick = {
            isPressed = true
            onClick()
            // Reset dopo un breve delay
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.2f),
                modifier = Modifier.size(52.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        modifier = Modifier.size(26.dp),
                        tint = color
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = color,
                maxLines = 2,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(100)
            isPressed = false
        }
    }
}

@Composable
fun ModernCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color,
    badge: String? = null,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(100),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 8.dp
        ),
        onClick = {
            isPressed = true
            onClick()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icona con background circolare
            Surface(
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.15f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Testi
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Badge (se presente)
            badge?.let {
                Surface(
                    shape = CircleShape,
                    color = iconColor,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Freccia
            if (badge == null) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Go",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(100)
            isPressed = false
        }
    }
}

data class DateTimeInfo(
    val dayOfWeek: String,
    val time: String
)

fun getCurrentDateTime(): DateTimeInfo {
    val dateFormat = SimpleDateFormat("EEEE", Locale.ENGLISH)
    val timeFormat = SimpleDateFormat("HH:mm", Locale.ENGLISH)
    val now = Date()

    return DateTimeInfo(
        dayOfWeek = dateFormat.format(now),
        time = timeFormat.format(now)
    )
}