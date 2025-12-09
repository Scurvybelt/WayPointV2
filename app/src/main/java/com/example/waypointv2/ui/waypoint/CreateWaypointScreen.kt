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
import androidx.compose.ui.unit.sp
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
                        title = viewModel.title.value,
                        onTitleChange = { viewModel.updateTitle(it) },
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
            .background(Color.White)
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Permisos Necesarios",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Light,
                fontSize = 32.sp,
                color = Color.Black
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Para crear waypoints necesitamos acceso a:",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                color = Color.Gray
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(40.dp))
        
        PermissionItem("📷", "Cámara", "Para tomar fotos")
        Spacer(modifier = Modifier.height(16.dp))
        PermissionItem("🎤", "Micrófono", "Para grabar notas de voz")
        Spacer(modifier = Modifier.height(16.dp))
        PermissionItem("📍", "Ubicación", "Para geolocalizar tus waypoints")
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = { permissionsState.launchMultiplePermissionRequest() },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                "Conceder Permisos",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(
            onClick = onBackClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Cancelar",
                color = Color.Black,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
fun PermissionItem(icon: String, title: String, description: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 32.sp
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                color = Color.Gray
            ),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AudioRecordingScreen(
    photoUri: Uri?,
    recordingDuration: Long,
    title: String,
    onTitleChange: (String) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onCancel: () -> Unit
) {
    var isRecording by remember { mutableStateOf(false) }
    var localTitle by remember { mutableStateOf(title) }
    
    // Sincronizar el título local con el del ViewModel
    LaunchedEffect(title) {
        localTitle = title
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Preview de la foto
            photoUri?.let { uri ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 0.dp
                    )
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(uri),
                        contentDescription = "Foto capturada",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Campo de título
            TextField(
                value = localTitle,
                onValueChange = { newValue ->
                    localTitle = newValue
                    onTitleChange(newValue)
                },
                placeholder = {
                    Text(
                        "Título de la nota",
                        color = Color.Gray.copy(alpha = 0.7f)
                    )
                },
                label = { 
                    Text(
                        "Título de la nota",
                        color = Color.Gray
                    ) 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = Color.Gray,
                    unfocusedLabelColor = Color.Gray.copy(alpha = 0.7f),
                    focusedIndicatorColor = Color.White,
                    unfocusedIndicatorColor = Color.Gray.copy(alpha = 0.5f),
                    cursorColor = Color.White,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledTextColor = Color.Gray,
                    disabledLabelColor = Color.Gray.copy(alpha = 0.5f),
                    disabledIndicatorColor = Color.Gray.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(0.dp),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Indicador de grabación
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isRecording) {
                    RecordingIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = formatDuration(recordingDuration),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Light,
                            color = Color.White
                        )
                    )
                } else {
                    Text(
                        text = "Graba una nota de voz",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Light,
                            color = Color.White
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
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
                containerColor = if (isRecording) Color.Red else Color.Black,
                contentColor = Color.White
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
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                color = Color.Black,
                strokeWidth = 3.dp
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.Black
                ),
                textAlign = TextAlign.Center
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
        containerColor = Color.White,
        title = {
            Text(
                "Error",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 20.sp,
                    color = Color.Black
                )
            )
        },
        text = {
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    color = Color.Gray
                )
            )
        },
        confirmButton = {
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    "Reintentar",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Cancelar",
                    color = Color.Black,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    )
}

fun formatDuration(seconds: Long): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", minutes, secs)
}
