package com.example.go2playproject.screens

import android.graphics.drawable.Icon
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.go2playproject.AuthState
import com.example.go2playproject.AuthViewModel
import com.example.go2playproject.ProfileUpdateState
import com.example.go2playproject.UserViewModel
import com.example.go2playproject.UsernameValidationState
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfilePage(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    authViewModel: AuthViewModel,
    userViewModel: UserViewModel
) {
    val authState = authViewModel.authState.observeAsState()
    val currentUser by userViewModel.currentUser.collectAsStateWithLifecycle()
    val isLoading by userViewModel.isLoading.collectAsStateWithLifecycle()
    val usernameValidation by userViewModel.usernameValidation.collectAsStateWithLifecycle()
    val profileUpdateState by userViewModel.profileUpdateState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var username by remember { mutableStateOf("") }
    var hasUsernameChanged by remember { mutableStateOf(false) }

    // Inizializza il campo username quando l'utente viene caricato
    LaunchedEffect(currentUser) {
        currentUser?.let {
            username = it.name
        }
    }

    // Gestione degli stati di aggiornamento del profilo
    LaunchedEffect(profileUpdateState) {
        when (profileUpdateState) {
            is ProfileUpdateState.Success -> {
                Toast.makeText(context, (profileUpdateState as ProfileUpdateState.Success).message, Toast.LENGTH_SHORT).show()
                userViewModel.resetProfileUpdateState()
                navController.navigateUp()
            }
            is ProfileUpdateState.Error -> {
                Toast.makeText(context, (profileUpdateState as ProfileUpdateState.Error).message, Toast.LENGTH_LONG)
            }
            else -> Unit
        }
    }

    // Verifica della disponibilità del nome utente con debounce
    LaunchedEffect(username, hasUsernameChanged) {
        if(hasUsernameChanged && username.isNotBlank() && username != currentUser?.name) {
            delay(500)
            userViewModel.checkUsernameAvailability(username)
        } else if (username == currentUser?.name) {
            userViewModel.resetUsernameValidation()
        }
    }

    // Gestione dello stato di autenticazione
    LaunchedEffect(authState.value) {
        when (authState.value) {
            is AuthState.Unauthenticated -> navController.navigate("login")
            else -> Unit
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Go Back")
                    }
                },
                actions = {
                    val canSave = username.isNotBlank() &&
                            username != currentUser?.name &&
                            usernameValidation is UsernameValidationState.Valid

                    IconButton(
                        onClick = {
                            userViewModel.updateUserProfile(username)
                        },
                        enabled = canSave && profileUpdateState !is ProfileUpdateState.Loading
                    ) {
                        if (profileUpdateState is ProfileUpdateState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Save",
                                tint = if (canSave) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Immagine del profilo
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
                    .clickable {
                        // TODO: Implementare selezione immagine
                        Toast.makeText(
                            context,
                            "Selezione immagine - da implementare",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile Photo",
                    tint = Color.DarkGray,
                    modifier = Modifier.size(64.dp)
                )
            }

            Text(
                text = "Touch to change Photo",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Campo email (sola lettura)
            OutlinedTextField(
                value = currentUser?.email ?: "",
                onValueChange = { },
                label = { Text("Email") },
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Campo nome utente
            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it
                    hasUsernameChanged = true
                },
                label = { Text("Username") },
                placeholder = { Text("Insert your username") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                isError = usernameValidation is UsernameValidationState.Invalid,
                trailingIcon = {
                    when (usernameValidation) {
                        is UsernameValidationState.Checking -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        }
                        is UsernameValidationState.Valid -> {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Username is valid",
                                tint = Color.Green
                            )
                        }
                        else -> null
                    }
                }
            )

            // Messaggio di validazione del nome utente
            when (usernameValidation) {
                is UsernameValidationState.Valid -> {
                    Text(
                        text = (usernameValidation as UsernameValidationState.Valid).message,
                        color = Color.Green,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 4.dp)
                    )
                }
                is UsernameValidationState.Invalid -> {
                    Text(
                        text = (usernameValidation as UsernameValidationState.Invalid).message,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 4.dp)
                    )
                }
                is UsernameValidationState.Checking -> {
                    Text(
                        text = "Checking availability...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 4.dp)
                    )
                }
                else -> {
                    if (username.isBlank()) {
                        Text(
                            text = "Username is mandatory to be find by the other users",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, top = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Info aggiuntive
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "ℹ️ Informations",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• The username has to be unique\n• Minimum 3 characters, maximum 30\n• Letters, numbers, underscore and single spaces\n• Is mandatory to be find by the other users",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}