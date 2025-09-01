package com.example.go2playproject

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.go2playproject.screens.AvailableMatchesPage
import com.example.go2playproject.screens.CreateAGroupPage
import com.example.go2playproject.screens.CreateAMatchPage
import com.example.go2playproject.screens.EditGroupPage
import com.example.go2playproject.screens.EditMatchPage
import com.example.go2playproject.screens.EditProfilePage
import com.example.go2playproject.screens.ExplorePage
import com.example.go2playproject.screens.FieldSelectionPage
import com.example.go2playproject.screens.GroupSelectionPage
import com.example.go2playproject.screens.HomePage
import com.example.go2playproject.screens.LoginPage
import com.example.go2playproject.screens.MyEventsPage
import com.example.go2playproject.screens.MyGroupsPage
import com.example.go2playproject.screens.ProfilePage
import com.example.go2playproject.screens.SignUpPage

/*
* Componente fondamentale per gestire la navigazione dell'App.
* MyAppNavigation functionè un Composable, il che vuol dire che descrive una parte
* della user interface
* */

@Composable
fun MyAppNavigation(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel,
    calcettoViewModel: CalcettoViewModel,
    userViewModel: UserViewModel
) {
    // Questa riga crea e ricorda una istanza NavHostController
    val navController = rememberNavController()

    /*
    NavHost è un Composable che serve da container per il mio grafo di navigazione
     */
    NavHost(
        // Dico al NavHost quale NavHostController usare per gestire la navigazione
        navController = navController,
        startDestination = "login",
    ) {
        /*
        Ogni composable è una destinazione del mio grafo
         */
        composable(route = "login") {
            LoginPage(modifier, navController, authViewModel)
        }
        composable(route = "signup") {
            SignUpPage(modifier, navController, authViewModel)
        }
        composable(route = "homepage") {
            HomePage(modifier, navController, authViewModel, calcettoViewModel, userViewModel)
        }
        composable(route = "profilepage") {
            ProfilePage(modifier, navController, authViewModel, userViewModel)
        }
        composable(route = "explorepage") {
            ExplorePage(modifier, navController, authViewModel, calcettoViewModel)
        }
        composable(route = "createamatchpage") {
            CreateAMatchPage(modifier, navController, authViewModel, calcettoViewModel)
        }
        composable(route = "myevents") {
            MyEventsPage(modifier, navController, authViewModel, calcettoViewModel)
        }
        composable(route = "mygroups") {
            MyGroupsPage(modifier, navController, authViewModel, calcettoViewModel)
        }
        composable(route = "available") {
            AvailableMatchesPage(modifier, navController, authViewModel, calcettoViewModel)
        }
        composable(route = "creategroup") {
            CreateAGroupPage(modifier, navController, authViewModel, calcettoViewModel)
        }
        composable(route = "editprofile"){
            EditProfilePage(modifier, navController, authViewModel, userViewModel)
        }
        composable(route = "fieldselection"){
            FieldSelectionPage(navController, authViewModel, calcettoViewModel)
        }
        composable(route = "groupselection") {
            GroupSelectionPage(navController, authViewModel, calcettoViewModel)
        }
        composable(route = "editgroup") {
            EditGroupPage(modifier, navController, authViewModel, calcettoViewModel)
        }
        composable(route = "editmatch") {
            EditMatchPage(modifier, navController, authViewModel, calcettoViewModel)
        }
    }
}

