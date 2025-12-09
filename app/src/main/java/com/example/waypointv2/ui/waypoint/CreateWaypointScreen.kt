package com.example.waypointv2.ui.waypoint

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import java.io.File

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CreateWaypointScreen(
    onBackClick: () -> Unit,
    viewModel: CreateWaypointViewModel = viewModel()
) {
    val context = LocalContext.current
    val captureState by viewModel.captureState.collectAsState()
    val photoUri by viewModel.photoUri.collectAsState()
    val recordingDuration by viewModel.recordingDuration.collectAsState()
    
    // Permisos necesarios
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    )
    
    // Launcher para la cámara
    val photoFile = remember {
        File(context.cacheDir, "photo_${System.currentTimeMillis()}.jpg")
    }
    
    val photoUriForCamera = remember {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )
    }
    
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            viewModel.onPhotoTaken(photoUriForCamera)
        } else {
            viewModel.onPhotoCancelled()
            onBackClick()
        }
    }
    
    
    // Efecto para iniciar el flujo cuando se abre la pantalla o cuando se conceden permisos
    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted) {
            viewModel.startCaptureFlow()
        }
    }
    
    // Efecto para lanzar la cámara cuando el estado es CapturingPhoto
    LaunchedEffect(captureState) {
        when (captureState) {
            is CaptureState.CapturingPhoto -> {
                if (permissionsState.allPermissionsGranted) {
                    cameraLauncher.launch(photoUriForCamera)
                }
            }
            is CaptureState.Success -> {
                onBackClick()
            }
            else -> {}
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Solicitar permisos si no están concedidos
        if (!permissionsState.allPermissionsGranted) {
            PermissionRequestScreen(
                permissionsState = permissionsState,
                onBackClick = onBackClick
            )
        } else {
            // Mostrar UI según el estado
            when (val state = captureState) {
                is CaptureState.RecordingAudio -> {
                    AudioRecordingScreen(
                        photoUri = photoUri,
                        recordingDuration = recordingDuration,
                        onStartRecording = { viewModel.startAudioRecording() },
                        onStopRecording = { viewModel.stopAudioRecording() },
                        onCancel = {
                            viewModel.cancelCapture()
                            onBackClick()
                        }
                    )
                }
                is CaptureState.ProcessingLocation,
                is CaptureState.UploadingData -> {
                    ProcessingScreen(
                        message = when (state) {
                            is CaptureState.ProcessingLocation -> "Obteniendo ubicación..."
                            is CaptureState.UploadingData -> state.progress
                            else -> "Procesando..."
                        }
                    )
                }
                is CaptureState.Error -> {
                    ErrorDialog(
                        message = state.message,
                        onRetry = { viewModel.retryUpload() },
                        onDismiss = {
                            viewModel.cancelCapture()
                            onBackClick()
                        }
                    )
                }
                else -> {
                    // Idle o CapturingPhoto - no mostrar nada (la cámara se abre automáticamente)
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionRequestScreen(
    permissionsState: com.google.accompanist.permissions.MultiplePermissionsState,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Permisos Necesarios",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Para crear waypoints necesitamos acceso a:",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        PermissionItem("📷 Cámara", "Para tomar fotos")
        PermissionItem("🎤 Micrófono", "Para grabar notas de voz")
        PermissionItem("📍 Ubicación", "Para geolocalizar tus waypoints")
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = { permissionsState.launchMultiplePermissionRequest() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Conceder Permisos")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        TextButton(onClick = onBackClick) {
            Text("Cancelar")
        }
    }
}

@Composable
fun PermissionItem(icon: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = description, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun AudioRecordingScreen(
    photoUri: Uri?,
    recordingDuration: Long,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onCancel: () -> Unit
) {
    var isRecording by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Botón de cancelar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onCancel) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancelar",
                        tint = Color.White
                    )
                }
            }
            
            // Preview de la foto
            photoUri?.let { uri ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(uri),
                        contentDescription = "Foto capturada",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Indicador de grabación
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isRecording) {
                    RecordingIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = formatDuration(recordingDuration),
                        style = MaterialTheme.typography.displayMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "Graba una nota de voz",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Botón de grabar/detener
            FloatingActionButton(
                onClick = {
                    if (isRecording) {
                        isRecording = false
                        onStopRecording()
                    } else {
                        isRecording = true
                        onStartRecording()
                    }
                },
                modifier = Modifier.size(80.dp),
                containerColor = if (isRecording) Color.Red else MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Check else Icons.Default.PlayArrow,
                    contentDescription = if (isRecording) "Detener" else "Grabar",
                    modifier = Modifier.size(40.dp),
                    tint = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun RecordingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "recording")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(Color.Red.copy(alpha = alpha))
    )
}

@Composable
fun ProcessingScreen(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )
        }
    }
}

@Composable
fun ErrorDialog(
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Error")
        },
        text = {
            Text(message)
        },
        confirmButton = {
            TextButton(onClick = onRetry) {
                Text("Reintentar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

fun formatDuration(seconds: Long): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", minutes, secs)
}
