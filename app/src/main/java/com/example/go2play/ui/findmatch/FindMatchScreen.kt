package com.example.go2play.ui.findmatch

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.go2play.data.model.Event
import com.example.go2play.data.model.EventStatus
import com.example.go2play.data.model.Field
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindMatchScreen(
    viewModel: FindMatchViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val findState by viewModel.findState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedEvent by remember { mutableStateOf<EventWithFieldInfo?>(null) }
    var showFilters by remember { mutableStateOf(false) }

    // Mostra errori
    LaunchedEffect(findState.error) {
        findState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    // Dialog dettagli evento
    selectedEvent?.let { eventInfo ->
        EventDetailDialog(
            event = eventInfo.event,
            field = eventInfo.field,
            currentUserId = findState.currentUserId,
            onDismiss = { selectedEvent = null },
            onJoin = {
                viewModel.joinEvent(eventInfo.event.id) {
                    selectedEvent = null
                }
            },
            isJoining = findState.isJoining
        )
    }

    // Dialog filtri
    if (showFilters) {
        FiltersDialog(
            selectedDate = findState.selectedDate,
            selectedFieldId = findState.selectedFieldId,
            availableFields = findState.availableFields,
            onDateSelected = { viewModel.selectDate(it) },
            onFieldSelected = { viewModel.selectField(it) },
            onDismiss = { showFilters = false },
            onClearFilters = { viewModel.clearFilters() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Find a Match") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Badge che indica i filtri attivi
                    if (findState.selectedDate != null || findState.selectedFieldId != null) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            val filterCount = listOfNotNull(
                                findState.selectedDate,
                                findState.selectedFieldId
                            ).size
                            Text(filterCount.toString())
                        }
                    }
                    IconButton(onClick = { showFilters = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filters")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                findState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                findState.events.isEmpty() -> {
                    EmptyEventsView(
                        hasFilters = findState.selectedDate != null || findState.selectedFieldId != null,
                        modifier = Modifier.align(Alignment.Center),
                        onClearFilters = { viewModel.clearFilters() }
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Header con info
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Available Matches",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${findState.events.size} events found",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                    Icon(
                                        Icons.Default.SportsSoccer,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        // Lista eventi
                        items(findState.events) { eventInfo ->
                            PublicEventCard(
                                event = eventInfo.event,
                                field = eventInfo.field,
                                onClick = { selectedEvent = eventInfo }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PublicEventCard(
    event: Event,
    field: Field,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = field.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                // Badge status
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (event.status) {
                        EventStatus.OPEN -> MaterialTheme.colorScheme.primary
                        EventStatus.FULL -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.outline
                    }.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = when (event.status) {
                            EventStatus.OPEN -> "Open"
                            EventStatus.FULL -> "Full"
                            else -> "Closed"
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = when (event.status) {
                            EventStatus.OPEN -> MaterialTheme.colorScheme.primary
                            EventStatus.FULL -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.outline
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Indirizzo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = "Location",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = field.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Data e ora
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = "Date",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = formatDate(event.date),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = "Time",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = event.timeSlot,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Giocatori e formato
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.People,
                        contentDescription = "Players",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "${event.currentPlayers.size}/${event.maxPlayers}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "${field.playerCapacity}v${field.playerCapacity}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // Descrizione (se presente)
            event.description?.let { description ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
fun EventDetailDialog(
    event: Event,
    field: Field,
    currentUserId: String?,
    onDismiss: () -> Unit,
    onJoin: () -> Unit,
    isJoining: Boolean
) {
    val isOrganizer = event.organizerId == currentUserId
    val isAlreadyJoined = currentUserId?.let { event.currentPlayers.contains(it) } ?: false

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Event Details",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                ) {
                    // Campo
                    DetailRow(
                        icon = Icons.Default.SportsSoccer,
                        label = "Field",
                        value = field.name
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    DetailRow(
                        icon = Icons.Default.LocationOn,
                        label = "Address",
                        value = field.address
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    // Data e ora
                    DetailRow(
                        icon = Icons.Default.CalendarMonth,
                        label = "Date",
                        value = formatDate(event.date)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    DetailRow(
                        icon = Icons.Default.Schedule,
                        label = "Time",
                        value = event.timeSlot
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    // Info partita
                    DetailRow(
                        icon = Icons.Default.People,
                        label = "Players",
                        value = "${event.currentPlayers.size}/${event.maxPlayers}"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    DetailRow(
                        icon = Icons.Default.Group,
                        label = "Format",
                        value = "${field.playerCapacity}v${field.playerCapacity}"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    DetailRow(
                        icon = Icons.Default.Euro,
                        label = "Price per person",
                        value = "€${String.format("%.2f", field.pricePerPerson)}"
                    )

                    // Descrizione
                    event.description?.let { description ->
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Description",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Pulsante Join
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 3.dp
                ) {
                    Button(
                        onClick = onJoin,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = event.status == EventStatus.OPEN && !isJoining && !isOrganizer && !isAlreadyJoined

                    ) {
                        if (isJoining) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Join",
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when {
                                    isOrganizer -> "You are the Organizer"
                                    isAlreadyJoined -> "Already Joined"
                                    event.status == EventStatus.FULL -> "Event full"
                                    else -> "Join Match"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

@Composable
fun FiltersDialog(
    selectedDate: LocalDate?,
    selectedFieldId: String?,
    availableFields: List<Field>,
    onDateSelected: (LocalDate?) -> Unit,
    onFieldSelected: (String?) -> Unit,
    onDismiss: () -> Unit,
    onClearFilters: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Filters",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Filtro Data
                Text(
                    text = "Date",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedDate == null,
                        onClick = { onDateSelected(null) },
                        label = { Text("All") }
                    )
                    FilterChip(
                        selected = selectedDate == LocalDate.now(),
                        onClick = { onDateSelected(LocalDate.now()) },
                        label = { Text("Today") }
                    )
                    FilterChip(
                        selected = selectedDate == LocalDate.now().plusDays(1),
                        onClick = { onDateSelected(LocalDate.now().plusDays(1)) },
                        label = { Text("Tomorrow") }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Filtro Campo
                Text(
                    text = "Field",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFieldId == null,
                        onClick = { onFieldSelected(null) },
                        label = { Text("All Fields") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    availableFields.forEach { field ->
                        FilterChip(
                            selected = selectedFieldId == field.id,
                            onClick = { onFieldSelected(field.id) },
                            label = { Text(field.name, maxLines = 1) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Pulsanti
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onClearFilters()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Clear All")
                    }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Apply")
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyEventsView(
    hasFilters: Boolean,
    modifier: Modifier = Modifier,
    onClearFilters: () -> Unit
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (hasFilters) "No matches found" else "No available matches",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (hasFilters) "Try adjusting your filters" else "Check back later for new events",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (hasFilters) {
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onClearFilters) {
                Text("Clear Filters")
            }
        }
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val date = LocalDate.parse(dateString)
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())
        date.format(formatter)
    } catch (e: Exception) {
        dateString
    }
}