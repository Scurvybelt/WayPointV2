package com.example.waypointv2.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaypointDetailScreen(
    waypoint: Waypoint,
    onBackClick: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val mediaPlayer = remember { 
        android.media.MediaPlayer().apply {
            setOnCompletionListener {
                isPlaying = false
            }
        }
    }
    
    DisposableEffect(waypoint.audioUrl) {
        onDispose {
            mediaPlayer.release()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Detalles",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
                .verticalScroll(rememberScrollState())
        ) {
            // Imagen del waypoint
            AsyncImage(
                model = waypoint.photoUrl,
                contentDescription = "Foto del waypoint",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .clip(RoundedCornerShape(0.dp)),
                contentScale = ContentScale.Crop
            )
            
            // Información del waypoint
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Título de la nota
                if (waypoint.title.isNotEmpty()) {
                    Text(
                        text = waypoint.title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 28.sp,
                            color = Color.Black
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Ubicación
                Text(
                    text = "Ubicación",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = waypoint.locationName.ifEmpty { "Ubicación desconocida" },
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Coordenadas
                Text(
                    text = "Coordenadas",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${String.format("%.6f", waypoint.latitude)}, ${String.format("%.6f", waypoint.longitude)}",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Fecha
                waypoint.timestamp?.let { date ->
                    Text(
                        text = "Fecha",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val formatter = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("es", "ES"))
                    Text(
                        text = formatter.format(date),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                    )
                }
                
                // Botón de reproducir audio
                if (waypoint.audioUrl.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Button(
                        onClick = {
                            if (isPlaying) {
                                mediaPlayer.pause()
                                isPlaying = false
                            } else {
                                try {
                                    if (!mediaPlayer.isPlaying) {
                                        mediaPlayer.reset()
                                        mediaPlayer.setDataSource(waypoint.audioUrl)
                                        mediaPlayer.prepare()
                                        mediaPlayer.start()
                                        isPlaying = true
                                    }
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(
                                        context,
                                        "Error al reproducir audio",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Check else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isPlaying) "Pausar Audio" else "Reproducir Audio",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

