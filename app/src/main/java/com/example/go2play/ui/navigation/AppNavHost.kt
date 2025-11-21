package com.example.go2play.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.go2play.ui.auth.AuthViewModel
import com.example.go2play.ui.auth.LoginScreen
import com.example.go2play.ui.auth.SignUpScreen
import com.example.go2play.ui.explore.ExploreScreen
import com.example.go2play.ui.groups.CreateGroupScreen
import com.example.go2play.ui.groups.GroupDetailScreen
import com.example.go2play.ui.groups.MyGroupsScreen
import com.example.go2play.ui.home.HomeScreen
import com.example.go2play.ui.profile.EditProfileScreen
import com.example.go2play.ui.profile.ProfileScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object Home : Screen("home")
    object Explore : Screen("explore")
    object Profile : Screen("profile")
    object EditProfile : Screen("edit_profile")
    object CreateGroup : Screen("create_group")
    object MyGroups : Screen("my_groups")
    object DetailGroup : Screen("detail_group")
}

@Composable
fun AppNavHost(navController: NavHostController) {
    // Crea un'istanza condivisa di AuthViewModel
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()

    // Osserva lo stato di autenticazione e reindirizza al login se necessario
    LaunchedEffect(authState.isAuthenticated) {
        if (!authState.isAuthenticated &&
            navController.currentDestination?.route != Screen.Login.route &&
            navController.currentDestination?.route != Screen.SignUp.route) {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = Screen.Login.route) {
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) },
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.SignUp.route) {
            SignUpScreen(
                viewModel = authViewModel,
                onNavigateToLogin = { navController.popBackStack() },
                onSignUpSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.SignUp.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            MainScaffold(navController, Screen.Home.route) {
                HomeScreen(
                    onNavigateToCreateGroup = { navController.navigate(Screen.CreateGroup.route) },
                    onNavigateToMyGroups = { navController.navigate(Screen.MyGroups.route)}
                )
            }
        }

        composable(Screen.Explore.route) {
            MainScaffold(navController, Screen.Explore.route) {
                ExploreScreen(
                    onFieldClick = { field ->
                        // TODO: Navigare ai dettagli del campo
                    }
                )
            }
        }

        composable(Screen.Profile.route) {
            MainScaffold(navController, Screen.Profile.route) {
                ProfileScreen(
                    onNavigateToEdit = { navController.navigate(Screen.EditProfile.route) },
                    onLogout = {
                        authViewModel.signOut()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable(Screen.EditProfile.route) {
            MainScaffold(navController, Screen.EditProfile.route) {
                EditProfileScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        composable(Screen.CreateGroup.route) {
            MainScaffold(navController, Screen.CreateGroup.route) {
                CreateGroupScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onGroupCreated = {
                        navController.popBackStack()
                    }
                )
            }
        }

        composable(Screen.MyGroups.route) {
            MainScaffold(navController, Screen.MyGroups.route) {
                MyGroupsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onGroupClick = { group ->
                        navController.navigate(Screen.DetailGroup.route + "/${group.id}")
                    }
                )
            }
        }

        composable(
            route = Screen.DetailGroup.route + "/{groupId}",
            arguments = listOf(navArgument("groupId") {type = NavType.StringType})
        ) { backStackEntry ->

            val groupId = backStackEntry.arguments?.getString("groupId")

            MainScaffold(navController, Screen.DetailGroup.route) {
                if(groupId != null) {
                    GroupDetailScreen(
                        groupId = groupId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                } else {
                    navController.popBackStack()
                }
            }
        }
    }
}

@Composable
private fun MainScaffold(
    navController: NavHostController,
    currentRoute: String,
    content: @Composable () -> Unit
) {
    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                selectedRoute = currentRoute,
                onNavigate = { route ->
                    navController.navigate(route) {
                        // Evita di accumulare troppi backstack
                        popUpTo(Screen.Home.route) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { padding ->
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.padding(padding)
        ) {
            content()
        }
    }
}