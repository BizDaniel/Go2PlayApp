package com.example.go2playproject.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import com.example.go2playproject.AuthState
import com.example.go2playproject.AuthViewModel
import com.example.go2playproject.CalcettoViewModel
import com.example.go2playproject.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditGroupPage(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    authViewModel: AuthViewModel,
    calcettoViewModel: CalcettoViewModel,
) {
    val authState = authViewModel.authState.observeAsState()
    val context = LocalContext.current

    val groupToEdit by calcettoViewModel.groupToEdit.collectAsState()

    var groupName by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedMembers by remember { mutableStateOf(mutableSetOf<User>()) }
    var isLoading by remember { mutableStateOf(true) }

    val searchResults by calcettoViewModel.userSearchResults.collectAsState()
    val isSearching by calcettoViewModel.isSearchingUsers.collectAsState()

    val db = FirebaseFirestore.getInstance()
    val currentUserId = authViewModel.auth.currentUser?.uid

    LaunchedEffect(groupToEdit) {
        val group = groupToEdit
        if (group != null) {
            groupName = group.name
            if (group.members.isNotEmpty()) {
                try {
                    // Carica TUTTI i documenti degli utenti in una sola richiesta (molto efficiente)
                    val userDocuments = db.collection("users")
                        .whereIn("__name__", group.members) // Cerca per ID documento
                        .get()
                        .await() // .await() è la versione asincrona che non blocca la UI

                    val memberUsers = userDocuments.mapNotNull { doc ->
                        doc.toObject(User::class.java)?.copy(userId = doc.id)
                    }
                    selectedMembers = memberUsers.toMutableSet()
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to load members: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } else {
                selectedMembers = mutableSetOf()
            }
            isLoading = false
        }
    }

    // Aggiorna risultati ricerca quando cambia la query
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            calcettoViewModel.searchUsers(searchQuery)
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
                title = { Text(text = "Edit Group") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate("mygroups") }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            BottomNavigationBar(navController, currentRoute = "editgroup")
        }
    ) { paddingValues ->
        if (isLoading || groupToEdit == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Nome gruppo
                item {
                    GroupNameSection(
                        groupName = groupName,
                        onGroupNameChange = { groupName = it }
                    )
                }

                // Ricerca utenti
                item {
                    UserSearchSection(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        isSearching = isSearching
                    )
                }

                // Membri selezionati
                if (selectedMembers.isNotEmpty()) {
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

                    item {
                        EditGroupButton(
                            groupName = groupName,
                            selectedMembers = selectedMembers.toList(),
                            onUpdateGroup = {
                                val currentUser = authViewModel.auth.currentUser
                                val group = groupToEdit
                                if (group != null && currentUser != null) {
                                    val memberIds = selectedMembers.map { it.userId }
                                    calcettoViewModel.updateGroup(
                                        groupId = group.groupId,
                                        groupName = groupName,
                                        newMembers = memberIds,
                                        currentUserId = currentUser.uid,
                                    )
                                }
                                calcettoViewModel.clearGroupToEdit()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EditGroupButton(
    groupName: String,
    selectedMembers: List<User>,
    onUpdateGroup: () -> Unit
) {
    Column {
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
            onClick = { onUpdateGroup() },
            modifier = Modifier.fillMaxWidth(),
            enabled = groupName.isNotBlank() && selectedMembers.size >= 1,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "Update Group (${selectedMembers.size + 1} members)",
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (selectedMembers.isEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Add at least 1 member to update the group",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}