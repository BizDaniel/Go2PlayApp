package com.example.go2playproject.screens

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import com.example.go2playproject.AuthState
import com.example.go2playproject.AuthViewModel
import com.example.go2playproject.CalcettoViewModel
import com.example.go2playproject.model.Field
import com.example.go2playproject.model.Match
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyEventsPage(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    authViewModel: AuthViewModel,
    calcettoViewModel: CalcettoViewModel
) {
    val authState = authViewModel.authState.observeAsState()
    val context = LocalContext.current
    val currentUserId = authViewModel.auth.currentUser?.uid

    val upcomingMatches by calcettoViewModel.upcomingMatches.collectAsState()
    val archivedMatches by calcettoViewModel.archivedMatches.collectAsState()
    val matchDetails by calcettoViewModel.selectedMatchDetails.collectAsState()
    val fields by calcettoViewModel.fields.collectAsState()

    var selectedTab by remember { mutableStateOf(Tab.Upcoming) }
    var showDetailsDialog by remember { mutableStateOf(false) }


    LaunchedEffect(authState.value) {
        when (authState.value) {
            is AuthState.Unauthenticated -> navController.navigate("login")
            is AuthState.Error -> Toast.makeText(
                context,
                (authState.value as AuthState.Error).message,
                Toast.LENGTH_SHORT
            ).show()
            is AuthState.Authenticated -> {
                val userId = authViewModel.auth.currentUser?.uid ?: ""
                calcettoViewModel.fetchUserMatches(userId)
            }
            else -> Unit
        }
    }

    if (showDetailsDialog && matchDetails == null) {
        MatchDetailsDialog(
            matchDetails = matchDetails!!,
            onDismiss = {
                showDetailsDialog = false
                calcettoViewModel.clearSelectedMatchDetails()
            }
        )
    }

    Scaffold(
        modifier = Modifier
            .navigationBarsPadding()
            .statusBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text(text = "My Events") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate("homepage") }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = { BottomNavigationBar(navController, currentRoute = "homepage") }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TabButton(
                    text = "Upcoming",
                    isSelected = selectedTab == Tab.Upcoming,
                    onClick = { selectedTab = Tab.Upcoming }
                )
                TabButton(
                    text = "Archive",
                    isSelected = selectedTab == Tab.Archive,
                    onClick = { selectedTab = Tab.Archive }
                )
            }


            when (selectedTab) {
                Tab.Upcoming -> UpcomingEventsList(
                    matches = upcomingMatches,
                    fields = fields,
                    onShowDetails = { matchId ->
                        calcettoViewModel.getMatchDetails(matchId)
                        showDetailsDialog = true
                    },
                    currentUserId = currentUserId.toString(),
                    calcettoViewModel = calcettoViewModel,
                    navController = navController
                )
                Tab.Archive -> ArchiveEventsList(
                    matches = archivedMatches,
                    fields = fields,
                    onShowDetails = { matchId ->
                        calcettoViewModel.getMatchDetails(matchId)
                        showDetailsDialog = true
                    }
                )
            }
        }
    }
}

enum class Tab {
    Upcoming,
    Archive
}

@Composable
fun UpcomingEventCard(
    match: Match,
    field: String,
    onShowDetails: () -> Unit,
    navController: NavHostController,
    calcettoViewModel: CalcettoViewModel,
    currentUserId: String
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Match - ${match.level}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Field: $field", fontSize = 14.sp)
                    Text(text = "Date: ${dateFormat.format(match.date)}", fontSize = 14.sp)
                    Text(text = "Time: ${match.timeSlot}", fontSize = 14.sp)
                    Text(
                        text = if (match.ispublic) "Public" else "Private",
                        color = if (match.ispublic) Color.Green else Color.Gray,
                        fontSize = 14.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .border(1.dp, Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.SportsSoccer,
                        contentDescription = "Match",
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Group,
                        contentDescription = "Players",
                        tint = LocalContentColor.current.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Players: ${match.players.size}/${match.maxPlayers}",
                        fontSize = 14.sp
                    )
                }

                Button(
                    onClick = onShowDetails,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text("Show Details")
                }

                if (currentUserId == match.creatorId) {
                    Button(
                        onClick = {
                            calcettoViewModel.setMatchToEdit(match)
                            navController.navigate("editmatch")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Text("Edit")
                    }
                } else {
                    // Pulsante per uscire dalla partita se l'utente non è il creatore
                    Button(
                        onClick = {
                            calcettoViewModel.leaveMatch(match.matchId, currentUserId)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Leave the match")
                    }
                }
            }
        }
    }
}

