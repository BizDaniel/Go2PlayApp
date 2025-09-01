package com.example.go2playproject.screens

import android.widget.Toast
import androidx.compose.animation.expandHorizontally
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawModifierNode
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.go2playproject.AuthState
import com.example.go2playproject.AuthViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import androidx.compose.foundation.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.res.painterResource
import com.example.go2playproject.CalcettoViewModel
import com.example.go2playproject.R
import com.example.go2playproject.model.Field


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorePage(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    authViewModel: AuthViewModel,
    calcettoViewModel: CalcettoViewModel
) {
    val authState = authViewModel.authState.observeAsState()
    val context = LocalContext.current

    val fields by calcettoViewModel.fields.collectAsState()

    var selectedTab by remember { mutableStateOf(TabExplore.Map) }

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
                title = { Text(text = "Trento, Italy") },
            )
        },
        bottomBar = {
            BottomNavigationBar(navController = navController, currentRoute = "explorepage")
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TabButtonScreen(
                    text = "Map",
                    isSelected = selectedTab == TabExplore.Map,
                    onClick = { selectedTab = TabExplore.Map }
                )
                TabButtonScreen(
                    text = "List",
                    isSelected = selectedTab == TabExplore.List,
                    onClick = { selectedTab = TabExplore.List }
                )
            }

            when (selectedTab) {
                TabExplore.Map -> MapScreen()
                TabExplore.List -> ListScreen(fields)
            }
        }
    }
}

enum class TabExplore {
    Map,
    List
}

@Composable
fun TabButtonScreen(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Text(text = text)
    }
}

@Composable
fun MapScreen() {
    // Inserisci qui la mappa. Per ora, mostriamo solo un testo.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Qui ci va la mappa", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun ListScreen(fields: List<Field>) {

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            FilterButton() // Pulsante filtro in cima alla lista
        }
        items(fields) { field ->
            FieldCard(fieldData = field)
        }
    }
}

@Composable
fun FilterButton() {
    Button(onClick = { /* TODO: Implementare la logica per il filtro */ },
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Text(text = "Filter")
    }
}


@Composable
fun FieldCard(fieldData: Field) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = fieldData.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(text = "Price: ${fieldData.pricePerHour}", fontSize = 14.sp)
            Text(text = "Location: ${fieldData.address}", fontSize = 14.sp)
            Button(onClick = { /* TODO: Implementare la logica per la visualizzazione della disponibilità */ },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(text = "View Availability")
            }
        }
    }
}



