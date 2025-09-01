package com.example.go2playproject.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.go2playproject.AuthState
import com.example.go2playproject.AuthViewModel
import com.example.go2playproject.CalcettoViewModel
import com.example.go2playproject.model.User
import androidx.compose.foundation.lazy.items


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAGroupPage(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    authViewModel: AuthViewModel,
    calcettoViewModel: CalcettoViewModel
) {
    val authState = authViewModel.authState.observeAsState()
    val context = LocalContext.current
    val currentUserId = authViewModel.auth.currentUser?.uid

    // Stati per la gestione del gruppo
    var groupName by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedMembers by remember { mutableStateOf(mutableSetOf<User>()) }

    // Stati per la ricerca utenti
    val searchResults by calcettoViewModel.userSearchResults.collectAsState()
    val isSearching by calcettoViewModel.isSearchingUsers.collectAsState()


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

    // Effettua la ricerca quando la query cambia
    LaunchedEffect(searchQuery) {
        if(searchQuery.isNotBlank()) {
            calcettoViewModel.searchUsers(searchQuery)
        }
    }

    Scaffold(
        modifier = Modifier
            .navigationBarsPadding()
            .statusBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text(text = "Create a Group") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate("homepage") }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            BottomNavigationBar(navController = navController, currentRoute = "homepage")
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Campo nome gruppo
            item {
                GroupNameSection(
                    groupName = groupName,
                    onGroupNameChange = { groupName = it }
                )
            }

            // Barra di ricerca
            item {
                UserSearchSection(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    isSearching = isSearching
                )
            }

            // Membri selezionati
            if(selectedMembers.isNotEmpty()){
                item {
                    SelectedMembersSection(
                        selectedMembers = selectedMembers.toList(),
                        onRemoveMember = { user ->
                            selectedMembers = selectedMembers.toMutableSet().apply { remove(user) }
                        }
                    )
                }
            }

            // Risultati ricerca
            if (searchQuery.isNotBlank() && searchResults.isNotEmpty()) {
                item {
                    Text(
                        text = "Search Results:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(searchResults.filter { user ->
                    !selectedMembers.contains(user) &&
                            user.userId != authViewModel.auth.currentUser?.uid
                }) { user ->
                    UserSearchResultItem(
                        user = user,
                        onAddUser = {
                            if (selectedMembers.size < 30) {
                                selectedMembers = selectedMembers.toMutableSet().apply { add(user) }
                            } else {
                                Toast.makeText(context, "Maximum 30 members allowed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                // Pulsante crea gruppo
                item {
                    CreateGroupButton(
                        groupName = groupName,
                        selectedMembers = selectedMembers.toList(),
                        onCreateGroup = { name, members ->
                            val currentUser = authViewModel.auth.currentUser
                            currentUser?.let { user ->
                                val memberIds = members.map { it.userId } + user.uid
                                val groupData = mapOf(
                                    "name" to name,
                                    "members" to memberIds,
                                    "matchesCreated" to 0,
                                    "creatorId" to user.uid
                                )
                                calcettoViewModel.createGroup(groupData)
                                Toast.makeText(context, "Group created successfully!", Toast.LENGTH_SHORT).show()
                                navController.navigate("mygroups")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun GroupNameSection(
    groupName: String,
    onGroupNameChange: (String) -> Unit
) {
    Column{
        Text(
            text = "Group Name",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = groupName,
            onValueChange = onGroupNameChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Enter group name") },
            singleLine = true
        )
    }
}

@Composable
fun UserSearchSection(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isSearching: Boolean
) {
    Column{
        Text(
            text = "Search Users:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search by username or email ") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search")
            },
            trailingIcon = {
                if(searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true
        )
        if(isSearching) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Searching...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SelectedMembersSection(
    selectedMembers: List<User>,
    onRemoveMember: (User) -> Unit
) {
    Column{
        Text(
            text = "Selected Members (${selectedMembers.size}/30):",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        selectedMembers.forEach { user ->
            SelectedMemberItem(
                user = user,
                onRemove = { onRemoveMember(user) }
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
fun SelectedMemberItem(
    user: User,
    onRemove: () -> Unit
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = user.name.ifEmpty { "Unknown User" },
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Remove,
                    contentDescription = "Remove member",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun UserSearchResultItem(
    user: User,
    onAddUser: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAddUser() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier

                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.DarkGray,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = user.name.ifEmpty { "Unknown User" },
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onAddUser) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add to group",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun CreateGroupButton(
    groupName: String,
    selectedMembers: List<User>,
    onCreateGroup: (String, List<User>) -> Unit
) {
    Column {
        // Informazioni sui requisiti
        Text(
            text = "Requirements:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "- Group name required\n - Minimum 2 members (including you)\n - Maximum 30 members",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onCreateGroup(groupName, selectedMembers) },
            modifier = Modifier.fillMaxWidth(),
            enabled = groupName.isNotBlank() && selectedMembers.size >= 1,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "Create Group (${selectedMembers.size + 1} members)",
                style = MaterialTheme.typography.titleMedium
            )
        }

        if(selectedMembers.isEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Add at least 1 member to create the group",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}