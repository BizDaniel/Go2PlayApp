package com.example.go2playproject.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.layout.ModifierInfo
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.go2playproject.AuthState
import com.example.go2playproject.AuthViewModel
import com.example.go2playproject.CalcettoViewModel
import com.example.go2playproject.model.Field
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldSelectionPage(
    navController: NavController,
    authViewModel: AuthViewModel,
    calcettoViewModel: CalcettoViewModel
) {
    val authState = authViewModel.authState.observeAsState()
    val context = LocalContext.current
    val fields by calcettoViewModel.fields.collectAsState()

    var selectedField by remember { mutableStateOf<Field?>(null) }
    var selectedDate by remember { mutableStateOf<Date?>(null) }
    var selectedTimeSlot by remember { mutableStateOf<String?>(null) }
    var showCalendar by remember { mutableStateOf(false) }
    var showTimeSlots by remember { mutableStateOf(false) }

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
        modifier = Modifier
            .navigationBarsPadding()
            .statusBarsPadding(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when {
                            showTimeSlots -> "Select time Slot"
                            showCalendar -> "Select Data"
                            else -> "Select Field"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        when {
                            showTimeSlots -> {
                                showTimeSlots = false
                                showCalendar = true
                            }
                            showCalendar -> {
                                showCalendar = false
                                selectedField = null
                            }
                            else -> navController.navigate("createamatch")
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            showTimeSlots -> {
                TimeSlotSelection(
                    modifier = Modifier.padding(paddingValues),
                    selectedField = selectedField!!,
                    selectedDate = selectedDate!!,
                    onTimeSlotSelected = { timeSlot ->
                        selectedTimeSlot = timeSlot
                        // Navigazione indietro per tornare alla CreateAMatchPage
                        // con la data selezionata
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("selectedField", selectedField)
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("selectedDate", dateFormat.format(selectedDate!!))
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("selectedTimeSlot", timeSlot)
                        navController.popBackStack()
                    }
                )
            }
            showCalendar -> {
                CalendarSelection(
                    modifier = Modifier.padding(paddingValues),
                    selectedField = selectedField!!,
                    onDateSelected = { date ->
                        selectedDate = date
                        showCalendar = false
                        showTimeSlots = true
                    }
                )
            }
            else -> {
                FieldListSelection(
                    modifier = Modifier.padding(paddingValues),
                    fields = fields,
                    onFieldSelected = { field ->
                        selectedField = field
                        showCalendar = true
                    }
                )
            }
        }
    }
}

@Composable
fun FieldListSelection(
    modifier: Modifier = Modifier,
    fields: List<Field>,
    onFieldSelected: (Field) -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(fields) { field ->
            FieldItem(
                field = field,
                onFieldSelected = { onFieldSelected(field) }
            )
        }
    }
}

@Composable
fun FieldItem(
    field: Field,
    onFieldSelected: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onFieldSelected() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = field.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = field.address,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "€${field.pricePerHour}/hour",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun CalendarSelection(
    modifier: Modifier = Modifier,
    selectedField: Field,
    onDateSelected: (Date) -> Unit
) {
    val calendar = Calendar.getInstance()
    val today = calendar.time
    val dates = remember {
        val dateList = mutableListOf<Date>()
        calendar.time = today
        // Mostra i prossimi 30 giorni
        for (i in 0..29) {
            dateList.add(calendar.time)
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        dateList
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = selectedField.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = selectedField.address,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Select Date:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(dates) { date ->
                DateItem(
                    date = date,
                    onDateSelected = { onDateSelected(date) }
                )
            }
        }
    }
}

@Composable
fun DateItem(
    date: Date,
    onDateSelected: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd", Locale.getDefault())
    val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
    val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())

    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onDateSelected() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = dayFormat.format(date),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = dateFormat.format(date),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = monthFormat.format(date),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TimeSlotSelection(
    modifier: Modifier = Modifier,
    selectedField: Field,
    selectedDate: Date,
    onTimeSlotSelected: (String) -> Unit
) {
    val timeSlots = remember {
        generateTimeSlots()
    }
    val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = selectedField.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = selectedField.address,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = dateFormat.format(selectedDate),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Available Time Slots:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(timeSlots) { timeSlot ->
                TimeSlotItem(
                    timeSlot = timeSlot,
                    onTimeSlotSelected = { onTimeSlotSelected(timeSlot) }
                )
            }
        }
    }
}

@Composable
fun TimeSlotItem(
    timeSlot: String,
    onTimeSlotSelected: () -> Unit
) {
    Button(
        onClick = onTimeSlotSelected,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = timeSlot,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

private fun generateTimeSlots(): List<String> {
    val slots = mutableListOf<String>()
    val calendar = Calendar.getInstance()

    // Start from 9:00 AM
    calendar.set(Calendar.HOUR_OF_DAY, 9)
    calendar.set(Calendar.MINUTE, 0)

    // Generate slots until 7:00 PM (19:00)
    while (calendar.get(Calendar.HOUR_OF_DAY) < 19) {
        val startTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.time)
        calendar.add(Calendar.MINUTE, 90) // Add 1.5 hours
        val endTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.time)

        slots.add("$startTime - $endTime")
    }

    return slots
}