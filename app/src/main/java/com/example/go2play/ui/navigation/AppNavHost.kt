package com.example.go2play.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
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
import com.example.go2play.ui.event.OrganizeEventScreen
import com.example.go2play.ui.events.MyEventsScreen
import com.example.go2play.ui.explore.ExploreScreen
import com.example.go2play.ui.findmatch.FindMatchScreen
import com.example.go2play.ui.finduser.FindUsersScreen
import com.example.go2play.ui.groups.CreateGroupScreen
import com.example.go2play.ui.groups.GroupDetailScreen
import com.example.go2play.ui.groups.MyGroupsScreen
import com.example.go2play.ui.home.HomeScreen
import com.example.go2play.ui.notifications.NotificationScreen
import com.example.go2play.ui.profile.EditProfileScreen
import com.example.go2play.ui.profile.ProfileScreen
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object Main : Screen("main")

    object EditProfile : Screen("edit_profile")
    object CreateGroup : Screen("create_group")
    object MyGroups : Screen("my_groups")
    object DetailGroup : Screen("detail_group")
    object OrganizeEvent : Screen("organize_event")
    object Notifications : Screen("notifications")
    object MyEvents : Screen("my_events")
    object FindMatch : Screen("find_match")
    object FindUsers : Screen("find_users")
}

@Composable
fun AppNavHost(navController: NavHostController) {
    // Crea un'istanza condivisa di AuthViewModel
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()

    // Mostra loading durante controllo sessione
    if(authState.isCheckingSession) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    // Determina la destinazione di start in base allo stato di autenticazione
    val startDestination = if (authState.isAuthenticated) {
        Screen.Main.route
    } else {
        Screen.Login.route
    }

    // se si disconnette → torna al login
    LaunchedEffect(authState.isAuthenticated) {
        if (!authState.isAuthenticated) {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) },
                onLoginSuccess = {
                    navController.navigate(Screen.Main.route) {
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
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.SignUp.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Main.route) {
            MainScreen(navController = navController, authViewModel = authViewModel)
        }

        composable(Screen.EditProfile.route) {
            SimpleScaffold(navController, Screen.EditProfile.route) {
                EditProfileScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        composable(Screen.CreateGroup.route) {
            SimpleScaffold(navController, Screen.CreateGroup.route) {
                CreateGroupScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onGroupCreated = {
                        navController.popBackStack()
                    }
                )
            }
        }

        composable(Screen.MyGroups.route) {
            SimpleScaffold(navController, Screen.MyGroups.route) {
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

            SimpleScaffold(navController, Screen.DetailGroup.route) {
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

        composable(
            route = Screen.OrganizeEvent.route + "/{fieldId}",
            arguments = listOf(navArgument("fieldId") { type = NavType.StringType })
        ) { backStackEntry ->

            val fieldId = backStackEntry.arguments?.getString("fieldId")

            SimpleScaffold(navController, "organize_event") {
                if(fieldId != null) {
                    OrganizeEventScreen(
                        fieldId = fieldId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                } else {
                    navController.popBackStack()
                }
            }
        }

        composable(Screen.Notifications.route) {
            SimpleScaffold(navController, Screen.Notifications.route) {
                NotificationScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        composable(Screen.MyEvents.route) {
            SimpleScaffold(navController, Screen.MyEvents.route) {
                MyEventsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        composable(Screen.FindMatch.route) {
            SimpleScaffold(navController, Screen.FindMatch.route) {
                FindMatchScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        composable(Screen.FindUsers.route) {
            SimpleScaffold(navController, Screen.FindUsers.route) {
                FindUsersScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun MainScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel
) {
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                selectedRoute = when (pagerState.currentPage) {
                    0 -> "explore"
                    1 -> "home"
                    2 -> "profile"
                    else -> "home"
                },
                onNavigate = { route ->
                    coroutineScope.launch {
                        val page = when (route) {
                            "explore" -> 0
                            "home" -> 1
                            "profile" -> 2
                            else -> 1
                        }
                        pagerState.animateScrollToPage(page)
                    }
                }
            )
        }
    ) { padding ->

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) { page ->

            when (page) {
                0 -> ExploreScreen(
                    onFieldClick = { field ->
                        navController.navigate(Screen.OrganizeEvent.route + "/${field.id}")
                    }
                )

                1 -> HomeScreen(
                    onNavigateToCreateGroup = {
                        navController.navigate(Screen.CreateGroup.route)
                    },
                    onNavigateToMyGroups = {
                        navController.navigate(Screen.MyGroups.route)
                    },
                    onNavigateToNotifications = {
                        navController.navigate(Screen.Notifications.route)
                    },
                    onNavigateToMyEvents = { navController.navigate(Screen.MyEvents.route)},
                    onNavigateToFindMatch = { navController.navigate(Screen.FindMatch.route)},
                    onNavigateToFindUsers = { navController.navigate(Screen.FindUsers.route)}
                )

                2 -> ProfileScreen(
                    onNavigateToEdit = {
                        navController.navigate(Screen.EditProfile.route)
                    },
                    onLogout = {
                        authViewModel.signOut()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SimpleScaffold(
    navController: NavHostController,
    currentRoute: String,
    content: @Composable () -> Unit
) {
    Scaffold { padding ->
        Box(modifier = Modifier.padding(padding)) {
            content()
        }
    }
}