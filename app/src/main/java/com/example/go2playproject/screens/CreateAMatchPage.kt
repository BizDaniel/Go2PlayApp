package com.example.go2playproject.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CheckboxDefaults.colors
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester.Companion.createRefs
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.navigation.NavHostController
import com.example.go2playproject.AuthState
import com.example.go2playproject.AuthViewModel
import com.example.go2playproject.R
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.navigation.navArgument
import com.example.go2playproject.CalcettoViewModel
import com.example.go2playproject.model.Field
import com.example.go2playproject.model.Group
import com.google.firebase.auth.FirebaseAuth
import kotlin.math.roundToInt


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAMatchPage(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    authViewModel: AuthViewModel,
    calcettoViewModel: CalcettoViewModel
) {
    val authState = authViewModel.authState.observeAsState()
    val context = LocalContext.current

    // Variabili di stato per creare la partita
    var isPrivate by remember { mutableStateOf(false) }
    var selectedField by remember { mutableStateOf<Field?>(null) }
    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }
    var numberOfPlayers by remember { mutableStateOf(10) }
    var sliderPosition by remember { mutableStateOf(0f) }
    val levels = listOf("Basic", "Intermediate", "Advanced")
    val level = levels.getOrElse(sliderPosition.roundToInt()) { "Basic" }
    var description by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf<Group?>(null)}

    /*
    NOTA:
    Attualmente le variabili su data, ora, campo e gruppo sono
    sempre null perchè non ho ancora implementato la logica per selezionarle
     */

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
            handle.get<Group>("selectedGroup")?.let { group ->
                selectedGroup = group
                handle.remove<Group>("selectedGroup")
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

    Scaffold(
        modifier = Modifier
            .navigationBarsPadding()
            .statusBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text(text = "Create a Match") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate("homepage") }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ChooseModeSection(
                    isPrivate = isPrivate,
                    onPrivateChange = { isPrivate = it }
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
                SetGroupSection(
                    navController,
                    isPrivate = isPrivate,
                    selectedGroup = selectedGroup
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
                ) // Aggiunta della nuova sezione
            }
            item {
                DescriptionSection(
                    description = description,
                    onDescriptionChange = { description = it }
                )
            }
            item {
                CreateButton(
                    isPrivate = isPrivate,
                    selectedField = selectedField,
                    selectedDate = selectedDate,
                    selectedTime = selectedTime,
                    numberOfPlayers = numberOfPlayers,
                    level = level,
                    description = description,
                    selectedGroup = selectedGroup,
                    calcettoViewModel = calcettoViewModel,
                    authViewModel = authViewModel,
                    navController = navController
                )
            }
        }
    }
}

@Composable
fun ChooseModeSection(
    isPrivate: Boolean,
    onPrivateChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = "Choose the mode", style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp))
        Switch(
            checked = isPrivate,
            onCheckedChange = onPrivateChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.Gray,
            )
        )
        Text(text = if (isPrivate) "private" else "public",
            style = TextStyle(fontSize = 16.sp))
    }
}

