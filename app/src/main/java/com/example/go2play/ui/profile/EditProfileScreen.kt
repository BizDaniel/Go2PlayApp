package com.example.go2play.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    viewModel: ProfileViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val profile = profileState.profile
    val context = LocalContext.current

    var username by remember { mutableStateOf("") }
    var selectedAge by remember { mutableStateOf<Int?>(null) }
    var selectedLevel by remember { mutableStateOf<String?>(null) }
    var selectedRoles by remember { mutableStateOf<List<String?>>(emptyList()) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    var ageExpanded by remember { mutableStateOf(false) }
    var levelExpanded by remember { mutableStateOf(false) }
    var rolesExpanded by remember { mutableStateOf(false) }

    val ageOptions = (10..90).toList()
    val levelOptions = listOf("Beginner", "Intermediate", "Advanced")
    val roleOptions = listOf("Goalkeeper", "Defender", "Midfielder", "Striker")

    // Inizializza i campi con i dati attuali
    LaunchedEffect(profile) {
        profile?.let {
            username = it.username
            selectedAge = it.age
            selectedLevel = it.level
            selectedRoles = it.preferredRoles?.split(", ")?.filter { role -> role.isNotBlank() } ?: emptyList()
        }
    }

    // Launcher per selezionare l'immagine
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        uri?.let {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(it)
                val bytes = inputStream?.readBytes()
                inputStream?.close()

                bytes?.let { imageBytes ->
                    viewModel.uploadAvatar(imageBytes)
                }
            } catch (e: Exception) {
                // Gestione errore
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    // Mostra messaggio di successo
    LaunchedEffect(profileState.uploadSuccess) {
        if (profileState.uploadSuccess) {
            snackbarHostState.showSnackbar("Foto profilo aggiornata!")
        }
    }

    // Mostra errori
    LaunchedEffect(profileState.error) {
        profileState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Modifica Profilo") },
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
        if (profileState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar con pulsante per cambiare foto
                Box(
                    modifier = Modifier.size(120.dp)
                ) {
                    if (selectedImageUri != null || profile?.avatarUrl != null) {
                        AsyncImage(
                            model = selectedImageUri ?: profile?.avatarUrl,
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Default avatar",
                                modifier = Modifier.size(60.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Pulsante camera
                    FloatingActionButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(40.dp),
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        if (profileState.isUploading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Cambia foto",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Username
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Email (non modificabile)
                OutlinedTextField(
                    value = profile?.email ?: "",
                    onValueChange = {},
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Età
                ExposedDropdownMenuBox(
                    expanded = ageExpanded,
                    onExpandedChange = { ageExpanded = !ageExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedAge?.toString() ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Age") },
                        trailingIcon = {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown"
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = ageExpanded,
                        onDismissRequest = { ageExpanded = false },
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        ageOptions.forEach { age ->
                            DropdownMenuItem(
                                text = { Text(age.toString()) },
                                onClick = {
                                    selectedAge = age
                                    ageExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Livello
                ExposedDropdownMenuBox(
                    expanded = levelExpanded,
                    onExpandedChange = { levelExpanded = !levelExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedLevel ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Level") },
                        trailingIcon = {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown"
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = levelExpanded,
                        onDismissRequest = { levelExpanded = false }
                    ) {
                        levelOptions.forEach { level ->
                            DropdownMenuItem(
                                text = { Text(level) },
                                onClick = {
                                    selectedLevel = level
                                    levelExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Ruoli preferiti
                ExposedDropdownMenuBox(
                    expanded = rolesExpanded,
                    onExpandedChange = { rolesExpanded = !rolesExpanded }
                ) {
                    OutlinedTextField(
                        value = if (selectedRoles.isEmpty()) "" else selectedRoles.joinToString(", "),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Preferred Roles") },
                        trailingIcon = {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown"
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        placeholder = { Text("Select roles") }
                    )

                    ExposedDropdownMenu(
                        expanded = rolesExpanded,
                        onDismissRequest = { rolesExpanded = false }
                    ) {
                        roleOptions.forEach { role ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(role)
                                        if (selectedRoles.contains(role)) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    selectedRoles = if (selectedRoles.contains(role)) {
                                        selectedRoles - role
                                    } else {
                                        selectedRoles + role
                                    }
                                }
                            )
                        }
                    }
                }


                Spacer(modifier = Modifier.height(32.dp))

                // Pulsante salva
                Button(
                    onClick = {
                        viewModel.updateProfile(
                            username = username,
                            age = selectedAge,
                            level = selectedLevel,
                            preferredRoles = if (selectedRoles.isEmpty()) null else selectedRoles.joinToString(", "),
                            onUpdateSuccess = onNavigateBack
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = username.isNotBlank() && !profileState.isLoading
                ) {
                    Text("Save Changes")
                }
            }
        }
    }
}

