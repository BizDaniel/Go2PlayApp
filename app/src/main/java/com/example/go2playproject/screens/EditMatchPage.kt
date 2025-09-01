package com.example.go2playproject.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.example.go2playproject.AuthState
import com.example.go2playproject.AuthViewModel
import com.example.go2playproject.CalcettoViewModel
import com.example.go2playproject.model.Field
import com.example.go2playproject.model.Group
import com.example.go2playproject.model.Match
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMatchPage(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    authViewModel: AuthViewModel,
    calcettoViewModel: CalcettoViewModel
) {
    val authState = authViewModel.authState.observeAsState()
    val context = LocalContext.current
    val matchToEdit by calcettoViewModel.matchToEdit.collectAsState()
    val fields by calcettoViewModel.fields.collectAsState()

    // Stati per l'editing
    var selectedField by remember { mutableStateOf<Field?>(null) }
    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }
    var numberOfPlayers by remember { mutableIntStateOf(10) }
    var sliderPosition by remember { mutableStateOf(0f) }
    var description by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(false) }

    val levels = listOf("Basic", "Intermediate", "Advanced")
    val level = levels.getOrElse(sliderPosition.roundToInt()) { "Basic" }

    // Inizializza i valori quando matchToEdit cambia
    LaunchedEffect(matchToEdit, fields) {
        matchToEdit?.let { match ->
            selectedField = fields.find { it.fieldId == match.fieldId }
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            selectedDate = dateFormat.format(match.date)
            selectedTime = match.timeSlot
            numberOfPlayers = match.maxPlayers
            description = match.description
            isPublic = match.ispublic

            // Imposta la posizione dello slider basata sul livello
            sliderPosition = when (match.level) {
                "Basic" -> 0f
                "Intermediate" -> 0.5f
                "Advanced" -> 1f
                else -> 0f
            }
        }
    }

    // Handle return data from field selection
    LaunchedEffect(navController.currentBackStackEntry) {
        navController.currentBackStackEntry?.savedStateHandle?.let { handle ->
            handle.get<Field>("selectedField")?.let { field ->
                selectedField = field
                handle.remove<Field>("selectedField")
            }
            handle.get<String>("selectedDate")?.let { date ->
                selectedDate = date
                handle.remove<String>("selectedDate")
            }
            handle.get<String>("selectedTimeSlot")?.let { timeSlot ->
                selectedTime = timeSlot
                handle.remove<String>("selectedTimeSlot")
            }
        }
    }

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

    // Se non c'è una partita da modificare, torna indietro
    LaunchedEffect(matchToEdit) {
        if (matchToEdit == null) {
            navController.popBackStack()
        }
    }

    Scaffold(
        modifier = Modifier
            .navigationBarsPadding()
            .statusBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text(text = "Edit Match") },
                navigationIcon = {
                    IconButton(onClick = {
                        calcettoViewModel.clearMatchToEdit()
                        navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { paddingValues ->
        matchToEdit?.let { match ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    EditVisibilitySection(
                        isPublic = isPublic,
                        onPublicChange = { newIsPublic ->
                            if (newIsPublic) {
                                isPublic = true
                                // Una volta reso pubblico, non può più tornare privato
                            }
                        },
                        canChangeToPrivate = false // Non permettere il cambio da pubblico a privato
                    )
                }

                item {
                    SelectPitchDateHourSection(
                        navController = navController,
                        selectedField = selectedField,
                        selectedDate = selectedDate,
                        selectedTime = selectedTime
                    )
                }

                item {
                    SelectNumberOfPlayersSection(
                        numberOfPlayers = numberOfPlayers,
                        onPlayersChange = { numberOfPlayers = it }
                    )
                }

                item {
                    LevelOfTheMatchSection(
                        sliderPosition = sliderPosition,
                        onSliderChange = { sliderPosition = it }
                    )
                }

                item {
                    DescriptionSection(
                        description = description,
                        onDescriptionChange = { description = it }
                    )
                }

                item {
                    UpdateButton(
                        match = match,
                        selectedField = selectedField,
                        selectedDate = selectedDate,
                        selectedTime = selectedTime,
                        numberOfPlayers = numberOfPlayers,
                        level = level,
                        description = description,
                        isPublic = isPublic,
                        calcettoViewModel = calcettoViewModel,
                        authViewModel = authViewModel,
                        navController = navController
                    )
                }
            }
        }
    }
}

@Composable
fun UpdateButton(
    match: Match,
    selectedField: Field?,
    selectedDate: String,
    selectedTime: String,
    numberOfPlayers: Int,
    level: String,
    description: String,
    isPublic: Boolean,
    calcettoViewModel: CalcettoViewModel,
    authViewModel: AuthViewModel,
    navController: NavHostController
) {
    val context = LocalContext.current

    Button(
        onClick = {
            val currentUser = authViewModel.auth.currentUser
            currentUser?.let { user ->
                // Verifica che l'utente sia il creatore
                if (match.creatorId != user.uid) {
                    Toast.makeText(
                        context,
                        "Only the creator can edit this match",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@Button
                }

                // Converti la data da String a Date
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val newDate = try {
                    dateFormat.parse(selectedDate) ?: Date()
                } catch (e: Exception) {
                    Date()
                }

                // Aggiorna la partita
                calcettoViewModel.updateMatch(
                    matchId = match.matchId,
                    newDescription = description,
                    newLevel = level,
                    newFieldId = selectedField?.fieldId ?: match.fieldId,
                    newDate = newDate,
                    newTimeSlot = selectedTime,
                    newMaxPlayers = numberOfPlayers,
                    isPublic = isPublic,
                    currentUserId = user.uid
                )

                // Se la partita è stata resa pubblica e prima era privata
                if (isPublic && !match.ispublic) {
                    calcettoViewModel.switchMatchToPublic(match.matchId, user.uid)
                }

                Toast.makeText(context, "Match updated successfully!", Toast.LENGTH_SHORT).show()
                calcettoViewModel.clearMatchToEdit()
                navController.popBackStack()
            }
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = selectedField != null && selectedDate.isNotEmpty() && selectedTime.isNotEmpty(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Text(
            text = "Update Match",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun EditVisibilitySection(
    isPublic: Boolean,
    onPublicChange: (Boolean) -> Unit,
    canChangeToPrivate: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPublic) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Match Visibility",
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
                androidx.compose.material3.Switch(
                    checked = isPublic,
                    onCheckedChange = { newValue ->
                        // Permetti solo il cambio da privato a pubblico
                        if (newValue || canChangeToPrivate) {
                            onPublicChange(newValue)
                        }
                    },
                    enabled = !isPublic || canChangeToPrivate, // Disabilita se è già pubblico
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color.Gray,
                        disabledCheckedThumbColor = Color.White,
                        disabledCheckedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Private",
                    fontSize = 14.sp,
                    color = if (!isPublic) MaterialTheme.colorScheme.primary else Color.Gray
                )
                Text(
                    text = "Public",
                    fontSize = 14.sp,
                    color = if (isPublic) MaterialTheme.colorScheme.primary else Color.Gray

                )
            }

            if (isPublic) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "⚠️ Once made public, the match cannot be changed back to private",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
