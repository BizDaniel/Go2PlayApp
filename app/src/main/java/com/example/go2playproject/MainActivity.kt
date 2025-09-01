package com.example.go2playproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.go2playproject.ui.theme.Go2PlayProjectTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val authViewModel : AuthViewModel by viewModels()
        val calcettoViewModel : CalcettoViewModel by viewModels()
        val userViewModel: UserViewModel by viewModels()

        //val testLoader = TestUserDataLoader()
        //testLoader.loadTestUsers()

        setContent {
            Go2PlayProjectTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MyAppNavigation(
                        modifier =  Modifier.padding(innerPadding),
                        authViewModel = authViewModel,
                        calcettoViewModel = calcettoViewModel,
                        userViewModel = userViewModel
                    )
                }
            }
        }
    }
}
