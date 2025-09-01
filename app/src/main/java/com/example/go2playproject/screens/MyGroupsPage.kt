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
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
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
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.go2playproject.AuthState
import com.example.go2playproject.AuthViewModel
import com.example.go2playproject.CalcettoViewModel
import com.example.go2playproject.model.Group

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyGroupsPage(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    authViewModel: AuthViewModel,
    calcettoViewModel: CalcettoViewModel
) {
    val authState = authViewModel.authState.observeAsState()
    val context = LocalContext.current

    val userGroups by calcettoViewModel.userGroups.collectAsState()
    val currentUser = authViewModel.auth.currentUser

    LaunchedEffect(currentUser) {
        if(currentUser != null) {
            calcettoViewModel.fetchUserGroups(currentUser.uid)
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
                title = { Text(text = "My Groups") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate("homepage") }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = { BottomNavigationBar(navController, currentRoute = "homepage") }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Se la lista è vuota, mostra un messaggio
            if(userGroups.isEmpty()) {
                item {
                    Text(
                        text = "You are not part of any group yet. Create one or ask a friend to add you!",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                items(userGroups) { group ->
                    GroupCard(
                        group = group,
                        currentUserId = currentUser?.uid,
                        onEditClick = { groupToEdit ->
                            // Navigazione alla edit page
                            calcettoViewModel.setGroupToEdit(groupToEdit)
                            navController.navigate("editgroup")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun GroupCard(
    group: Group,
    currentUserId: String?,
    onEditClick: (Group) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = group.name,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Group,
                    contentDescription = "Members",
                    tint = LocalContentColor.current.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(text = "Members: ${group.members.size}", fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Matches created: ${group.matchesCreated}",
                color = Color.Green,
                fontSize = 14.sp
            )

            if (group.creatorId == currentUserId) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onEditClick(group) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Edit Group",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Edit Group")
                }
            }
        }
    }
}