@Composable
fun ArchiveEventCard(
    match: Match,
    field: String,
    onShowDetails: () -> Unit,
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Match - ${match.level}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Field: $field", fontSize = 14.sp)
                    Text(text = "Date: ${dateFormat.format(match.date)}", fontSize = 14.sp)
                    Text(text = "Time: ${match.timeSlot}", fontSize = 14.sp)
                    Text(
                        text = if (match.ispublic) "Public" else "Private",
                        color = if (match.ispublic) Color.Green else Color.Gray,
                        fontSize = 14.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .border(1.dp, Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.SportsSoccer,
                        contentDescription = "Match",
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Group,
                        contentDescription = "Players",
                        tint = LocalContentColor.current.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Players: ${match.players.size}/${match.maxPlayers}",
                        fontSize = 14.sp
                    )
                }

                Button(
                    onClick = onShowDetails,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text("Show Details")
                }
            }
        }
    }
}

@Composable
fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Text(text = text)
    }
}

@Composable
fun UpcomingEventsList(
    matches: List<Match>,
    fields: List<Field>,
    onShowDetails: (String) -> Unit,
    navController: NavHostController,
    calcettoViewModel: CalcettoViewModel,
    currentUserId: String
) {
    if (matches.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No upcoming events",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(matches) { match ->
                val field = fields.find { it.fieldId == match.fieldId }
                UpcomingEventCard(
                    match = match,
                    field = field?.name ?: "Unknown Field",
                    currentUserId = currentUserId,
                    calcettoViewModel = calcettoViewModel,
                    navController = navController,
                    onShowDetails = { onShowDetails(match.matchId) }
                )
            }
        }
    }
}

@Composable
fun ArchiveEventsList(
    matches: List<Match>,
    fields: List<Field>,
    onShowDetails: (String) -> Unit
) {
    if (matches.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No archived events",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(matches) { match ->
                val field = fields.find { it.fieldId == match.fieldId }
                ArchiveEventCard(
                    match = match,
                    field = field?.name ?: "Unknown Field",
                    onShowDetails = { onShowDetails(match.matchId) }
                )
            }
        }
    }
}

@Composable
fun MatchDetailsDialog(
    matchDetails: CalcettoViewModel.MatchDetails,
    onDismiss: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header con titolo e pulsante chiudi
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Match Details",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Informazioni del campo
                DetailRow(
                    icon = Icons.Default.LocationOn,
                    title = "Field",
                    content = matchDetails.field?.name ?: "Unknown Field"
                )

                if (matchDetails.field?.address?.isNotEmpty() == true) {
                    Text(
                        text = matchDetails.field.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 40.dp, bottom = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Data e orario
                DetailRow(
                    icon = Icons.Default.Schedule,
                    title = "Date & Time",
                    content = "${dateFormat.format(matchDetails.match.date)} - ${matchDetails.match.timeSlot}"
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Livello
                DetailRow(
                    icon = Icons.Default.SportsSoccer,
                    title = "Level",
                    content = matchDetails.match.level
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Privacy
                DetailRow(
                    icon = if (matchDetails.match.ispublic) Icons.Default.Group else Icons.Default.Person,
                    title = "Privacy",
                    content = if (matchDetails.match.ispublic) "Public Match" else "Private Match"
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Stato completamento
                DetailRow(
                    icon = Icons.Default.SportsSoccer,
                    title = "Status",
                    content = if (matchDetails.match.isCompleted) "Completed" else "Active"
                )

                // Descrizione se presente
                if (matchDetails.match.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Description",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = matchDetails.match.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sezione giocatori
                Text(
                    text = "Players (${matchDetails.match.players.size}/${matchDetails.match.maxPlayers})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Lista giocatori
                if (matchDetails.players.isNotEmpty()) {
                    matchDetails.players.forEach { player ->
                        PlayerItem(
                            name = player.name,
                            email = player.email,
                            isCreator = player.userId == matchDetails.match.creatorId
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                } else {
                    Text(
                        text = "Loading players...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Pulsante di chiusura
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun PlayerItem(
    name: String,
    email: String,
    isCreator: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Person,
            contentDescription = "Player",
            modifier = Modifier.size(16.dp),
            tint = if (isCreator) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isCreator) FontWeight.Bold else FontWeight.Normal
                )
                if (isCreator) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "(Creator)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}