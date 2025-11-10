package com.example.go2playproject

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import com.example.go2playproject.model.Field
import com.example.go2playproject.model.Group
import com.example.go2playproject.model.Match
import com.example.go2playproject.model.User
import com.example.go2playproject.screens.ExplorePage
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.internal.OpDescriptor
import java.util.Calendar
import java.util.Date

class CalcettoViewModel : ViewModel() {

    // Ottengo un'istanza di Firestore
    private val db = FirebaseFirestore.getInstance()

    // Stato per la lista dei campi disponibili
    private val _fields = MutableStateFlow<List<Field>>(emptyList())
    // Versione pubblica e sola lettura per la UI
    val fields: StateFlow<List<Field>> = _fields

    // Stato per la lista delle partite dell'utente corrente
    private val _userMatches = MutableStateFlow<List<Match>>(emptyList())
    val userMatches: StateFlow<List<Match>> = _userMatches

    private val _upcomingMatches = MutableStateFlow<List<Match>>(emptyList())
    val upcomingMatches: StateFlow<List<Match>> = _upcomingMatches

    private val _archivedMatches = MutableStateFlow<List<Match>>(emptyList())
    val archivedMatches: StateFlow<List<Match>> = _archivedMatches

    // Stato per la lista dei gruppi dell'utente corrente
    private val _userGroups = MutableStateFlow<List<Group>>(emptyList())
    val userGroups: StateFlow<List<Group>> = _userGroups

    // Stati per la ricerca utenti
    private val _userSearchResults = MutableStateFlow<List<User>>(emptyList())
    val userSearchResults: StateFlow<List<User>> = _userSearchResults

    private val _isSearchingUsers = MutableStateFlow(false)
    val isSearchingUsers: StateFlow<Boolean> = _isSearchingUsers

    // Stati per le partite disponibili
    private val _availableMatches = MutableStateFlow<List<Match>>(emptyList())
    val availableMatches: StateFlow<List<Match>> = _availableMatches

    private val _isLoadingMatches = MutableStateFlow(false)
    val isLoadingMatches: StateFlow<Boolean> = _isLoadingMatches

    private val _groupToEdit = MutableStateFlow<Group?>(null)
    val groupToEdit: StateFlow<Group?> = _groupToEdit.asStateFlow()

    private val _selectedMatchDetails = MutableStateFlow<MatchDetails?>(null)
    val selectedMatchDetails: StateFlow<MatchDetails?> = _selectedMatchDetails

    private val _matchToEdit = MutableStateFlow<Match?>(null)
    val matchToEdit: StateFlow<Match?> = _matchToEdit.asStateFlow()

    private val _fieldAvailability = MutableStateFlow<List<TimeSlotAvailability>>(emptyList())
    val fieldAvailability: StateFlow<List<TimeSlotAvailability>> = _fieldAvailability

    private val _isLoadingAvailability = MutableStateFlow(false)
    val isLoadingAvailability: StateFlow<Boolean> = _isLoadingAvailability

    init {
        // Avvia il caricamento dei dati all'avvio del ViewModel
        fetchFields()
        // Posso chiamare altre funzioni di recupero dati
    }

    data class MatchDetails(
        val match: Match,
        val field: Field?,
        val players: List<User>,
        val creator: User?
    )

    data class TimeSlotAvailability(
        val timeSlot: String,
        val isAvailable: Boolean,
        val matchId: String? = null,
        val isUserMatch: Boolean = false
    )

