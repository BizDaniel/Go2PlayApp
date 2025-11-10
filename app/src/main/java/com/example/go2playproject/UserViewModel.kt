package com.example.go2playproject

import androidx.lifecycle.ViewModel
import com.example.go2playproject.model.User
import com.example.go2playproject.screens.ProfilePage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val USERNAME_REGEX = "^[a-zA-Z0-9_]+( [a-zA-Z0-9_]+)*\$"

class UserViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Stato per l'utente corrente
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    // Stato per il caricamento
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Stato per la validazione del nome utente
    private val _usernameValidation = MutableStateFlow<UsernameValidationState>(UsernameValidationState.Idle)
    val usernameValidation: StateFlow<UsernameValidationState> = _usernameValidation

    // Stato per gli aggiornamenti del profilo
    private val _profileUpdateState = MutableStateFlow<ProfileUpdateState>(ProfileUpdateState.Idle)
    val profileUpdateState: StateFlow<ProfileUpdateState> = _profileUpdateState


    init {
        loadCurrentUser()
    }

    /**
     * Funzione: Carica i dati dell'utente corrente
     */
    fun loadCurrentUser() {
        val currentUserId = auth.currentUser?.uid ?: return

        _isLoading.value = true

        db.collection("users").document(currentUserId)
            .get()
            .addOnSuccessListener { document ->
                if(document.exists()) {
                    val user = document.toObject(User::class.java)?.copy(userId = document.id)
                    _currentUser.value = user
                }
                _isLoading.value = false
            }
            .addOnFailureListener { e ->
                println("Error in loading the user data: $e")
                _isLoading.value = false
            }
    }

    /**
     * Controlla se il nome utente è disponibile
     */
    fun checkUsernameAvailability(username: String) {
        if(username.isBlank()) {
            _usernameValidation.value = UsernameValidationState.Idle
            return
        }

        val trimmedUsername = username.trim()

        if(trimmedUsername.length < 3) {
            _usernameValidation.value = UsernameValidationState.Invalid("The Username has to be at least of 3 characters")
            return
        }

        if(trimmedUsername.length > 30) {
            _usernameValidation.value = UsernameValidationState.Invalid("The Username cannot be up to 20 characters")
            return
        }

        if(!trimmedUsername.matches(Regex(USERNAME_REGEX))) {
            _usernameValidation.value = UsernameValidationState.Invalid("The Username can contain letters, numbers, underscore and single spaces")
            return
        }


        _usernameValidation.value = UsernameValidationState.Checking

        db.collection("users")
            .whereEqualTo("name", trimmedUsername)
            .get()
            .addOnSuccessListener { documents ->
                val currentUserId = auth.currentUser?.uid
                val isUsernameAlreadyTaken = documents.documents.any { doc ->
                    doc.id != currentUserId
                }

                _usernameValidation.value = if(isUsernameAlreadyTaken) {
                    UsernameValidationState.Invalid("Username is already taken")
                } else {
                    UsernameValidationState.Valid("Username available")
                }
            }
            .addOnFailureListener { e ->
                println("Error in checking the username: $e")
                _usernameValidation.value = UsernameValidationState.Invalid("Error in checking the username")
            }
    }

    /** Funzione:
     * Aggiorna il profilo dell'utente
     */
    fun updateUserProfile(name: String, profileImageUrl: String? = null) {
        val currentUserId = auth.currentUser?.uid ?: return

        val trimmedName = name.trim()

        if(trimmedName.isBlank()) {
            _profileUpdateState.value = ProfileUpdateState.Error("The username cannot be blank")
            return
        }

        _profileUpdateState.value = ProfileUpdateState.Loading

        // Prima verifica che il nome utente sia ancora disponibile
        db.collection("users")
            .whereEqualTo("name", trimmedName)
            .get()
            .addOnSuccessListener { documents ->
                val isUsernameAlreadyTaken = documents.documents.any { doc ->
                    doc.id != currentUserId
                }

                if(isUsernameAlreadyTaken) {
                    _profileUpdateState.value = ProfileUpdateState.Error("Username already taken")
                    return@addOnSuccessListener
                }

                // Procedo con l'aggiornamento
                val updates = mutableMapOf<String, Any>(
                    "name" to trimmedName
                )

                profileImageUrl?.let {
                    updates["profileImageUrl"] = it
                }

                db.collection("users").document(currentUserId)
                    .update(updates)
                    .addOnSuccessListener {
                        _profileUpdateState.value = ProfileUpdateState.Success("Profile updated with success")
                        loadCurrentUser() // Ricarica i dati aggiornati
                    }
                    .addOnFailureListener { e ->
                        println("Error in profile updating: $e")
                        _profileUpdateState.value = ProfileUpdateState.Error("Error in updating the profile")
                    }
            }
            .addOnFailureListener { e ->
                println("Error in the final checking of the username: $e")
                _profileUpdateState.value = ProfileUpdateState.Error("Error in the final checking of the username")
            }
    }

    /**
     * Reimposta lo stato di validazione del nome utente
     */
    fun resetUsernameValidation() {
        _usernameValidation.value = UsernameValidationState.Idle
    }

    /**
     * Reimposta lo stato di aggiornamento del profilo
     */
    fun resetProfileUpdateState() {
        _profileUpdateState.value = ProfileUpdateState.Idle
    }
}

/**
 * Stati possibili per la validazione del nome utente
 */
sealed class UsernameValidationState {
    object Idle : UsernameValidationState()
    object Checking : UsernameValidationState()
    data class Valid(val message: String) : UsernameValidationState()
    data class Invalid(val message: String) : UsernameValidationState()
}

/**
 * Stati possibili per aggiornamento profilo
 */
sealed class ProfileUpdateState {
    object Idle : ProfileUpdateState()
    object Loading : ProfileUpdateState()
    data class Success(val message: String) : ProfileUpdateState()
    data class Error(val message: String) : ProfileUpdateState()
}