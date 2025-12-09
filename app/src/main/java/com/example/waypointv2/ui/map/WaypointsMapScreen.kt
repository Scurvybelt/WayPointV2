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
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
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
    
    // Función para crear un icono personalizado minimalista para waypoints
    val waypointIcon = remember {
        try {
            createWaypointIcon()
        } catch (e: Exception) {
            android.util.Log.e("WaypointsMapScreen", "Error creating icon: ${e.message}", e)
            null // Si falla, usar el icono por defecto
        }
    }
    
    // Log para debug
    androidx.compose.runtime.LaunchedEffect(waypoints.size) {
        android.util.Log.d("WaypointsMapScreen", "Waypoints count: ${waypoints.size}")
        waypoints.forEach { wp ->
            android.util.Log.d("WaypointsMapScreen", "Waypoint: ${wp.title} at ${wp.latitude}, ${wp.longitude}")
        }
    }
    
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
                        compassEnabled = true,
                        myLocationButtonEnabled = false
                    )
                ) {
                    // Agregar marcadores para cada waypoint
                    waypoints.forEach { waypoint ->
                        val markerIcon = waypointIcon ?: createSimpleBlackIcon()
                        Marker(
                            state = MarkerState(
                                position = LatLng(waypoint.latitude, waypoint.longitude)
                            ),
                            title = waypoint.title.ifEmpty { "Waypoint" },
                            snippet = waypoint.locationName.ifEmpty { 
                                "${String.format("%.4f", waypoint.latitude)}, ${String.format("%.4f", waypoint.longitude)}" 
                            },
                            icon = markerIcon
                        )
                    }
                }
            }
        }
    }
}

/**
 * Crea un icono personalizado minimalista para los waypoints
 * Diseño: Círculo negro con un punto blanco en el centro (estilo waypoint)
 */
private fun createWaypointIcon(): BitmapDescriptor? {
    return try {
        val size = 100 // Tamaño del icono en píxeles (reducido para evitar problemas)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        
        if (bitmap == null) {
            android.util.Log.e("WaypointsMapScreen", "Failed to create bitmap")
            return null
        }
        
        val canvas = Canvas(bitmap)
        
        // Círculo exterior negro (borde)
        val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            style = Paint.Style.FILL
        }
        
        // Círculo interior blanco
        val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
        }
        
        // Punto central negro
        val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            style = Paint.Style.FILL
        }
        
        val centerX = size / 2f
        val centerY = size / 2f
        val radius = size / 2f
        
        // Dibujar círculo exterior negro (borde más grueso)
        canvas.drawCircle(centerX, centerY, radius - 6f, outerPaint)
        
        // Dibujar círculo interior blanco
        canvas.drawCircle(centerX, centerY, radius - 12f, innerPaint)
        
        // Dibujar punto central negro
        canvas.drawCircle(centerX, centerY, radius / 4f, centerPaint)
        
        BitmapDescriptorFactory.fromBitmap(bitmap)
    } catch (e: Exception) {
        android.util.Log.e("WaypointsMapScreen", "Error creating waypoint icon: ${e.message}", e)
        e.printStackTrace()
        null
    }
}

/**
 * Crea un icono simple negro como fallback
 */
private fun createSimpleBlackIcon(): BitmapDescriptor {
    return try {
        val size = 80
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            style = Paint.Style.FILL
        }
        
        val centerX = size / 2f
        val centerY = size / 2f
        
        // Dibujar círculo negro simple
        canvas.drawCircle(centerX, centerY, size / 2f - 4f, paint)
        
        BitmapDescriptorFactory.fromBitmap(bitmap)
    } catch (e: Exception) {
        // Si todo falla, usar el marcador por defecto en gris (más cercano a negro)
        BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
    }
}

