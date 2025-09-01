package com.example.go2playproject

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// Struttura ViewModel per gestire l'autenticazione per l'app usando FIrebase
// Authentication

/*
* Questa è la classe centrale per tutta la logica di autenticazione, usa
* istanza FirebaseAuth della classe FirebaseAuth. Si tratta dell'oggetto chiave
* che ci permette di fare azioni come sign in, sign out e sign out.
* */

class AuthViewModel : ViewModel() {

    /*
    * Ottengo una istanza di FirebaseAuth, classe di Firebase per
    * registrare, loggare e gestire gli utenti
    * */
    internal val auth : FirebaseAuth = FirebaseAuth.getInstance()

    private val db = FirebaseFirestore.getInstance()

    /*
    * _authState è un LiveData mutabile, usata per aggiornare internamente
    * lo stato che in questo caso è il login riuscito, fallito, ecc...
    * Quindi serve per capire in che stato del processo di autenticazione siamo
    * */
    private val _authState = MutableLiveData<AuthState>()
    /*
    * authState è la versione pubblica e sola lettura che userà la UI. Si tratta
    * della public-facing LiveData object. Altre parti della mia app, come la UI
    * osserveranno questo oggetto. Quando il valore in _authState cambia, gli osservatori
    * sono notificati automaticamente.
    * */
    val authState: LiveData<AuthState> = _authState

    /*
    * Questo blocco init viene chiamato quando AuthViewModel viene creato
    * */
    init {
        checkAuthStatus()
    }

    /*
    Funzione che controllo se un utente è registrato
     */
    fun checkAuthStatus() {
        if(auth.currentUser == null){
            _authState.value = AuthState.Unauthenticated
        } else {
            _authState.value = AuthState.Authenticated
        }
    }

    fun login(email: String, password: String) {

        if(email.isEmpty() || password.isEmpty()){
            _authState.value = AuthState.Error("Email and password required")
            return
        }

        _authState.value = AuthState.Loading

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if(task.isSuccessful){
                    _authState.value = AuthState.Authenticated
                } else {
                    _authState.value = AuthState.Error(task.exception?.message?:"Something went wrong")
                }
            }
    }

    fun signup(email: String, password: String) {

        if(email.isEmpty() || password.isEmpty()){
            _authState.value = AuthState.Error("Email and password required")
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if(task.isSuccessful){
                    // Crea il documento utente
                    val user = auth.currentUser
                    user?.let {
                        createUserDocument(it.uid, email)
                    }
                    _authState.value = AuthState.Authenticated
                } else {
                    _authState.value = AuthState.Error(task.exception?.message?:"Something went wrong")
                }
            }
    }

    fun signout() {
        auth.signOut()
        _authState.value = AuthState.Unauthenticated
    }

    /**
     * Funzione: crea il documento utente in Firestore
     */
    private fun createUserDocument(userId: String, email: String) {
        val userData = mapOf(
            "name" to "", // L'utente potrà aggiornare il nome dal profilo
            "email" to email,
            "groupsId" to emptyList<String>(),
            "profileImageUrl" to null
        )

        db.collection("users").document(userId)
            .set(userData)
            .addOnSuccessListener {
                println("User document created successfully!")
            }
            .addOnFailureListener { e ->
                println("Error in creating the user document: $e")
                // Lo stato di auth non lo cambio perchè l'autenticazione
                // è comunque risucita
                // L'utente può comunque usare l'app, ma non verrà trovato nelle
                // ricerche
            }
    }

}

/*
* sealed class, una classe che rappresenta un numero finito di
* stati concreti
* */
sealed class AuthState{
    object Authenticated : AuthState()
    object Unauthenticated: AuthState()
    object Loading : AuthState()
    data class Error(val message : String) : AuthState()
    data class Success(val message: String) : AuthState()
}
