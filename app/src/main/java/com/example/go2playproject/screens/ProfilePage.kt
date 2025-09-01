package com.example.go2playproject.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.go2playproject.AuthState
import com.example.go2playproject.AuthViewModel
import com.example.go2playproject.UserViewModel

@Composable
fun ProfilePage(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    authViewModel: AuthViewModel,
    userViewModel: UserViewModel
) {
    val authState = authViewModel.authState.observeAsState()
    val context = LocalContext.current
    val isLoading by userViewModel.isLoading.collectAsStateWithLifecycle()
    val currentUser by userViewModel.currentUser.collectAsStateWithLifecycle()

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

    Surface(
        modifier = Modifier
            .fillMaxSize(),
        color = Color.White
    ) {
        if(isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                // Header con foto profilo e pulsante modifica
                Box {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(Color.LightGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile Picture",
                            tint = Color.DarkGray,
                            modifier = Modifier.size(64.dp)
                        )
                    }

                    // Pulsante modifica profilo
                    IconButton(
                        onClick = { navController.navigate("editprofile") },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                CircleShape
                            )
                            .size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Profile",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Nome Utente
                Text(
                    text = if (currentUser?.name?.isNotBlank() == true) {
                        currentUser!!.name
                    } else {
                        "Username not setted"
                    },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (currentUser?.name?.isNotBlank() == true) {
                        Color.Black
                    } else {
                        Color.Gray
                    }
                )

                // Email
                Text(
                    text = currentUser?.email ?: "",
                    fontSize = 16.sp,
                    color = Color.Gray
                )

                // Messaggio se il nome utente non è impostato
                if(currentUser?.name?.isBlank() == true) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "⚠\uFE0F Set an username to be find by the other users",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Some statistics",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Example Review:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                val reliabilityRating = 4
                val punctualityRating = 3
                val sportsmanshipRating = 5


                ReviewItem(label = "Reliability", reliabilityRating)
                ReviewItem(label = "Punctuality", punctualityRating)
                ReviewItem(label = "Sportsmanship", sportsmanshipRating)

                Spacer(modifier = Modifier.height(16.dp))

                val averageRating = (reliabilityRating + punctualityRating + sportsmanshipRating) / 3.0
                Text(
                    text = String.format("%.1f", averageRating),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary // Puoi personalizzare il colore
                )

                Spacer(modifier = Modifier.height(32.dp))

                TextButton(
                    onClick = {
                        authViewModel.signout()
                    }
                ) {
                    Text(text = "Sign Out")
                }

                Spacer(modifier = Modifier.weight(1f))

                BottomNavigationBar(navController = navController, currentRoute = "profilepage")
            }
        }
    }
}

@Composable
fun ReviewItem(label: String, rating: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 4.dp)
    ) {
        Text(text = label, fontSize = 14.sp, color = Color.Black.copy(alpha = 0.5f))
        RatingBar(rating = rating)
    }
}

@Composable
fun RatingBar(rating: Int) {
    Row {
        for (i in 1..5) {
            Icon(
                imageVector = if (i <= rating) Icons.Filled.Circle else Icons.Outlined.Circle,
                contentDescription = null, // Decorative element
                tint = if (i <= rating) Color.Green else Color.Gray,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