@Composable
fun SelectPitchDateHourSection(
    navController: NavHostController,
    selectedField: Field?,
    selectedDate: String,
    selectedTime: String
) {
    // Se non è stato selezionato nulla, mostra il bottone per iniziare la selezione
    if(selectedField == null || selectedDate.isEmpty() || selectedTime.isEmpty()) {
        Button(
            onClick = { navController.navigate("fieldselection")},
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.CalendarMonth,
                    contentDescription = "Calendar",
                    modifier = Modifier.padding(end = 8.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Select Pitch, Date and Hour",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    } else {
        // Se i dati sono stati selezionati, mostra la card di riepilogo
        SelectionSummaryCard(
            field = selectedField,
            date = selectedDate,
            time = selectedTime,
            onClick = {
                // Cliccando sulla card, l'utente può modificare la sua scelta
                navController.navigate("explorepage")
            }
        )
    }
}

@Composable
fun SelectionSummaryCard(
    field: Field,
    date: String,
    time: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick), // Rende la card cliccabile
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = field.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = field.address,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Divider(modifier = Modifier.padding(vertical = 4.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Date: $date",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Time: $time",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun SetGroupSection(
    navController: NavHostController,
    isPrivate: Boolean,
    selectedGroup: Group?
) {
    // Se è stato selezionato un gruppo mostra la Card di riepilogo
    if(isPrivate && selectedGroup != null) {
        GroupSummaryCard(
            group = selectedGroup,
            onClick = { navController.navigate("groupselection")}
        )
    } else {
        Button(
            onClick = { navController.navigate("groupselection") },
            modifier = Modifier.fillMaxWidth(),
            // Il bottone è abilitato solo se la partita è privata
            enabled = isPrivate,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContainerColor = Color.LightGray.copy(alpha = 0.5f)
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Group,
                    contentDescription = "Group",
                    modifier = Modifier.padding(4.dp),
                    tint = if (isPrivate) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
                )
                Text(
                    text = if (isPrivate) "Set Group" else "Only for private matches",
                    color = if (isPrivate) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
                )
            }
        }
    }
}

@Composable
fun SelectNumberOfPlayersSection(
    numberOfPlayers: Int,
    onPlayersChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = "Select the number of players", style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (numberOfPlayers > 6) onPlayersChange(numberOfPlayers - 1) }) { // Evita numeri negativi
                Icon(Icons.Default.Remove, contentDescription = "Remove Player")
            }
            Text(
                text = "$numberOfPlayers",
                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            IconButton(onClick = { if(numberOfPlayers < 22) onPlayersChange(numberOfPlayers + 1) }) {
                Icon(Icons.Default.Add, contentDescription = "Add Player")
            }
        }
    }
}

@Composable
fun LevelOfTheMatchSection(
    sliderPosition: Float,
    onSliderChange: (Float) -> Unit
) {
    val levels = listOf("Basic", "Intermediate", "Advanced")
    val level = levels.getOrElse(sliderPosition.roundToInt()) { "Basic" }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Level of the match", style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp))
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = sliderPosition,
            onValueChange = onSliderChange,
            valueRange = 0f..1f,
            steps = 1, // Definisce 3 punti selezionabili
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = Color.LightGray,
            ),
            onValueChangeFinished = {
                // Opzionale: fare qualcosa quando l'utente rilascia lo slider
            },
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(levels[0], style = TextStyle(fontSize = 14.sp, color = Color.Gray))
            Text(levels[1], style = TextStyle(fontSize = 14.sp, color = Color.Gray))
            Text(levels[2], style = TextStyle(fontSize = 14.sp, color = Color.Gray))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Selected level: $level", style = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp))
    }
}


@Composable
fun DescriptionSection(
    description: String,
    onDescriptionChange: (String) -> Unit
) {
    Column {
        Text(text = "Description (write some extra info):", style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp))
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = description,
            onValueChange = onDescriptionChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("write") },
            colors = TextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
            textStyle = TextStyle(fontSize = 16.sp),
            minLines = 3, //Imposta altezza minima
        )
    }
}

@Composable
fun GroupSummaryCard(
    group: Group,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Group, contentDescription = "Selected Group", modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Selected Group",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun CreateButton(
    isPrivate: Boolean,
    selectedField: Field?,
    selectedDate: String,
    selectedTime: String,
    numberOfPlayers: Int,
    level: String,
    description: String,
    selectedGroup: Group?,
    calcettoViewModel: CalcettoViewModel,
    authViewModel: AuthViewModel,
    navController: NavHostController
) {
    Button(
        onClick = {
            // Ottieni l'ID dell'utente corrente
            val currentUser = authViewModel.auth.currentUser
            currentUser?.let { user ->
                val matchData = mapOf(
                    "fieldId" to (selectedField?.fieldId ?: ""),
                    "creatorId" to user.uid,
                    "date" to selectedDate, // Convertire in Date se necessario
                    "timeSlot" to selectedTime,
                    "players" to listOf(user.uid),
                    "ispublic" to !isPrivate,
                    "maxPlayers" to numberOfPlayers,
                    "groupId" to if (isPrivate) selectedGroup?.groupId else null,
                    "description" to description,
                    "level" to level
                )
                val safeMatchData = matchData.filterValues { it != null } as Map<String, Any>
                calcettoViewModel.createMatch(safeMatchData)
                navController.popBackStack()
            }
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = selectedField != null && selectedDate.isNotEmpty() && selectedTime.isNotEmpty()
    ) {
        Text(text = "Create", style = TextStyle(fontSize = 18.sp))
    }
}
