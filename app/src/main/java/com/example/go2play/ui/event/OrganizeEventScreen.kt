package com.example.go2play.ui.event

import android.widget.Toast
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.go2play.data.model.Group
import com.example.go2play.data.model.SlotStatus
import org.threeten.bp.LocalDate
import java.util.Locale
import org.threeten.bp.format.TextStyle


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizeEventScreen(
    fieldId: String,
    viewModel: OrganizeEventViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val eventState by viewModel.eventState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(fieldId) {
        viewModel.loadField(fieldId)
    }

    LaunchedEffect(eventState.error) {
        eventState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    if (eventState.showDateTimePicker) {
        DateTimePickerDialog(
            selectedDate = eventState.selectedDate,
            selectedTimeSlot = eventState.selectedTimeSlot,
            availableSlots = eventState.availableSlots,
            isLoadingSlots = eventState.isLoading,
            onDateSelected = { date -> viewModel.selectDate(date) },
            onTimeSlotSelected = { slot -> viewModel.selectTimeSlot(slot) },
            onDismiss = { viewModel.toggleDateTimePicker() },
            onConfirm = { viewModel.toggleDateTimePicker() }
        )
    }

    if (eventState.showGroupPicker) {
        GroupPickerDialog(
            groups = eventState.userGroups,
            selectedGroup = eventState.selectedGroup,
            onGroupSelected = { group -> viewModel.selectGroup(group) },
            onDismiss = { viewModel.toggleGroupPicker() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Organize Event", fontWeight = FontWeight.Bold) },
                windowInsets = WindowInsets(0),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                eventState.isLoading && eventState.field == null -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                eventState.field == null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Field not found")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onNavigateBack) {
                            Text("Go Back")
                        }
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp)
                    ) {
                        // Info campo
                        Text(
                            text = eventState.field!!.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = eventState.field!!.address,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Card selezione data e ora
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { viewModel.toggleDateTimePicker() }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Date & Time",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    if (eventState.selectedDate != null && eventState.selectedTimeSlot != null) {
                                        val formatter = org.threeten.bp.format.DateTimeFormatter.ofPattern("dd MMM yyyy")
                                        Text(
                                            text = "${eventState.selectedDate!!.format(formatter)}",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = eventState.selectedTimeSlot!!,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        Text(
                                            text = "Select date and time",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(
                                    Icons.Default.CalendarMonth,
                                    contentDescription = "Select date",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))

                        // Privacy toggle
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Event Privacy",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (eventState.isPrivate) "Only your group can join" else "Anyone can join",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = eventState.isPrivate,
                                        onCheckedChange = { viewModel.togglePrivacy(it) }
                                    )
                                }

                                // Gruppo selezionato o pulsante aggiungi gruppo
                                if (eventState.isPrivate) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(16.dp))

                                    if (eventState.selectedGroup != null) {
                                        // Mostra gruppo selezionato
                                        SelectedGroupCard(
                                            group = eventState.selectedGroup!!,
                                            onRemove = { viewModel.removeSelectedGroup() },
                                            onChange = { viewModel.toggleGroupPicker() }
                                        )
                                    } else {
                                        // Pulsante per aggiungere gruppo
                                        OutlinedButton(
                                            onClick = { viewModel.toggleGroupPicker() },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                Icons.Default.GroupAdd,
                                                contentDescription = "Add group",
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Add Group")
                                        }

                                        if (eventState.userGroups.isEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "You don't have any groups yet",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Descrizione evento
                        OutlinedTextField(
                            value = eventState.description,
                            onValueChange = { viewModel.updateDescription(it) },
                            label = { Text("Event Description (optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5,
                            shape = RoundedCornerShape(30.dp),
                            placeholder = { Text("Add details about your match...") }
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Info partita
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Match Info",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                InfoRow(
                                    icon = Icons.Default.People,
                                    label = "Format",
                                    value = "${eventState.field!!.playerCapacity}v${eventState.field!!.playerCapacity}"
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                InfoRow(
                                    icon = Icons.Default.Group,
                                    label = "Total Players",
                                    value = "${eventState.field!!.playerCapacity * 2}"
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                InfoRow(
                                    icon = Icons.Default.Schedule,
                                    label = "Duration",
                                    value = "1h 30min"
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Pulsante crea evento
                        Button(
                            onClick = {
                                viewModel.createEvent {
                                    Toast.makeText(context, "Event organized successfully!", Toast.LENGTH_SHORT).show()
                                    onNavigateBack()
                            } },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            enabled = viewModel.canCreateEvent(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (eventState.isCreating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Creating...")
                            } else {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Create",
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Create Event",
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
}

@Composable
fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun DateTimePickerDialog(
    selectedDate: LocalDate?,
    selectedTimeSlot: String?,
    availableSlots: List<com.example.go2play.data.model.TimeSlot>,
    isLoadingSlots: Boolean,
    onDateSelected: (LocalDate) -> Unit,
    onTimeSlotSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Select Date & Time",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Calendario
                CalendarView(
                    selectedDate = selectedDate,
                    onDateSelected = onDateSelected
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Time slots
                if (selectedDate != null) {
                    Text(
                        text = "Available Time Slots",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isLoadingSlots) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                count = availableSlots.size
                            ) { index ->
                                val slot = availableSlots[index]
                                TimeSlotItem(
                                    slot = slot,
                                    isSelected = slot.displayTime == selectedTimeSlot,
                                    onClick = {
                                        if (slot.status != SlotStatus.BOOKED) {
                                            onTimeSlotSelected(slot.displayTime)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Legenda
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        LegendItem(color = MaterialTheme.colorScheme.primary, label = "Selected")
                        LegendItem(color = MaterialTheme.colorScheme.surface, label = "Available")
                        LegendItem(color = MaterialTheme.colorScheme.errorContainer, label = "Booked")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Pulsanti
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        enabled = selectedDate != null && selectedTimeSlot != null
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarView(
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val maxDate = today.plusDays(30)

    var currentMonth by remember { mutableStateOf(today.withDayOfMonth(1)) }

    Column {
        // Header mese
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    val newMonth = currentMonth.minusMonths(1)
                    if (!newMonth.isBefore(today.withDayOfMonth(1))) {
                        currentMonth = newMonth
                    }
                },
                enabled = currentMonth.isAfter(today.withDayOfMonth(1))
            ) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month")
            }

            Text(
                text = currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) +
                        " ${currentMonth.year}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = {
                    val newMonth = currentMonth.plusMonths(1)
                    if (!newMonth.isAfter(maxDate.withDayOfMonth(1))) {
                        currentMonth = newMonth
                    }
                },
                enabled = currentMonth.plusMonths(1).isBefore(maxDate.withDayOfMonth(1).plusDays(1))
            ) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next month")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Giorni della settimana
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Griglia giorni
        val daysInMonth = currentMonth.lengthOfMonth()
        val firstDayOfWeek = currentMonth.dayOfWeek.value

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 250.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Spazi vuoti prima del primo giorno
            items(firstDayOfWeek - 1) {
                Spacer(modifier = Modifier.height(40.dp))
            }

            // Giorni del mese
            items(daysInMonth) { dayIndex ->
                val day = currentMonth.plusDays(dayIndex.toLong())
                val isSelectable = !day.isBefore(today) && !day.isAfter(maxDate)
                val isSelected = day == selectedDate

                DayItem(
                    day = day.dayOfMonth,
                    isSelected = isSelected,
                    isSelectable = isSelectable,
                    onClick = {
                        if (isSelectable) {
                            onDateSelected(day)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun DayItem(
    day: Int,
    isSelected: Boolean,
    isSelectable: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(40.dp)
            .clickable(enabled = isSelectable, onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = when {
            isSelected -> MaterialTheme.colorScheme.primary
            else -> Color.Transparent
        }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    !isSelectable -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    else -> MaterialTheme.colorScheme.onSurface
                },
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun TimeSlotItem(
    slot: com.example.go2play.data.model.TimeSlot,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when (slot.status) {
        SlotStatus.SELECTED -> MaterialTheme.colorScheme.primary
        SlotStatus.BOOKED -> MaterialTheme.colorScheme.errorContainer
        SlotStatus.AVAILABLE -> MaterialTheme.colorScheme.surface
    }

    val contentColor = when (slot.status) {
        SlotStatus.SELECTED -> MaterialTheme.colorScheme.onPrimary
        SlotStatus.BOOKED -> MaterialTheme.colorScheme.onErrorContainer
        SlotStatus.AVAILABLE -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = slot.status != SlotStatus.BOOKED, onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
        border = if (slot.status == SlotStatus.AVAILABLE) {
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = slot.displayTime,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = contentColor
            )

            when (slot.status) {
                SlotStatus.SELECTED -> Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = contentColor
                )
                SlotStatus.BOOKED -> Text(
                    text = "Booked",
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor
                )
                else -> {}
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(color, RoundedCornerShape(4.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun GroupPickerItem(
    group: Group,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Immagine del gruppo
                if (group.groupImageUrl != null) {
                    AsyncImage(
                        model = group.groupImageUrl,
                        contentDescription = "Group image",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Groups,
                            contentDescription = "Default group image",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${group.memberIDs.size} members",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun GroupPickerDialog(
    groups: List<Group>,
    selectedGroup: Group?,
    onGroupSelected: (Group) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Select Group",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (groups.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Groups,
                            contentDescription = "No groups",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "You don't have any groups",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(groups.size) { index ->
                            val group = groups[index]
                            GroupPickerItem(
                                group = group,
                                isSelected = group.id == selectedGroup?.id,
                                onClick = { onGroupSelected(group) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
fun SelectedGroupCard(
    group: Group,
    onRemove: () -> Unit,
    onChange: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Immagine del gruppo
                if (group.groupImageUrl != null) {
                    AsyncImage(
                        model = group.groupImageUrl,
                        contentDescription = "Group image",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Groups,
                            contentDescription = "Default group image",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "${group.memberIDs.size} members",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            Row {
                IconButton(onClick = onChange) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Change group",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove group",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}
