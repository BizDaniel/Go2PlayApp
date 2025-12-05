package com.example.go2play.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SignUpScreen(
    viewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    onSignUpSuccess: () -> Unit
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }

    // Controlla disponibilità username quando cambia
    LaunchedEffect(username) {
        if (username.isNotBlank()) {
            viewModel.checkUsernameAvailability(username)
        } else {
            viewModel.resetUsernameCheck()
        }
    }

    // Se registrazione ok → vai alla home
    LaunchedEffect(authState.isAuthenticated) {
        if (authState.isAuthenticated) onSignUpSuccess()
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Sign Up", style = MaterialTheme.typography.headlineLarge)

            Spacer(Modifier.height(24.dp))

            // Campo Email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            // Campo Username con indicatore di disponibilità
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    when {
                        authState.isCheckingUsername -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        }
                        authState.isUsernameAvailable == true && username.isNotBlank() -> {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Available",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        authState.isUsernameAvailable == false -> {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Not available",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                supportingText = {
                    when {
                        username.isNotBlank() && username.length < 3 -> {
                            Text(
                                "Username must be at least 3 characters",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        authState.isUsernameAvailable == false -> {
                            Text(
                                "Username not available",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        authState.isUsernameAvailable == true -> {
                            Text(
                                "Username available",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                isError = username.isNotBlank() && (username.length < 3 || authState.isUsernameAvailable == false)
            )

            Spacer(Modifier.height(16.dp))

            // Campo password
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { viewModel.signUp(email, password, username) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !authState.isLoading
            ) {
                Text("Create Account")
            }

            Spacer(Modifier.height(16.dp))

            TextButton(onClick = onNavigateToLogin) {
                Text("Do you have an Account yet? Sign In")
            }

            authState.error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
