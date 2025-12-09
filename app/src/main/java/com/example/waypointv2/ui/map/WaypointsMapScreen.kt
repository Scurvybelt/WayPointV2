package com.example.waypointv2.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.waypointv2.ui.home.HomeViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaypointsMapScreen(
    onBackClick: () -> Unit,
    homeViewModel: HomeViewModel = viewModel()
) {
    val waypoints by homeViewModel.waypoints.collectAsState()
    
    // Calcular posición inicial del mapa (centro de todos los waypoints o ubicación por defecto)
    val defaultLocation = LatLng(37.4220, -122.0840) // Google HQ por defecto
    
    // Calcular la posición inicial de la cámara
    val initialCameraPosition = remember(waypoints) {
        val target = if (waypoints.isNotEmpty()) {
            val avgLat = waypoints.map { it.latitude }.average()
            val avgLng = waypoints.map { it.longitude }.average()
            LatLng(avgLat, avgLng)
        } else {
            defaultLocation
        }
        val zoom = if (waypoints.size == 1) 15f else 10f
        
        CameraPosition.Builder()
            .target(target)
            .zoom(zoom)
            .build()
    }
    
    val cameraPositionState = rememberCameraPositionState()
    
    // Inicializar y actualizar la posición de la cámara cuando cambien los waypoints
    androidx.compose.runtime.LaunchedEffect(waypoints.size) {
        // Esperar un poco para que el mapa se inicialice completamente
        delay(500)
        
        try {
            val target = if (waypoints.isNotEmpty()) {
                val avgLat = waypoints.map { it.latitude }.average()
                val avgLng = waypoints.map { it.longitude }.average()
                LatLng(avgLat, avgLng)
            } else {
                defaultLocation
            }
            val zoom = if (waypoints.size == 1) 15f else 10f
            
            val newPosition = CameraPosition.Builder()
                .target(target)
                .zoom(zoom)
                .build()
            
            val cameraUpdate = CameraUpdateFactory.newCameraPosition(newPosition)
            
            // Intentar mover la cámara de forma segura
            try {
                cameraPositionState.move(cameraUpdate)
            } catch (e: IllegalStateException) {
                // Si el mapa aún no está listo, intentar de nuevo después de un delay
                delay(500)
                try {
                    cameraPositionState.move(cameraUpdate)
                } catch (e2: Exception) {
                    // Si aún falla, ignorar silenciosamente
                }
            }
        } catch (e: Exception) {
            // Ignorar errores silenciosamente
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Mapa de Waypoints",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Light,
                            fontSize = 20.sp,
                            color = Color.Black
                        )
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black
                )
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (waypoints.isEmpty()) {
                // Mensaje cuando no hay waypoints
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay waypoints para mostrar",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = Color.Gray,
                            fontWeight = FontWeight.Normal
                        )
                    )
                }
            } else {
                // Mapa con marcadores
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        mapType = MapType.NORMAL
                    ),
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = true,
                        compassEnabled = true
                    )
                ) {
                    // Agregar marcadores para cada waypoint
                    waypoints.forEach { waypoint ->
                        Marker(
                            state = MarkerState(
                                position = LatLng(waypoint.latitude, waypoint.longitude)
                            ),
                            title = waypoint.title.ifEmpty { "Waypoint" },
                            snippet = waypoint.locationName.ifEmpty { 
                                "${waypoint.latitude}, ${waypoint.longitude}" 
                            }
                        )
                    }
                }
            }
        }
    }
}