    /**
     * Recupero tutti i campi da Firestore in tempo reale
     * Usa un Listener per aggiornare automaticamente la lista
     */
    fun fetchFields() {
        db.collection("fields")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    println("Errore nel recupero dei campi: $e")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val fieldList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Field::class.java)?.copy(fieldId = doc.id)
                    }
                    _fields.value = fieldList
                }
            }
    }

    /**
     * Funzione che mi dice la data attuale
     */
    private fun getStartOfDay(date: Date = Date()): Date {
        return Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }

    private fun getEndOfDay(date: Date = Date()): Date {
        return Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND,59)
            set(Calendar.MILLISECOND, 999)
        }.time
    }

    /**
     * Funzione: separa le partita tra upcoming e archived
     */
    private fun separateMatches(matches: List<Match>) {
        val today = getStartOfDay()

        val upcoming = matches.filter { match ->
            !match.isCompleted && match.date.after(today)
        }.sortedBy { it.date }

        val archived = matches.filter { match ->
            match.isCompleted || match.date.before(today) || match.date.equals(today)
        }.sortedByDescending { it.date }

        _upcomingMatches.value = upcoming
        _archivedMatches.value = archived
    }

    /**
     * Funzione: Ottieni i dettagli di una partita
     */
    fun getMatchDetails(matchId: String) {
        val match = _userMatches.value.find { it.matchId == matchId } ?: return

        val field = _fields.value.find { it.fieldId == match.fieldId }

        val playerIds = match.players
        if (playerIds.isEmpty()) {
            _selectedMatchDetails.value = MatchDetails(
                match = match,
                field = field,
                players = emptyList(),
                creator = null
            )
            return
        }

        db.collection("users")
            .whereIn("__name__", playerIds)
            .get()
            .addOnSuccessListener { documents ->
                val players = documents.documents.mapNotNull { doc ->
                    doc.toObject(User::class.java)?.copy(userId = doc.id)
                }

                val creator = players.find { it.userId == match.creatorId }

                _selectedMatchDetails.value = MatchDetails(
                    match = match,
                    field = field,
                    players = players,
                    creator = creator
                )
            }
            .addOnFailureListener { e ->
                println("Errore nel recupero dei dettagli dei giocatori: $e")
                _selectedMatchDetails.value = MatchDetails(
                    match = match,
                    field = field,
                    players = emptyList(),
                    creator = null
                )
            }
    }

    /**
     * Funzione: Pulisce i dettagli della partita selezionata
     */
    fun clearSelectedMatchDetails() {
        _selectedMatchDetails.value = null
    }

    /**
     * Recupero le partite correnti di un User
     * Questa funzione dovrebbe essere chiamata dopo che
     * l'utente ha effettuato l'autenticazione
     */
    fun fetchUserMatches(userId: String) {
        db.collection("matches")
            .whereArrayContains("players", userId) // Filtra le partite a cui l'utente partecipa
            .addSnapshotListener { snapshot, e ->
                if(e != null) {
                    println("Errore nel recupero delle partite dell'utennte: $e")
                    return@addSnapshotListener
                }
                if(snapshot != null) {
                    val matchList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Match::class.java)?.copy(matchId = doc.id)
                    }
                    _userMatches.value = matchList

                    separateMatches(matchList)
                }
            }
    }

    /**
     * Recupero i gruppi a cui l'utente corrente appartiene
     */
    fun fetchUserGroups(userId: String) {
        db.collection("groups")
            .whereArrayContains("members", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    println("Errore nel recupero dei gruppi dell'utente: $e")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val groupList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Group::class.java)?.copy(groupId = doc.id)
                    }
                    _userGroups.value = groupList
                }
            }
    }

    /**
     * Funzione: cerca utenti per nome o email
     */
    fun searchUsers(query: String) {
        if (query.isBlank()) {
            _userSearchResults.value = emptyList()
            return
        }

        _isSearchingUsers.value = true

        // Ricerca per nome
        db.collection("users")
            .whereGreaterThanOrEqualTo("name", query)
            .whereLessThanOrEqualTo("name", query + '\uf8ff')
            .get()
            .addOnSuccessListener { nameResults ->
                // Riecrca per email (case-sensitive)
                db.collection("users")
                    .whereGreaterThanOrEqualTo("email", query.lowercase())
                    .whereLessThanOrEqualTo("email", query.lowercase() + '\uf8ff')
                    .get()
                    .addOnSuccessListener { emailResults ->
                        val nameUsers = nameResults.documents.mapNotNull { doc ->
                            doc.toObject(User::class.java)?.copy(userId = doc.id)
                        }

                        val emailUsers = emailResults.documents.mapNotNull { doc ->
                            doc.toObject(User::class.java)?.copy(userId = doc.id)
                        }

                        // Combina i risultati e rimuove i duplicati
                        val allUsers = (nameUsers + emailUsers).distinctBy { it.userId }

                        // Filtra ulteriormente i risultati per query parziali
                        val filteredUsers = allUsers.filter { user ->
                            user.name.contains(query, ignoreCase = true) ||
                            user.email.contains(query, ignoreCase = true)
                        }
                        _userSearchResults.value = filteredUsers
                        _isSearchingUsers.value = false
                    }
                    .addOnFailureListener { e ->
                        println("Error in the search by email: $e")
                        _isSearchingUsers.value = false
                    }
            }
            .addOnFailureListener { e ->
                println("Error in search by name: $e")
                _isSearchingUsers.value = false
            }
    }

    /**
     * Funzione: Crea un nuovo utente (da chiamare dopo la registrazione)
     */
    fun createUser(userId: String, name: String, email: String) {
        val userData = mapOf(
            "name" to name,
            "email" to email,
            "groupsId" to emptyList<String>()
        )

        db.collection("users").document(userId)
            .set(userData)
            .addOnSuccessListener {
                println("Created user successfully!")
            }
            .addOnFailureListener { e ->
                println("Error in creation of the user: $e")
            }
    }


    fun createMatch(matchData: Map<String, Any>) {
        db.collection("matches")
            .add(matchData)
            .addOnSuccessListener {
                println("Match created successfully!")
            }
            .addOnFailureListener { e ->
                println("Error in creating the match: $e")
            }
    }

    fun createGroup(groupData: Map<String, Any>) {
        db.collection("groups")
            .add(groupData)
            .addOnSuccessListener { documentReference ->
                println("Group created successfully with ID: ${documentReference.id}")

                // Aggiorno il campo groupsId di tutti i membri
                val members = groupData["members"] as? List<String> ?: emptyList()
                updateUsersGroupsList(members, documentReference.id)
            }
            .addOnFailureListener { e ->
                println("Error in group creation: $e")
            }
    }

    /**
     * Funzione: funzione per impostare il gruppo da modificare
     */
    fun setGroupToEdit(group: Group) {
        _groupToEdit.value = group
    }

    /**
     * Funzione: per ripulire lo stato
     */
    fun clearGroupToEdit(){
        _groupToEdit.value = null
    }

    /**
     * Funzione: per modificare un gruppo
     */
    fun updateGroup(
        groupId: String,
        groupName: String,
        newMembers: List<String>,
        currentUserId: String
    ) {
        val groupRef = db.collection("groups").document(groupId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(groupRef)
            val group = snapshot.toObject(Group::class.java)

            if (group == null) {
                throw Exception("Group not found")
            }

            // Controllo se il currentUSer è il creatore
            if(group.creatorId != currentUserId) {
                throw Exception("Only the group creator can edit it.")
            }

            val oldMembers = group.members
            val updatedMembers = newMembers

            // Identifico i membri da aggiungere e rimuovere
            val membersToAdd = newMembers.filter { it !in oldMembers }
            val membersToRemove = oldMembers.filter { it !in newMembers }

            // Aggiorna il documento del gruppo
            transaction.update(groupRef, "name", groupName)
            transaction.update(groupRef, "members", updatedMembers)

            // Aggiorna la lista dei gruppi di ogni user
            for (userId in membersToAdd) {
                db.collection("users").document(userId)
                    .update("groupsId", com.google.firebase.firestore.FieldValue.arrayUnion(groupId))
            }

            for (userId in membersToRemove) {
                db.collection("users").document(userId)
                    .update("groupsId", com.google.firebase.firestore.FieldValue.arrayRemove(groupId))
            }
        }.addOnSuccessListener {
            println("Group updated successfully!")
        }.addOnFailureListener { e ->
            println("Error in updating the group: $e")
        }
    }

    /**
     * Funzione: Aggiorna la lista dei gruppi per tutti gli utenti membri
     */
    private fun updateUsersGroupsList(usersIds: List<String>, groupId: String) {
        usersIds.forEach { userId ->
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    val user = document.toObject(User::class.java)
                    if (user != null) {
                        val updatedGroupsList = user.groupsId.toMutableList()
                        if(!updatedGroupsList.contains(groupId)) {
                            updatedGroupsList.add(groupId)

                            db.collection("users").document(userId)
                                .update("groupsId", updatedGroupsList)
                                .addOnSuccessListener {
                                    println("Updated group list for user: $userId")
                                }
                                .addOnFailureListener { e ->
                                    println("Error in updating the group list for user: $userId, $e")
                                }
                        }
                    }
                }
        }
    }

    /**
    Funzione: Cambia una partita da privata a pubblica,
     Può essere chiamata solo dal creatore della partita
     */
    fun switchMatchToPublic(matchId: String, currentUserID: String) {
        val matchRef = db.collection("matches").document(matchId)

        matchRef.get().addOnSuccessListener { document ->
            val match = document.toObject(Match::class.java)

            if(match == null) {
                println("Match not found")
                return@addOnSuccessListener
            }

            // Verifico che l'utente corrente sia il creatore
            if(match?.creatorId == currentUserID) {
                val updates = mapOf(
                    "ispublic" to true,
                    "groupId" to null // Rimuovi associazione gruppo
                )

                matchRef.update(updates)
                    .addOnSuccessListener {
                        println("Match successfully made public!")
                    }
                    .addOnFailureListener { e ->
                        println("Error in made public the match: $e")
                    }
            } else {
                println("Only the creator can make the match public")
            }
        }

    }

    /**
     * Funzione: Recupera tutte le partite disponibili per l'utente
     * Include:
     * - Partite pubbliche a cui l'utente non partecipa ancora
     * - Partite private dei gruppi di cui l'utente fa parte
     * - Solo partite future
     */
    fun fetchAvailableMatches(userId: String) {
        _isLoadingMatches.value = true

        // Prima recupera i gruppi dell'utente
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { userDoc ->
                val userGroups = userDoc.get("groupsId") as? List<String> ?: emptyList()

                // Data di oggi
                val today = getStartOfDay()

                // Data tra 30 giorni
                val thirtyDaysFromNow = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_MONTH, 30)
                }.time

                // Query per partite pubbliche
                db.collection("matches")
                    .whereEqualTo("ispublic", true)
                    .whereGreaterThanOrEqualTo("date", today)
                    .whereLessThanOrEqualTo("date", thirtyDaysFromNow)
                    .get()
                    .addOnSuccessListener { publicSnapshot ->
                        val publicMatches = publicSnapshot.documents.mapNotNull { doc ->
                            doc.toObject(Match::class.java)?.copy(matchId = doc.id)
                        }.filter { match ->
                            // Filtra solo le partite a cui l'utente non partecipa
                            !match.players.contains(userId) &&
                            // Filtra solo partite non piene
                            match.players.size < match.maxPlayers
                        }

                        // Se l'utente ha gruppi, recupera anche le partite private
                        if(userGroups.isNotEmpty()) {
                            db.collection("matches")
                                .whereEqualTo("ispublic", false)
                                .whereIn("groupId", userGroups)
                                .whereGreaterThanOrEqualTo("date", today)
                                .whereLessThanOrEqualTo("date", thirtyDaysFromNow)
                                .get()
                                .addOnSuccessListener { privateSnapshot ->
                                    val privateMatches = privateSnapshot.documents.mapNotNull { doc ->
                                        doc.toObject(Match::class.java)?.copy(matchId = doc.id)
                                    }.filter { match ->
                                        // Filtra solo partite a cui l'utente non partecipa
                                        !match.players.contains(userId) &&
                                        match.players.size < match.maxPlayers
                                    }

                                    // Combina e ordina per data
                                    val allMatches = (publicMatches + privateMatches)
                                        .sortedBy { it.date }

                                    _availableMatches.value = allMatches
                                    _isLoadingMatches.value = false
                                }
                                .addOnFailureListener { e ->
                                    println("Error in fetching the private matches: $e")
                                    // Mostra almeno le partite pubbliche
                                    _availableMatches.value = publicMatches.sortedBy { it.date }
                                    _isLoadingMatches.value = false
                                }
                        } else {
                            // Se non ha gruppi, mostra solo partite pubbliche
                            _availableMatches.value = publicMatches.sortedBy { it.date }
                            _isLoadingMatches.value = false
                        }
                    }
                    .addOnFailureListener { e ->
                        println("Error in fetching the public matches: $e")
                        _availableMatches.value = emptyList()
                        _isLoadingMatches.value = false
                    }
            }
            .addOnFailureListener { e ->
                println("Error in fetching the user data: $e")
                _isLoadingMatches.value = false
            }
    }

    /**
     * Funzione: Permette all'utente di unirsi a una partita
     */
    fun joinMatch(matchId: String, userId: String) {
        val matchRef = db.collection("matches").document(matchId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(matchRef)
            val match = snapshot.toObject(Match::class.java)

            if(match != null) {
                // Verifica che ci sia ancora posto
                if(match.players.size < match.maxPlayers) {
                    // Verifica che l'utente non sia già iscritto all'evento
                    if(!match.players.contains(userId)) {
                        val updatedPlayers = match.players.toMutableList()
                        updatedPlayers.add(userId)
                        transaction.update(matchRef, "players", updatedPlayers)
                    } else {
                        throw Exception("You are already joined to this event")
                    }
                } else {
                    throw Exception("The event is full")
                }
            } else {
                throw Exception("Event not found")
            }
        }.addOnSuccessListener {
            println("You joined with success!")
            fetchAvailableMatches(userId)
            fetchUserMatches(userId)
        }.addOnFailureListener { e ->
            println("Error in joining to the event: ${e.message}")
        }
    }

    /**
     * Lascia una partita (rimuove l'utente dalla lista dei giocatori)
     */
    fun leaveMatch(matchId: String, userId: String) {
        val matchRef = db.collection("matches").document(matchId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(matchRef)
            val match = snapshot.toObject(Match::class.java)

            if(match != null) {
                if(match.players.contains(userId)) {
                    val updatedPlayers = match.players.toMutableList()
                    updatedPlayers.remove(userId)
                    transaction.update(matchRef, "players", updatedPlayers)
                } else {
                    throw Exception("You are not subscribed to this match")
                }
            } else {
                throw Exception("Match not found")
            }
        }.addOnSuccessListener {
            println("Match left with success!")
            fetchAvailableMatches(userId)
            fetchUserMatches(userId)
        }.addOnFailureListener { e ->
            println("Error in leaving the match: ${e.message}")
        }
    }

    /**
     * Funzione: imposta la partita da modificare
     */
    fun setMatchToEdit(match: Match) {
        _matchToEdit.value = match
    }

    /**
     * Funzione: pulisce lo stato _matchToEdit
     */
    fun clearMatchToEdit() {
        _matchToEdit.value = null
    }

    /**
     * Funzione: logica di aggiornamento su Firestore
     */
    fun updateMatch(
        matchId: String,
        newDescription: String,
        newLevel: String,
        newFieldId: String,
        newDate: Date,
        newTimeSlot: String,
        newMaxPlayers: Int,
        isPublic: Boolean,
        currentUserId: String
    ) {
        val matchRef = db.collection("matches").document(matchId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(matchRef)
            val match = snapshot.toObject(Match::class.java)

            if (match == null) {
                throw Exception("Match not found")
            }

            // Controllo se utente è il creatore
            if (match.creatorId != currentUserId) {
                throw Exception("Only the creator can edit this match")
            }

            // Aggiorna le proprietà della partita
            val updates = mutableMapOf<String, Any>(
                "description" to newDescription,
                "level" to newLevel,
                "fieldId" to newFieldId,
                "date" to newDate,
                "timeSlot" to newTimeSlot,
                "maxPlayers" to newMaxPlayers
            )

            // Se la partita diventa pubblica, rimuovo il gruppo
            if(isPublic && !match.ispublic) {
                updates["ispublic"] = true
                updates["groupId"] = FieldValue.delete()
            }

            // applico gli aggiornamenti
            transaction.update(matchRef, updates)
        }.addOnSuccessListener {
            println("Match updated successfully!")
            // Potremmo voler ricaricare le partite dell'utente
            fetchUserMatches(currentUserId)
        }.addOnFailureListener { e ->
            println("Error in updating the match: $e")
        }
    }

    /**
     * Funzione: Recupera la disponibilità di un campo per una data specifica
     */
    fun fetchFieldAvailability(fieldId: String, date: Date) {
        _isLoadingAvailability.value = true

        val startOfDay = getStartOfDay()

        val endOfDay = getEndOfDay()

        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid

        // Genera tutti gli slot disponibili
        val allSlots = generateTimeSlots()

        // Recupera le partite prenotate per quel campo in quella data
        db.collection("matches")
            .whereEqualTo("fieldId", fieldId)
            .whereGreaterThanOrEqualTo("date", startOfDay)
            .whereLessThanOrEqualTo("date", endOfDay)
            .get()
            .addOnSuccessListener { documents ->
                val bookedSlots = documents.mapNotNull { doc ->
                    val match = doc.toObject(Match::class.java).copy(matchId = doc.id)
                    match.timeSlot
                }

                val bookedMatches = documents.associate { doc ->
                    val match = doc.toObject(Match::class.java).copy(matchId = doc.id)
                    match.timeSlot to match
                }

                // Crea la lista di disponibilità
                val availabilityList = allSlots.map { slot ->
                    val match = bookedMatches[slot]
                    TimeSlotAvailability(
                        timeSlot = slot,
                        isAvailable = !bookedSlots.contains(slot),
                        matchId = match?.matchId,
                        isUserMatch = match?.players?.contains(currentUserId) == true
                    )
                }

                _fieldAvailability.value = availabilityList
                _isLoadingAvailability.value = false
            }
            .addOnFailureListener { e ->
                println("Error fetching field availability: $e")
                // In caso di errore, mostra tutti gli slot come disponibili
                _fieldAvailability.value = allSlots.map { slot ->
                    TimeSlotAvailability(
                        timeSlot = slot,
                        isAvailable = true,
                        matchId = null,
                        isUserMatch = false
                    )
                }
                _isLoadingAvailability.value = false
            }
    }

    /**
     * Funzione: Genera gli slot orari disponibili per un campo
     * Da 9:00 alle 22:30 con slot da 1.5 ore
     */
    @SuppressLint("DefaultLocale")
    private fun generateTimeSlots(): List<String> {
        val slots = mutableListOf<String>()
        val calendar = Calendar.getInstance()

        // Start from 9:00 AM
        calendar.set(Calendar.HOUR_OF_DAY, 9)
        calendar.set(Calendar.MINUTE, 0)

        // Generate slots until 9:00 PM (21:00)
        while (calendar.get(Calendar.HOUR_OF_DAY) < 21) {
            val startTime = String.format("%02d:%02d",
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE))

            calendar.add(Calendar.MINUTE, 90) // Add 1.5 hours

            val endTime = String.format("%02d:%02d",
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE))

            slots.add("$startTime - $endTime")
        }

        return slots
    }

    /**
     * Funzione: Verifica se uno slot è disponibile prima di creare una partita
     */
    fun checkSlotAvailability(
        fieldId: String,
        date: Date,
        timeSlot: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val startOfDay = getStartOfDay()

        val endOfDay = getEndOfDay()

        db.collection("matches")
            .whereEqualTo("fieldId", fieldId)
            .whereGreaterThanOrEqualTo("date", startOfDay)
            .whereLessThanOrEqualTo("date", endOfDay)
            .whereEqualTo("timeSlot", timeSlot)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    onResult(true, null) // Disponibile
                } else {
                    onResult(false, "This time slot is already booked")
                }
            }
            .addOnFailureListener { e ->
                onResult(false, "Error checking availability: ${e.message}")
            }
    }

    /**
     * Funzione: Pulisce lo stato della disponibilità
     */
    fun clearFieldAvailability() {
        _fieldAvailability.value = emptyList()
    }
}

